import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final VMWriter vmWriter;
    private final SymbolTable classSymbolTable;
    private final SymbolTable subroutineSymbolTable;
    private String subroutineName;
    private JackTokenizer.Keyword subroutineType;
    private String className;
    private JackTokenizer.Keyword subroutineReturnType;

    public CompilationEngine(InputStream is, OutputStream os) {
        // Initialize the tokenizer and VMWriter
        this.tokenizer = new JackTokenizer(is);
        this.vmWriter = new VMWriter(os);

        this.classSymbolTable = new SymbolTable();
        this.subroutineSymbolTable = new SymbolTable();
    }

    public void compileClass() throws IOException {
        // Create the class element
        tokenizer.advance();
        if (tokenizer.keyWord() == JackTokenizer.Keyword.CLASS) {
            // Create the 'class' keyword element
            tokenizer.advance();

            if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                throw new IllegalStateException("Class expected a class name is not an identifier");
            }

            // write the class's name
            this.className = tokenizer.identifier();
            tokenizer.advance();

            if (tokenizer.symbol() != '{') {
                throw new IllegalStateException("Class expected '{' but found " + tokenizer.symbol());
            }

            // dequeue '{'
            tokenizer.advance();

            while (tokenizer.hasMoreTokens()) {
                if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                    switch (tokenizer.keyWord()) {
                        case STATIC, FIELD -> compileClassVarDec();
                        case CONSTRUCTOR, FUNCTION, METHOD -> compileSubroutine();
                        default ->
                                throw new IllegalStateException("Class expected a keyword but found " + tokenizer.keyWord());
                    }
                }
            }
        } else {
            throw new IllegalStateException("Current token is not a class");
        }
        // dequeueing '}' is not needed
    }

    public void compileClassVarDec() {
        JackTokenizer.Keyword kind = tokenizer.keyWord();

        // 'dequeue' static or field
        tokenizer.advance();

        // write type
        if (tokenizer.tokenType() != JackTokenizer.TokenType.KEYWORD && tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
            throw new IllegalStateException("ClassVarDec expected a type but found " + tokenizer.tokenType());
        }
        String type = tokenizer.keyWord() != null ? tokenizer.keyWord().toString() : tokenizer.identifier();
        tokenizer.advance();

        // write identifier
        if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
            throw new IllegalStateException("ClassVarDec expected an identifier but found " + tokenizer.tokenType());
        }
        classSymbolTable.define(tokenizer.identifier(), type, kind);
        tokenizer.advance();

        boolean foundEnd = false;
        while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && !foundEnd) {
            switch (tokenizer.symbol()) {
                case ',' -> {
                    tokenizer.advance();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                        throw new IllegalStateException("ClassVarDec expected an identifier but found " + tokenizer.tokenType());
                    }
                    classSymbolTable.define(tokenizer.identifier(), type, kind);
                    tokenizer.advance();
                }
                case ';' -> {
                    tokenizer.advance();
                    // break out of the loop
                    foundEnd = true;
                }
                default ->
                        throw new IllegalStateException("ClassVarDec expected ',' or ';' but found " + tokenizer.symbol());
            }
        }
    }

    public void compileSubroutine() throws IOException {
        // create a new subroutine symbol table
        this.subroutineSymbolTable.reset();
        this.subroutineType = tokenizer.keyWord();
        // dequeue 'constructor'/'function'/'method'
        tokenizer.advance();

        if (this.subroutineType == JackTokenizer.Keyword.METHOD) {
            // add 'this' to the subroutine symbol table
            this.subroutineSymbolTable.define("this", this.className, SymbolTable.Kind.ARG);
        }
        if (this.subroutineType == JackTokenizer.Keyword.CONSTRUCTOR) {
            // add 'this' to the subroutine symbol table
            this.subroutineSymbolTable.define("this", this.className, SymbolTable.Kind.VAR);
        }

        // save the return type
        this.subroutineReturnType = tokenizer.keyWord();

        // dequeue 'void' or type
        tokenizer.advance();

        // save the subroutine name
        this.subroutineName = tokenizer.identifier();

        // dequeue subroutine name
        tokenizer.advance();

        // 'dequeue '('
        tokenizer.advance();

        // fill the subroutine symbol table with the parameters
        compileParameterList();
        // dequeue ')'
        tokenizer.advance();

        compileSubroutineBody();
    }

    public void compileParameterList() {
        while (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            String type = tokenizer.keyWord() != null ? tokenizer.keyWord().toString() : tokenizer.identifier();
            // dequeue type
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                throw new IllegalStateException("ParameterList expected an identifier but found " + tokenizer.tokenType());
            }
            String identifier = tokenizer.identifier();
            // dequeue identifier
            tokenizer.advance();

            subroutineSymbolTable.define(identifier, type, SymbolTable.Kind.ARG);
            if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                tokenizer.advance();
            }
        }
    }

    public void compileSubroutineBody() throws IOException {
        if (tokenizer.symbol() != '{') {
            throw new IllegalStateException("SubroutineBody expected '{' but found " + tokenizer.symbol());
        }
        // dequeue '{'
        tokenizer.advance();

        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyWord() == JackTokenizer.Keyword.VAR) {
            compileVarDec();
        }
        // write function
        vmWriter.writeFunction(className + "." + subroutineName, subroutineSymbolTable.varCount(SymbolTable.Kind.VAR));

        if (subroutineType == JackTokenizer.Keyword.CONSTRUCTOR) {
            vmWriter.writePush(VMWriter.Segment.CONST, classSymbolTable.varCount(SymbolTable.Kind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        } else if (subroutineType == JackTokenizer.Keyword.METHOD) {
            vmWriter.writePush(VMWriter.Segment.ARG, 0);
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        }

        compileStatements();

        // write return if void
        if (subroutineReturnType == JackTokenizer.Keyword.VOID) {
            vmWriter.writePush(VMWriter.Segment.CONST, 0);
        }
        // dequeue '}'
        tokenizer.advance();
    }

    public void compileVarDec() {
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyWord() == JackTokenizer.Keyword.VAR) {
            // write var
            JackTokenizer.Keyword kindKeyword = tokenizer.keyWord();
            tokenizer.advance();

            // write type
            String type = tokenizer.keyWord() != null ? tokenizer.keyWord().toString() : tokenizer.identifier();
            tokenizer.advance();

            // write identifier
            subroutineSymbolTable.define(tokenizer.identifier(), type, kindKeyword);
            tokenizer.advance();

            while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL) {
                if (tokenizer.symbol() == ',') {
                    tokenizer.advance();
                    if (tokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {
                        subroutineSymbolTable.define(tokenizer.identifier(), type, kindKeyword);
                        tokenizer.advance();
                    } else {
                        throw new IllegalStateException("ClassVarDec expected an identifier but found " + tokenizer.tokenType());
                    }
                } else if (tokenizer.symbol() == ';') {
                    tokenizer.advance();
                    break;
                } else {
                    throw new IllegalStateException("ClassVarDec expected ',' or ';' but found " + tokenizer.symbol());
                }
            }
        }
    }

    public void compileStatements() throws IOException {
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
    }

    public void compileLet() throws IOException {
        if (tokenizer.keyWord() != JackTokenizer.Keyword.LET) {
            throw new IllegalStateException("Let expected a keyword but found " + tokenizer.keyWord());
        }
        // dequeue 'let'
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
            throw new IllegalStateException("Let expected an identifier but found " + tokenizer.tokenType());
        }
        // save 'varName'
        String varName = tokenizer.identifier();
        tokenizer.advance();

        // handle array access
        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '[') {
            // dequeue '['
            tokenizer.advance();
            vmWriter.writePush(VMWriter.Segment.fromKind(kindOf(varName)), indexOf(varName));
            // compile expression1
            compileExpression();
            vmWriter.writeArithmetic(VMWriter.Command.ADD);
            // dequeue ']'
            tokenizer.advance();
            // dequeue '='
            tokenizer.advance();
            // compile expression2
            compileExpression();

            // save the value
            vmWriter.writePop(VMWriter.Segment.TEMP, 0);
            // handle array access
            vmWriter.writePop(VMWriter.Segment.POINTER, 1);
            // dequeue the value
            vmWriter.writePush(VMWriter.Segment.TEMP, 0);
            // dequeue the array
            vmWriter.writePop(VMWriter.Segment.THAT, 0);
        } else { // handle simple variable

            // dequeue '='
            tokenizer.advance();

            compileExpression();

            vmWriter.writePop(VMWriter.Segment.fromKind(kindOf(varName)), indexOf(varName));
        }
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ';') {
            throw new IllegalStateException("Let expected ';' but found " + tokenizer.symbol());
        }
        // dequeue ';'
        tokenizer.advance();
    }

    public void compileIf() throws IOException {
        if (tokenizer.keyWord() != JackTokenizer.Keyword.IF) {
            throw new IllegalStateException("If expected a keyword but found " + tokenizer.keyWord());
        }
        String l1 = "IF" + VMWriter.newLabel();
        String l2 = "IF" + VMWriter.newLabel();
        // dequeue 'if'
        //noinspection DuplicatedCode
        tokenizer.advance();
        compileExpression();

        vmWriter.writeArithmetic(VMWriter.Command.NOT);
        vmWriter.writeIf(l1);

        // dequeue '{'
        tokenizer.advance();

        compileStatements();
        vmWriter.writeGoto(l2);
        vmWriter.writeLabel(l1);

        // dequeue '}'
        tokenizer.advance();

        // check for else
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyWord() == JackTokenizer.Keyword.ELSE) {
            // dequeue 'else'
            tokenizer.advance();

            // dequeue '{'
            tokenizer.advance();

            compileStatements();

            // dequeue '}'
            tokenizer.advance();
        }
        vmWriter.writeLabel(l2);
    }

    public void compileWhile() throws IOException {
        String l1 = "WHILE" + VMWriter.newLabel();
        String l2 = "WHILE" + VMWriter.newLabel();
        vmWriter.writeLabel(l1);

        if (tokenizer.keyWord() != JackTokenizer.Keyword.WHILE) {
            throw new IllegalStateException("While expected a keyword but found " + tokenizer.keyWord());
        }
        // dequeue 'while'
        //noinspection DuplicatedCode
        tokenizer.advance();

        compileExpression();
        vmWriter.writeArithmetic(VMWriter.Command.NOT);
        vmWriter.writeIf(l2);

        // dequeue '{'
        tokenizer.advance();

        compileStatements();
        vmWriter.writeGoto(l1);
        vmWriter.writeLabel(l2);

        // dequeue '}'
        tokenizer.advance();
    }

    public void compileDo() throws IOException {
        if (tokenizer.keyWord() != JackTokenizer.Keyword.DO) {
            throw new IllegalStateException("Do expected a keyword but found " + tokenizer.keyWord());
        }
        // dequeue 'do'
        tokenizer.advance();

        compileExpression();
        vmWriter.writePop(VMWriter.Segment.TEMP, 0); // ignore the return value

        // dequeue ';'
        tokenizer.advance();
    }

    public void compileReturn() throws IOException {
        if (tokenizer.keyWord() != JackTokenizer.Keyword.RETURN) {
            throw new IllegalStateException("Return expected a keyword but found " + tokenizer.keyWord());
        }
        tokenizer.advance(); // dequeue 'return'

        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ';') {
            compileExpression();
        } else {
            vmWriter.writePush(VMWriter.Segment.CONST, 0);
        }

        vmWriter.writeReturn();

        tokenizer.advance(); // dequeue ';'
    }

    public void compileExpression() throws IOException {
        // write term
        compileTerm();

        while (tokenizer.isOp()) { // can only be bi-nary operators
            // save 'op' for later
            char op = tokenizer.symbol();
            tokenizer.advance();
            // push 'term'
            compileTerm();
            // write op
            switch (op) {
                case '+' -> vmWriter.writeArithmetic(VMWriter.Command.ADD);
                case '-' -> vmWriter.writeArithmetic(VMWriter.Command.SUB);
                case '*' -> vmWriter.writeCall("Math.multiply", 2);
                case '/' -> vmWriter.writeCall("Math.divide", 2);
                case '&' -> vmWriter.writeArithmetic(VMWriter.Command.AND);
                case '|' -> vmWriter.writeArithmetic(VMWriter.Command.OR);
                case '<' -> vmWriter.writeArithmetic(VMWriter.Command.LT);
                case '>' -> vmWriter.writeArithmetic(VMWriter.Command.GT);
                case '=' -> vmWriter.writeArithmetic(VMWriter.Command.EQ);
            }
        }
    }

    public void compileTerm() throws IOException {
        switch (tokenizer.tokenType()) {
            case INT_CONST -> {
                int intValue = tokenizer.intVal();
                vmWriter.writePush(VMWriter.Segment.CONST, intValue);
                // dequeue 'intConst'
                tokenizer.advance();
            }
            case STRING_CONST -> {
                String stringValue = tokenizer.stringVal();

                // create a new string object
                vmWriter.writePush(VMWriter.Segment.CONST, stringValue.length());
                vmWriter.writeCall("String.new", 1);
                // call String.appendChar for each character
                for (char c : stringValue.toCharArray()) {
                    vmWriter.writePush(VMWriter.Segment.CONST, c);
                    vmWriter.writeCall("String.appendChar", 2);
                }
                // dequeue the string value
                tokenizer.advance();
            }
            case KEYWORD -> {
                if (tokenizer.keyWord().isKeywordConstant()) {
                    if (tokenizer.keyWord() == JackTokenizer.Keyword.TRUE) {
                        vmWriter.writePush(VMWriter.Segment.CONST, 1);
                        vmWriter.writeArithmetic(VMWriter.Command.NEG);
                    } else if (tokenizer.keyWord() == JackTokenizer.Keyword.FALSE || tokenizer.keyWord() == JackTokenizer.Keyword.NULL) {
                        vmWriter.writePush(VMWriter.Segment.CONST, 0);
                    } else if (tokenizer.keyWord() == JackTokenizer.Keyword.THIS) {
                        vmWriter.writePush(VMWriter.Segment.POINTER, 0);
                    }
                    tokenizer.advance();
                } else {
                    throw new IllegalStateException("Term expected a keyword constant but found " + tokenizer.keyWord());
                }
            }
            case IDENTIFIER -> {
                // dequeue 'identifier'
                String identifier = tokenizer.identifier();
                tokenizer.advance();

                if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '.') {
                    // dequeue '.'
                    tokenizer.advance();
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.IDENTIFIER) {
                        throw new IllegalStateException("SubroutineCall expected an identifier but found " + tokenizer.tokenType());
                    }
                    String subroutineName = tokenizer.identifier();
                    // dequeue 'identifier'
                    tokenizer.advance();

                    int nArgs = 0;
                    if (!typeOf(identifier).equals(identifier)) {
                        vmWriter.writePush(VMWriter.Segment.fromKind(kindOf(identifier)), indexOf(identifier));
                        nArgs++;
                    }

                    nArgs += compileExpressionList();
                    vmWriter.writeCall(typeOf(identifier) + "." + subroutineName, nArgs);

                } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '[') {
                    // dequeue '['
                    tokenizer.advance();
                    vmWriter.writePush(VMWriter.Segment.fromKind(kindOf(identifier)), indexOf(identifier));
                    compileExpression();
                    vmWriter.writeArithmetic(VMWriter.Command.ADD);
                    // dequeue ']'
                    if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ']') {
                        throw new IllegalStateException("Term expected ']' but found " + tokenizer.symbol());
                    }
                    tokenizer.advance();
                    // dequeue the value
                    vmWriter.writePop(VMWriter.Segment.POINTER, 1);
                    vmWriter.writePush(VMWriter.Segment.THAT, 0);
                } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '(') {
                    vmWriter.writePush(VMWriter.Segment.POINTER, 0);
                    int n = compileExpressionList();
                    vmWriter.writeCall(className + "." + identifier, n + 1);
                } else {
                    vmWriter.writePush(VMWriter.Segment.fromKind(kindOf(identifier)), indexOf(identifier));
                }
            }
            case SYMBOL -> {
                if (tokenizer.symbol() == '(') {
                    // dequeue '('
                    tokenizer.advance();
                    compileExpression();
                    // dequeue ')'
                    tokenizer.advance();
                } else if (tokenizer.isUnaryOp()) {
                    char unaryOp = tokenizer.symbol();
                    // dequeue the operator
                    tokenizer.advance();
                    compileTerm();
                    switch (unaryOp) {
                        case '-' -> vmWriter.writeArithmetic(VMWriter.Command.NEG);
                        case '~' -> vmWriter.writeArithmetic(VMWriter.Command.NOT);
                    }
                } else {
                    throw new IllegalStateException("Term expected a symbol but found " + tokenizer.symbol());
                }
            }
        }
    }

    public int compileExpressionList() throws IOException {
        // dequeue '('
        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '(') {
            tokenizer.advance();
        }
        int numOfExpressions = 0;
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            do {
                compileExpression();
                numOfExpressions++;
                if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                    // dequeue ','
                    tokenizer.advance();
                }
            } while (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')');
        }
        // dequeue ')'
        tokenizer.advance();
        return numOfExpressions;
    }

    private SymbolTable.Kind kindOf(String name) {
        if (subroutineSymbolTable.kindOf(name) != SymbolTable.Kind.NONE) {
            return subroutineSymbolTable.kindOf(name);
        } else {
            return classSymbolTable.kindOf(name);
        }
    }

    private int indexOf(String name) {
        if (subroutineSymbolTable.kindOf(name) != SymbolTable.Kind.NONE) {
            return subroutineSymbolTable.indexOf(name);
        } else if (classSymbolTable.kindOf(name) != SymbolTable.Kind.NONE) {
            return classSymbolTable.indexOf(name);
        } else {
            throw new IllegalStateException("Identifier not found: " + name);
        }
    }

    private String typeOf(String name) {
        if (subroutineSymbolTable.kindOf(name) != SymbolTable.Kind.NONE) {
            return subroutineSymbolTable.typeOf(name);
        } else {
            return classSymbolTable.typeOf(name);
        }
    }

}