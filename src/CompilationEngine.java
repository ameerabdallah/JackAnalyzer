import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final OutputStreamWriter osw;
    private int indent = 0;

    public CompilationEngine(JackTokenizer tokenizer, OutputStream os) throws ParserConfigurationException,
            IOException {
        this.tokenizer = tokenizer;
        this.osw = new OutputStreamWriter(os);
    }

    public void compileClass() throws IOException {
        printStartTagln("class");
        // Create the class element
        tokenizer.advance();
        if (tokenizer.keyWord() == JackTokenizer.Keyword.CLASS) {
            // Create the 'class' keyword element
            writeToken();

            tokenizer.advance();

            if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                throw new IllegalStateException("Class expected a class name is not an identifier");
            }

            // write the class's name
            writeToken();
            tokenizer.advance();

            if (tokenizer.symbol() != '{') {
                throw new IllegalStateException("Class expected '{' but found " + tokenizer.symbol());
            }

            // Create the '{' symbol element
            writeToken();
            tokenizer.advance();

            while (tokenizer.hasMoreTokens()) {
                if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                    switch (tokenizer.keyWord()) {
                        case STATIC, FIELD -> {
                            compileClassVarDec();
                        }
                        case CONSTRUCTOR, FUNCTION, METHOD -> {
                            compileSubroutine();
                        }
                        default ->
                                throw new IllegalStateException("Class expected a keyword but found " + tokenizer.keyWord());
                    }
                }
            }
        } else {
            throw new IllegalStateException("Current token is not a class");
        }
        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '}') {
            writeToken();
        } else {
            throw new IllegalStateException("Expected '}' " + tokenizer.tokenType());
        }
        printEndTagln("class");
    }

    public void compileClassVarDec() throws IOException {
        printStartTagln("classVarDec");
        // write static or field
        writeToken();
        tokenizer.advance();

        // write type
        writeToken();
        tokenizer.advance();

        // write identifier
        writeToken();
        tokenizer.advance();

        boolean foundEnd = false;
        while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && !foundEnd) {
            switch (tokenizer.symbol()) {
                case ',' -> {
                    writeToken();
                    tokenizer.advance();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                        throw new IllegalStateException("ClassVarDec expected an identifier but found " + tokenizer.tokenType());
                    }
                    writeToken();
                    tokenizer.advance();
                }
                case ';' -> {
                    writeToken();
                    tokenizer.advance();
                    // break out of the loop
                    foundEnd = true;
                }
                default ->
                        throw new IllegalStateException("ClassVarDec expected ',' or ';' but found " + tokenizer.symbol());
            }
        }
        printEndTagln("classVarDec");
    }

    public void compileSubroutine() throws IOException {
        printStartTagln("subroutineDec");
        // write constructor, function or method
        writeToken();
        tokenizer.advance();
        // write void or type
        writeToken();
        tokenizer.advance();
        // write subroutine name
        if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
            throw new IllegalStateException("Subroutine expected an identifier but found " + tokenizer.tokenType());
        }
        writeToken();
        tokenizer.advance();
        if (tokenizer.symbol() != '(') {
            throw new IllegalStateException("Subroutine expected '(' but found " + tokenizer.symbol());
        }
        // write '('
        writeToken();
        tokenizer.advance();
        // write parameter list
        compileParameterList();
        if (tokenizer.symbol() != ')') {
            throw new IllegalStateException("Subroutine expected ')' but found " + tokenizer.symbol());
        }
        // write ')'
        writeToken();
        tokenizer.advance();
        if (tokenizer.symbol() != '{') {
            throw new IllegalStateException("Subroutine expected '{' but found " + tokenizer.symbol());
        }
        compileSubroutineBody();
        printEndTagln("subroutineDec");
    }

    public void compileParameterList() throws IOException {
        printStartTagln("parameterList");
        while (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            // write type
            writeToken();
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                throw new IllegalStateException("ParameterList expected an identifier but found " + tokenizer.tokenType());
            }
            // write identifier
            writeToken();
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                writeToken();
                tokenizer.advance();
            }
        }
        printEndTagln("parameterList");
    }

    public void compileSubroutineBody() throws IOException {
        printStartTagln("subroutineBody");
        if (tokenizer.symbol() != '{') {
            throw new IllegalStateException("SubroutineBody expected '{' but found " + tokenizer.symbol());
        }
        // write '{'
        writeToken();
        tokenizer.advance();

        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyWord() == JackTokenizer.Keyword.VAR) {
            compileVarDec();
        }

        compileStatements();
        if (tokenizer.symbol() != '}') {
            throw new IllegalStateException("SubroutineBody expected '}' but found " + tokenizer.symbol());
        }
        // write '}'
        writeToken();
        tokenizer.advance();

        printEndTagln("subroutineBody");
    }

    public void compileVarDec() throws IOException {
        printStartTagln("varDec");
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyWord() == JackTokenizer.Keyword.VAR) {
            // write var
            writeToken();
            tokenizer.advance();

            // write type
            writeToken();
            tokenizer.advance();

            // write identifier
            writeToken();
            tokenizer.advance();

            while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL) {
                if (tokenizer.symbol() == ',') {
                    writeToken();
                    tokenizer.advance();
                    if (tokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {
                        writeToken();
                        tokenizer.advance();
                    } else {
                        throw new IllegalStateException("ClassVarDec expected an identifier but found " + tokenizer.tokenType());
                    }
                } else if (tokenizer.symbol() == ';') {
                    writeToken();
                    tokenizer.advance();
                    break;
                } else {
                    throw new IllegalStateException("ClassVarDec expected ',' or ';' but found " + tokenizer.symbol());
                }
            }
        }
        printEndTagln("varDec");
    }

    public void compileStatements() throws IOException {
        printStartTagln("statements");
        while (true) {
            if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                switch (tokenizer.keyWord()) {
                    case LET -> compileLet();
                    case IF -> compileIf();
                    case WHILE -> compileWhile();
                    case DO -> compileDo();
                    case RETURN -> compileReturn();
                    default -> throw new IllegalStateException("Expected a statement but found " + tokenizer.keyWord());
                }
            } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '}') {
                break;
            } else {
                throw new IllegalStateException("Expected a statement but found " + tokenizer.tokenType());
            }
        }
        printEndTagln("statements");
    }

    public void compileLet() throws IOException {
        printStartTagln("letStatement");
        if (tokenizer.keyWord() != JackTokenizer.Keyword.LET) {
            throw new IllegalStateException("Let expected a keyword but found " + tokenizer.keyWord());
        }
        // write let
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
            throw new IllegalStateException("Let expected an identifier but found " + tokenizer.tokenType());
        }
        // write identifier
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '[') {
            // write '['
            writeToken();
            tokenizer.advance();
            compileExpression();
            if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ']') {
                throw new IllegalStateException("Let expected ']' but found " + tokenizer.symbol());
            }
            // write ']'
            writeToken();
            tokenizer.advance();
        }
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '=') {
            throw new IllegalStateException("Let expected '=' but found " + tokenizer.symbol());
        }
        // write '='
        writeToken();
        tokenizer.advance();
        compileExpression();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ';') {
            throw new IllegalStateException("Let expected ';' but found " + tokenizer.symbol());
        }
        // write ';'
        writeToken();
        tokenizer.advance();
        printEndTagln("letStatement");
    }

    public void compileIf() throws IOException {
        printStartTagln("ifStatement");
        if (tokenizer.keyWord() != JackTokenizer.Keyword.IF) {
            throw new IllegalStateException("If expected a keyword but found " + tokenizer.keyWord());
        }
        // write if
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '(') {
            throw new IllegalStateException("If expected '(' but found " + tokenizer.symbol());
        }
        // write '('
        writeToken();
        tokenizer.advance();
        compileExpression();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            throw new IllegalStateException("If expected ')' but found " + tokenizer.symbol());
        }
        // write ')'
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '{') {
            throw new IllegalStateException("If expected '{' but found " + tokenizer.symbol());
        }
        // write '{'
        writeToken();
        tokenizer.advance();
        compileStatements();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '}') {
            throw new IllegalStateException("If expected '}' but found " + tokenizer.symbol());
        }
        // write '}'
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyWord() == JackTokenizer.Keyword.ELSE) {
            // write else
            //noinspection DuplicatedCode
            writeToken();
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '{') {
                throw new IllegalStateException("If expected '{' but found " + tokenizer.symbol());
            }
            // write '{'
            writeToken();
            tokenizer.advance();
            compileStatements();
            if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '}') {
                throw new IllegalStateException("If expected '}' but found " + tokenizer.symbol());
            }
            // write '}'
            writeToken();
            tokenizer.advance();
        }
        printEndTagln("ifStatement");
    }

    public void compileWhile() throws IOException {
        printStartTagln("whileStatement");
        if (tokenizer.keyWord() != JackTokenizer.Keyword.WHILE) {
            throw new IllegalStateException("While expected a keyword but found " + tokenizer.keyWord());
        }
        // write while
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '(') {
            throw new IllegalStateException("While expected '(' but found " + tokenizer.symbol());
        }
        // write '('
        writeToken();
        tokenizer.advance();
        compileExpression();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            throw new IllegalStateException("While expected ')' but found " + tokenizer.symbol());
        }
        // write ')'
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '{') {
            throw new IllegalStateException("While expected '{' but found " + tokenizer.symbol());
        }
        // write '{'
        writeToken();
        tokenizer.advance();
        compileStatements();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '}') {
            throw new IllegalStateException("While expected '}' but found " + tokenizer.symbol());
        }
        // write '}'
        writeToken();
        tokenizer.advance();
        printEndTagln("whileStatement");
    }

    public void compileDo() throws IOException {
        printStartTagln("doStatement");
        if (tokenizer.keyWord() != JackTokenizer.Keyword.DO) {
            throw new IllegalStateException("Do expected a keyword but found " + tokenizer.keyWord());
        }
        // write do
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
            throw new IllegalStateException("Do expected an identifier but found " + tokenizer.tokenType());
        }
        // write identifier
        writeToken();
        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '.') {
            // write '.'
            //noinspection DuplicatedCode
            writeToken();
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                throw new IllegalStateException("SubroutineCall expected an identifier but found " + tokenizer.tokenType());
            }
            // write identifier
            writeToken();
            tokenizer.advance();
        }
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '(') {
            throw new IllegalStateException("SubroutineCall expected '(' but found " + tokenizer.symbol());
        }
        // write '('
        writeToken();
        tokenizer.advance();
        compileExpressionList();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            throw new IllegalStateException("SubroutineCall expected ')' but found " + tokenizer.symbol());
        }
        // write ')'
        writeToken();
        tokenizer.advance();
        // write ';'
        writeToken();
        tokenizer.advance();
        printEndTagln("doStatement");
    }

    public void compileReturn() throws IOException {
        printStartTagln("returnStatement");
        if (tokenizer.keyWord() != JackTokenizer.Keyword.RETURN) {
            throw new IllegalStateException("Return expected a keyword but found " + tokenizer.keyWord());
        }
        // write return
        writeToken();
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ';') {
            compileExpression();
        }
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ';') {
            throw new IllegalStateException("Return expected ';' but found " + tokenizer.symbol());
        }
        // write ';'
        writeToken();
        tokenizer.advance();
        printEndTagln("returnStatement");
    }

    public void compileExpression() throws IOException {
        printStartTagln("expression");
        // write term
        compileTerm();
        while (tokenizer.isOp()) {
            // write operator
            writeToken();
            tokenizer.advance();
            // write term
            compileTerm();
        }
        printEndTagln("expression");
    }

    public void compileTerm() throws IOException {
        printStartTagln("term");
        switch (tokenizer.tokenType()) {
            case INT_CONST, STRING_CONST -> {
                writeToken();
                tokenizer.advance();
            }
            case KEYWORD -> {
                if (tokenizer.keyWord().isKeywordConstant()) {
                    writeToken();
                    tokenizer.advance();
                } else {
                    throw new IllegalStateException("Term expected a keyword constant but found " + tokenizer.keyWord());
                }
            }
            case IDENTIFIER -> {
                writeToken();
                tokenizer.advance();

                if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '.') {
                    // write '.'
                    writeToken();
                    tokenizer.advance();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                        throw new IllegalStateException("SubroutineCall expected an identifier but found " + tokenizer.tokenType());
                    }
                    // write identifier
                    writeToken();
                    tokenizer.advance();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != '(') {
                        throw new IllegalStateException("SubroutineCall expected '(' but found " + tokenizer.symbol());
                    }
                    // write '('
                    writeToken();
                    tokenizer.advance();
                    compileExpressionList();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
                        throw new IllegalStateException("SubroutineCall expected ')' but found " + tokenizer.symbol());
                    }
                    // write ')'
                    writeToken();
                    tokenizer.advance();
                } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '[') {
                    // write '['
                    writeToken();
                    tokenizer.advance();
                    compileExpression();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ']') {
                        throw new IllegalStateException("Term expected ']' but found " + tokenizer.symbol());
                    }
                    // write ']'
                    writeToken();
                    tokenizer.advance();
                }
            }
            case SYMBOL -> {
                if (tokenizer.symbol() == '(') {
                    // write '('
                    writeToken();
                    tokenizer.advance();
                    compileExpression();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
                        throw new IllegalStateException("Term expected ')' but found " + tokenizer.symbol());
                    }
                    // write ')'
                    writeToken();
                    tokenizer.advance();
                } else if (tokenizer.isUnaryOp()) {
                    // write unary operator
                    writeToken();
                    tokenizer.advance();
                    compileTerm();
                } else {
                    throw new IllegalStateException("Term expected a symbol but found " + tokenizer.symbol());
                }
            }
        }
        printEndTagln("term");
    }

    public int compileExpressionList() throws IOException {
        printStartTagln("expressionList");
        int numOfExpressions = 0;
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            do {
                compileExpression();
                numOfExpressions++;
                if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                    // write ','
                    writeToken();
                    tokenizer.advance();
                }
            } while (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')');
        }
        printEndTagln("expressionList");
        return numOfExpressions;
    }

    private void printKeyword() throws IOException {
        print(tokenizer.keyWord().getKeyword());
    }

    private void printTokenIdentifier() throws IOException {
        print(tokenizer.identifier());
    }

    private void printTokenSymbol() throws IOException {
        String symbol = tokenizer.symbol() == '<' ? "&lt;" : tokenizer.symbol() == '>' ? "&gt;" :
                tokenizer.symbol() == '&' ? "&amp;" : String.valueOf(
                tokenizer.symbol());
        print(symbol);
    }

    private void printTokenIntConst() throws IOException {
        print(String.valueOf(tokenizer.intVal()));
    }

    private void printTokenStringConst() throws IOException {
        String stringConst = tokenizer.stringVal()
                .replaceAll("&", "&amp);")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;");
        print(stringConst);
    }

    private void writeToken() throws IOException {
        String tag = tokenizer.tokenType().toXMLTag();
        printStartTag(tag);
        switch (tokenizer.tokenType()) {
            case KEYWORD -> printKeyword();
            case IDENTIFIER -> printTokenIdentifier();
            case SYMBOL -> printTokenSymbol();
            case INT_CONST -> printTokenIntConst();
            case STRING_CONST -> printTokenStringConst();
        }
        printEndTag(tag);
    }

    private void print(String s) throws IOException {
        osw.write(" " + s + " ");
        osw.flush();
    }

    private void indent() throws IOException {
        for (int i = 0; i < indent; i++) {
            osw.write("  ");
        }
    }

    private void printStartTag(String tag) throws IOException {
        indent();
        osw.write("<" + tag + ">");
        osw.flush();
    }

    private void printEndTag(String tag) throws IOException {
        osw.write("</" + tag + ">\n");
        osw.flush();
    }

    private void printStartTagln(String tag) throws IOException {
        indent();
        indent++;
        osw.write("<" + tag + ">\n");
        osw.flush();
    }

    private void printEndTagln(String tag) throws IOException {
        indent--;
        indent();
        osw.write("</" + tag + ">\n");
        osw.flush();
    }
}
