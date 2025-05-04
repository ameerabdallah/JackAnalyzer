import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Map<Kind, Integer> kindCount;
    private final Map<String, Symbol> symbolTable;

    public SymbolTable() {
        // Initialize the symbol table
        this.symbolTable = new HashMap<>();
        this.kindCount = new HashMap<>();
        // Initialize the kind count
        for (Kind kind : Kind.values()) {
            this.kindCount.put(kind, 0);
        }
    }

    public void reset() {
        // Clear the symbol table
        this.symbolTable.clear();
        // Reset the kind count
        for (Kind kind : Kind.values()) {
            this.kindCount.put(kind, 0);
        }
    }

    public void define(String name, String type, Kind kind) {
        Symbol symbol = new Symbol(name, type, kind, kindCount.get(kind));
        kindCount.put(kind, kindCount.get(kind) + 1);

        // Add a new identifier to the symbol table
        symbolTable.put(name, symbol);
    }

    public void define(String name, String type, JackTokenizer.Keyword kindKeyword) {
        // Define a new identifier with the given name, type, and kind
        define(name, type, Kind.fromKeyword(kindKeyword));
    }

    public int varCount(Kind kind) {
        // Return the number of variables of the given kind
        return kindCount.get(kind);
    }

    public Kind kindOf(String name) {
        // Return the kind of the identifier
        Symbol symbol = symbolTable.get(name);
        return symbol != null ? symbol.getKind() : Kind.NONE;
    }

    public String typeOf(String name) {
        // Return the type of the identifier
        Symbol symbol = symbolTable.get(name);
        return symbol != null ? symbol.getType() : name;
    }

    public int indexOf(String name) {
        // Return the index of the identifier
        Symbol symbol = symbolTable.get(name);
        return symbol != null ? symbol.getIndex() : -1;
    }

    public enum Kind {
        STATIC, FIELD, ARG, VAR, NONE;

        public String toString() {
            return switch (this) {
                case STATIC -> "static";
                case FIELD -> "field";
                case ARG -> "argument";
                case VAR -> "local";
                case NONE -> null;
            };
        }

        public static Kind fromKeyword(JackTokenizer.Keyword keyword) {
            return switch (keyword) {
                case STATIC -> STATIC;
                case FIELD -> FIELD;
                case VAR -> VAR;
                default -> NONE;
            };
        }
    }

    public static class Symbol {
        private final String name;
        private final Kind kind;
        private final String type;
        private final int index;

        public Symbol(String name, String type, Kind kind, int index) {
            this.name = name;
            this.kind = kind;
            this.type = type;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public Kind getKind() {
            return kind;
        }

        public String getType() {
            return type;
        }

        public int getIndex() {
            return index;
        }
    }
}
