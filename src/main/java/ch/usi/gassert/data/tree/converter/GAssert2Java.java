package ch.usi.gassert.data.tree.converter;

import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeEval;
import ch.usi.gassert.data.tree.TreeReaderGAssert;
import ch.usi.gassert.util.FileUtils;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.usi.gassert.Config.EVAL_NUMBER_PRECISION;
import static ch.usi.gassert.util.Assert.assertAlways;

/**
 * Converts GAssert expressions to Java 2 syntax.
 *
 * Generated Java expression may require the following imports:
 *  * Math
 */
public abstract class GAssert2Java {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static String refactorVariableName(final String variable, final Optional<String> classNameIfStatic) {
        // Refactor variable name
        final String prefix = variable.substring(0, 2);
        final String suffix = variable.substring(variable.length() - 2);
        final String[] splitVariableName = variable.substring(2, variable.length() - 2).split("\\.");
        if (classNameIfStatic.isPresent() && splitVariableName[0].equals("this")) {
            // Refactor case 1: i_this and o_this become <className> because the method is static
            // This is actually not needed, you can access static members with an instance, even if it's null
            splitVariableName[0] = classNameIfStatic.get();
        } else {
            // Refactor case 2: i_variable.member_f becomes i_variable_f.member
            splitVariableName[0] = prefix + splitVariableName[0] + suffix;
        }
        return String.join(".", splitVariableName);
    }

    public static String refactorVariableName(final String variable) {
        return refactorVariableName(variable, Optional.empty());
    }

    public static String isFiniteCheck(final String variable) {
        return "Double.isFinite((double)" + variable + ")";
    }

    public static String isNotFiniteCheck(final String variable) {
        return "!" + isFiniteCheck(variable);
    }

    public static String addVariableChecks(final String assertion, final Set<String> variables,
                                           final Function<String,String> checkFn, final String joinOperator) {
        return "(" + variables.stream()
                    .map(GAssert2Java::refactorVariableName)
                    .map(checkFn)
                    .collect(Collectors.joining(" " + joinOperator + " "))
                + " " + joinOperator + " (" + assertion + "))";
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static String GAssertTree2Java(final Tree tree, final Optional<String> classNameIfStatic) {
        /*
         * From ch.usi.gassert.Functions:
         *      functionsMathReturnsMath = Arrays.asList("+", "*", "-", "/");
         *      functionsMathReturnsBoolean = Arrays.asList("==", "<", ">", "<=", ">=", "<>");
         *      functionsBooleanReturnsBoolean = Arrays.asList("&&", "||", "<>", "=>", "<=>");
         *      functionsUnaryBooleanReturnsBoolean =Arrays.asList("NOT");
         */
        if (tree.isLeaf()) {
            String value = tree.toString();
            if (TreeEval.isBooleanLiteral(value)) {
                return value;
            } else if (TreeEval.isNumericLiteral(value)) {
                // Represent all numerics as doubles
                return String.valueOf(Double.parseDouble(value));
            } else {
                // Refactor variable name
                final String variableName = refactorVariableName(value);
                if (tree.getType() == Tree.Type.SEQUENCE) {
                    // Wrap all sequences with our helper class
                    return "(ch.usi.gassert.data.types.Sequence.fromValue(" + variableName + "))";
                } else if(tree.getType() == Tree.Type.NUMBER) {
                    // Represent all numerics as doubles
                    return "((double) " + variableName + ")";
                } else {
                    return variableName;
                }
            }

        } else {
            String value = tree.getValue().toString();
            final Tree leftTree = tree.getLeft();
            final Tree rightTree = tree.getRight();
            String left = leftTree != null ? GAssertTree2Java(leftTree, classNameIfStatic) : null;
            String right = rightTree != null ? GAssertTree2Java(rightTree, classNameIfStatic) : null;
            switch (value) {
                case "/":
                    // ch.usi.gassert.data.tree.TreeEval.protectedDivision(double,double,double)
                    return "((Math.abs(" + right + ") < " + EVAL_NUMBER_PRECISION + ")" + " ? 1.0 : (" + left + " / " + right + "))";
                case "<=>":
                    value = "==";
                case "==":
                    switch (tree.getLeft().getType()) {
                        case BOOLEAN:
                            break;
                        case NUMBER:
                            // ch.usi.gassert.data.tree.TreeEval.numericEquals(double,double,double)
                            return "(Math.abs(" + left + " - " + right + ") < " + EVAL_NUMBER_PRECISION + ")";
                        case SEQUENCE:
                            return "(" + left + ".equals(" + right + ", " + EVAL_NUMBER_PRECISION + "))";
                        default:
                            throw new RuntimeException("Unknown Tree type: " + tree.getLeft().getType());
                    }
                    break;
                case "<>":
                    value = "!=";
                case "!=":
                    switch (tree.getLeft().getType()) {
                        case BOOLEAN:
                            break;
                        case NUMBER:
                            // ch.usi.gassert.data.tree.TreeEval.numericEquals(double,double,double)
                            return "(Math.abs(" + left + " - " + right + ") >= " + EVAL_NUMBER_PRECISION + ")";
                        case SEQUENCE:
                            return "(!" + left + ".equals(" + right + ", " + EVAL_NUMBER_PRECISION + "))";
                        default:
                            throw new RuntimeException("Unknown Tree type: " + tree.getLeft().getType());
                    }
                    break;
                case "=>":
                    // ch.usi.gassert.data.tree.TreeEval.asBoolean(double)
                    if (tree.getLeft().getType() == Tree.Type.NUMBER) {
                        left = "(Math.abs(" + left + ") < " + EVAL_NUMBER_PRECISION + ")";
                    }
                    if (tree.getRight().getType() == Tree.Type.NUMBER) {
                        right = "(Math.abs(" + right + ") < " + EVAL_NUMBER_PRECISION + ")";
                    }
                    return "(!" + left + " || " + right + ")";
                case "<":
                case ">":
                case "<=":
                case ">=":
                    // ch.usi.gassert.data.tree.TreeEval.asNumeric(boolean)
                    if (tree.getLeft().getType() == Tree.Type.BOOLEAN) {
                        left = "(" + left + " ? 1.0 : 0.0)";
                    }
                    if (tree.getRight().getType() == Tree.Type.BOOLEAN) {
                        right = "(" + right + " ? 1.0 : 0.0)";
                    }
                    break;
                case "&&":
                case "||":
                    // ch.usi.gassert.data.tree.TreeEval.asBoolean(double)
                    if (tree.getLeft().getType() == Tree.Type.NUMBER) {
                        left = "(Math.abs(" + left + ") < " + EVAL_NUMBER_PRECISION + ")";
                    }
                    if (tree.getRight().getType() == Tree.Type.NUMBER) {
                        right = "(Math.abs(" + right + ") < " + EVAL_NUMBER_PRECISION + ")";
                    }
                    break;
                // Functions (BOOLEAN)
                case "NOT":
                    // NOT(BOOLEAN)
                    return "(!" + left + ")";
                // Functions (NUMBER)
                case "ABS":
                    // ABS(NUMBER)
                    return "(Math.abs(" + left + "))";
                // Functions (SEQUENCE)
                case "string":
                    // string(number)
                    return "(ch.usi.gassert.data.types.Sequence.fromNumber(" + left + "))";
                case "length":
                    // length(sequence)
                    return "(" + left + ".length())";
                case "flip":
                    // flip(sequence)
                    return "(" + left + ".flip())";
                case "remove":
                    // remove(sequence, index)
                    return "(" + left + ".remove(((Number)(" + right + ")).intValue()))";
                case "truncate":
                    // truncate(sequence, index)
                    return "(" + left + ".truncate(((Number)(" + right + ")).intValue()))";
                case "sum":
                    // sum(sequence)
                    return "(" + left + ".sum())";
            }
            return "(" + left + " " + value + " " + right + ")";
        }
    }

    public static String GAssertTree2Java(final Tree tree) {
        // This should always be OK, the following code works:
        // Integer num = null; num.MAX_VALUE;
        return GAssertTree2Java(tree, Optional.empty());
    }

    public static void GAssert2JavaAssertion(File inputFile, File outputFile, Map<String, Class<?>> variableTypes) throws IOException {
        final String gassertAssertion = FileUtils.readContentFile(inputFile);
        final Tree tree = TreeReaderGAssert.getTree(gassertAssertion, variableTypes);
        final String javaAssertion = GAssertTree2Java(tree);
        assertAlways(outputFile.getParentFile().exists() || outputFile.getParentFile().mkdirs(), "Couldn't generate directory for " + outputFile);
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(javaAssertion);
        }
    }

    public static String getGAssertMRIPAssertion(File inputFile, String mrip) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            while (true) {
                final String mr = reader.readLine().trim();
                final String gassertAssertion = reader.readLine().trim();
                if (mr.equals(mrip)) {
                    return gassertAssertion;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding MRIP \"" + mrip + "\" in \"" + inputFile.getAbsolutePath() + "\"", e);
        }
    }

    public static void GAssert2JavaAssertion(File inputFile, String mrip, File outputFile, Map<String, Class<?>> variableTypes) throws IOException {
        final String gassertAssertion = getGAssertMRIPAssertion(inputFile, mrip);
        final Tree tree = TreeReaderGAssert.getTree(gassertAssertion, variableTypes);
        final String javaAssertion = GAssertTree2Java(tree);
        assertAlways(outputFile.getParentFile().exists() || outputFile.getParentFile().mkdirs(), "Couldn't generate directory for " + outputFile);
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(javaAssertion);
        }
    }

    public static void GAssert2JavaAssertion(File inputFile, File outputFile, File stateFile) throws IOException {
        final Map<String, Class<?>> variableTypes;
        try (final JsonReader reader = new JsonReader(new FileReader(stateFile))) {
            variableTypes = VariablesManager
                    .fromVariableValues(TestExecution.fromJson(reader).getVariables().getValues())
                    .makeMetamorphic()
                    .getVariableTypes();
        }
        GAssert2JavaAssertion(inputFile, outputFile, variableTypes);
    }

    public static void GAssert2JavaAssertion(File inputFile, String mrip, File outputFile, File stateFile) throws IOException {
        final Map<String, Class<?>> variableTypes;
        try (final JsonReader reader = new JsonReader(new FileReader(stateFile))) {
            variableTypes = VariablesManager
                    .fromVariableValues(TestExecution.fromJson(reader).getVariables().getValues())
                    .makeMetamorphic()
                    .getVariableTypes();
        }
        GAssert2JavaAssertion(inputFile, mrip, outputFile, variableTypes);
    }

    public static void main(final String[] args) throws IOException {
        if (args.length != 3 && args.length != 4) {
            System.err.println("Wrong number of parameters: 3 arguments expected, got " + args.length);
            System.err.println("Input (GAssert) assertion file");
            System.err.println("Output (Java) assertion file");
            System.err.println("State file");
            System.err.println("[Selected MRIP (for *.mrip.txt files)]");
            System.exit(1);
        }
        final Iterator<String> argsIter = Arrays.stream(args).iterator();
        final String inputFile = argsIter.next();
        final String outputFile = argsIter.next();
        final String stateFile = argsIter.next();
        if (argsIter.hasNext()) {
            final String mrip = argsIter.next();
            GAssert2JavaAssertion(new File(inputFile), mrip, new File(outputFile), new File(stateFile));
        } else {
            GAssert2JavaAssertion(new File(inputFile), new File(outputFile), new File(stateFile));
        }
    }

}
