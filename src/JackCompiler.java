import java.io.*;

public class JackCompiler {
    private static void usage() {
        System.out.printf("Usage: java %s <inputfile[.jack] | directory>", JackCompiler.class.getName());
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            usage();
            return;
        }

        // check if it is a directory
        File file = new File(args[0]);

        String[] inputFileNames;
        if (file.isDirectory()) {
            inputFileNames = file.list((dir, name) -> name.endsWith(".jack"));
            if (inputFileNames == null || inputFileNames.length == 0) {
                System.out.println("No .jack files found in the directory.");
                usage();
                return;
            }
        } else if (file.isFile() && file.getName().endsWith(".jack")) {
            inputFileNames = new String[]{file.getName()};
        } else {
            usage();
            return; // This line should not be reached due to the usage() method
        }


        for (String inputFileName : inputFileNames) {
            String inputFilePath = file.isDirectory() ?
                    file.getAbsolutePath() + File.separator + inputFileName : file.getAbsolutePath();
            // Create the output directory if it doesn't exist
            File outputDir = new File(file.getAbsolutePath() + File.separator + "output");
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    System.out.println("Failed to create output directory.");
                    return;
                }
            }
            String outputFileName = outputDir + File.separator + inputFileName.substring(0, inputFileName.length() - 5) + ".vm";

            try (InputStream inputStream = new FileInputStream(inputFilePath)) {
                try (OutputStream outputStream = new FileOutputStream(outputFileName)) {
                    new CompilationEngine(inputStream, outputStream).compileClass();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}