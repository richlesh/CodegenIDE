import org.fife.ui.rsyntaxtextarea.*;
import javax.swing.text.Segment;
import java.util.Set;

/**
 * TokenMaker for Swift syntax highlighting in RSyntaxTextArea.
 */
public class SwiftTokenMaker extends AbstractTokenMaker {

    // Declaration keywords
    private static final Set<String> KEYWORDS = Set.of(
        "associatedtype", "class", "deinit", "enum", "extension",
        "fileprivate", "func", "import", "init", "inout", "internal",
        "let", "open", "operator", "private", "precedencegroup",
        "protocol", "public", "rethrows", "static", "struct",
        "subscript", "typealias", "var",
        // Statement keywords
        "break", "case", "catch", "continue", "default", "defer",
        "do", "else", "fallthrough", "for", "guard", "if", "in",
        "repeat", "return", "throw", "switch", "where", "while",
        // Expression/type keywords
        "as", "is", "try", "await", "async"
    );

    // Types and literals
    private static final Set<String> TYPES = Set.of(
        "Any", "Self", "Int", "Int8", "Int16", "Int32", "Int64",
        "UInt", "UInt8", "UInt16", "UInt32", "UInt64",
        "Float", "Double", "Bool", "String", "Character",
        "Void", "Optional", "Array", "Dictionary", "Set",
        "AnyObject", "AnyClass", "Error", "Never"
    );

    // Constants and special values
    private static final Set<String> CONSTANTS = Set.of(
        "true", "false", "nil", "self", "super"
    );

    // Context keywords (can be identifiers in other contexts)
    private static final Set<String> CONTEXT_KEYWORDS = Set.of(
        "associativity", "convenience", "didSet", "dynamic", "final",
        "get", "indirect", "infix", "lazy", "left", "mutating", "none",
        "nonmutating", "optional", "override", "postfix", "precedence",
        "prefix", "Protocol", "required", "right", "set", "some",
        "Type", "unowned", "weak", "willSet", "throws", "nonisolated",
        "isolated", "consuming", "borrowing", "sending"
    );

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap map = new TokenMap();
        for (String s : KEYWORDS) map.put(s, Token.RESERVED_WORD);
        for (String s : TYPES) map.put(s, Token.DATA_TYPE);
        for (String s : CONSTANTS) map.put(s, Token.LITERAL_BOOLEAN);
        for (String s : CONTEXT_KEYWORDS) map.put(s, Token.RESERVED_WORD_2);
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

        if (initialTokenType == Token.COMMENT_MULTILINE) {
            currentTokenType = Token.COMMENT_MULTILINE;
            currentTokenStart = offset;
        }

        for (int i = offset; i < end; i++) {
            char c = array[i];

            switch (currentTokenType) {
                case Token.NULL:
                    currentTokenStart = i;
                    if (c == '/' && i + 1 < end && array[i + 1] == '/') {
                        currentTokenType = Token.COMMENT_EOL;
                        i++;
                    } else if (c == '/' && i + 1 < end && array[i + 1] == '*') {
                        currentTokenType = Token.COMMENT_MULTILINE;
                        i++;
                    } else if (c == '"') {
                        if (i + 2 < end && array[i + 1] == '"' && array[i + 2] == '"') {
                            currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                            i += 2; // skip to third quote, will handle multi-line
                        } else {
                            currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                        }
                    } else if (c == '#' && i + 1 < end && array[i + 1] == '"') {
                        currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                        i++;
                    } else if (Character.isDigit(c)) {
                        currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
                    } else if (c == '0' && i + 1 < end && (array[i + 1] == 'x' || array[i + 1] == 'X')) {
                        currentTokenType = Token.LITERAL_NUMBER_HEXADECIMAL;
                        i++;
                    } else if (c == '.' && i + 1 < end && Character.isDigit(array[i + 1])) {
                        currentTokenType = Token.LITERAL_NUMBER_FLOAT;
                    } else if (Character.isLetter(c) || c == '_' || c == '@') {
                        currentTokenType = Token.IDENTIFIER;
                    } else if (c == '#') {
                        currentTokenType = Token.PREPROCESSOR;
                    } else if (Character.isWhitespace(c)) {
                        currentTokenType = Token.WHITESPACE;
                    } else {
                        addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                    }
                    break;

                case Token.COMMENT_EOL:
                    // Continues to end of line
                    break;

                case Token.COMMENT_MULTILINE:
                    if (c == '*' && i + 1 < end && array[i + 1] == '/') {
                        i++;
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

                case Token.IDENTIFIER:
                    if (!Character.isLetterOrDigit(c) && c != '_') {
                        addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--;
                    }
                    break;

                case Token.PREPROCESSOR:
                    if (!Character.isLetterOrDigit(c) && c != '_') {
                        addToken(text, currentTokenStart, i - 1, Token.PREPROCESSOR, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--;
                    }
                    break;

                case Token.WHITESPACE:
                    if (!Character.isWhitespace(c)) {
                        addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--;
                    }
                    break;

                case Token.LITERAL_NUMBER_DECIMAL_INT:
                    if (!Character.isDigit(c) && c != '_' && c != '.' && c != 'e' && c != 'E') {
                        addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--;
                    }
                    break;

                case Token.LITERAL_NUMBER_HEXADECIMAL:
                    if (!isHexChar(c) && c != '_') {
                        addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_HEXADECIMAL, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--;
                    }
                    break;

                case Token.LITERAL_NUMBER_FLOAT:
                    if (!Character.isDigit(c) && c != '_' && c != 'e' && c != 'E' && c != '+' && c != '-') {
                        addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_FLOAT, newStartOffset + currentTokenStart);
                        currentTokenType = Token.NULL;
                        i--;
                    }
                    break;
            }
        }

        // End of line
        switch (currentTokenType) {
            case Token.NULL:
                addNullToken();
                break;
            case Token.COMMENT_MULTILINE:
                addToken(text, currentTokenStart, end - 1, Token.COMMENT_MULTILINE, newStartOffset + currentTokenStart);
                break;
            case Token.IDENTIFIER:
                addToken(text, currentTokenStart, end - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                addNullToken();
                break;
            default:
                addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
                addNullToken();
                break;
        }

        return firstToken;
    }

    private boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[]{"//", null};
    }

    @Override
    public boolean getMarkOccurrencesOfTokenType(int type) {
        return type == Token.IDENTIFIER;
    }

    @Override
    public boolean getShouldIndentNextLineAfter(Token token) {
        return false;
    }
}
