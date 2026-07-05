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
