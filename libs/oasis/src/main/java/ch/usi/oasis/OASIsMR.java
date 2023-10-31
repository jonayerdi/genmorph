package ch.usi.oasis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.evosuite.Properties;
import org.evosuite.TestSuiteGenerator;
import org.evosuite.coverage.mutation.StrongMutationTestFitness;
import org.evosuite.coverage.mutation.ValuePair;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.shaded.org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//TO-DO:
//compilation of the transformed source code
//EvoSuite Mutations extended
//Copy Original Version

public class OASIsMR {

    private String srcLocation;
    private String binLocation;
    private String className;
    private String methodName;
    private String testFileLocation;
    private CompilationUnit initialCu;
    private String subjectRoot;
    private Map<String, HashMap<String, HashMap<String, ValuePair>>> valuePairs;

    public OASIsMR(final String srcLocation, final String binLocation, final String className, final String methodName,
                   final String testFileLocation, final String subjectRoot) {
        this.srcLocation = srcLocation;
        this.binLocation = binLocation;
        this.className = className;
        this.methodName = methodName;
        this.testFileLocation = testFileLocation;
        this.subjectRoot = subjectRoot;
        final String fullName = getFullName(srcLocation, className);
        initialCu = getInitialCu(fullName);
    }


    public static void main(final String[] args) {
        final int argsSize = 10;
//        if (args.length != argsSize) {
//            System.out.println(args.length + "ERROR we should have 10 arguments!");
//            System.exit(-1);
//        }
//        final String subjectRoot = args[0];
//        final String srcFileLocation = args[1];
//        final String binFileLocation = args[2];
//        final String methodName = args[3];
//        final String className = args[4];
//        final String testFileLocation = args[5];
//        final int fpBudget = Integer.parseInt(args[6]);
//        final int fnBudget = Integer.parseInt(args[7]);
//        final String mode = args[8];
//        final boolean debug = Boolean.parseBoolean(args[9]);

        final String subjectRoot = "/Users/usi/Documents/JavaSetup/";
        final String srcFileLocation = "/Users/usi/Documents/JavaSetup/src/main/java/";
        final String binFileLocation = "/Users/usi/Documents/JavaSetup/build/classes/java/main/";
        final String methodName = "calculate_mr_test_harness";
        final String className = "ch.usi.fep.StringExampleClass";
        final String testFileLocation = "/Users/usi/Documents/GAssert/GAssert/min-gassert-for-oasis/";
        final int fpBudget = 60;
        final int fnBudget = 225;
        final String mode = "FP";
        final boolean debug = false;

        final OASIsMR oasis = new OASIsMR(srcFileLocation, binFileLocation, className,
                methodName, testFileLocation, subjectRoot);
        final PrintStream stdout = System.out;
//        if (debug) {
//            final String errFile = "evo_err.txt";
//            final String outFile = "evo_out.txt";
//            oasis.writeOutputToFile(errFile, outFile);
//        } else {
//            System.setOut(new PrintStream(new OutputStream() {
//                public void write(final int b) {
//                    //DO NOTHING
//                }
//            }));
//            // System.setErr(new PrintStream(new OutputStream() {
//            //    public void write(final int b) {
//            //        //DO NOTHING
//            //     }
//            // }));
//        }
        // I need to know if is FP or FN to avoid collisions in tests names
        System.setProperty("mode", mode);
        if (mode.equals("FP")) {
            final boolean result = oasis.detectFP(fpBudget);
            System.setOut(stdout);
            System.out.println(result);
        } else if (mode.equals("FN")) {
            final boolean result = oasis.detectFN(fnBudget);
            System.setOut(stdout);
            System.out.println(result);
        } else {
            new RuntimeException("something is wrong why no mode?");
        }
    }


    private boolean detectFP(final int fpBudget) {
        System.setProperty("oasisFPresult", "undefined");
        final String fullName = getFullName(srcLocation, className);
        final String lineList = transformForFP(fullName, methodName);
        System.out.println("lineList:" + lineList);
        System.out.println("fullName:" + fullName);
        Properties.SEARCH_BUDGET = fpBudget;
        Properties.TEST_DIR = testFileLocation;
        Properties.LINE_LIST = lineList;
        Properties.CRITERION = new Properties.Criterion[]{Properties.Criterion.BRANCH};
        Properties.ASSERTIONS = false;
        Properties.STRATEGY = Properties.STRATEGY.ONEBRANCH;
        Properties.TARGET_METHOD_PREFIX = methodName;
        Properties.CP = binLocation;
        System.out.println("className:" + className);
        System.out.println("binLocation:" + binLocation);
        Properties.TARGET_CLASS = className;
        Properties.NO_RUNTIME_DEPENDENCY = true;
        //Properties.JUNIT_SUFFIX = "_ESTest.java";
        compileJavaCode();
        System.out.println("compiled");
        final TestSuiteGenerator generator = new TestSuiteGenerator();
        generator.generateTestSuite();
        rewriteFileToInitial(fullName);
        return TestSuiteWriter.testsGenerated;

    }

    private boolean detectFN(final int fnBudget) {
        //try {
        Properties.LINE_LIST = "";
        Properties.STRATEGY = Properties.STRATEGY.EVOSUITE;
        Properties.CRITERION = null;
        Properties.SEARCH_BUDGET = fnBudget;
        Properties.TEST_DIR = testFileLocation;
        Properties.REPORT_DIR = testFileLocation;
        Properties.ASSERTIONS = false;
        Properties.TARGET_METHOD_PREFIX = methodName;
        Properties.CP = binLocation;
        Properties.TARGET_CLASS = className;
        final String fullName = getFullName(srcLocation, className);
        final String[] lists = transformForFN(fullName, methodName).split("/");
        Properties.CRITERION = new Properties.Criterion[]{Properties.Criterion.STRONGMUTATION};
        Properties.MINIMIZE = true;
        Properties.MUTATED_LINE_LIST = lists[1];
        Properties.MUTATION_REPLACEMENT_LIST = lists[0];
        Properties.TEST_ARCHIVE = false;
        Properties.TEST_COMMENTS = true;
        Properties.LOG_LEVEL = "debug";
        compileJavaCode();
        final TestSuiteGenerator generator = new TestSuiteGenerator();
        generator.generateTestSuite();
        rewriteFileToInitial(fullName);
        //compileJavaCode();
        valuePairs = StrongMutationTestFitness.valuePairs;
        return !StrongMutationTestFitness.valuePairs.isEmpty();

        // } catch (final Exception | Error e) {
        //     System.err.println("SUBJECT CANNOT COMPILE!!!!!!:   " + e.getMessage());
        //     return false;
        // }
    }

    private void writeOutputToFile(final String errFile, final String outFile) {
        try {
            System.setOut(new PrintStream(new File(outFile)));
            System.setErr(new PrintStream(new File(errFile)));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static String getFullName(final String fileLocation, final String className) {
        return fileLocation + "/" + className.replace(".", "/") + ".java";
    }

    private static String transformForFP(final String fileLocation, final String methodName) {
        try {
            final FalsePositiveTransformation rawi = new FalsePositiveTransformation(fileLocation, methodName, new ArrayList<String>(), new ArrayList<String>());
            rawi.findAssertions(1);
            rawi.rewriteFile();
            rawi.getAssertLines();
            String lineList = rawi.getLineList().toString();
            lineList = lineList.replace("[", "");
            lineList = lineList.replace("]", "");
            lineList = lineList.replace(" ", "");
            return lineList;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String transformForFN(final String fileLocation, final String methodName) {
        try {
            final String location = fileLocation;
            String outputRelationFile = "/Users/usi/Downloads/demo.MyClass$powInt$0@MR0.jor.txt";
            String inputRelationFile = "/Users/usi/Downloads/demo.MyClass$powInt$0@MR0.jir.txt";
            final FalseNegativeTransformation fnt = new FalseNegativeTransformation(location, methodName);
            fnt.analyseAssertions();
            fnt.addMainHashMap();
            //fnt.createHarnessMethod();
            fnt.rewriteJavaFile(location);

            //get line numbers of the harness method

            fnt.setBeforeLines(fnt.getInitialLineNumbers(fnt.getInitial_cu()));
            final String instrLines = fnt.getInstrumentationLines(location);
            final HashMap<Integer, Integer> replaceLines = new HashMap<Integer, Integer>();

            for (final Node node : fnt.getBeforeLines().keySet()) {
                if (fnt.getAfterLines().containsKey(node)) {
                    replaceLines.put(fnt.getBeforeLines().get(node), fnt.getAfterLines().get(node));
                }
            }

            String replaceStr = replaceLines.toString();
            replaceStr = replaceStr.replace("{", "").replace("}", "").replace(" ", "").replace("=", ":");
            System.out.println("replaceStr:" + replaceStr);
            System.out.println("instrLines:" + instrLines);
            return replaceStr + "/" + instrLines;
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private CompilationUnit getInitialCu(final String fileLocation) {
        final FileInputStream in;
        CompilationUnit cu = null;
        try {
            in = new FileInputStream(fileLocation);
            cu = JavaParser.parse(in);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
        return cu;
    }

    private void compileJavaCode() {
        Process p = null;
        final ProcessBuilder pb = new ProcessBuilder("./gradlew", "build", "-x", "test");
        pb.directory(new File(subjectRoot));
        try {
            p = pb.start();
            final String stderr = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
            final String stdout = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
            System.out.println("stderr");
            System.out.println(stderr);
            System.out.println("stdout");
            System.out.println(stdout);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /*Runtime rt = Runtime.getRuntime();
        String fullName = getFullName(srcLocation, className);
        File file = new File(fullName.replace(".java", ".bin"));
        file.delete();

        try {
            Process pr = rt.exec(subjecRoot + " gradlew clean;" + subjecRoot + " gradlew jar");
            pr.waitFor();
            String stderr = IOUtils.toString(pr.getErrorStream(), Charset.defaultCharset());
            String stdout = IOUtils.toString(pr.getInputStream(), Charset.defaultCharset());
            System.out.println(stderr);
            System.out.println(stdout);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    }

    private void rewriteFileToInitial(final String fileLocation) {
        final File file = new File(fileLocation);
        try {
            final FileWriter fw = new FileWriter(file);
            fw.write(initialCu.toString());
            System.out.println(initialCu.toString());
            fw.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void printValuePairs() {
        for (final String key : valuePairs.keySet()) {
            for (final String key1 : valuePairs.get(key).keySet()) {
                for (final String key2 : valuePairs.get(key).get(key1).keySet()) {
                    final ValuePair valuePair = valuePairs.get(key).get(key1).get(key2);
                    System.out.println("keys:" + key + " " + key1 + " " + key2 + " " + valuePair.getOriginalValue() +
                            " " + valuePair.getMutatedValue());
                    System.out.println("--------------------------------------");
                }
            }
        }
    }

    public Map<String, HashMap<String, HashMap<String, ValuePair>>> getValuePairs() {
        return valuePairs;
    }

    public void setValuePairs(final Map<String, HashMap<String, HashMap<String, ValuePair>>> valuePairs) {
        this.valuePairs = valuePairs;
    }

}
