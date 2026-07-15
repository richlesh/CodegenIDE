import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class CodegenSyntaxHighlighter {

    private static final Set<String> KEYWORDS = Set.of(
        "BeginProgram", "EndProgram", "BeginMain", "EndMain",
        "BeginFunction", "EndFunction", "BeginBlock", "EndBlock",
        "BeginIf", "ElseIf", "Else", "EndIf",
        "BeginEval", "ElseIfEval", "ElseEval", "EndEval",
        "BeginWhile", "EndWhile", "BeginDoWhile", "EndDoWhile",
        "BeginFor", "EndFor", "BeginForEach", "EndForEach",
        "BeginSwitch", "EndSwitch", "Case", "Default",
        "BeginTry", "EndTry", "Catch", "BeginFinally", "EndFinally",
        "BeginDefer", "EndDefer",
        "BeginClass", "EndClass", "BeginEnum", "EndEnum",
        "Return", "Break", "Continue",
        "Public", "Private", "Protected",
        "Throw", "CanThrow", "Assertion", "Invariant",
        "Precondition", "Postcondition", "Debug", "Delete"
    );

    private static final Set<String> DIRECTIVES = Set.of(
        "ScalarDecl", "ListDecl", "EndListDecl", "MapDecl", "EndMapDecl",
        "TupleDecl", "EndTupleDecl", "UnpackTuple", "UserTypeDecl",
        "Statement", "Expression", "Assign", "Print", "PrintLine",
        "Pragma", "Comment", "BeginComment", "EndComment"
    );

    private static final Set<String> TYPE_KEYWORDS = Set.of(
        "void", "auto", "async", "await", "static", "const", "final",
        "Const", "Final", "let", "inout", "try", "true", "false", "new",
        "in", "extends", "implements", "throws"
    );

    private static final Set<String> BUILTINS = loadBuiltins();

    private static Set<String> loadBuiltins() {
        Set<String> builtins = new HashSet<>(Set.of(
            "format", "string", "char", "byte", "short", "int", "long",
            "int16", "int32", "int64", "double",
            "isNull", "listsize", "mapsize", "mapkeys", "mapkeysAsList"
        ));
        java.util.regex.Pattern funcPattern = java.util.regex.Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\(");
        try (InputStream in = CodegenSyntaxHighlighter.class.getResourceAsStream("/codegen_funcs.txt")) {
            if (in == null) return builtins;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("\t")) continue;
                    java.util.regex.Matcher m = funcPattern.matcher(line);
                    if (m.find()) {
                        builtins.add(m.group(1));
                    }
                }
            }
        } catch (IOException ignored) {}
        return Collections.unmodifiableSet(builtins);
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(?<BLOCKCOMMENT>/#[\\s\\S]*?#/)" +
        "|(?<LINECOMMENT>Comment:.*)" +
        "|(?<INLINECOMMENT>/\\*[\\s\\S]*?\\*/)" +
        "|(?<STRING>(?:[fo])?\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")" +
        "|(?<BACKTICK>`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`)" +
        "|(?<CHAR>'[^'\\\\]'|'\\\\[^']')" +
        "|(?<HEX>0[xX][0-9A-Fa-f]+)" +
        "|(?<FLOAT>-?\\d+\\.\\d*(?:[eE][+-]?\\d+)?|-?\\.\\d+(?:[eE][+-]?\\d+)?|-?\\d+[eE][+-]?\\d+)" +
        "|(?<INT>-?\\d+L?)" +
        "|(?<TYPE><[a-zA-Z_][a-zA-Z0-9_*?!]*>)" +
        "|(?<WORD>[a-zA-Z_][a-zA-Z0-9_]*)"
    );

    private final Style normal;
    private final Style keyword;
    private final Style directive;
    private final Style comment;
    private final Style string;
    private final Style number;
    private final Style builtin;
    private final Style typeStyle;
    private final Style typeKeyword;

    private final JTextPane textPane;

    public static final String[] CATEGORY_NAMES = {
        "Normal", "Keywords", "Directives", "Comments",
        "Strings", "Numbers", "Built-ins", "Types", "Type Keywords"
    };

    public CodegenSyntaxHighlighter(JTextPane pane) {
        this.textPane = pane;
        StyledDocument doc = pane.getStyledDocument();

        normal = doc.addStyle("normal", null);
        StyleConstants.setForeground(normal, Color.BLACK);

        keyword = doc.addStyle("keyword", null);
        StyleConstants.setForeground(keyword, new Color(0, 0, 180));
        StyleConstants.setBold(keyword, true);

        directive = doc.addStyle("directive", null);
        StyleConstants.setForeground(directive, new Color(128, 0, 128));
        StyleConstants.setBold(directive, true);

        comment = doc.addStyle("comment", null);
        StyleConstants.setForeground(comment, new Color(0x99, 0x99, 0x99));
        StyleConstants.setItalic(comment, true);

        string = doc.addStyle("string", null);
        StyleConstants.setForeground(string, new Color(163, 21, 21));

        number = doc.addStyle("number", null);
        StyleConstants.setForeground(number, new Color(180, 100, 0));

        builtin = doc.addStyle("builtin", null);
        StyleConstants.setForeground(builtin, new Color(200, 0, 100));

        typeStyle = doc.addStyle("type", null);
        StyleConstants.setForeground(typeStyle, new Color(0, 100, 100));
        StyleConstants.setBold(typeStyle, true);

        typeKeyword = doc.addStyle("typeKeyword", null);
        StyleConstants.setForeground(typeKeyword, new Color(100, 0, 150));
    }

    public Color getColor(int index) {
        return StyleConstants.getForeground(getStyleByIndex(index));
    }

    public void setColor(int index, Color c) {
        StyleConstants.setForeground(getStyleByIndex(index), c);
    }

    private Style getStyleByIndex(int index) {
        return switch (index) {
            case 0 -> normal;
            case 1 -> keyword;
            case 2 -> directive;
            case 3 -> comment;
            case 4 -> string;
            case 5 -> number;
            case 6 -> builtin;
            case 7 -> typeStyle;
            case 8 -> typeKeyword;
            default -> normal;
        };
    }

    public void highlight() {
        StyledDocument doc = textPane.getStyledDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        doc.setCharacterAttributes(0, text.length(), normal, true);

        Matcher m = TOKEN_PATTERN.matcher(text);
        while (m.find()) {
            Style style = null;
            if (m.group("BLOCKCOMMENT") != null || m.group("LINECOMMENT") != null || m.group("INLINECOMMENT") != null) {
                style = comment;
            } else if (m.group("STRING") != null || m.group("BACKTICK") != null || m.group("CHAR") != null) {
                style = string;
            } else if (m.group("HEX") != null || m.group("FLOAT") != null || m.group("INT") != null) {
                style = number;
            } else if (m.group("TYPE") != null) {
                style = typeStyle;
            } else if (m.group("WORD") != null) {
                String word = m.group();
                if (KEYWORDS.contains(word)) style = keyword;
                else if (DIRECTIVES.contains(word)) style = directive;
                else if (TYPE_KEYWORDS.contains(word)) style = typeKeyword;
                else if (BUILTINS.contains(word)) style = builtin;
            }
            if (style != null) {
                doc.setCharacterAttributes(m.start(), m.end() - m.start(), style, true);
            }
        }
    }
}
