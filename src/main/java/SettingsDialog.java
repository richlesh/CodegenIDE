import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class SettingsDialog {

    public static final String[] CATEGORY_NAMES = {
        "Normal", "Keywords", "Directives", "Comments",
        "Strings", "Numbers", "Built-ins", "Types", "Type Keywords"
    };

    public static void show(JFrame parent, RSyntaxTextArea editor, TerminalPanel console,
                            AppSettings settings) {
        JDialog dialog = new JDialog(parent, "Settings", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // === LEFT COLUMN ===
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));

        // --- Fonts group ---
        JPanel fontsPanel = new JPanel(new GridBagLayout());
        fontsPanel.setBorder(BorderFactory.createTitledBorder("Fonts"));
        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.insets = new Insets(4, 5, 4, 5);
        fgbc.anchor = GridBagConstraints.WEST;

        String[] monoFonts = getMonospacedFonts();
        String[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        int[] sizeValues = {9, 10, 11, 12, 13, 14, 16, 18, 20, 24, 28};
        String[] sizes = new String[sizeValues.length];
        for (int i = 0; i < sizeValues.length; i++) sizes[i] = sizeValues[i] + "pt";

        // Code Font
        fgbc.gridx = 0; fgbc.gridy = 0;
        fontsPanel.add(new JLabel("Code Font:"), fgbc);
        JComboBox<String> fontCombo = new JComboBox<>(monoFonts);
        fontCombo.setSelectedItem(editor.getFont().getFamily());
        fgbc.gridx = 1; fgbc.fill = GridBagConstraints.HORIZONTAL; fgbc.weightx = 1;
        fontsPanel.add(fontCombo, fgbc);

        // Code Size
        fgbc.gridx = 0; fgbc.gridy = 1; fgbc.fill = GridBagConstraints.NONE; fgbc.weightx = 0;
        fontsPanel.add(new JLabel("Code Size:"), fgbc);
        JComboBox<String> sizeCombo = new JComboBox<>(sizes);
        int currentSize = editor.getFont().getSize();
        for (int i = 0; i < sizeValues.length; i++) {
            if (sizeValues[i] == currentSize) { sizeCombo.setSelectedIndex(i); break; }
        }
        fgbc.gridx = 1; fgbc.fill = GridBagConstraints.HORIZONTAL; fgbc.weightx = 1;
        fontsPanel.add(sizeCombo, fgbc);

        // AI Font
        fgbc.gridx = 0; fgbc.gridy = 2; fgbc.fill = GridBagConstraints.NONE; fgbc.weightx = 0;
        fontsPanel.add(new JLabel("AI Font:"), fgbc);
        JComboBox<String> aiFontCombo = new JComboBox<>(allFonts);
        aiFontCombo.setSelectedItem(settings.aiFontName);
        fgbc.gridx = 1; fgbc.fill = GridBagConstraints.HORIZONTAL; fgbc.weightx = 1;
        fontsPanel.add(aiFontCombo, fgbc);

        // AI Size
        fgbc.gridx = 0; fgbc.gridy = 3; fgbc.fill = GridBagConstraints.NONE; fgbc.weightx = 0;
        fontsPanel.add(new JLabel("AI Size:"), fgbc);
        JComboBox<String> aiSizeCombo = new JComboBox<>(sizes);
        for (int i = 0; i < sizeValues.length; i++) {
            if (sizeValues[i] == settings.aiFontSize) { aiSizeCombo.setSelectedIndex(i); break; }
        }
        fgbc.gridx = 1; fgbc.fill = GridBagConstraints.HORIZONTAL; fgbc.weightx = 1;
        fontsPanel.add(aiSizeCombo, fgbc);

        fontsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftColumn.add(fontsPanel);
        leftColumn.add(Box.createVerticalStrut(10));

        // --- LLM Settings group ---
        JPanel llmPanel = new JPanel(new GridBagLayout());
        llmPanel.setBorder(BorderFactory.createTitledBorder("LLM Settings"));
        GridBagConstraints lgbc = new GridBagConstraints();
        lgbc.insets = new Insets(4, 5, 4, 5);
        lgbc.anchor = GridBagConstraints.WEST;

        String[][] vendorData = {
            {"Alibaba", "https://www.alibabacloud.com/help/en/model-studio/get-api-key", "https://dashscope-us.aliyuncs.com/compatible-mode/v1"},
            {"Anthropic", "https://console.anthropic.com/settings/keys", "https://api.anthropic.com/v1"},
            {"DeepSeek", "https://platform.deepseek.com/api_keys", "https://api.deepseek.com/v1"},
            {"Google", "https://aistudio.google.com/apikey", "https://generativelanguage.googleapis.com/v1beta/openai"},
            {"Ollama", "https://ollama.com", "http://localhost:11434/v1"},
            {"OpenAI", "https://platform.openai.com/api-keys", "https://api.openai.com/v1"},
        };

        lgbc.gridx = 0; lgbc.gridy = 0;
        llmPanel.add(new JLabel("Vendor:"), lgbc);
        String[] vendorNames = new String[vendorData.length];
        for (int i = 0; i < vendorData.length; i++) vendorNames[i] = vendorData[i][0];
        JComboBox<String> vendorCombo = new JComboBox<>(vendorNames);
        if (settings.llmVendor != null) vendorCombo.setSelectedItem(settings.llmVendor);
        lgbc.gridx = 1; lgbc.fill = GridBagConstraints.HORIZONTAL; lgbc.weightx = 1;
        llmPanel.add(vendorCombo, lgbc);

        lgbc.gridx = 0; lgbc.gridy = 1; lgbc.fill = GridBagConstraints.NONE; lgbc.weightx = 0;
        llmPanel.add(new JLabel("Model:"), lgbc);
        JComboBox<String> modelCombo = new JComboBox<>();
        modelCombo.setEditable(true);
        lgbc.gridx = 1; lgbc.fill = GridBagConstraints.HORIZONTAL; lgbc.weightx = 1;
        llmPanel.add(modelCombo, lgbc);

        lgbc.gridx = 0; lgbc.gridy = 2; lgbc.fill = GridBagConstraints.NONE; lgbc.weightx = 0;
        llmPanel.add(new JLabel("API Key:"), lgbc);
        JPasswordField apiKeyField = new JPasswordField(settings.llmApiKey != null ? settings.llmApiKey : "", 20);
        lgbc.gridx = 1; lgbc.fill = GridBagConstraints.HORIZONTAL; lgbc.weightx = 1;
        llmPanel.add(apiKeyField, lgbc);

        JLabel apiKeyLink = new JLabel("<html><a href=''>Get API key...</a></html>");
        apiKeyLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lgbc.gridx = 1; lgbc.gridy = 3;
        llmPanel.add(apiKeyLink, lgbc);

        // Fetch models from vendor API
        Runnable fetchModels = () -> {
            int vi = vendorCombo.getSelectedIndex();
            String apiKey = new String(apiKeyField.getPassword()).trim();
            String baseUrl = vendorData[vi][2];
            modelCombo.removeAllItems();
            if (apiKey.isEmpty() && !"Ollama".equals(vendorData[vi][0])) {
                if (settings.llmModel != null) modelCombo.addItem(settings.llmModel);
                return;
            }
            new Thread(() -> {
                try {
                    java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(baseUrl + "/models"))
                        .header("Content-Type", "application/json")
                        .GET();
                    if ("Anthropic".equals(vendorData[vi][0])) {
                        reqBuilder.header("x-api-key", apiKey);
                        reqBuilder.header("anthropic-version", "2023-06-01");
                    } else if (!apiKey.isEmpty()) {
                        reqBuilder.header("Authorization", "Bearer " + apiKey);
                    }
                    java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                        .send(reqBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
                    String body = resp.body();
                    java.util.List<String> models = new java.util.ArrayList<>();
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                    while (m.find()) models.add(m.group(1));
                    SwingUtilities.invokeLater(() -> {
                        modelCombo.removeAllItems();
                        for (String mod : models) modelCombo.addItem(mod);
                        if (settings.llmModel != null) modelCombo.setSelectedItem(settings.llmModel);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        if (settings.llmModel != null) modelCombo.addItem(settings.llmModel);
                    });
                }
            }).start();
        };
        fetchModels.run();
        vendorCombo.addActionListener(e -> fetchModels.run());
        apiKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private javax.swing.Timer debounce = new javax.swing.Timer(500, e -> fetchModels.run());
            { debounce.setRepeats(false); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
        });

        apiKeyLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try { Desktop.getDesktop().browse(java.net.URI.create(vendorData[vendorCombo.getSelectedIndex()][1])); }
                catch (Exception ignored) {}
            }
        });

        llmPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftColumn.add(llmPanel);
        leftColumn.add(Box.createVerticalGlue());

        // === RIGHT COLUMN ===
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));

        // --- Syntax Colors group ---
        JPanel colorsPanel = new JPanel(new GridBagLayout());
        colorsPanel.setBorder(BorderFactory.createTitledBorder("Syntax Colors"));
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.insets = new Insets(3, 5, 3, 5);
        cgbc.anchor = GridBagConstraints.WEST;

        Color[] colors = new Color[CATEGORY_NAMES.length];
        int colCount = 2;
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            int col = i % colCount;
            int row = i / colCount;
            cgbc.gridy = row;
            cgbc.gridx = col * 2;
            cgbc.fill = GridBagConstraints.NONE;
            cgbc.weightx = 0;
            colorsPanel.add(new JLabel(CATEGORY_NAMES[i] + ":"), cgbc);

            colors[i] = settings.colors[i];
            JPanel colorPanel = new JPanel(new BorderLayout(4, 0));
            JTextField field = new JTextField(String.format("#%02x%02x%02x",
                colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue()), 7);
            field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JPanel swatch = new JPanel();
            swatch.setBackground(colors[i]);
            swatch.setPreferredSize(new Dimension(24, 24));
            swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            colorPanel.add(field, BorderLayout.CENTER);
            colorPanel.add(swatch, BorderLayout.EAST);

            final int idx = i;
            field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                private void update() {
                    try {
                        Color c = Color.decode(field.getText().trim());
                        colors[idx] = c;
                        swatch.setBackground(c);
                    } catch (NumberFormatException ignored) {}
                }
            });
            swatch.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    Color c = JColorChooser.showDialog(dialog, "Choose Color", colors[idx]);
                    if (c != null) {
                        colors[idx] = c;
                        swatch.setBackground(c);
                        field.setText(String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
                    }
                }
            });

            cgbc.gridx = col * 2 + 1;
            cgbc.fill = GridBagConstraints.HORIZONTAL;
            cgbc.weightx = 0.5;
            colorsPanel.add(colorPanel, cgbc);
        }

        colorsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(colorsPanel);
        rightColumn.add(Box.createVerticalStrut(10));

        // --- Console Colors group ---
        JPanel consoleColorsPanel = new JPanel(new GridBagLayout());
        consoleColorsPanel.setBorder(BorderFactory.createTitledBorder("Console Colors"));
        GridBagConstraints cngbc = new GridBagConstraints();
        cngbc.insets = new Insets(4, 5, 4, 5);
        cngbc.anchor = GridBagConstraints.WEST;

        Color[] conFg = {settings.consoleFg};
        Color[] conBg = {settings.consoleBg};

        cngbc.gridx = 0; cngbc.gridy = 0;
        consoleColorsPanel.add(new JLabel("Foreground:"), cngbc);
        JPanel conFgSwatch = new JPanel();
        conFgSwatch.setBackground(conFg[0]);
        conFgSwatch.setPreferredSize(new Dimension(60, 24));
        conFgSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        conFgSwatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        conFgSwatch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color c = JColorChooser.showDialog(dialog, "Console Foreground", conFg[0]);
                if (c != null) { conFg[0] = c; conFgSwatch.setBackground(c); }
            }
        });
        cngbc.gridx = 1;
        consoleColorsPanel.add(conFgSwatch, cngbc);

        cngbc.gridx = 2;
        consoleColorsPanel.add(new JLabel("  Background:"), cngbc);
        JPanel conBgSwatch = new JPanel();
        conBgSwatch.setBackground(conBg[0]);
        conBgSwatch.setPreferredSize(new Dimension(60, 24));
        conBgSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        conBgSwatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        conBgSwatch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color c = JColorChooser.showDialog(dialog, "Console Background", conBg[0]);
                if (c != null) { conBg[0] = c; conBgSwatch.setBackground(c); }
            }
        });
        cngbc.gridx = 3;
        consoleColorsPanel.add(conBgSwatch, cngbc);

        consoleColorsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(consoleColorsPanel);
        rightColumn.add(Box.createVerticalStrut(10));

        // --- Chat Colors group ---
        JPanel chatColorsPanel = new JPanel(new GridBagLayout());
        chatColorsPanel.setBorder(BorderFactory.createTitledBorder("Chat Colors"));
        GridBagConstraints ccgbc = new GridBagConstraints();
        ccgbc.insets = new Insets(4, 5, 4, 5);
        ccgbc.anchor = GridBagConstraints.WEST;

        Color[] userColor = {settings.userPromptColor};
        Color[] aiColor = {settings.aiResponseColor};

        ccgbc.gridx = 0; ccgbc.gridy = 0;
        chatColorsPanel.add(new JLabel("User Prompt:"), ccgbc);
        JPanel userSwatch = new JPanel();
        userSwatch.setBackground(userColor[0]);
        userSwatch.setPreferredSize(new Dimension(60, 24));
        userSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        userSwatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userSwatch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color c = JColorChooser.showDialog(dialog, "User Prompt Color", userColor[0]);
                if (c != null) { userColor[0] = c; userSwatch.setBackground(c); }
            }
        });
        ccgbc.gridx = 1;
        chatColorsPanel.add(userSwatch, ccgbc);

        ccgbc.gridx = 2;
        chatColorsPanel.add(new JLabel("  AI Response:"), ccgbc);
        JPanel aiSwatch = new JPanel();
        aiSwatch.setBackground(aiColor[0]);
        aiSwatch.setPreferredSize(new Dimension(60, 24));
        aiSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        aiSwatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        aiSwatch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color c = JColorChooser.showDialog(dialog, "AI Response Color", aiColor[0]);
                if (c != null) { aiColor[0] = c; aiSwatch.setBackground(c); }
            }
        });
        ccgbc.gridx = 3;
        chatColorsPanel.add(aiSwatch, ccgbc);

        chatColorsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(chatColorsPanel);
        rightColumn.add(Box.createVerticalGlue());

        // === MAIN LAYOUT: two columns side by side + developer tools below ===
        JPanel columnsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints mgbc = new GridBagConstraints();
        mgbc.insets = new Insets(10, 10, 5, 5);
        mgbc.anchor = GridBagConstraints.NORTH;
        mgbc.fill = GridBagConstraints.BOTH;
        mgbc.weighty = 1;

        mgbc.gridx = 0; mgbc.gridy = 0; mgbc.weightx = 0.5;
        columnsPanel.add(leftColumn, mgbc);

        mgbc.gridx = 1; mgbc.insets = new Insets(10, 5, 5, 10); mgbc.weightx = 0.5;
        columnsPanel.add(rightColumn, mgbc);

        // --- Developer Tools group (spans both columns) ---
        JPanel devToolsPanel = new JPanel(new GridBagLayout());
        devToolsPanel.setBorder(BorderFactory.createTitledBorder("Developer Tools"));
        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.insets = new Insets(4, 5, 4, 5);
        dgbc.anchor = GridBagConstraints.WEST;

        String[][] toolDefs = {
            {"C++ (g++):", "g++", settings.cppPath},
            {"Java (javac):", "javac", settings.javaPath},
            {"JavaScript (node):", "node", settings.nodePath},
            {"Perl:", "perl", settings.perlPath},
            {"Python:", "python3", settings.pythonPath},
            {"Rust (rustc):", "rustc", settings.rustPath},
            {"Swift (swiftc):", "swiftc", settings.swiftPath},
        };

        JTextField[] toolFields = new JTextField[toolDefs.length];
        for (int i = 0; i < toolDefs.length; i++) {
            dgbc.gridy = i;

            // Label
            dgbc.gridx = 0; dgbc.fill = GridBagConstraints.NONE; dgbc.weightx = 0;
            devToolsPanel.add(new JLabel(toolDefs[i][0]), dgbc);

            // Text field - show configured path, or auto-detected path if empty
            String configuredPath = toolDefs[i][2];
            String detectedPath = AppSettings.detectToolPath(toolDefs[i][1]);
            String displayPath = (configuredPath != null && !configuredPath.isEmpty()) ? configuredPath : detectedPath;
            toolFields[i] = new JTextField(displayPath, 30);
            dgbc.gridx = 1; dgbc.fill = GridBagConstraints.HORIZONTAL; dgbc.weightx = 1;
            devToolsPanel.add(toolFields[i], dgbc);

            // Browse button
            JButton browseBtn = new JButton("Browse...");
            final int idx = i;
            browseBtn.addActionListener(ev -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                String currentVal = toolFields[idx].getText().trim();
                if (!currentVal.isEmpty()) {
                    fc.setCurrentDirectory(new java.io.File(currentVal).getParentFile());
                }
                if (fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                    toolFields[idx].setText(fc.getSelectedFile().getAbsolutePath());
                }
            });
            dgbc.gridx = 2; dgbc.fill = GridBagConstraints.NONE; dgbc.weightx = 0;
            devToolsPanel.add(browseBtn, dgbc);
        }

        // Note at bottom of developer tools
        dgbc.gridx = 0; dgbc.gridy = toolDefs.length; dgbc.gridwidth = 3;
        dgbc.fill = GridBagConstraints.NONE; dgbc.weightx = 0;
        dgbc.insets = new Insets(8, 5, 4, 5);
        JLabel noteLabel = new JLabel("Note: delete any path to auto-detect.");
        noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC));
        devToolsPanel.add(noteLabel, dgbc);

        mgbc.gridx = 0; mgbc.gridy = 1; mgbc.gridwidth = 2;
        mgbc.weighty = 0; mgbc.fill = GridBagConstraints.HORIZONTAL;
        mgbc.insets = new Insets(5, 10, 10, 10);
        columnsPanel.add(devToolsPanel, mgbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");
        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        cancelBtn.addActionListener(e -> dialog.dispose());
        okBtn.addActionListener(e -> {
            settings.fontName = (String) fontCombo.getSelectedItem();
            settings.fontSize = sizeValues[sizeCombo.getSelectedIndex()];
            settings.aiFontName = (String) aiFontCombo.getSelectedItem();
            settings.aiFontSize = sizeValues[aiSizeCombo.getSelectedIndex()];
            System.arraycopy(colors, 0, settings.colors, 0, colors.length);
            settings.llmVendor = (String) vendorCombo.getSelectedItem();
            settings.llmModel = (String) modelCombo.getSelectedItem();
            String key = new String(apiKeyField.getPassword()).trim();
            settings.llmApiKey = key.isEmpty() ? null : key;
            settings.userPromptColor = userColor[0];
            settings.aiResponseColor = aiColor[0];
            settings.consoleFg = conFg[0];
            settings.consoleBg = conBg[0];
            // Developer tool paths
            settings.cppPath = toolFields[0].getText().trim();
            settings.javaPath = toolFields[1].getText().trim();
            settings.nodePath = toolFields[2].getText().trim();
            settings.perlPath = toolFields[3].getText().trim();
            settings.pythonPath = toolFields[4].getText().trim();
            settings.rustPath = toolFields[5].getText().trim();
            settings.swiftPath = toolFields[6].getText().trim();
            settings.save();

            dialog.dispose();
        });

        dialog.add(new JScrollPane(columnsPanel), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static String[] getMonospacedFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] allFonts = ge.getAvailableFontFamilyNames();
        List<String> mono = new ArrayList<>();
        for (String name : allFonts) {
            Font font = new Font(name, Font.PLAIN, 12);
            if (!font.canDisplay('A') || !font.canDisplay('z') || !font.canDisplay('0')) continue;
            java.awt.FontMetrics fm = new Canvas().getFontMetrics(font);
            if (fm.charWidth('m') == fm.charWidth('i') && fm.charWidth('m') > 0) {
                mono.add(name);
            }
        }
        if (mono.isEmpty()) mono.add(Font.MONOSPACED);
        return mono.toArray(new String[0]);
    }
}
