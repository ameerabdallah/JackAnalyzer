import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class VMWriter implements AutoCloseable{
    private final OutputStreamWriter out;
    private static int LABEL_COUNT = 0;

    public VMWriter(OutputStream out) {
        this.out = new OutputStreamWriter(out);
    }

    public void writePush(Segment segment, int index) throws IOException {
        out.write("push " + segment.toString() + " " + index + "\n");
        out.flush();
    }

    public void writePop(Segment segment, int index) throws IOException {
        out.write("pop " + segment.toString() + " " + index + "\n");
        out.flush();
    }

    public void writeArithmetic(Command command) throws IOException {
        out.write(command.toString() + "\n");
        out.flush();
    }

    public void writeLabel(String label) throws IOException {
        out.write("label " + label + "\n");
        out.flush();
    }

    public void writeGoto(String label) throws IOException {
        out.write("goto " + label + "\n");
        out.flush();
    }

    public void writeIf(String label) throws IOException {
        out.write("if-goto " + label + "\n");
        out.flush();
    }

    public void writeCall(String name, int nArgs) throws IOException {
        out.write("call " + name + " " + nArgs + "\n");
        out.flush();
    }

    public void writeFunction(String name, int nVars) throws IOException {
        out.write("function " + name + " " + nVars + "\n");
        out.flush();
    }

    public void writeReturn() throws IOException {
        out.write("return\n");
        out.flush();
    }

    public enum Command {
        ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT;
        public String toString() {
            return switch (this) {
                case ADD -> "add";
                case SUB -> "sub";
                case NEG -> "neg";
                case EQ -> "eq";
                case GT -> "gt";
                case LT -> "lt";
                case AND -> "and";
                case OR -> "or";
                case NOT -> "not";
            };
        }
    }

    public enum Segment {
        CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP;
        public String toString() {
            return switch (this) {
                case CONST -> "constant";
                case ARG -> "argument";
                case LOCAL -> "local";
                case STATIC -> "static";
                case THIS -> "this";
                case THAT -> "that";
                case POINTER -> "pointer";
                case TEMP -> "temp";
            };
        }

        public static Segment fromKind(SymbolTable.Kind kind) {
            return switch (kind) {
                case STATIC -> STATIC;
                case FIELD -> THIS;
                case ARG -> ARG;
                case VAR -> LOCAL;
                default -> throw new IllegalArgumentException("Invalid kind: " + kind);
            };
        }
    }

    public static String newLabel() {
        return "LABEL" + LABEL_COUNT++;
    }

    @Override
    public void close() throws Exception {
        out.close();
    }
}
