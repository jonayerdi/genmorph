package ch.usi.oasis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.evosuite.Properties;
import org.evosuite.TestSuiteGenerator;
import org.evosuite.coverage.mutation.StrongMutationTestFitness;
import org.evosuite.coverage.mutation.ValuePair;
import org.evosuite.junit.writer.TestSuiteWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

//TO-DO:
//compilation of the transformed source code
//EvoSuite Mutations extended
//Copy Original Version

public class OASIs {

    private String srcLocation;
    private String binLocation;
    private String className;
    private String methodName;
    private String testFileLocation;
    private CompilationUnit initialCu;
    private String subjectRoot;
    private Map<String, HashMap<String, HashMap<String, ValuePair>>> valuePairs;


    public OASIs(final String srcLocation, final String binLocation, final String className, final String methodName,
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

    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    public static String joinClasspaths(String... classpaths) {
        return joinClasspaths(Arrays.stream(classpaths));
    }

    public static String joinClasspaths(Stream<String> classpaths) {
        final StringBuilder sb = new StringBuilder();
        classpaths.forEach(part -> sb.append(part).append(PATH_SEPARATOR));
        return sb.length() == 0 ? sb.toString() : sb.substring(0, sb.length() - PATH_SEPARATOR.length());
    }

    public static List<String> loadAssertions(final File file) throws IOException {
        final List<String> assertions = new ArrayList<>(16);
        try (final Scanner sc = new Scanner(file)) {
            while(sc.hasNextLine()) {
                final String line = sc.nextLine().trim();
                if (!line.isEmpty()) {
                    assertions.add(line);
                }
            }
        }
        return assertions;
    }

    public static void main(final String[] args) throws IOException {
        final Iterator<String> argsIter = Arrays.stream(args).iterator();
        // Args
        final String subjectRoot = argsIter.next();
        final String className = argsIter.next();
        final String methodName = argsIter.next();
        final List<String> irs = loadAssertions(new File(argsIter.next()));
        final List<String> ors = loadAssertions(new File(argsIter.next()));
        final String testFileLocation = argsIter.next();
        final int timeBudget = Integer.parseInt(argsIter.next());
        final boolean debug = Boolean.parseBoolean(argsIter.next());
        // Paths
        final String harnessMethodName = methodName + "_mr_test_harness";
        final String OASIsSourceFiles = subjectRoot + "/oasis_src/";
        final String classpath = subjectRoot + "/target/classes/";
        final File OASIsSourceFile = new File(OASIs.getFullName(OASIsSourceFiles, className));
        final String OASIsClasspath = joinClasspaths(OASIsSourceFiles, classpath);
        // Debugging
        final PrintStream stdout = System.out;
        final PrintStream stderr = System.err;
        final PrintStream tmpout;
        final PrintStream tmperr;
        if (debug) {
            try {
                tmpout = new PrintStream(testFileLocation + "oasis_out.txt");
                tmperr = new PrintStream(testFileLocation + "oasis_err.txt");
            } catch (Exception e) {
                throw new RuntimeException("Error creating oasis output files", e);
            }
        } else {
            tmpout = new PrintStream(new OutputStream() { public void write(final int b) {} });
            tmperr = new PrintStream(new OutputStream() { public void write(final int b) {} });
        }
        System.setOut(tmpout);
        System.setErr(tmperr);
        // OASIs
        try {
            final String lineList = OASIs.transformForFP(OASIsSourceFile.getPath(), methodName, irs, ors);
            final OASIs oasis = new OASIs(OASIsSourceFiles, OASIsClasspath, className, harnessMethodName, testFileLocation, subjectRoot);
            final boolean foundFPs = oasis.detectFP(timeBudget, lineList);
        } finally {
            // Debugging
            System.setOut(stdout);
            System.setErr(stderr);
            if (tmpout != stdout) {
                tmpout.close();
            }
            if (tmperr != stderr) {
                tmperr.close();
            }
            // Force-kill Evosuite subprocesses which may be stuck
            System.exit(0);
        }
    }

    public static void  main2(final String[] args) {
//        final int argsSize = 13;
//        if (args.length != argsSize) {
//            System.out.println(args.length + "ERROR we should have 13 arguments!");
//            System.exit(-1);
//        }
        final String subjectRoot = args[0];
        final String srcFileLocation = args[1];
        final String binFileLocation = args[2];
        final String methodName = args[3];
        final String className = args[4];
        final String testFileLocation = args[5];
        final int fpBudget = Integer.parseInt(args[6]);
        final String lineList = args[7];
        final boolean debug = Boolean.parseBoolean(args[8]);

        //final int fnBudget = Integer.parseInt(args[7]);
        final int fnBudget = 0;
        //final String mode = args[8];
        final String mode = "FP";

//        final String subjectRoot = "/Users/usi/Documents/GAssert/MRs/commons-math3-3.6.1-src-1/";
//        final String srcFileLocation = subjectRoot + "/src/main/java/";
//        final String binFileLocation = subjectRoot + "/src/main/java/";
//        final String methodName = "sin";
//        final String className = "org.apache.commons.math3.util.FastMath";
//        final String testFileLocation = "/Users/usi/Documents/GAssert/GAssert/min-gassert-for-oasis/";
//        final int fpBudget = 240;
//        final String lineList = "3851,3857,3863";
//        final boolean debug = true;

        final OASIs oasis = new OASIs(srcFileLocation, binFileLocation, className,
                methodName, testFileLocation, subjectRoot);
        final PrintStream stdout = System.out;
        if (debug) {
            final String errFile = testFileLocation + "evo_err.txt";
            final String outFile = testFileLocation + "evo_out.txt";
            oasis.writeOutputToFile(errFile, outFile);
        } else {
            System.setOut(new PrintStream(new OutputStream() {
                public void write(final int b) {
                    //DO NOTHING
                }
            }));
            // System.setErr(new PrintStream(new OutputStream() {
            //    public void write(final int b) {
            //        //DO NOTHING
            //     }
            // }));
        }
        // I need to know if is FP or FN to avoid collisions in tests names
        System.setProperty("mode", mode);
        if (mode.equals("FP")) {
            final boolean result = oasis.detectFP(fpBudget, lineList);
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

    //private boolean detectFP(final int fpBudget, final String inputRelFile, final String outputRelFile, final String execLogFile) {
    public boolean detectFP(final int fpBudget, String lineList) {
        System.setProperty("oasisFPresult", "undefined");
        final String fullName = getFullName(srcLocation, className);
        //final String lineList = transformForFP(fullName, methodName, inputRelList, outputRelList, testFileLocation + className + "_harn.java", execLogFile );
        System.out.println("lineList:" + lineList);
        System.out.println("fullName:" + fullName);
        Properties.SEARCH_BUDGET = fpBudget;
        Properties.TEST_DIR = testFileLocation;
        Properties.REPORT_DIR = testFileLocation;
        Properties.LINE_LIST = lineList;
        Properties.CRITERION = new Properties.Criterion[]{Properties.Criterion.BRANCH};
        Properties.ASSERTIONS = false;
        Properties.MINIMIZE = true;
        Properties.STRATEGY = Properties.STRATEGY.EVOSUITE;
        //Properties.STRATEGY = Properties.STRATEGY.MOSUITE;
        Properties.TARGET_METHOD_PREFIX = methodName;
        Properties.CP = binLocation;
        System.out.println("className:" + className);
        System.out.println("binLocation:" + binLocation);
        System.out.println("lineList:" + lineList);
        Properties.TARGET_CLASS = className;
        Properties.NO_RUNTIME_DEPENDENCY = true;
        //Properties.JUNIT_SUFFIX = "_ESTest.java";
        compileJavaCode();
        System.out.println("compiled");
        final TestSuiteGenerator generator = new TestSuiteGenerator();
        generator.generateTestSuite();
        //rewriteFileToInitial(fullName);
        return TestSuiteWriter.testsGenerated;
    }

    private boolean detectFN(final int fnBudget) {
        //try {
        Properties.LINE_LIST = "";
        Properties.STRATEGY = Properties.STRATEGY.EVOSUITE;
        Properties.CRITERION = null;
        Properties.SEARCH_BUDGET = fnBudget;
        Properties.TEST_DIR = testFileLocation;
        Properties.ASSERTIONS = false;
        Properties.TARGET_METHOD_PREFIX = "gcd(II)I";
        //Properties.TARGET_METHOD_PREFIX = "gcd_mr_test_harness";
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
        compileJavaCode();
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

    public static String getFullName(final String fileLocation, final String className) {
        return fileLocation + "/" + className.replace(".", "/") + ".java";
    }

    //private static String transformForFP(final String fileLocation, final String methodName, final String inputRelFile, final String outputRelFile,
    //                                     final String harnessLocation, final String execLogFile) {
    public static String transformForFP(final String fileLocation, final String methodName, final List<String> inputRelList, final List<String> outputRelList) {
        try {
            final FalsePositiveTransformation rawi = new FalsePositiveTransformation(fileLocation, methodName, inputRelList, outputRelList);
            rawi.createHarnessMethod();
            rawi.rewriteFile();

            //rawi.writeHarnessedFile(harnessLocation);
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
            String outputRelationFile = "/Users/usi/Downloads/output_rel.txt";
            String inputRelationFile = "/Users/usi/Downloads/input_rel.txt";
            //final FalseNegativeTransformation fnt = new FalseNegativeTransformation(location, methodName);
            final FalseNegativeTransformationMR fnt = new FalseNegativeTransformationMR(location, methodName, inputRelationFile, outputRelationFile);
            fnt.analyseAssertions();
            fnt.addMainHashMap();
            //fnt.createHarnessMethod();
            fnt.rewriteJavaFile(location);

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
        //final ProcessBuilder pb = new ProcessBuilder("./gradlew", "build", "-x", "test");

        final ProcessBuilder pb = new ProcessBuilder("javac", "-cp", binLocation, className.replace(".", "/") + ".java");
        //pb.directory(new File(subjectRoot));
        pb.directory(new File(srcLocation));
        try {
            p = pb.start();
            p.waitFor();
//            final String stderr = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
//            final String stdout = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
//            System.out.println("stderr");
//            System.out.println(stderr);
//            System.out.println("stdout");
//            System.out.println(stdout);
        } catch (final Exception e) {
            throw new RuntimeException("Error running javac process: " + pb.command(), e);
        }
//        Runtime rt = Runtime.getRuntime();
//        String fullName = getFullName(srcLocation, className);
//        File file = new File(fullName.replace(".java", ".bin"));
//        file.delete();
//
//        try {
//            Process pr = rt.exec(subjectRoot + " gradlew clean;" + subjectRoot + " gradlew jar");
//            pr.waitFor();
//            String stderr = IOUtils.toString(pr.getErrorStream(), Charset.defaultCharset());
//            String stdout = IOUtils.toString(pr.getInputStream(), Charset.defaultCharset());
//            System.out.println(stderr);
//            System.out.println(stdout);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

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
