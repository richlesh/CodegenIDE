import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

public class CodegenApp {
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static boolean splashShown = false;

    // Path to the Codegen lib directory
    private static final String CODEGEN_CLASSPATH = System.getProperty("codegen.classpath",
        resolveLibPath());

    private static String resolveLibPath() {
        // Try to find lib/ relative to the jar/class location
        try {
            java.net.URL url = CodegenApp.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File codeLoc = new java.io.File(url.toURI());
            java.io.File baseDir;
            if (codeLoc.isDirectory()) {
                // Running from target/classes — go up to project root
                baseDir = codeLoc.getParentFile().getParentFile();
            } else {
                // Running from a jar — lib/ is next to the jar
                baseDir = codeLoc.getParentFile();
            }
            java.io.File libDir = new java.io.File(baseDir, "lib");
            if (libDir.isDirectory()) return libDir.getAbsolutePath() + "/*";
        } catch (Exception ignored) {}
        // Fallback: try current working directory
        java.io.File cwdLib = new java.io.File("lib");
        if (cwdLib.isDirectory()) return cwdLib.getAbsolutePath() + "/*";
        return "lib/*";
    }

    private JFrame frame;
    private RSyntaxTextArea codeEditor;
    private EditorPanel editorPanel;
    private TerminalPanel console;
    private JTextField argsField;
    private Path currentFile;
    private AppSettings settings;
    private boolean undoInProgress = false;
    private final java.util.Deque<String> undoStack = new java.util.ArrayDeque<>();
    private final java.util.Deque<String> redoStack = new java.util.ArrayDeque<>();
    private static final int MAX_UNDO = 200;
    private String lastSavedText = "";
    private boolean modified = false;
    private JMenuItem saveItem;
    private JMenuItem undoItem, redoItem;
    private FindReplaceDialog findReplaceDialog;
    private File lastDirectory;
    private JSplitPane mainSplit;
    private JMenuBar menuBar;
    private JToolBar consoleToolBar;
    private volatile Thread runThread;
    private volatile Process currentProcess;
    private JMenuItem stopItem;
    private AIChatPanel aiChatPanel;
    private JSplitPane aiSplit;
    private PreviewPanel previewPanel;
    private JSplitPane editorPreviewSplit;

    public static void main(String[] args) {
        if (!IS_WINDOWS && !IS_MAC) {
            checkXWayland();
        }
        if (IS_MAC) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "CodegenIDE");
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new CodegenApp().createAndShowGUI());
    }

    void createAndShowGUI() {
        settings = AppSettings.load();
        frame = new JFrame("CodegenIDE");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (promptSaveIfNeeded()) {
                    saveWindowState();
                    frame.dispose();
                    exitIfLastWindow();
                }
            }
        });

        frame.setJMenuBar(createMenuBar());
        frame.add(createMainPanel(), BorderLayout.CENTER);

        applySettings();

        // Restore window size
        frame.setSize(settings.windowWidth, settings.windowHeight);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Restore divider positions after frame is visible and laid out
        SwingUtilities.invokeLater(() -> {
            if (settings.mainDivider > 0) mainSplit.setDividerLocation(settings.mainDivider);
            if (settings.editorPreviewDivider > 0) editorPreviewSplit.setDividerLocation(settings.editorPreviewDivider);
            if (settings.aiVisible && settings.aiDivider > 0) aiSplit.setDividerLocation(settings.aiDivider);
            // Show splash screen on startup if not licensed
            if (!splashShown && !LicenseDialog.isLicensed(settings)) {
                splashShown = true;
                SplashScreen.show();
            }
        });
    }

    private void applySettings() {
        Font font = new Font(settings.fontName, Font.PLAIN, settings.fontSize);
        editorPanel.setEditorFont(font);
        console.setFont(settings.fontName, settings.fontSize);
        console.setColors(settings.consoleFg, settings.consoleBg);
        if (consoleToolBar != null) {
            for (Component c : consoleToolBar.getComponents()) c.setFont(font);
        }
        SyntaxScheme scheme = codeEditor.getSyntaxScheme();
        scheme.getStyle(Token.IDENTIFIER).foreground = settings.colors[0];
        scheme.getStyle(Token.RESERVED_WORD).foreground = settings.colors[1];
        scheme.getStyle(Token.RESERVED_WORD).font = font.deriveFont(Font.BOLD);
        scheme.getStyle(Token.FUNCTION).foreground = settings.colors[2];
        scheme.getStyle(Token.FUNCTION).font = font.deriveFont(Font.BOLD);
        scheme.getStyle(Token.COMMENT_EOL).foreground = settings.colors[3];
        scheme.getStyle(Token.COMMENT_MULTILINE).foreground = settings.colors[3];
        scheme.getStyle(Token.COMMENT_DOCUMENTATION).foreground = settings.colors[3];
        scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = settings.colors[4];
        scheme.getStyle(Token.LITERAL_CHAR).foreground = settings.colors[4];
        scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = settings.colors[5];
        scheme.getStyle(Token.LITERAL_NUMBER_HEXADECIMAL).foreground = settings.colors[5];
        scheme.getStyle(Token.RESERVED_WORD_2).foreground = settings.colors[6];
        scheme.getStyle(Token.VARIABLE).foreground = settings.colors[7];
        scheme.getStyle(Token.VARIABLE).font = font.deriveFont(Font.BOLD);
        scheme.getStyle(Token.DATA_TYPE).foreground = settings.colors[8];
        codeEditor.setSyntaxScheme(scheme);
        codeEditor.setForeground(settings.colors[0]);
        codeEditor.revalidate();
        codeEditor.repaint();
        if (aiChatPanel != null) aiChatPanel.updateFont();
        if (previewPanel != null) previewPanel.setEditorFont(font);
    }

    private JMenuBar createMenuBar() {
        menuBar = new JMenuBar();

        Runnable aboutAction = () -> AboutDialog.show(frame, settings);
        Runnable settingsAction = () -> {
            SettingsDialog.show(frame, codeEditor, console, settings);
            applySettings();
        };
        Runnable quitAction = () -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JFrame && w.isVisible()) {
                    w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
                    if (w.isVisible()) return;
                }
            }
            System.exit(0);
        };

        if (IS_MAC) {
            Desktop desktop = Desktop.getDesktop();
            desktop.setAboutHandler(e -> aboutAction.run());
            desktop.setPreferencesHandler(e -> settingsAction.run());
            desktop.setQuitHandler((e, response) -> {
                quitAction.run();
                response.cancelQuit();
            });
        } else {
            JMenu appMenu = new JMenu("CodegenIDE");
            JMenuItem aboutItem = new JMenuItem("About CodegenIDE");
            aboutItem.addActionListener(e -> aboutAction.run());
            appMenu.add(aboutItem);
            appMenu.addSeparator();
            JMenuItem settingsItem = new JMenuItem("Settings");
            settingsItem.addActionListener(e -> settingsAction.run());
            appMenu.add(settingsItem);
            appMenu.addSeparator();
            JMenuItem licenseItem = new JMenuItem("License Key...");
            licenseItem.addActionListener(e -> LicenseDialog.show(frame, settings));
            appMenu.add(licenseItem);
            appMenu.addSeparator();
            JMenuItem quitItem = new JMenuItem("Quit CodegenIDE");
            quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
            quitItem.addActionListener(e -> quitAction.run());
            appMenu.add(quitItem);
            menuBar.add(appMenu);
        }

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newItem.addActionListener(e -> SwingUtilities.invokeLater(() -> { saveWindowState(); new CodegenApp().createAndShowGUI(); }));
        JMenuItem openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openItem.addActionListener(e -> openFile());
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        closeItem.addActionListener(e -> { if (promptSaveIfNeeded()) { saveWindowState(); frame.dispose(); exitIfLastWindow(); } });
        saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.setEnabled(false);
        saveItem.addActionListener(e -> saveFile());
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveFileAs());
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(closeItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        if (IS_MAC) {
            fileMenu.addSeparator();
            JMenuItem licenseItem = new JMenuItem("License Key...");
            licenseItem.addActionListener(e -> LicenseDialog.show(frame, settings));
            fileMenu.add(licenseItem);
        }

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoItem.addActionListener(e -> performUndo());
        undoItem.setEnabled(false);
        redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(e -> performRedo());
        JMenuItem cutItem = new JMenuItem("Cut");
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        cutItem.addActionListener(e -> codeEditor.cut());
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        copyItem.addActionListener(e -> {
            if (codeEditor.getSelectedText() != null) codeEditor.copy();
            else {
                String sel = console.getSelectedText();
                if (sel != null && !sel.isEmpty()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(sel), null);
                }
            }
        });
        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        pasteItem.addActionListener(e -> codeEditor.paste());
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        JMenuItem findItem = new JMenuItem("Find...");
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        findItem.addActionListener(e -> {
            if (findReplaceDialog == null) findReplaceDialog = new FindReplaceDialog(frame, codeEditor);
            findReplaceDialog.show();
        });
        editMenu.add(findItem);
        editMenu.addSeparator();
        JMenuItem shiftLeftItem = new JMenuItem("Shift Selection Left");
        shiftLeftItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        shiftLeftItem.addActionListener(e -> shiftIndent(false));
        editMenu.add(shiftLeftItem);
        JMenuItem shiftRightItem = new JMenuItem("Shift Selection Right");
        shiftRightItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        shiftRightItem.addActionListener(e -> shiftIndent(true));
        editMenu.add(shiftRightItem);

        // Generate menu
        JMenu generateMenu = new JMenu("Generate");
        JMenuItem genCpp = new JMenuItem("C++");
        genCpp.addActionListener(e -> generateLanguage("cpp"));
        JMenuItem genJava = new JMenuItem("Java");
        genJava.addActionListener(e -> generateLanguage("java"));
        JMenuItem genJs = new JMenuItem("Javascript");
        genJs.addActionListener(e -> generateLanguage("js"));
        JMenuItem genPerl = new JMenuItem("Perl");
        genPerl.addActionListener(e -> generateLanguage("pl"));
        JMenuItem genPython = new JMenuItem("Python");
        genPython.addActionListener(e -> generateLanguage("py"));
        JMenuItem genSwift = new JMenuItem("Swift");
        genSwift.addActionListener(e -> generateLanguage("swift"));
        JMenuItem genRust = new JMenuItem("Rust");
        genRust.addActionListener(e -> generateLanguage("rs"));
        generateMenu.add(genCpp);
        generateMenu.add(genJava);
        generateMenu.add(genJs);
        generateMenu.add(genPerl);
        generateMenu.add(genPython);
        generateMenu.add(genRust);
        generateMenu.add(genSwift);
        generateMenu.addSeparator();
        JMenuItem genAll = new JMenuItem("Generate All");
        genAll.addActionListener(e -> generateAll());
        generateMenu.add(genAll);

        // Run menu
        JMenu runMenu = new JMenu("Run");
        JMenuItem runCpp = new JMenuItem("C++");
        runCpp.addActionListener(e -> runLanguage("cpp"));
        JMenuItem runJava = new JMenuItem("Java");
        runJava.addActionListener(e -> runLanguage("java"));
        JMenuItem runJs = new JMenuItem("Javascript");
        runJs.addActionListener(e -> runLanguage("js"));
        JMenuItem runPerl = new JMenuItem("Perl");
        runPerl.addActionListener(e -> runLanguage("pl"));
        JMenuItem runPython = new JMenuItem("Python");
        runPython.addActionListener(e -> runLanguage("py"));
        JMenuItem runSwift = new JMenuItem("Swift");
        runSwift.addActionListener(e -> runLanguage("swift"));
        JMenuItem runRust = new JMenuItem("Rust");
        runRust.addActionListener(e -> runLanguage("rs"));
        runMenu.add(runCpp);
        runMenu.add(runJava);
        runMenu.add(runJs);
        runMenu.add(runPerl);
        runMenu.add(runPython);
        runMenu.add(runRust);
        runMenu.add(runSwift);
        runMenu.addSeparator();
        stopItem = new JMenuItem("Stop");
        stopItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        stopItem.setEnabled(false);
        stopItem.addActionListener(e -> stopRunning());
        runMenu.add(stopItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem onlineDocsItem = new JMenuItem("Online Documentation");
        onlineDocsItem.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new java.net.URI("https://glowingcat.com/")); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
        helpMenu.add(onlineDocsItem);
        helpMenu.addSeparator();
        JCheckBoxMenuItem aiToggleItem = new JCheckBoxMenuItem("AI Assistant");
        aiToggleItem.setSelected(settings.aiVisible);
        aiToggleItem.addActionListener(e -> toggleAIPanel(aiToggleItem.isSelected()));
        helpMenu.add(aiToggleItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(generateMenu);
        menuBar.add(runMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JSplitPane createMainPanel() {
        editorPanel = new EditorPanel();
        codeEditor = editorPanel.getTextArea();
        codeEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        // Undo/redo key bindings
        int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        codeEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), "safe-undo");
        codeEditor.getActionMap().put("safe-undo", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { performUndo(); }
        });
        codeEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | KeyEvent.SHIFT_DOWN_MASK), "safe-redo");
        codeEditor.getActionMap().put("safe-redo", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { performRedo(); }
        });

        // Shift indent key bindings
        codeEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, mod), "shift-left");
        codeEditor.getActionMap().put("shift-left", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { shiftIndent(false); }
        });
        codeEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, mod), "shift-right");
        codeEditor.getActionMap().put("shift-right", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { shiftIndent(true); }
        });

        codeEditor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { markModified(); recordUndo(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { markModified(); recordUndo(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
            private void recordUndo() {
                if (undoInProgress) return;
                String current = codeEditor.getText();
                if (current.equals(lastSavedText)) return;
                undoStack.push(lastSavedText);
                if (undoStack.size() > MAX_UNDO) ((java.util.ArrayDeque<String>)undoStack).removeLast();
                redoStack.clear();
                lastSavedText = current;
                updateUndoRedo();
            }
        });

        console = new TerminalPanel(settings.fontName, settings.fontSize);
        console.setColors(settings.consoleFg, settings.consoleBg);
        JScrollBar consoleScrollBar = new JScrollBar(JScrollBar.VERTICAL);
        console.attachScrollBar(consoleScrollBar);

        JPanel consoleScrollPanel = new JPanel(new BorderLayout());
        consoleScrollPanel.add(console, BorderLayout.CENTER);
        consoleScrollPanel.add(consoleScrollBar, BorderLayout.EAST);

        consoleToolBar = new JToolBar();
        consoleToolBar.setFloatable(false);
        consoleToolBar.add(new JLabel("Args: "));
        argsField = new JTextField(20);
        argsField.setMaximumSize(new Dimension(300, 24));
        consoleToolBar.add(argsField);
        Font toolbarFont = new Font(settings.fontName, Font.PLAIN, settings.fontSize);
        for (Component c : consoleToolBar.getComponents()) c.setFont(toolbarFont);

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(consoleToolBar, BorderLayout.NORTH);
        consolePanel.add(consoleScrollPanel, BorderLayout.CENTER);

        // Preview panel (right of editor, above console)
        previewPanel = new PreviewPanel();
        previewPanel.synchronizeScrolling(editorPanel.getScrollPane());
        editorPanel.setMinimumSize(new Dimension(0, 0));
        editorPreviewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, previewPanel);
        editorPreviewSplit.setResizeWeight(0.5);
        editorPreviewSplit.setContinuousLayout(true);

        mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPreviewSplit, consolePanel);
        mainSplit.setResizeWeight(0.7);

        // AI Chat panel (right side)
        aiChatPanel = new AIChatPanel(codeEditor, console, settings);
        aiSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainSplit, aiChatPanel);
        aiSplit.setResizeWeight(1.0);
        if (!settings.aiVisible) {
            aiChatPanel.setVisible(false);
            aiSplit.setDividerSize(0);
        } else {
            aiSplit.setDividerLocation(aiSplit.getPreferredSize().width - 380);
        }
        return aiSplit;
    }

    private void openFile() {
        if (!promptSaveIfNeeded()) return;
        FileDialog fd = new FileDialog(frame, "Open Codegen Source", FileDialog.LOAD);
        if (lastDirectory != null) fd.setDirectory(lastDirectory.getAbsolutePath());
        else fd.setDirectory(System.getProperty("user.dir"));
        fd.setFilenameFilter((dir, name) -> name.endsWith(".src"));
        fd.setVisible(true);
        if (fd.getFile() != null) {
            lastDirectory = new File(fd.getDirectory());
            loadFile(Path.of(fd.getDirectory(), fd.getFile()));
        }
    }

    void loadFile(Path path) {
        console.clear();
        try {
            String content = Files.readString(path);
            if (content.indexOf('\t') >= 0) {
                StringBuilder sb = new StringBuilder();
                int col = 0;
                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '\t') {
                        int spaces = 4 - (col % 4);
                        sb.append(" ".repeat(spaces));
                        col += spaces;
                    } else if (c == '\n') {
                        sb.append(c);
                        col = 0;
                    } else {
                        sb.append(c);
                        col++;
                    }
                }
                content = sb.toString();
            }
            undoInProgress = true;
            codeEditor.setText(content);
            codeEditor.setCaretPosition(0);
            undoInProgress = false;
            currentFile = path;
            frame.setTitle("CodegenIDE - " + path.getFileName());
            undoStack.clear();
            redoStack.clear();
            lastSavedText = codeEditor.getText();
            modified = false;
            saveItem.setEnabled(false);
            updateUndoRedo();
            previewPanel.setSourceFile(currentFile);
        } catch (IOException e) {
            appendConsole("Error opening file: " + e.getMessage() + "\n");
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }
        try {
            Files.writeString(currentFile, codeEditor.getText());
            modified = false;
            saveItem.setEnabled(false);
        } catch (IOException e) {
            appendConsole("Error saving file: " + e.getMessage() + "\n");
        }
    }

    private void saveFileAs() {
        FileDialog fd = new FileDialog(frame, "Save Codegen Source", FileDialog.SAVE);
        if (lastDirectory != null) fd.setDirectory(lastDirectory.getAbsolutePath());
        else fd.setDirectory(System.getProperty("user.dir"));
        if (currentFile != null) fd.setFile(currentFile.getFileName().toString());
        else fd.setFile("untitled.src");
        fd.setFilenameFilter((dir, name) -> name.endsWith(".src"));
        fd.setVisible(true);
        if (fd.getFile() != null) {
            lastDirectory = new File(fd.getDirectory());
            currentFile = Path.of(fd.getDirectory(), fd.getFile());
            if (!currentFile.toString().endsWith(".src")) {
                currentFile = Path.of(currentFile.toString() + ".src");
            }
            saveFile();
            frame.setTitle("CodegenIDE - " + currentFile.getFileName());
        }
    }

    // ===== GENERATE METHODS =====

    private void generateLanguage(String lang) {
        if (currentFile == null) {
            appendConsole("No file open. Open a .src file first.\n");
            return;
        }
        if (modified) saveFile();
        if (modified) return; // save was cancelled

        String visitorClass = getVisitorClass(lang);
        String srcPath = currentFile.toString();
        String bn = currentFile.getFileName().toString();
        if (bn.endsWith(".src")) bn = bn.substring(0, bn.length() - 4);
        final String baseName = bn;

        // Determine destination path based on language
        final String ext = getExtension(lang);
        Path destDir = currentFile.getParent().resolve("gen");
        try { Files.createDirectories(destDir); } catch (IOException ignored) {}
        String destPath = destDir.resolve(baseName + ext).toString();

        console.clear();
        appendConsole("> Generating " + lang.toUpperCase() + " from " + currentFile.getFileName() + "...\n");

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", CODEGEN_CLASSPATH,
                    visitorClass,
                    "--source=" + srcPath,
                    "--dest=" + destPath
                );
                pb.redirectErrorStream(true);
                pb.directory(currentFile.getParent().toFile());
                Process proc = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String l = line;
                        SwingUtilities.invokeLater(() -> appendConsole(l + "\n"));
                    }
                }
                int exitCode = proc.waitFor();
                SwingUtilities.invokeLater(() -> {
                    if (exitCode == 0) {
                        appendConsole("> Generation complete: " + baseName + ext + "\n");
                        previewPanel.refreshPreview();
                    } else {
                        appendConsole("> Generation failed (exit code " + exitCode + ")\n");
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> appendConsole("Error: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    private void generateAll() {
        if (currentFile == null) {
            appendConsole("No file open. Open a .src file first.\n");
            return;
        }
        if (modified) saveFile();
        if (modified) return;

        console.clear();
        appendConsole("> Generating all languages from " + currentFile.getFileName() + "...\n");

        new Thread(() -> {
            String[] langs = {"cpp", "java", "js", "pl", "py", "rs", "swift"};
            for (String lang : langs) {
                String visitorClass = getVisitorClass(lang);
                String srcPath = currentFile.toString();
                String baseName = currentFile.getFileName().toString();
                if (baseName.endsWith(".src")) baseName = baseName.substring(0, baseName.length() - 4);
                String ext = getExtension(lang);
                Path destDir = currentFile.getParent().resolve("gen");
                try { Files.createDirectories(destDir); } catch (IOException ignored) {}
                String destPath = destDir.resolve(baseName + ext).toString();

                final String langLabel = lang.toUpperCase();
                final String finalBaseName = baseName;
                final String finalExt = ext;
                SwingUtilities.invokeLater(() -> appendConsole("> Generating " + langLabel + "...\n"));

                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "java", "-cp", CODEGEN_CLASSPATH,
                        visitorClass,
                        "--source=" + srcPath,
                        "--dest=" + destPath
                    );
                    pb.redirectErrorStream(true);
                    pb.directory(currentFile.getParent().toFile());
                    Process proc = pb.start();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String l = line;
                            SwingUtilities.invokeLater(() -> appendConsole(l + "\n"));
                        }
                    }
                    int exitCode = proc.waitFor();
                    if (exitCode != 0) {
                        SwingUtilities.invokeLater(() -> appendConsole("> " + langLabel + " generation failed\n"));
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> appendConsole("Error: " + ex.getMessage() + "\n"));
                }
            }
            SwingUtilities.invokeLater(() -> {
                appendConsole("> All generation complete.\n");
                previewPanel.refreshPreview();
            });
        }).start();
    }

    // ===== RUN METHODS =====

    private void runLanguage(String lang) {
        if (currentFile == null) {
            appendConsole("No file open. Open a .src file first.\n");
            return;
        }
        if (modified) saveFile();
        if (modified) return;

        if (runThread != null) {
            stopRunning();
            return;
        }

        String baseName = currentFile.getFileName().toString();
        if (baseName.endsWith(".src")) baseName = baseName.substring(0, baseName.length() - 4);
        String ext = getExtension(lang);
        Path destDir = currentFile.getParent().resolve("gen");
        try { Files.createDirectories(destDir); } catch (IOException ignored) {}
        Path langSource = destDir.resolve(baseName + ext);

        // Check if language-specific source needs regeneration
        boolean needsRegen = !Files.exists(langSource);
        if (!needsRegen) {
            try {
                needsRegen = Files.getLastModifiedTime(currentFile).compareTo(
                    Files.getLastModifiedTime(langSource)) > 0;
            } catch (IOException ignored) { needsRegen = true; }
        }

        // Load the target language source into the preview pane
        previewPanel.setSelectedLanguage(lang);

        console.clear();
        final String fBaseName = baseName;
        final boolean fNeedsRegen = needsRegen;

        new Thread(() -> {
            runThread = Thread.currentThread();
            try {
                // Regenerate if needed
                if (fNeedsRegen) {
                    SwingUtilities.invokeLater(() -> appendConsole("> Regenerating " + lang.toUpperCase() + " source...\n"));
                    String visitorClass = getVisitorClass(lang);
                    String destPath = langSource.toString();
                    ProcessBuilder pb = new ProcessBuilder(
                        "java", "-cp", CODEGEN_CLASSPATH,
                        visitorClass,
                        "--source=" + currentFile.toString(),
                        "--dest=" + destPath
                    );
                    pb.redirectErrorStream(true);
                    pb.directory(currentFile.getParent().toFile());
                    Process proc = pb.start();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String l = line;
                            SwingUtilities.invokeLater(() -> appendConsole(l + "\n"));
                        }
                    }
                    int exitCode = proc.waitFor();
                    if (exitCode != 0) {
                        SwingUtilities.invokeLater(() -> appendConsole("> Generation failed. Cannot run.\n"));
                        return;
                    }
                }

                // Compile and run based on language
                compileAndRun(lang, fBaseName, destDir);

            } catch (InterruptedException ie) {
                SwingUtilities.invokeLater(() -> appendConsole("\n> Interrupted.\n"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> appendConsole("Error: " + ex.getMessage() + "\n"));
            } finally {
                currentProcess = null;
                runThread = null;
                SwingUtilities.invokeLater(() -> stopItem.setEnabled(false));
            }
        }).start();
    }

    private void compileAndRun(String lang, String baseName, Path dir) throws Exception {
        String userArgs = argsField.getText().trim();
        java.util.List<String> argsList = userArgs.isEmpty() ?
            java.util.List.of() : java.util.List.of(userArgs.split("\\s+"));

        switch (lang) {
            case "cpp" -> {
                // Copy Utils.hpp (overwrite if it already exists)
                Path utilsHpp = dir.resolve("Utils.hpp");
                try (InputStream hppIn = getClass().getResourceAsStream("/utils/Utils.hpp")) {
                    if (hppIn != null) {
                        Files.copy(hppIn, utilsHpp, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                SwingUtilities.invokeLater(() -> appendConsole("> Compiling C++...\n"));
                java.util.List<String> compileCmd = new java.util.ArrayList<>(java.util.List.of(
                    "g++", "-std=c++20", "-w", baseName + ".cpp", "-o", baseName,
                    "-I/opt/homebrew/include", "-L/opt/homebrew/lib", "-lfmt"));
                runProcess(compileCmd, dir);
                if (Files.exists(dir.resolve(baseName))) {
                    SwingUtilities.invokeLater(() -> appendConsole("> Running...\n"));
                    java.util.List<String> runCmd = new java.util.ArrayList<>();
                    runCmd.add("./" + baseName);
                    runCmd.addAll(argsList);
                    runProcess(runCmd, dir);
                }
            }
            case "java" -> {
                // Copy Utils.jar (overwrite if it already exists)
                Path utilsJarDest = dir.resolve("Utils.jar");
                try (InputStream jarIn = getClass().getResourceAsStream("/utils/Utils.jar")) {
                    if (jarIn != null) {
                        Files.copy(jarIn, utilsJarDest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                SwingUtilities.invokeLater(() -> appendConsole("> Compiling Java...\n"));
                String utilsJar = getUtilsJarPath();
                java.util.List<String> compileCmd = new java.util.ArrayList<>(java.util.List.of(
                    "javac", "-cp", ".:" + utilsJar, "-Xlint", baseName + ".java"));
                runProcess(compileCmd, dir);
                if (Files.exists(dir.resolve(baseName + ".class"))) {
                    SwingUtilities.invokeLater(() -> appendConsole("> Running...\n"));
                    java.util.List<String> runCmd = new java.util.ArrayList<>();
                    runCmd.add("java");
                    runCmd.add("-ea");
                    runCmd.add("-cp");
                    runCmd.add(".:" + utilsJar);
                    runCmd.add(baseName);
                    runCmd.addAll(argsList);
                    runProcess(runCmd, dir);
                }
            }
            case "js" -> {
                // Copy Utils.js (overwrite if it already exists)
                Path utilsJs = dir.resolve("Utils.js");
                try (InputStream jsIn = getClass().getResourceAsStream("/utils/Utils.js")) {
                    if (jsIn != null) {
                        Files.copy(jsIn, utilsJs, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                SwingUtilities.invokeLater(() -> appendConsole("> Running Javascript...\n"));
                java.util.List<String> runCmd = new java.util.ArrayList<>();
                runCmd.add("node");
                runCmd.add(baseName + ".js");
                runCmd.addAll(argsList);
                runProcess(runCmd, dir);
            }
            case "pl" -> {
                // Copy Utils.pm (overwrite if it already exists)
                Path utilsPm = dir.resolve("Utils.pm");
                try (InputStream pmIn = getClass().getResourceAsStream("/utils/Utils.pm")) {
                    if (pmIn != null) {
                        Files.copy(pmIn, utilsPm, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                SwingUtilities.invokeLater(() -> appendConsole("> Running Perl...\n"));
                java.util.List<String> runCmd = new java.util.ArrayList<>();
                runCmd.add("perl");
                runCmd.add(baseName + ".pl");
                runCmd.addAll(argsList);
                ProcessBuilder pb = new ProcessBuilder(runCmd);
                pb.directory(dir.toFile());
                pb.environment().put("PERL5LIB", ".");
                pb.redirectErrorStream(true);
                runProcessBuilder(pb);
            }
            case "py" -> {
                // Copy Utils.py (overwrite if it already exists)
                Path utilsPy = dir.resolve("Utils.py");
                try (InputStream pyIn = getClass().getResourceAsStream("/utils/Utils.py")) {
                    if (pyIn != null) {
                        Files.copy(pyIn, utilsPy, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                SwingUtilities.invokeLater(() -> appendConsole("> Running Python...\n"));
                java.util.List<String> runCmd = new java.util.ArrayList<>();
                runCmd.add("python3");
                runCmd.add(baseName + ".py");
                runCmd.addAll(argsList);
                runProcess(runCmd, dir);
            }
            case "swift" -> {
                 // Copy Utils.swift (overwrite if it already exists)
                Path utilsSwift = dir.resolve("Utils.swift");
                try (InputStream sdIn = getClass().getResourceAsStream("/utils/Utils.swift")) {
                    if (sdIn != null) {
                        Files.copy(sdIn, utilsSwift, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                SwingUtilities.invokeLater(() -> appendConsole("> Compiling Swift...\n"));
                java.util.List<String> compileCmd = new java.util.ArrayList<>(java.util.List.of(
                    "swiftc", baseName + ".swift", "Utils.swift"));
                runProcess(compileCmd, dir);
                if (Files.exists(dir.resolve(baseName))) {
                    SwingUtilities.invokeLater(() -> appendConsole("> Running...\n"));
                    java.util.List<String> runCmd = new java.util.ArrayList<>();
                    runCmd.add("./" + baseName);
                    runCmd.addAll(argsList);
                    ProcessBuilder pb = new ProcessBuilder(runCmd);
                    pb.directory(dir.toFile());
                    pb.environment().put("DYLD_LIBRARY_PATH", getSwiftUtilsPath());
                    pb.redirectErrorStream(true);
                    runProcessBuilder(pb);
                }
            }
            case "rs" -> {
                // Create rs_proj/src/bin/ directory structure if it doesn't exist
                Path rsProj = dir.resolve("rs_proj");
                Path rsSrcBin = rsProj.resolve("src").resolve("bin");
                if (!Files.exists(rsSrcBin)) {
                    Files.createDirectories(rsSrcBin);
                }

                // Create Cargo.toml if it doesn't exist
                Path cargoToml = rsProj.resolve("Cargo.toml");
                try (InputStream cargoIn = getClass().getResourceAsStream("/utils/Cargo.toml")) {
                    if (cargoIn != null) {
                        Files.copy(cargoIn, cargoToml, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                // Copy utils.rs if it doesn't exist
                Path utilsRs = rsSrcBin.resolve("utils.rs");
                try (InputStream utilsIn = getClass().getResourceAsStream("/utils/utils.rs")) {
                    if (utilsIn != null) {
                        Files.copy(utilsIn, utilsRs, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                // Copy the generated .rs source into rs_proj/src/bin/
                Path rsSource = dir.resolve(baseName + ".rs");
                Path rsDest = rsSrcBin.resolve(baseName + ".rs");
                Files.copy(rsSource, rsDest, StandardCopyOption.REPLACE_EXISTING);

                SwingUtilities.invokeLater(() -> appendConsole("> Compiling Rust...\n"));
                java.util.List<String> compileCmd = new java.util.ArrayList<>(java.util.List.of(
                    "cargo", "build", "--bin", baseName));
                runProcess(compileCmd, rsProj);

                Path binaryPath = rsProj.resolve("target").resolve("debug").resolve(baseName);
                if (Files.exists(binaryPath)) {
                    SwingUtilities.invokeLater(() -> appendConsole("> Running...\n"));
                    java.util.List<String> runCmd = new java.util.ArrayList<>();
                    runCmd.add(binaryPath.toString());
                    runCmd.addAll(argsList);
                    runProcess(runCmd, rsProj);
                }
            }
        }
        SwingUtilities.invokeLater(() -> appendConsole("> Done.\n"));
    }

    private void runProcess(java.util.List<String> cmd, Path dir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        runProcessBuilder(pb);
    }

    private void stopRunning() {
        Process proc = currentProcess;
        if (proc != null) {
            proc.destroyForcibly();
        }
        Thread t = runThread;
        if (t != null) {
            t.interrupt();
        }
    }

    private void runProcessBuilder(ProcessBuilder pb) throws Exception {
        Process proc = pb.start();
        currentProcess = proc;
        SwingUtilities.invokeLater(() -> stopItem.setEnabled(true));

        // Connect terminal input to process stdin
        PipedOutputStream inputPipe = new PipedOutputStream();
        PipedInputStream pipedIn = new PipedInputStream(inputPipe);
        console.enableInput(inputPipe);

        // Forward terminal input to process stdin
        OutputStream procStdin = proc.getOutputStream();
        Thread inputThread = new Thread(() -> {
            try {
                byte[] buf = new byte[256];
                int n;
                while ((n = pipedIn.read(buf)) != -1) {
                    procStdin.write(buf, 0, n);
                    procStdin.flush();
                }
            } catch (IOException ignored) {}
        });
        inputThread.setDaemon(true);
        inputThread.start();

        // Read output character-by-character so prompts without newlines display immediately
        try (InputStream is = proc.getInputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                String chunk = new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
                SwingUtilities.invokeLater(() -> console.write(chunk));
            }
        }
        proc.waitFor();

        // Clean up input
        console.disableInput();
        try { inputPipe.close(); } catch (IOException ignored) {}
        try { pipedIn.close(); } catch (IOException ignored) {}
        try { procStdin.close(); } catch (IOException ignored) {}

        currentProcess = null;
        SwingUtilities.invokeLater(() -> stopItem.setEnabled(false));
    }

    private String getVisitorClass(String lang) {
        return switch (lang) {
            case "cpp" -> "org.pureprogrammer.CodegenCPPVisitor";
            case "java" -> "org.pureprogrammer.CodegenJavaVisitor";
            case "js" -> "org.pureprogrammer.CodegenJavascriptVisitor";
            case "pl" -> "org.pureprogrammer.CodegenPerlVisitor";
            case "py" -> "org.pureprogrammer.CodegenPythonVisitor";
            case "rs" -> "org.pureprogrammer.CodegenRustVisitor";
            case "swift" -> "org.pureprogrammer.CodegenSwiftVisitor";
            default -> throw new IllegalArgumentException("Unknown language: " + lang);
        };
    }

    private String getExtension(String lang) {
        return switch (lang) {
            case "cpp" -> ".cpp";
            case "java" -> ".java";
            case "js" -> ".js";
            case "pl" -> ".pl";
            case "py" -> ".py";
            case "rs" -> ".rs";
            case "swift" -> ".swift";
            default -> "";
        };
    }

    private String getUtilsJarPath() {
        // Extract the lib directory from CODEGEN_CLASSPATH (strip the /*)
        String cp = CODEGEN_CLASSPATH;
        if (cp.endsWith("/*")) cp = cp.substring(0, cp.length() - 2);
        java.io.File utils = new java.io.File(cp, "Utils.jar");
        if (utils.exists()) return utils.getAbsolutePath();
        return "Utils.jar";
    }

    private String getSwiftUtilsPath() {
        // Look for Swift Utils (libUtils.dylib + Utils.swiftmodule) in utils/ sibling of lib/
        String cp = CODEGEN_CLASSPATH;
        if (cp.endsWith("/*")) cp = cp.substring(0, cp.length() - 2);
        java.io.File libDir = new java.io.File(cp);
        java.io.File utilsDir = new java.io.File(libDir.getParentFile(), "utils");
        if (utilsDir.isDirectory()) return utilsDir.getAbsolutePath();
        // Fallback: check if source directory has the files
        return ".";
    }

    // ===== EDITING UTILITIES =====

    private void markModified() {
        if (!undoInProgress && !modified) {
            modified = true;
            saveItem.setEnabled(true);
        }
        updateUndoRedo();
    }

    private void updateUndoRedo() {
        undoItem.setEnabled(!undoStack.isEmpty());
        redoItem.setEnabled(!redoStack.isEmpty());
    }

    private void performUndo() {
        if (undoStack.isEmpty()) return;
        undoInProgress = true;
        redoStack.push(lastSavedText);
        lastSavedText = undoStack.pop();
        int caret = codeEditor.getCaretPosition();
        codeEditor.setText(lastSavedText);
        codeEditor.setCaretPosition(Math.min(caret, lastSavedText.length()));
        undoInProgress = false;
        updateUndoRedo();
    }

    private void performRedo() {
        if (redoStack.isEmpty()) return;
        undoInProgress = true;
        undoStack.push(lastSavedText);
        lastSavedText = redoStack.pop();
        int caret = codeEditor.getCaretPosition();
        codeEditor.setText(lastSavedText);
        codeEditor.setCaretPosition(Math.min(caret, lastSavedText.length()));
        undoInProgress = false;
        updateUndoRedo();
    }

    private void shiftIndent(boolean right) {
        try {
            int selStart = codeEditor.getSelectionStart();
            int selEnd = codeEditor.getSelectionEnd();
            if (selStart == selEnd) {
                int line = codeEditor.getLineOfOffset(selStart);
                selStart = codeEditor.getLineStartOffset(line);
                selEnd = codeEditor.getLineEndOffset(line);
                if (selEnd > selStart && codeEditor.getText().charAt(selEnd - 1) == '\n') selEnd--;
            } else {
                int startLine = codeEditor.getLineOfOffset(selStart);
                int endLine = codeEditor.getLineOfOffset(selEnd - 1);
                selStart = codeEditor.getLineStartOffset(startLine);
                selEnd = codeEditor.getLineEndOffset(endLine);
                if (selEnd > selStart && codeEditor.getText().charAt(selEnd - 1) == '\n') selEnd--;
            }
            String text = codeEditor.getText().substring(selStart, selEnd);
            String[] lines = text.split("\n", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (right) {
                    sb.append("    ").append(lines[i]);
                } else {
                    String line = lines[i];
                    int remove = 0;
                    while (remove < 4 && remove < line.length() && line.charAt(remove) == ' ') remove++;
                    sb.append(line.substring(remove));
                }
                if (i < lines.length - 1) sb.append("\n");
            }
            codeEditor.select(selStart, selEnd);
            codeEditor.replaceSelection(sb.toString());
            codeEditor.select(selStart, selStart + sb.length());
        } catch (javax.swing.text.BadLocationException ignored) {}
    }

    private boolean promptSaveIfNeeded() {
        if (!modified) return true;
        int result = JOptionPane.showConfirmDialog(frame,
            "Do you want to save changes?", "CodegenIDE",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            saveFile();
            return !modified;
        }
        return result == JOptionPane.NO_OPTION;
    }

    private void exitIfLastWindow() {
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame && w.isDisplayable()) return;
        }
        System.exit(0);
    }

    private void saveWindowState() {
        settings.windowWidth = frame.getWidth();
        settings.windowHeight = frame.getHeight();
        settings.mainDivider = mainSplit.getDividerLocation();
        settings.editorPreviewDivider = editorPreviewSplit.getDividerLocation();
        if (aiChatPanel.isVisible()) {
            settings.aiDivider = aiSplit.getDividerLocation();
        }
        settings.save();
    }

    private void toggleAIPanel(boolean visible) {
        settings.aiVisible = visible;
        settings.save();
        aiChatPanel.setVisible(visible);
        if (visible) {
            aiSplit.setDividerSize(UIManager.getInt("SplitPane.dividerSize"));
            aiSplit.setDividerLocation(frame.getWidth() - 400);
        } else {
            aiSplit.setDividerSize(0);
        }
        aiSplit.revalidate();
    }

    private void appendConsole(String text) {
        console.write(text);
    }

    private static void checkXWayland() {
        String display = System.getenv("DISPLAY");
        if (display == null || display.isEmpty()) {
            System.err.println("Error: CodegenIDE requires an X11 display (DISPLAY not set).");
            System.err.println("If running Wayland, install XWayland: sudo apt install xwayland");
            System.exit(1);
        }
    }
}
