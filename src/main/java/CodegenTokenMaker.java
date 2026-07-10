import org.fife.ui.rsyntaxtextarea.*;
import javax.swing.text.Segment;
import java.util.Set;

/**
 * Custom TokenMaker for Codegen language syntax highlighting
 * in RSyntaxTextArea.
 */
public class CodegenTokenMaker extends AbstractTokenMaker {

    // Codegen statement keywords
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

    // Statement prefixes / directives
    private static final Set<String> DIRECTIVES = Set.of(
        "ScalarDecl", "ListDecl", "EndListDecl", "MapDecl", "EndMapDecl",
        "TupleDecl", "EndTupleDecl", "UnpackTuple", "UserTypeDecl",
        "Statement", "Expression", "Assign", "Print", "PrintLine",
        "Pragma", "Comment", "BeginComment", "EndComment"
    );

    // Type/modifier keywords
    private static final Set<String> TYPE_KEYWORDS = Set.of(
        "void", "auto", "async", "await", "static", "const", "final",
        "Const", "Final", "let", "inout", "try", "true", "false", "new",
        "in", "extends", "implements", "throws"
    );

    // Built-in functions
    private static final Set<String> BUILTINS = Set.of(
        "format", "string", "char", "byte", "short", "int", "long",
        "int16", "int32", "int64", "double",
        "isNull", "listsize", "mapsize", "mapkeys", "mapkeysAsList",
        "IRANGE", "FRANGE", "XRANGE"
    );

    // Comparison operator keywords
    private static final Set<String> OPERATORS = Set.of(
        "lt", "gt", "le", "ge", "eq", "ne"
    );

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap map = new TokenMap();
        for (String s : KEYWORDS) map.put(s, Token.RESERVED_WORD);
        for (String s : DIRECTIVES) map.put(s, Token.FUNCTION);
        for (String s : TYPE_KEYWORDS) map.put(s, Token.DATA_TYPE);
        for (String s : BUILTINS) map.put(s, Token.RESERVED_WORD_2);
        for (String s : OPERATORS) map.put(s, Token.OPERATOR);
        return map;
    }

    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        if (tokenType == Token.IDENTIFIER) {
            int value = wordsToHighlight.get(segment, start, end);
            if (value != -1) {
                tokenType = value;
            }
        }
        super.addToken(segment, start, end, tokenType, startOffset);
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();

        char[] array = text.array;
        int offset = text.offset;
        int count = text.count;
        int end = offset + count;

        int newStartOffset = startOffset - offset;

        int currentTokenStart = offset;
        int currentTokenType = Token.NULL;

        // Handle continuation of multi-line comment
        if (initialTokenType == Token.COMMENT_MULTILINE) {
            currentTokenType = Token.COMMENT_MULTILINE;
            currentTokenStart = offset;
        }
        // Handle continuation of block comment (BeginComment:...EndComment)
        if (initialTokenType == Token.COMMENT_DOCUMENTATION) {
            currentTokenType = Token.COMMENT_DOCUMENTATION;
            currentTokenStart = offset;
        }

        for (int i = offset; i < end; i++) {
            char c = array[i];

            switch (currentTokenType) {
                case Token.NULL:
                    currentTokenStart = i;
                    if (c == '/' && i + 1 < end && array[i + 1] == '#') {
                        // Multi-line comment /#...#/
                        currentTokenType = Token.COMMENT_MULTILINE;
                        i++; // skip #
                        break;
                    }
                    if (c == '/' && i + 1 < end && array[i + 1] == '*') {
                        // Inline comment /*...*/
                        currentTokenType = Token.COMMENT_MULTILINE;
                        i++; // skip *
                        break;
                    }
                    if (c == '"') {
                        currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                    } else if (c == 'f' && i + 1 < end && array[i + 1] == '"') {
                        currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                        i++; // skip the opening quote
                    } else if (c == 'o' && i + 1 < end && array[i + 1] == '"') {
                        currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                        i++; // skip the opening quote
                    } else if (c == '`') {
                        currentTokenType = Token.LITERAL_BACKQUOTE;
                    } else if (c == '\'') {
                        currentTokenType = Token.LITERAL_CHAR;
                    } else if (c == '<' && i + 1 < end && (Character.isLetter(array[i + 1]) || array[i + 1] == '_')) {
                        // Type annotation <type>
                        currentTokenType = Token.VARIABLE;
                    } else if (c == '[' || c == '{' || c == '\u00AB') {
                        // List/map/tuple type annotations [type] {type} «type»
                        addToken(text, currentTokenStart, i, Token.VARIABLE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    } else if (Character.isDigit(c)) {
                        currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
                    } else if (c == '-' && i + 1 < end && Character.isDigit(array[i + 1])) {
                        currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
                    } else if (c == '0' && i + 1 < end && (array[i + 1] == 'x' || array[i + 1] == 'X')) {
                        currentTokenType = Token.LITERAL_NUMBER_HEXADECIMAL;
                        i++; // skip x
                    } else if (Character.isLetter(c) || c == '_') {
                        currentTokenType = Token.IDENTIFIER;
                    } else if (Character.isWhitespace(c)) {
                        currentTokenType = Token.WHITESPACE;
                    } else if (c == '#' && i + 1 < end) {
                        // Single line comment # to end of line
                        currentTokenType = Token.COMMENT_EOL;
                    } else {
                        // Operators, punctuation
                        addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.COMMENT_EOL:
                    // Continues to end of line
                    break;

                case Token.COMMENT_MULTILINE:
                    if (c == '#' && i + 1 < end && array[i + 1] == '/') {
                        // End of /# ... #/ comment
                        i++; // skip /
                        addToken(text, currentTokenStart, i, Token.COMMENT_MULTILINE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    } else if (c == '*' && i + 1 < end && array[i + 1] == '/') {
                        // End of /* ... */ comment
                        i++; // skip /
                        addToken(text, currentTokenStart, i, Token.COMMENT_MULTILINE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.COMMENT_DOCUMENTATION:
                    // BeginComment:...EndComment block - check for EndComment
                    if (c == 'E' && matchesAt(array, i, end, "EndComment")) {
                        // Include "EndComment" in the comment
                        i += "EndComment".length() - 1;
                        addToken(text, currentTokenStart, i, Token.COMMENT_MULTILINE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.LITERAL_STRING_DOUBLE_QUOTE:
                    if (c == '\\' && i + 1 < end) {
                        i++; // skip escape
                    } else if (c == '"') {
                        addToken(text, currentTokenStart, i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.LITERAL_BACKQUOTE:
                    if (c == '\\' && i + 1 < end) {
                        i++; // skip escape
                    } else if (c == '`') {
                        addToken(text, currentTokenStart, i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.LITERAL_CHAR:
                    if (c == '\\' && i + 1 < end) {
                        i++; // skip escape
                    } else if (c == '\'') {
                        addToken(text, currentTokenStart, i, Token.LITERAL_CHAR, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.VARIABLE:
                    // Type annotation <type> - wait for closing >
                    if (c == '>') {
                        addToken(text, currentTokenStart, i, Token.VARIABLE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.IDENTIFIER:
                    if (!Character.isLetterOrDigit(c) && c != '_') {
                        // Check if followed by colon (directive-style "Keyword:")
                        String word = new String(array, currentTokenStart, i - currentTokenStart);
                        int tokenType2 = classifyWord(word);

                        // Check for "Comment:" on the line - whole rest of line is comment
                        if (word.equals("Comment") && c == ':') {
                            // This is a comment line - mark rest as comment
                            addToken(text, currentTokenStart, end - 1, Token.COMMENT_EOL, newStartOffset + currentTokenStart);
                            currentTokenType = Token.NULL;
                            i = end - 1; // skip to end
                        } else if (word.equals("BeginComment") && c == ':') {
                            // Start of block comment - use COMMENT_DOCUMENTATION state
                            // End-of-line handler will create the token without addNullToken
                            // so RSyntaxTextArea continues with this state on the next line
                            currentTokenType = Token.COMMENT_DOCUMENTATION;
                            i = end - 1;
                        } else {
                            addToken(text, currentTokenStart, i - 1, tokenType2, newStartOffset + currentTokenStart);
                            currentTokenType = Token.NULL;
                            i--; // re-process
                        }
                    }
                    break;

                case Token.WHITESPACE:
                    if (!Character.isWhitespace(c)) {
                        addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--; // re-process
                    }
                    break;

                case Token.LITERAL_NUMBER_DECIMAL_INT:
                    if (!Character.isDigit(c) && c != '.' && c != 'e' && c != 'E'
                        && c != '+' && c != '-' && c != ',' && c != 'L') {
                        addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--; // re-process
                    }
                    break;

                case Token.LITERAL_NUMBER_HEXADECIMAL:
                    if (!isHexChar(c)) {
                        addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_HEXADECIMAL, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--; // re-process
                    }
                    break;
            }
        }

        // End of line: finalize current token
        switch (currentTokenType) {
            case Token.NULL:
                addNullToken();
                break;
            case Token.COMMENT_MULTILINE:
                addToken(text, currentTokenStart, end - 1, Token.COMMENT_MULTILINE, newStartOffset + currentTokenStart);
                break;
            case Token.COMMENT_DOCUMENTATION:
                addToken(text, currentTokenStart, end - 1, Token.COMMENT_DOCUMENTATION, newStartOffset + currentTokenStart);
                break;
            case Token.IDENTIFIER:
                String word = new String(array, currentTokenStart, end - currentTokenStart);
                int tokenType2 = classifyWord(word);
                addToken(text, currentTokenStart, end - 1, tokenType2, newStartOffset + currentTokenStart);
                addNullToken();
                break;
            default:
                addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
                addNullToken();
                break;
        }

        return firstToken;
    }

    private int classifyWord(String word) {
        if (KEYWORDS.contains(word)) return Token.RESERVED_WORD;
        if (DIRECTIVES.contains(word)) return Token.FUNCTION;
        if (TYPE_KEYWORDS.contains(word)) return Token.DATA_TYPE;
        if (BUILTINS.contains(word)) return Token.RESERVED_WORD_2;
        if (OPERATORS.contains(word)) return Token.OPERATOR;
        return Token.IDENTIFIER;
    }

    private boolean matchesAt(char[] array, int pos, int end, String target) {
        if (pos + target.length() > end) return false;
        for (int i = 0; i < target.length(); i++) {
            if (array[pos + i] != target.charAt(i)) return false;
        }
        return true;
    }

    private boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[]{"Comment: ", null};
    }

    @Override
    public boolean getMarkOccurrencesOfTokenType(int type) {
        return type == Token.IDENTIFIER || type == Token.VARIABLE;
    }

    @Override
    public boolean getShouldIndentNextLineAfter(Token token) {
        return false;
    }
}
