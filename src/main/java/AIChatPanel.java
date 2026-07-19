/*
 * (c) 2026 Richard Lesh
 */

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class AIChatPanel extends JPanel {
    private final JPanel chatPanel;
    private final JScrollPane chatScroll;
    private final JTextArea inputArea;
    private final JButton sendBtn;
    private final RSyntaxTextArea codeEditor;
    private final PreviewPanel previewPanel;
    private final TerminalPanel console;
    private final AppSettings settings;
    private final List<Map<String, String>> messages = new ArrayList<>();
    private String systemPrompt;
    private final ImageIcon humanIcon;
    private final ImageIcon aiIcon;
    private JLabel pulsingAiLabel;
    private javax.swing.Timer pulseTimer;
    private Runnable onCodeChanged;
    private Runnable statusUpdater;
    private volatile Thread currentThread;
    private int aiPromptCount = 0;
    private float pulseAlpha = 0f;

    public AIChatPanel(RSyntaxTextArea codeEditor, PreviewPanel previewPanel, TerminalPanel console, AppSettings settings) {
        super(new BorderLayout());
        this.codeEditor = codeEditor;
        this.previewPanel = previewPanel;
        this.console = console;
        this.settings = settings;
        this.systemPrompt = buildSystemPrompt();

        // Load icons
        var humanUrl = AIChatPanel.class.getResource("/human.png");
        var aiUrl = AIChatPanel.class.getResource("/AI.png");
        humanIcon = humanUrl != null ? new ImageIcon(new ImageIcon(humanUrl).getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH)) : null;
        aiIcon = aiUrl != null ? new ImageIcon(new ImageIcon(aiUrl).getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH)) : null;

        setPreferredSize(new Dimension(380, 0));
        setMinimumSize(new Dimension(200, 0));
        setBorder(BorderFactory.createTitledBorder("AI Assistant"));

        chatPanel = new JPanel() {
            @Override public Dimension getPreferredSize() {
                if (getParent() != null) {
                    int w = getParent().getWidth();
                    if (w > 0) {
                        Dimension d = super.getPreferredSize();
                        return new Dimension(w, d.height);
                    }
                }
                return super.getPreferredSize();
            }
        };
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(245, 245, 245));
        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        inputArea = new JTextArea(3, 20);
        inputArea.setFont(new Font(settings.aiFontName, Font.PLAIN, settings.aiFontSize));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) {
                    e.consume();
                    inputArea.insert("\n", inputArea.getCaretPosition());
                }
            }
        });
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sendBtn = new JButton("Send");
        JButton clearBtn = new JButton("Clear");
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        btnPanel.add(sendBtn);
        btnPanel.add(clearBtn);
        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(btnPanel, BorderLayout.EAST);

        JLabel statusBar = new JLabel(" ");
        statusBar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.statusUpdater = () -> {
            int sp = systemPrompt.length();
            int prog = codeEditor.getText().length();
            String pv = previewPanel.getTextArea().getText();
            int preview = (pv != null && !pv.startsWith("// No generated")) ? pv.length() : 0;
            int out = console.getText().length();
            statusBar.setText(String.format("<html>LLM System Prompt: %,d chars &nbsp;&nbsp; Current Program: %,d chars<br>Preview Program: %,d chars &nbsp;&nbsp; Current Output: %,d chars</html>", sp, prog, preview, out));
        };
        statusUpdater.run();

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputPanel, BorderLayout.CENTER);
        southPanel.add(statusBar, BorderLayout.SOUTH);

        add(chatScroll, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        clearBtn.addActionListener(e -> { messages.clear(); chatPanel.removeAll(); chatPanel.revalidate(); chatPanel.repaint(); });
    }

    public void setOnCodeChanged(Runnable callback) {
        this.onCodeChanged = callback;
    }

    public void updateFont() {
        Font font = new Font(settings.aiFontName, Font.PLAIN, settings.aiFontSize);
        inputArea.setFont(font);
        for (Component c : chatPanel.getComponents()) {
            updateFontRecursive(c, font);
        }
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    private void updateFontRecursive(Component c, Font font) {
        if (c instanceof JTextArea) c.setFont(font);
        if (c instanceof JTextPane) c.setFont(font);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) updateFontRecursive(child, font);
        }
    }

    private void sendMessage() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;
        inputArea.setText("");
        addUserBubble(text);
        statusUpdater.run();

        // Show splash screen every 10 prompts when not licensed
        aiPromptCount++;
        if (aiPromptCount % 10 == 0 && !LicenseDialog.isLicensed(settings)) {
            SplashScreen.show();
        }

        String previewCode = previewPanel.getTextArea().getText();
        String context = "Current source code:\n```\n" + codeEditor.getText() + "\n```\n\n"
            + (previewCode != null && !previewCode.isEmpty() && !previewCode.startsWith("// No generated")
                ? "Generated " + previewPanel.getSelectedLanguageName() + " code:\n```\n" + previewCode + "\n```\n\n" : "")
            + "Console output:\n```\n" + console.getText() + "\n```";

        if (messages.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", context + "\n\nUser request: " + text));

        sendBtn.setEnabled(false);
        startPulse();
        currentThread = new Thread(() -> {
            try {
                String response = callLLM();
                SwingUtilities.invokeLater(() -> {
                    stopPulse();
                    processResponse(response);
                    sendBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    stopPulse();
                    if (!Thread.currentThread().isInterrupted())
                        addAiBubble("Error: " + ex.getMessage());
                    sendBtn.setEnabled(true);
                });
            }
        });
        currentThread.start();
    }

    private void addUserBubble(String text) {
        Color uColor = settings.userPromptColor;
        JPanel bubble = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(uColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 12));

        JLabel icon = new JLabel(humanIcon);
        icon.setVerticalAlignment(SwingConstants.TOP);
        bubble.add(icon, BorderLayout.WEST);

        JTextArea msg = new JTextArea(text);
        msg.setFont(new Font(settings.aiFontName, Font.PLAIN, settings.aiFontSize));
        msg.setForeground(textColorForBackground(uColor));
        msg.setOpaque(false);
        msg.setEditable(false);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        bubble.add(msg, BorderLayout.CENTER);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        row.add(bubble, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        chatPanel.add(row);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private void addAiBubble(String text) {
        Color aiColor = settings.aiResponseColor;
        JPanel bubble = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(aiColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 12));

        JLabel icon = new JLabel(aiIcon);
        icon.setVerticalAlignment(SwingConstants.TOP);
        bubble.add(icon, BorderLayout.WEST);

        JTextPane msg = new JTextPane();
        msg.setOpaque(false);
        msg.setEditable(false);
        msg.setFont(new Font(settings.aiFontName, Font.PLAIN, settings.aiFontSize));
        msg.setForeground(textColorForBackground(aiColor));
        renderStyledMessage(msg, text, settings.aiFontName, settings.aiFontSize, settings.fontName);
        bubble.add(msg, BorderLayout.CENTER);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        row.add(bubble, BorderLayout.CENTER);

        chatPanel.add(row);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private void addCodeApprovalBubble(String explanation, String newCode) {
        if (!explanation.isEmpty()) {
            addAiBubble(explanation);
        }

        // Code summary with Show/Hide toggle
        int lineCount = newCode.split("\n", -1).length;
        JPanel codeRow = new JPanel(new BorderLayout());
        codeRow.setOpaque(false);
        codeRow.setBorder(BorderFactory.createEmptyBorder(2, 14, 4, 6));
        codeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel summaryLabel = new JLabel("\u2713 Code suggestion (" + lineCount + " lines)");
        summaryLabel.setFont(new Font(settings.aiFontName, Font.ITALIC, settings.aiFontSize));
        summaryLabel.setForeground(new Color(80, 80, 80));

        JButton toggleBtn = new JButton("Show");
        toggleBtn.setFont(new Font(settings.aiFontName, Font.PLAIN, settings.aiFontSize - 1));
        toggleBtn.setBorderPainted(true);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);
        headerPanel.add(summaryLabel);
        headerPanel.add(toggleBtn);
        codeRow.add(headerPanel, BorderLayout.NORTH);

        JTextArea codeArea = new JTextArea(newCode);
        codeArea.setFont(new Font(settings.fontName, Font.PLAIN, settings.aiFontSize));
        codeArea.setEditable(false);
        codeArea.setLineWrap(false);
        codeArea.setBackground(new Color(240, 240, 240));
        codeArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        codeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        codeScroll.setPreferredSize(new Dimension(0, Math.min(300, lineCount * (settings.aiFontSize + 4) + 16)));
        codeScroll.setVisible(false);
        codeRow.add(codeScroll, BorderLayout.CENTER);

        toggleBtn.addActionListener(e -> {
            boolean showing = codeScroll.isVisible();
            codeScroll.setVisible(!showing);
            toggleBtn.setText(showing ? "Show" : "Hide");
            chatPanel.revalidate();
            chatPanel.repaint();
            scrollToBottom();
        });

        chatPanel.add(codeRow);

        // Allow/Reject buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(2, 14, 6, 6));
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel prompt = new JLabel("Apply code changes?");
        prompt.setFont(new Font(settings.aiFontName, Font.BOLD, settings.aiFontSize));
        JButton allowBtn = new JButton("Allow");
        JButton rejectBtn = new JButton("Reject");
        allowBtn.addActionListener(e -> {
            codeEditor.setText(newCode);
            if (onCodeChanged != null) onCodeChanged.run();
            allowBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            prompt.setText("Code applied.");
        });
        rejectBtn.addActionListener(e -> {
            allowBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            prompt.setText("Changes rejected.");
        });
        JPanel btnStack = new JPanel(new GridLayout(2, 1, 0, 2));
        btnStack.setOpaque(false);
        btnStack.add(allowBtn);
        btnStack.add(rejectBtn);
        btnRow.add(prompt);
        btnRow.add(btnStack);

        chatPanel.add(btnRow);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private void startPulse() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        pulsingAiLabel = new JLabel(aiIcon) {
            @Override protected void paintComponent(Graphics g) {
                if (pulseAlpha > 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = getWidth() / 2, cy = getHeight() / 2, r = Math.max(getWidth(), getHeight()) / 2 + 4;
                    float[] dist = {0.3f, 1.0f};
                    Color[] colors = {new Color(50, 130, 255, (int)(pulseAlpha * 160)), new Color(50, 130, 255, 0)};
                    g2.setPaint(new RadialGradientPaint(cx, cy, r, dist, colors));
                    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        pulsingAiLabel.setVerticalAlignment(SwingConstants.TOP);
        row.add(pulsingAiLabel, BorderLayout.WEST);
        JLabel thinking = new JLabel("Thinking...");
        thinking.setFont(new Font(settings.aiFontName, Font.ITALIC, settings.aiFontSize));
        thinking.setForeground(Color.GRAY);
        row.add(thinking, BorderLayout.CENTER);
        JButton cancelBtn = new JButton("\u2715");
        cancelBtn.setForeground(Color.RED);
        cancelBtn.setFont(cancelBtn.getFont().deriveFont(Font.BOLD, 14f));
        cancelBtn.setBorderPainted(false);
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.setToolTipText("Cancel");
        cancelBtn.addActionListener(e -> {
            if (currentThread != null) currentThread.interrupt();
            stopPulse();
            sendBtn.setEnabled(true);
        });
        row.add(cancelBtn, BorderLayout.EAST);

        chatPanel.add(row);
        chatPanel.revalidate();
        scrollToBottom();

        pulseTimer = new javax.swing.Timer(80, new ActionListener() {
            boolean increasing = true;
            @Override public void actionPerformed(ActionEvent e) {
                if (increasing) { pulseAlpha += 0.08f; if (pulseAlpha >= 1f) { pulseAlpha = 1f; increasing = false; } }
                else { pulseAlpha -= 0.08f; if (pulseAlpha <= 0f) { pulseAlpha = 0f; increasing = true; } }
                if (pulsingAiLabel != null) pulsingAiLabel.repaint();
            }
        });
        pulseTimer.start();
    }

    private void stopPulse() {
        if (pulseTimer != null) { pulseTimer.stop(); pulseTimer = null; }
        pulseAlpha = 0f;
        if (pulsingAiLabel != null) pulsingAiLabel.repaint();
        int count = chatPanel.getComponentCount();
        if (count > 0) chatPanel.remove(count - 1);
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = chatScroll.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }

    private String callLLM() throws Exception {
        String vendor = settings.llmVendor;
        String apiKey = settings.llmApiKey;
        String model = settings.llmModel;

        String baseUrl = switch (vendor) {
            case "Alibaba" -> "https://dashscope-us.aliyuncs.com/compatible-mode/v1";
            case "Anthropic" -> "https://api.anthropic.com/v1";
            case "DeepSeek" -> "https://api.deepseek.com/v1";
            case "Generic" -> settings.llmEndpoint != null && !settings.llmEndpoint.isEmpty()
                ? (settings.llmEndpoint.endsWith("/") ? settings.llmEndpoint.substring(0, settings.llmEndpoint.length() - 1) : settings.llmEndpoint)
                : "https://api.openai.com/v1";
            case "Google" -> "https://generativelanguage.googleapis.com/v1beta/openai";
            case "Moonshot" -> "https://api.moonshot.ai/v1";
            case "Ollama" -> "http://localhost:11434/v1";
            case "OpenAI" -> "https://api.openai.com/v1";
            default -> "https://api.openai.com/v1";
        };

        if ("Anthropic".equals(vendor)) {
            return callAnthropic(apiKey, model);
        }

        StringBuilder body = new StringBuilder();
        body.append("{\"model\":\"").append(model).append("\",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) body.append(",");
            body.append("{\"role\":\"").append(messages.get(i).get("role"))
                .append("\",\"content\":").append(jsonString(messages.get(i).get("content"))).append("}");
        }
        body.append("]}");

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()));

        if (apiKey != null && !apiKey.isEmpty()) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> resp = HttpClient.newHttpClient()
            .send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

        String respBody = resp.body();
        String content = extractJsonValue(respBody, "content");
        if (content == null) throw new RuntimeException("Unexpected response: " + respBody.substring(0, Math.min(300, respBody.length())));
        messages.add(Map.of("role", "assistant", "content", content));
        return content;
    }

    private String callAnthropic(String apiKey, String model) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":\"").append(model).append("\",\"max_tokens\":128000,");
        String sys = messages.stream().filter(m -> "system".equals(m.get("role"))).map(m -> m.get("content")).findFirst().orElse("");
        body.append("\"system\":").append(jsonString(sys)).append(",\"messages\":[");
        boolean first = true;
        for (var m : messages) {
            if ("system".equals(m.get("role"))) continue;
            if (!first) body.append(",");
            body.append("{\"role\":\"").append(m.get("role"))
                .append("\",\"content\":").append(jsonString(m.get("content"))).append("}");
            first = false;
        }
        body.append("]}");

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        String respBody = resp.body();
        String content = extractJsonValue(respBody, "text");
        if (content == null) throw new RuntimeException("Unexpected response: " + respBody.substring(0, Math.min(300, respBody.length())));
        messages.add(Map.of("role", "assistant", "content", content));
        return content;
    }

    private void processResponse(String response) {
        int codeStart = response.indexOf("```codegen\n");
        if (codeStart < 0) codeStart = response.indexOf("```src\n");
        if (codeStart < 0) codeStart = response.indexOf("```\n");

        if (codeStart >= 0) {
            int blockStart = response.indexOf("\n", codeStart) + 1;
            int blockEnd = response.indexOf("\n```", blockStart);
            if (blockEnd > blockStart) {
                String newCode = response.substring(blockStart, blockEnd);
                String explanation = response.substring(0, codeStart).trim();
                if (blockEnd + 4 < response.length()) {
                    String after = response.substring(blockEnd + 4).trim();
                    if (!after.isEmpty()) explanation += (explanation.isEmpty() ? "" : "\n") + after;
                }
                addCodeApprovalBubble(explanation, newCode);
                return;
            }
        }
        addAiBubble(response);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI coding assistant embedded in CodegenIDE, a desktop IDE for the Codegen programming language. ");
        sb.append("You help users write, edit, debug, and improve Codegen source code.\n\n");
        sb.append("Your capabilities:\n");
        sb.append("- Help write new code (functions, algorithms, data structures)\n");
        sb.append("- Debug errors shown in the console output\n");
        sb.append("- Improve existing code (refactoring, optimization, bug fixes)\n");
        sb.append("- Explain code behavior and logic\n");
        sb.append("- Help with Codegen language syntax and features\n\n");
        sb.append("IMPORTANT RESPONSE FORMAT RULES:\n");
        sb.append("- When the user asks you to modify, add to, rewrite, or generate code for the document, ");
        sb.append("ALWAYS respond with the COMPLETE updated source wrapped in a ``` code block. ");
        sb.append("Include ALL existing content plus your changes. The user will be given Accept/Reject buttons.\n");
        sb.append("- For questions, explanations, or discussions that don't require code changes, ");
        sb.append("respond in plain text without a code block.\n\n");
        sb.append("You have access to the user's current source code and console output.\n\n");
        sb.append("=== Codegen Language Grammar ===\n");
        String codegenG4 = readResource("/Codegen.g4");
        if (codegenG4 != null) sb.append(codegenG4).append("\n\n");
        String macrodefG4 = readResource("/Macrodef.g4");
        if (macrodefG4 != null) sb.append("=== Macrodef Grammar ===\n").append(macrodefG4).append("\n");
        String funcs = readResource("/codegen_funcs.txt");
        if (funcs != null) {
            sb.append("=== Codegen Built-in Functions ===\n");
            for (String line : funcs.split("\n")) {
                if (line.startsWith("#") || (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)))) {
                    sb.append(line).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String readResource(String path) {
        try (var in = getClass().getResourceAsStream(path)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { return null; }
    }

    private static Color textColorForBackground(Color bg) {
        float rf = bg.getRed() / 255f, gf = bg.getGreen() / 255f, bf = bg.getBlue() / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float lightness = (max + min) / 2f;
        return lightness >= 0.65f ? Color.BLACK : Color.WHITE;
    }

    private static void renderStyledMessage(JTextPane pane, String text, String fontName, int fontSize, String codeFontName) {
        StyledDocument doc = pane.getStyledDocument();

        Style normal = doc.addStyle("normal", null);
        StyleConstants.setFontFamily(normal, fontName);
        StyleConstants.setFontSize(normal, fontSize);
        StyleConstants.setForeground(normal, pane.getForeground());

        Style bold = doc.addStyle("bold", normal);
        StyleConstants.setBold(bold, true);

        Style italic = doc.addStyle("italic", normal);
        StyleConstants.setItalic(italic, true);

        Style code = doc.addStyle("code", normal);
        StyleConstants.setFontFamily(code, codeFontName);

        Style header = doc.addStyle("header", normal);
        StyleConstants.setBold(header, true);
        StyleConstants.setFontSize(header, fontSize + 4);

        boolean inCodeBlock = false;
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            if (inCodeBlock) {
                insertText(doc, line + "\n", code);
                continue;
            }
            // Table lines
            if (line.startsWith("|") || line.matches("^\\|?[\\s:|-]+\\|?$")) {
                insertText(doc, line + "\n", code);
                continue;
            }
            // Headers
            if (line.startsWith("### ")) { insertText(doc, line.substring(4) + "\n", header); continue; }
            if (line.startsWith("## ")) { insertText(doc, line.substring(3) + "\n", header); continue; }
            if (line.startsWith("# ")) { insertText(doc, line.substring(2) + "\n", header); continue; }
            // List items
            String content = line;
            if (line.startsWith("- ") || line.startsWith("* ")) content = "\u2022 " + line.substring(2);
            // Render inline formatting
            renderInline(doc, content, normal, bold, italic, code);
            insertText(doc, "\n", normal);
        }
    }

    private static void renderInline(StyledDocument doc, String text, Style normal, Style bold, Style italic, Style code) {
        int i = 0;
        while (i < text.length()) {
            // Inline code: `...`
            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i) {
                    insertText(doc, text.substring(i + 1, end), code);
                    i = end + 1;
                    continue;
                }
            }
            // Bold: **...**
            if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                int end = text.indexOf("**", i + 2);
                if (end > i) {
                    insertText(doc, text.substring(i + 2, end), bold);
                    i = end + 2;
                    continue;
                }
            }
            // Italic: *...*
            if (text.charAt(i) == '*') {
                int end = text.indexOf('*', i + 1);
                if (end > i && !(i + 1 < text.length() && text.charAt(i + 1) == '*')) {
                    insertText(doc, text.substring(i + 1, end), italic);
                    i = end + 1;
                    continue;
                }
            }
            // Plain text
            int next = text.length();
            for (int j = i + 1; j < text.length(); j++) {
                char c = text.charAt(j);
                if (c == '`' || c == '*') { next = j; break; }
            }
            insertText(doc, text.substring(i, next), normal);
            i = next;
        }
    }

    private static void insertText(StyledDocument doc, String text, Style style) {
        try { doc.insertString(doc.getLength(), text, style); } catch (BadLocationException ignored) {}
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.lastIndexOf(pattern);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) return null;
        int i = colonIdx + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'u':
                        if (i + 5 < json.length()) {
                            sb.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                            i += 4;
                        }
                        break;
                    default: sb.append('\\').append(next); break;
                }
                i += 2;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
