package ch.usi.methodtest;

import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.util.*;
import java.util.function.Function;

import static ch.usi.gassert.util.FileUtils.readContentFile;

public class MethodTestsProcessor {

    private static final XStream xstream = new XStream();

    static {
        xstream.allowTypesByWildcard(new String[] {
                "**"
        });
    }

    static class DefaultTestNameProvider {
        int index = 0;
        String getTestName() {
            return "test" + index++;
        }
    }

    public static Function<String, String> makeTestNameProvider() {
        final DefaultTestNameProvider testNameProvider = new DefaultTestNameProvider();
        return oldName -> testNameProvider.getTestName();
    }

    public static void processInputFiles(final List<File> inputsFiles, boolean deleteCorrupt, boolean deleteDuplicates) {
        final Set<String> visitedMethods = new HashSet<>();
        for (File inputsFile : inputsFiles) {
            try {
                String serializedMethodTest = readContentFile(inputsFile);
                if (visitedMethods.add(serializedMethodTest)) {
                    try {
                        MethodTest methodTest = (MethodTest) xstream.fromXML(serializedMethodTest);
                    } catch (Exception e) {
                        System.err.println("Error deserializing method inputs: " + e.getClass().getSimpleName());
                        System.err.println(inputsFile.getName());
                        if (deleteCorrupt) {
                            boolean whoCares = inputsFile.delete();
                        }
                    }
                } else {
                    System.err.println("Duplicate method inputs");
                    System.err.println(inputsFile.getName());
                    if (deleteDuplicates) {
                        boolean whoCares = inputsFile.delete();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error opening inputs file: " + e.getClass().getSimpleName());
                System.err.println(inputsFile.getName());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Wrong number of parameters: 1 argument expected, got " + args.length);
            System.err.println("Serialized inputs directory");
            System.exit(1);
        }

        final String serializedInputsFilename = args[0];
        final File inFile = new File(serializedInputsFilename);
        final List<File> inputsFiles;
        if (inFile.isDirectory()) {
            inputsFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(inFile.listFiles(
                    (file, name) -> name.endsWith(MethodTest.EXTENSION)))));
        } else {
            throw new RuntimeException(serializedInputsFilename + " is not a directory!");
        }
        processInputFiles(inputsFiles, true, true);
    }

}
