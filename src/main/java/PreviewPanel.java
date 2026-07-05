import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

/**
 * Preview pane that shows generated source code for a selected target language.
 * Non-editable, with line numbers and a language selector combo box.
 */
public class PreviewPanel extends JPanel {

    private static final String SWIFT_SYNTAX = "text/swift";
    private static final String[] LANGUAGES = {"C++", "Java", "Javascript", "Perl", "Python", "Rust", "Swift"};
    private static final String[] EXTENSIONS = {".cpp", ".java", ".js", ".pl", ".py", ".rs", ".swift"};
    private static final String[] SYNTAX_STYLES = {
        SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS,
        SyntaxConstants.SYNTAX_STYLE_JAVA,
        SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT,
        SyntaxConstants.SYNTAX_STYLE_PERL,
        SyntaxConstants.SYNTAX_STYLE_PYTHON,
        SyntaxConstants.SYNTAX_STYLE_RUST,
        SWIFT_SYNTAX
    };

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final JComboBox<String> langCombo;
    private Path currentSourceFile;

    public PreviewPanel() {
        super(new BorderLayout());

        // Register Swift syntax style
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping(SWIFT_SYNTAX, "SwiftTokenMaker");

        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        textArea.setCodeFoldingEnabled(false);
        textArea.setAntiAliasingEnabled(true);
        textArea.setTabSize(4);
        textArea.setEditable(false);
        textArea.setHighlightCurrentLine(false);
        textArea.setMarkOccurrences(false);
        textArea.setPaintTabLines(false);
        textArea.setAnimateBracketMatching(false);
        textArea.setBracketMatchingEnabled(false);

        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setIconRowHeaderEnabled(false);
        scrollPane.setFoldIndicatorEnabled(false);
        scrollPane.getGutter().setBookmarkingEnabled(false);

        // Language selector toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(new JLabel("Preview:"));
        langCombo = new JComboBox<>(LANGUAGES);
        langCombo.setSelectedIndex(0);
        langCombo.addActionListener(e -> refreshPreview());
        toolbar.add(langCombo);

        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setPreferredSize(new Dimension(400, 0));
        setMinimumSize(new Dimension(200, 0));
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public RTextScrollPane getScrollPane() {
        return scrollPane;
    }

    public void setEditorFont(Font font) {
        textArea.setFont(font);
        scrollPane.getGutter().setLineNumberFont(font);
    }

    /**
     * Set up synchronized scrolling between source editor and this preview pane.
     * Moving either scrollbar will move the other to the same relative position.
     */
    public void synchronizeScrolling(RTextScrollPane sourceScrollPane) {
        JScrollBar sourceVBar = sourceScrollPane.getVerticalScrollBar();
        JScrollBar previewVBar = scrollPane.getVerticalScrollBar();

        // Flag to prevent recursive scroll events
        final boolean[] syncing = {false};

        sourceVBar.addAdjustmentListener(e -> {
            if (syncing[0]) return;
            syncing[0] = true;
            int max = sourceVBar.getMaximum() - sourceVBar.getVisibleAmount();
            if (max > 0) {
                double ratio = (double) sourceVBar.getValue() / max;
                int previewMax = previewVBar.getMaximum() - previewVBar.getVisibleAmount();
                previewVBar.setValue((int) (ratio * previewMax));
            }
            syncing[0] = false;
        });

        previewVBar.addAdjustmentListener(e -> {
            if (syncing[0]) return;
            syncing[0] = true;
            int max = previewVBar.getMaximum() - previewVBar.getVisibleAmount();
            if (max > 0) {
                double ratio = (double) previewVBar.getValue() / max;
                int sourceMax = sourceVBar.getMaximum() - sourceVBar.getVisibleAmount();
                sourceVBar.setValue((int) (ratio * sourceMax));
            }
            syncing[0] = false;
        });
    }

    /**
     * Set the current source file path. Called when a file is opened or saved.
     */
    public void setSourceFile(Path sourceFile) {
        this.currentSourceFile = sourceFile;
        refreshPreview();
    }

    /**
     * Refresh the preview by loading the generated file for the selected language.
     */
    public void refreshPreview() {
        if (currentSourceFile == null) {
            textArea.setText("");
            return;
        }

        int idx = langCombo.getSelectedIndex();
        String ext = EXTENSIONS[idx];
        textArea.setSyntaxEditingStyle(SYNTAX_STYLES[idx]);

        // Determine the generated file path
        String baseName = currentSourceFile.getFileName().toString();
        if (baseName.endsWith(".src")) baseName = baseName.substring(0, baseName.length() - 4);
        Path generatedFile = currentSourceFile.getParent().resolve(baseName + ext);

        if (Files.exists(generatedFile)) {
            try {
                String content = Files.readString(generatedFile);
                textArea.setText(content);
                textArea.setCaretPosition(0);
            } catch (IOException e) {
                textArea.setText("// Error reading: " + e.getMessage());
            }
        } else {
            textArea.setText("// No generated file found: " + generatedFile.getFileName());
        }
    }
}
