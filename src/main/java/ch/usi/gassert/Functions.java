package ch.usi.gassert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class defines the functions that are allowed by the assertions
 * we might think to other additional ones
 */
public class Functions {

    public final static List<String> functionsMathReturnsMath;
    public final static List<String> functionsMathReturnsBoolean;
    public final static List<String> functionsBooleanReturnsBoolean;
    public final static List<String> functionsUnaryBooleanReturnsBoolean;
    public final static List<String> functionsUnaryMathReturnsMath;
    public final static Set<String> mathOperators;
    public final static Set<String> booleanOperators;
    // Extra functions for evaluation only
    public final static List<String> functionsMathReturnsMathExtra;
    public final static List<String> functionsMathReturnsBooleanExtra;
    public final static List<String> functionsBooleanReturnsBooleanExtra;
    public final static List<String> functionsUnaryBooleanReturnsBooleanExtra;
    public final static List<String> functionsUnaryMathReturnsMathExtra;
    public final static Set<String> mathOperatorsExtra;
    public final static Set<String> booleanOperatorsExtra;
    // Functions for sequences
    public final static List<String> functionsUnaryMathReturnsSequence;
    public final static List<String> functionsUnarySequenceReturnsMath;
    public final static List<String> functionsUnarySequenceReturnsSequence;
    public final static List<String> functionsBinarySequenceReturnsBoolean;
    public final static List<String> functionsBinarySequenceMathReturnsSequence;
    // Special function effects (Useful for detecting redundancies in expressions)
    public final static List<String> functionsReentrant;
    public final static List<String> functionsSymmetric;

    static {
        // Functions used for generation
        functionsMathReturnsMath = Arrays.asList("+", "*", "-", "/");
        functionsMathReturnsBoolean = Arrays.asList("==", "<", ">", "<=", ">=", "<>");
        functionsBooleanReturnsBoolean = Arrays.asList("&&", "||", "<>", /* implies */ "=>", /* iff */ "<=>");
        functionsUnaryBooleanReturnsBoolean = Arrays.asList("NOT");
        functionsUnaryMathReturnsMath = Arrays.asList("ABS");
        // Groups
        mathOperators = new HashSet<>();
        mathOperators.addAll(functionsMathReturnsMath);
        mathOperators.addAll(functionsUnaryMathReturnsMath);
        booleanOperators = new HashSet<>();
        booleanOperators.addAll(functionsMathReturnsBoolean);
        booleanOperators.addAll(functionsBooleanReturnsBoolean);
        booleanOperators.addAll(functionsUnaryBooleanReturnsBoolean);
        // Extra functions for evaluation only
        functionsMathReturnsMathExtra = Arrays.asList("^");
        functionsMathReturnsBooleanExtra = Arrays.asList();
        functionsBooleanReturnsBooleanExtra = Arrays.asList();
        functionsUnaryBooleanReturnsBooleanExtra = Arrays.asList();
        functionsUnaryMathReturnsMathExtra = Arrays.asList();
        // Groups
        mathOperatorsExtra = new HashSet<>();
        mathOperatorsExtra.addAll(functionsMathReturnsMathExtra);
        mathOperatorsExtra.addAll(functionsUnaryMathReturnsMathExtra);
        booleanOperatorsExtra = new HashSet<>();
        booleanOperatorsExtra.addAll(functionsMathReturnsBooleanExtra);
        booleanOperatorsExtra.addAll(functionsBooleanReturnsBooleanExtra);
        booleanOperatorsExtra.addAll(functionsUnaryBooleanReturnsBooleanExtra);
        // Functions for sequences
        functionsUnaryMathReturnsSequence = Arrays.asList("string");
        functionsUnarySequenceReturnsMath = Arrays.asList("length", "sum");
        functionsUnarySequenceReturnsSequence = Arrays.asList("flip");
        functionsBinarySequenceReturnsBoolean = Arrays.asList("==", "<>");
        functionsBinarySequenceMathReturnsSequence = Arrays.asList("remove", "truncate");
        // Special function effects (Useful for detecting redundancies in expressions)
        functionsReentrant = Arrays.asList("ABS");
        functionsSymmetric = Arrays.asList("NOT", "flip");
    }

    public static boolean isBooleanOp(final String op) {
        return booleanOperators.contains(op) || booleanOperatorsExtra.contains(op);
    }

    public static boolean isMathOp(final String op) {
        return mathOperators.contains(op) || mathOperatorsExtra.contains(op);
    }

    public static boolean isOp(final String op) {
        return booleanOperators.contains(op) || mathOperators.contains(op) ||
                booleanOperatorsExtra.contains(op) || mathOperatorsExtra.contains(op);
    }

    public static boolean isMathReturnsMath(final String op) {
        return functionsMathReturnsMath.contains(op) || functionsMathReturnsMathExtra.contains(op);
    }

    public static boolean isMathReturnsBoolean(final String op) {
        return functionsMathReturnsBoolean.contains(op) || functionsMathReturnsBooleanExtra.contains(op);
    }

    public static boolean isBooleanReturnsBoolean(final String op) {
        return functionsBooleanReturnsBoolean.contains(op) || functionsBooleanReturnsBooleanExtra.contains(op);
    }

    public static boolean isUnaryReturnsBoolean(final String op) {
        return functionsUnaryBooleanReturnsBoolean.contains(op) || functionsUnaryBooleanReturnsBooleanExtra.contains(op);
    }

    public static boolean isUnaryReturnsMath(final String op) {
        return functionsUnaryMathReturnsMath.contains(op) || functionsUnaryMathReturnsMathExtra.contains(op) || functionsUnarySequenceReturnsMath.contains(op);
    }

    public static boolean isUnaryMathReturnsSequence(final String op) {
        return functionsUnaryMathReturnsSequence.contains(op);
    }

    public static boolean isUnarySequenceReturnsMath(final String op) {
        return functionsUnarySequenceReturnsMath.contains(op);
    }

    public static boolean isUnarySequenceReturnsSequence(final String op) {
        return functionsUnarySequenceReturnsSequence.contains(op);
    }

    public static boolean isBinarySequenceReturnsBoolean(final String op) {
        return functionsBinarySequenceReturnsBoolean.contains(op);
    }

    public static boolean isBinarySequenceReturnsSequence(final String op) {
        return functionsBinarySequenceMathReturnsSequence.contains(op);
    }

    public static boolean isReentrant(final String op) {
        return functionsReentrant.contains(op);
    }

    public static boolean isSymmetric(final String op) {
        return functionsSymmetric.contains(op);
    }

}
