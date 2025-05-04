import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class JackTokenizer {
    // Arbitrary number of space characters, new line characters, and comments
    // Three types of comments: single line, multi-line, and block comments
    // /* comment until */, /** API comment until */ and // comment until new line
    private static final String DELIMITER_PATTERN = "\\s+|/\\*.*\\*/|//.*\\n|\\r?\\n";
    private final Scanner scanner;
    private String currentToken;
    private TokenType tokenType;

    public JackTokenizer(InputStream inputStream) {
        // Initialize the tokenizer with the input stream
        this.scanner = new Scanner(inputStream);
        // Delimiter is
        this.scanner.useDelimiter(DELIMITER_PATTERN);
        this.currentToken = null;
        this.tokenType = null;
    }

    public boolean hasMoreTokens() {
        // Check if there are more tokens to read
        try {
            return scanner.hasNext();
        } catch (Exception e) {
            return false;
        }
    }

    public void advance() throws IOException {
        // Read the next token from the input stream
        if (hasMoreTokens()) {
            currentToken = scanner.next();
            tokenType = getTokenType();
        } else {
            throw new IOException("No more tokens available");
        }
    }

    private TokenType getTokenType() {
        // Determine the type of the current token
        if (currentToken.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            if (Keyword.isKeyword(currentToken)) {
                return TokenType.KEYWORD;
            } else {
                return TokenType.IDENTIFIER;
            }
        } else if (currentToken.matches("\\d+")) {
            return TokenType.INT_CONST;
        } else if (currentToken.startsWith("\"") && currentToken.endsWith("\"")) {
            return TokenType.STRING_CONST;
        } else {
            return TokenType.SYMBOL;
        }
    }

    public TokenType tokenType() {
        // Return the type of the current token
        return tokenType;
    }

    public String identifier() {
        if (tokenType == TokenType.IDENTIFIER) {
            return currentToken;
        } else {
            throw new IllegalStateException("Current token is not an identifier");
        }
    }

    public int intVal() {
        if (tokenType == TokenType.INT_CONST) {
            return Integer.parseInt(currentToken);
        } else {
            throw new IllegalStateException("Current token is not an integer constant");
        }
    }

    public String stringVal() {
        if (tokenType == TokenType.STRING_CONST) {
            return currentToken.substring(1, currentToken.length() - 1);
        } else {
            throw new IllegalStateException("Current token is not a string constant");
        }
    }

    public enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST
    }

    public enum Keyword {
        CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID,
        VAR, STATIC, FIELD, LET, DO, IF, ELSE, WHILE, RETURN,
        TRUE, FALSE, NULL, THIS;

        public static boolean isKeyword(String token) {
            for (Keyword keyword : Keyword.values()) {
                if (keyword.name().equals(token.toUpperCase())) {
                    return true;
                }
            }
            return false;
        }
    }
}
