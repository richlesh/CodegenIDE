import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;
import javax.swing.*;
import java.awt.*;

/**
 * Wraps RSyntaxTextArea in an RTextScrollPane with line numbers.
 * Uses CodegenTokenMaker for syntax highlighting.
 */
public class EditorPanel extends JPanel {

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final Gutter gutter;

    public EditorPanel() {
        super(new BorderLayout());

        // Disable code templates globally before creating the text area
        RSyntaxTextArea.setTemplatesEnabled(false);

        // Register custom syntax style for Codegen
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/codegen", "CodegenTokenMaker");

        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle("text/codegen");
        textArea.setCodeFoldingEnabled(false);
        textArea.setAntiAliasingEnabled(true);
        textArea.setTabSize(4);
        textArea.setTabsEmulated(true);
        textArea.setAutoIndentEnabled(false);
        textArea.setMarkOccurrences(false);
        textArea.setHighlightCurrentLine(false);
        textArea.setPaintTabLines(false);
        textArea.setAnimateBracketMatching(false);
        textArea.setBracketMatchingEnabled(false);
        textArea.setTemplatesEnabled(false);
        textArea.setCloseCurlyBraces(false);
        textArea.setCloseMarkupTags(false);
        textArea.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        textArea.putClientProperty("caretWidth", 2);
        // Disable macOS auto-correction/completion
        textArea.putClientProperty("JComponent.inputMethodSupported", Boolean.FALSE);
        textArea.enableInputMethods(false);

        // Disable word completion triggers that can duplicate text
        javax.swing.InputMap im = textArea.getInputMap();
        // Remove Ctrl+Enter (DumbCompleteWord) binding
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER,
            java.awt.event.InputEvent.CTRL_DOWN_MASK), "none");
        // Remove Shift+Ctrl+Space that might trigger completion
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK), "none");

        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setIconRowHeaderEnabled(false);
        scrollPane.setFoldIndicatorEnabled(false);

        gutter = scrollPane.getGutter();
        gutter.setBookmarkingEnabled(false);

        add(scrollPane, BorderLayout.CENTER);
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public RTextScrollPane getScrollPane() {
        return scrollPane;
    }

    public Gutter getGutter() {
        return gutter;
    }

    /**
     * Apply font settings to the text area and gutter.
     */
    public void setEditorFont(Font font) {
        textArea.setFont(font);
        gutter.setLineNumberFont(font);
    }
}
