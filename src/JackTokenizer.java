import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {
    private String currentToken;
    private TokenType tokenType;
    private final Queue<String> tokenQueue = new ArrayDeque<>();
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"[^\"]*\"|\\S+");


    public JackTokenizer(InputStream inputStream) {
        // Initialize the tokenizer with the input stream
        // Arbitrary number of space characters, new line characters, and comments
        // Three types of comments: single line, multi-line, and block comments
        // /* comment until */, /** API comment until */ and // comment until new line
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        ArrayList<String> lines = new ArrayList<>();
        final boolean[] multiLineComment = {false};
        br.lines().forEach(line -> {
            // Remove comments
            line = line.trim();
            if (multiLineComment[0]) {
                if (line.contains("*/")) {
                    multiLineComment[0] = false;
                    line = line.substring(line.indexOf("*/") + 2);
                } else {
                    return; // Skip the entire line
                }
            } else if (line.startsWith("/*") && !line.endsWith("*/")) {
                multiLineComment[0] = true;
                line = line.substring(0, line.indexOf("/*"));
            } else if (line.contains("//")) {
                line = line.substring(0, line.indexOf("//"));
            }
            line = line.replaceAll("/\\*.*?\\*/", ""); // Remove block comments

            if (!line.isBlank()) {
                lines.add(line);
            }
        });
        for (String line : lines) {
            // add spaces around symbols unless they are part of a string constant
            line = line.replaceAll("([{}()\\[\\].,;+\\-*/&|<>=~])", " $1 ");


            Matcher matcher = TOKEN_PATTERN.matcher(line);
            List<String> tokens = new ArrayList<>();

            while (matcher.find()) {
                tokens.add(matcher.group());
            }

            for (String token : tokens) {
                if (!token.isBlank()) {
                    // Add the token to the queue
                    tokenQueue.add(token);
                }
            }
        }

        this.currentToken = null;
        this.tokenType = null;
    }

    public boolean hasMoreTokens() {
        // Check if there are more tokens to read
        try {
            return !this.tokenQueue.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void advance() {
        // Read the next token from the input stream
        if (this.hasMoreTokens()) {
            this.currentToken = this.tokenQueue.poll();
            this.tokenType = getTokenType();
        } else {
            throw new IllegalStateException("No more tokens to read");
        }
    }

    private TokenType getTokenType() {
        // Determine the type of the current token
        if (this.currentToken.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            if (Keyword.isKeyword(this.currentToken)) {
                return TokenType.KEYWORD;
            } else {
                return TokenType.IDENTIFIER;
            }
        } else if (this.currentToken.matches("\\d+")) {
            return TokenType.INT_CONST;
        } else if (this.currentToken.startsWith("\"") && this.currentToken.endsWith("\"")) {
            return TokenType.STRING_CONST;
        } else {
            switch (this.currentToken) {
                case "{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~" -> {
                    return TokenType.SYMBOL;
                }
                default -> throw new IllegalArgumentException("Unknown token: " + this.currentToken);
            }
        }
    }

    public TokenType tokenType() {
        // Return the type of the current token
        return this.tokenType;
    }

    public String identifier() {
        if (this.tokenType == TokenType.IDENTIFIER) {
            return this.currentToken;
        } else {
            throw new IllegalStateException("Current token is not an identifier");
        }
    }

    public char symbol() {
        if (this.tokenType == TokenType.SYMBOL) {
            return this.currentToken.charAt(0);
        } else {
            throw new IllegalStateException("Current token is not a symbol");
        }
    }

    public boolean isOp() {
        if (this.tokenType == TokenType.SYMBOL) {
            char symbol = this.symbol();
            return switch (symbol) {
                case '+', '-', '*', '/', '&', '|', '<', '>', '=' -> true;
                default -> false;
            };
        } else {
            throw new IllegalStateException("Current token is not a symbol");
        }
    }

    public boolean isUnaryOp() {
        if (this.tokenType == TokenType.SYMBOL) {
            char symbol = this.symbol();
            return switch (symbol) {
                case '-', '~' -> true;
                default -> false;
            };
        } else {
            throw new IllegalStateException("Current token is not a symbol");
        }
    }

    public short intVal() {
        if (this.tokenType == TokenType.INT_CONST) {
            return Short.parseShort(this.currentToken);
        } else {
            throw new IllegalStateException("Current token is not an integer constant");
        }
    }

    public String stringVal() {
        if (this.tokenType == TokenType.STRING_CONST) {
            return this.currentToken.substring(1, this.currentToken.length() - 1);
        } else {
            throw new IllegalStateException("Current token is not a string constant");
        }
    }

    public Keyword keyWord() {
        return Keyword.getKeyword(this.currentToken);
    }

    public enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST
    }

    public enum Keyword {
        CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID,
        VAR, STATIC, FIELD, LET, DO, IF, ELSE, WHILE, RETURN,
        TRUE, FALSE, NULL, THIS;

        private static final Map<String, Keyword> MAP = Map.ofEntries(
                Map.entry("class", CLASS),
                Map.entry("method", METHOD),
                Map.entry("function", FUNCTION),
                Map.entry("constructor", CONSTRUCTOR),
                Map.entry("int", INT),
                Map.entry("boolean", BOOLEAN),
                Map.entry("char", CHAR),
                Map.entry("void", VOID),
                Map.entry("var", VAR),
                Map.entry("static", STATIC),
                Map.entry("field", FIELD),
                Map.entry("let", LET),
                Map.entry("do", DO),
                Map.entry("if", IF),
                Map.entry("else", ELSE),
                Map.entry("while", WHILE),
                Map.entry("return", RETURN),
                Map.entry("true", TRUE),
                Map.entry("false", FALSE),
                Map.entry("null", NULL),
                Map.entry("this", THIS)
            );

        public static Keyword getKeyword(String keyword) {
            return MAP.get(keyword);
        }

        public static boolean isKeyword(String token) {
            return getKeyword(token) != null;
        }

        public boolean isKeywordConstant() {
            return this == TRUE || this == FALSE || this == NULL || this == THIS;
        }
    }
}
