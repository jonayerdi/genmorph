package ch.usi.methodtest;

import ch.usi.gassert.util.FileUtils;
import org.mu.testcase.metamorphic.MRInfo;
import org.mu.testcase.metamorphic.MRInfoDB;

import com.github.javaparser.utils.StringEscapeUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.usi.gassert.util.Assert.assertAlways;
import static ch.usi.gassert.util.FileUtils.SEPARATORS;
import static ch.usi.gassert.util.FileUtils.escapeNonAlphanum;

/**
 * Generate a test suite for evaluating MRs with PITest.
 */
public abstract class PITestGenerator {

    public static String valueLiteral(final String canonicalName, final Object value) {
        if (value == null) {
            return "null";
        } else if(value instanceof Long) {
            return value.toString() + "L";
        } else if (value instanceof Float) {
            final Float v = (Float) value;
            if (v.isNaN()) {
                return "Float.NaN";
            } else if (v.isInfinite()) {
                return v > 0.0f ? "Float.POSITIVE_INFINITY" : "Float.NEGATIVE_INFINITY";
            }
        } else if (value instanceof Double) {
            final Double v = (Double) value;
            if (v.isNaN()) {
                return "Double.NaN";
            } else if (v.isInfinite()) {
                return v > 0.0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
            }
        } else if (value instanceof String) {
            return "\"" + StringEscapeUtils.escapeJava((String) value) + "\"";
        } else if(value instanceof Character) {
            return "'" + StringEscapeUtils.escapeJava(((Character) value).toString()) + "'";
        } else if (value.getClass().isArray()) {
            assertAlways(
                canonicalName.endsWith("[]"),
                "Invalid Array class canonical name: \"" + canonicalName + "\""
            );
            final String elementName = canonicalName.substring(0, canonicalName.length() - 2);
            final int length = Array.getLength(value);
            if (length == 0) {
                return "new " + canonicalName + " {}";
            }
            return "new " + canonicalName + " {\n\t\t" 
                + IntStream.range(0, length)
                    .mapToObj(i -> valueLiteral(elementName, Array.get(value, i)))
                    .collect(Collectors.joining(",\n\t\t"))
                + "\n\t\t}";
        } else if (value instanceof List) {
            final List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "new " + canonicalName + "()";
            }
            return "new " + canonicalName + "(java.util.Arrays.asList(\n\t\t"
                + list.stream()
                    .map(o -> valueLiteral(o != null ? o.getClass().getCanonicalName() : null, o))
                    .collect(Collectors.joining(",\n\t\t"))
                + "\n\t\t))";
        }
        return value.toString();
    }

    public static String loadMethodTestVariables(MethodTest methodTest, String prefix, String suffix) {
        // FIXME: This should be done by loading the MethodTest in the generated code,
        //  valueLiteral(param.value) works only for null and certain classes
        final StringBuilder sb = new StringBuilder();
        for (final MethodParameter param : methodTest.methodParameters) {
            final String declarationClass = param.clazz.getCanonicalName();
            final String valueClass = param.value != null ? param.value.getClass().getCanonicalName() : null;
            sb.append("\t\t").append(declarationClass).append(" ").append(prefix).append(param.name).append(suffix)
                    .append(" = ").append(valueLiteral(valueClass, param.value)).append(";\n");
        }
        return sb.substring(0, sb.length() - 1); // Strip trailing newline
    }

    public static String callMethod(MethodTest methodTest, String prefix, String suffix) throws ClassNotFoundException, NoSuchMethodException {
        final StringBuilder sb = new StringBuilder("\t\t");
        final Class<?>[] parameterClasses = Arrays.stream(methodTest.methodParameters)
                .skip(1)
                .map(p -> p.clazz)
                .toArray(Class[]::new);
        final MethodParameter thiz = methodTest.methodParameters[0];
        final Class<?> returnVarType = Class.forName(thiz.clazz.getName())
                .getDeclaredMethod(methodTest.methodName, parameterClasses)
                .getReturnType();
        if (!void.class.equals(returnVarType)) {
            final String returnVarTypeName = returnVarType.getName();
            final String returnVarName = prefix + "return" + suffix;
            sb.append(returnVarTypeName).append(" ").append(returnVarName).append(" = ");
        }
        if (thiz.value == null) {
            sb.append(thiz.clazz.getName());
        } else {
            sb.append(prefix).append(thiz.name).append(suffix);
        }
        sb.append(".").append(methodTest.methodName).append("(");
        for (int i = 1 ; i < methodTest.methodParameters.length ; ++i) {
            sb.append(prefix).append(methodTest.methodParameters[i].name).append(suffix);
            if (i < methodTest.methodParameters.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(");");
        return sb.toString();
    }

    public static String generateAssertionCheck(String outputRelation) {
        return "\t\torg.junit.Assert.assertTrue(" + outputRelation + ");";
        //return "\t\tif (!(" + outputRelation + ")) { org.junit.Assert.fail(); }";
    }

    public static String generateMethodBody(MethodTest source, MethodTest followup, String outputRelation) throws ClassNotFoundException, NoSuchMethodException {
        return loadMethodTestVariables(source, "o_", "_s") + "\n"
                + loadMethodTestVariables(followup, "o_", "_f") + "\n"
                + callMethod(source, "o_", "_s") + "\n"
                + callMethod(followup, "o_" , "_f") + "\n"
                + loadMethodTestVariables(source, "i_", "_s") + "\n"
                + loadMethodTestVariables(followup, "i_", "_f") + "\n"
                + generateAssertionCheck(outputRelation);
    }

    public static String generateTestMethod(String methodName, MethodTest source, MethodTest followup, String outputRelation) throws ClassNotFoundException, NoSuchMethodException {
        final String methodBody = generateMethodBody(source, followup, outputRelation);
        return "\t@Test\n\tpublic void " + methodName + "() {\n" + methodBody + "\n\t}\n";
    }

    public static String getMethodTestFilename(Path path, String sutName, String testId) {
        return path.resolve(sutName + SEPARATORS[0] + testId + MethodTest.EXTENSION).toString();
    }

    public static String generateTestMethods(String sutName, String outputRelation, MRInfoDB mrinfos, Path sourcesPath, Path followupsPath) throws IOException, ClassNotFoundException, NoSuchMethodException {
        final StringBuilder methods = new StringBuilder();
        for (final MRInfo mrinfo : mrinfos.getMRInfos()) {
            final String methodName = escapeNonAlphanum(mrinfo.followup, '_');
            final MethodTest source = MethodTest.fromXML(new FileReader(getMethodTestFilename(sourcesPath, sutName, mrinfo.source)));
            final MethodTest followup = MethodTest.fromXML(new FileReader(getMethodTestFilename(followupsPath, sutName, mrinfo.followup)));
            methods.append(generateTestMethod(methodName, source, followup, outputRelation)).append("\n");
        }
        return methods.toString();
    }

    public static String generateTestSuite(String sutName, String outputRelation, MRInfoDB mrinfos, Path sourcesPath, Path followupsPath, String testSuiteClass) throws IOException, ClassNotFoundException, NoSuchMethodException {
        final String testMethods = generateTestMethods(sutName, outputRelation, mrinfos, sourcesPath, followupsPath);
        return "import org.junit.Test;\n\npublic class " + testSuiteClass + " {\n\n" + testMethods + "}\n";
    }

    public static void writeTestSuite(String sutName, String outputRelation, MRInfoDB mrinfos, Path sourcesPath, Path followupsPath, File outputFile) throws IOException, ClassNotFoundException, NoSuchMethodException {
        final String testSuiteFilename = outputFile.getName();
        assertAlways(testSuiteFilename.endsWith(".java"), "Invalid Java source filename: " + testSuiteFilename);
        assertAlways(outputFile.getParentFile().exists() || outputFile.getParentFile().mkdirs(), "Couldn't generate directory for " + outputFile);
        final String testSuiteClass = testSuiteFilename.substring(0, testSuiteFilename.length() - ".java".length());
        final String testSuiteCode = generateTestSuite(sutName, outputRelation, mrinfos, sourcesPath, followupsPath, testSuiteClass);
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(testSuiteCode);
        }
    }

    public static void writeTestSuites(String sutName, File mrsDir, File sourcesDir, File followupsDir, String outputTestPrefix) throws IOException, ClassNotFoundException, NoSuchMethodException {
        final Path sourcesPath = sourcesDir.toPath().resolve(sutName);
        for (final File followupsExperimentsDir : Objects.requireNonNull(followupsDir.listFiles(File::isDirectory))) {
            final File followupsExperimentsSUTDir = followupsExperimentsDir.toPath().resolve(sutName).toFile();
            if (followupsExperimentsSUTDir.exists()) {
                for (final File cmrip : Objects.requireNonNull(followupsExperimentsSUTDir.listFiles(f -> f.getName().endsWith(".cmrip")))) {
                    final String mrName = cmrip.getName().substring(0, cmrip.getName().length() - ".cmrip".length());
                    final String mrNameWithoutSUT = mrName.split(Pattern.quote(SEPARATORS[0]))[1];
                    final Path followupsPath = followupsExperimentsSUTDir.toPath();
                    final MRInfoDB mrinfos = new MRInfoDB(
                        Arrays.stream(Objects.requireNonNull(followupsExperimentsSUTDir.listFiles(f -> f.getName().endsWith(".methodinputs"))))
                            .map(f -> f.getName().substring(0, f.getName().length() - ".methodinputs".length()).split(Pattern.quote(SEPARATORS[0])))
                            .filter(t -> t.length == 3 && t[2].equals(mrNameWithoutSUT))
                            .map(t -> new MRInfo(mrName, t[1], t[1] + SEPARATORS[0] + t[2]))
                            .collect(Collectors.toList())
                    );
                    final File outputRelationFile = mrsDir.toPath()
                            .resolve(followupsExperimentsDir.getName())
                            .resolve(sutName)
                            .resolve(mrName + ".jor.txt")
                            .toFile();
                    if (outputRelationFile.exists()) {
                        final String outputRelation = FileUtils.readContentFile(outputRelationFile);
                        final String testName = followupsExperimentsDir.getName() + "_" + mrName;
                        final String testClassName = outputTestPrefix + "_" + escapeNonAlphanum(testName, '_');
                        final File outputFile = new File(testClassName + ".java");
                        writeTestSuite(sutName, outputRelation, mrinfos, sourcesPath, followupsPath, outputFile);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException {
        if (args.length != 5) {
            System.err.println("Wrong number of parameters: 5 arguments expected, got " + args.length);
            System.err.println("SUT name");
            System.err.println("MRs directory");
            System.err.println("Source test inputs directory");
            System.err.println("Follow-up test inputs directory");
            System.err.println("Output tests prefix");
            System.exit(1);
        }
        final Iterator<String> argsIter = Arrays.stream(args).iterator();
        final String sutName = argsIter.next();
        final String mrsDir = argsIter.next();
        final String sourcesDir = argsIter.next();
        final String followupsDir = argsIter.next();
        final String outputTestPrefix = argsIter.next();
        writeTestSuites(sutName, new File(mrsDir), new File(sourcesDir), new File(followupsDir), outputTestPrefix);
    }

}
