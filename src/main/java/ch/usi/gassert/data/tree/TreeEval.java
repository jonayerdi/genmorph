package ch.usi.gassert.data.tree;

import ch.usi.gassert.data.types.Sequence;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.Pair;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeEval {

    public static boolean evalBool(final Tree root, final Map<String, Object> name2value, final double precision) {
        return bool(eval(root, name2value, precision));
    }

    public static double evalNum(final Tree root, final Map<String, Object> name2value, final double precision) {
        return num(eval(root, name2value, precision));
    }

    public static Object eval(final Tree root, final Map<String, Object> name2value, final double precision) {
        if (root.isLeaf()) {
            final Object value = root.getValue();
            final String valueString = value.toString();
            try {
                // Literal value or variable
                return isLiteral(valueString) ? value : name2value.get(valueString);
            } catch (Exception e) {
                throw new RuntimeException("Cannot evaluate leaf node: " + value, e);
            }
        } else {
            final Object leftValue = TreeEval.eval(root.getLeft(), name2value, precision);
            final Object rightValue = root.getRight() != null ? TreeEval.eval(root.getRight(), name2value, precision) : null;
            final String operator = root.getValue().toString();
            switch (operator) {
                /* NUMERIC OPERATORS */
                case "+": return num(leftValue) + num(rightValue);
                case "-": return num(leftValue) - num(rightValue);
                case "*": return num(leftValue) * num(rightValue);
                case "/": return protectedDivision(num(leftValue), num(rightValue), precision);
                case "^": return Math.pow(num(leftValue), num(rightValue));
                /* NUMBER TO BOOLEAN OPERATORS */
                case "==": return eq(leftValue, rightValue, precision);
                case "!=": case "<>": return !bool(eq(leftValue, rightValue, precision));
                case "<": return num(leftValue) < num(rightValue);
                case ">": return num(leftValue) > num(rightValue);
                case "<=": return num(leftValue) <= num(rightValue);
                case ">=": return num(leftValue) >= num(rightValue);
                /* BOOLEAN OPERATORS */
                case "&&": return bool(leftValue) && bool(rightValue);
                case "||": return bool(leftValue) || bool(rightValue);
                case "=>": return !bool(leftValue) || bool(rightValue);
                case "<=>": return bool(leftValue) == bool(rightValue);
                /* FUNCTIONS */
                case "NOT": return !bool(leftValue);
                case "ABS": return Math.abs(num(leftValue));
                case "string": return Sequence.fromNumber(num(leftValue));
                case "length": return (double)seq(leftValue).length();
                case "sum": return seq(leftValue).sum();
                case "flip": return seq(leftValue).flip();
                case "remove": return seq(leftValue).remove(((Number)rightValue).intValue());
                case "truncate": return seq(leftValue).truncate(((Number)rightValue).intValue());
                /* DEFAULT */
                default: throw new UnsupportedOperationException("Unknown operator: " + operator);
            }
        }
    }

    @Deprecated
    private static boolean isUnaryFunction(String valueString) {
        return valueString.contains("(");
    }

    @Deprecated
    public static Pair<String, String> splitUnaryFunction(String valueString) {
        final Pattern pattern = Pattern.compile("^(.+)*\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(valueString);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid unary function expression: " + valueString);
        }
        return Pair.of(matcher.group(1), matcher.group(2));
    }

    public static boolean isLiteral(String value) {
        return isNumericLiteral(value) || isBooleanLiteral(value);
    }

    public static boolean isNumericLiteral(String value) {
        char first = value.charAt(0);
        return Character.isDigit(first) || (first == '-' && value.length() > 1 && Character.isDigit(value.charAt(1)));
    }

    public static boolean isBooleanLiteral(String value) {
        return value.equals("true") || value.equals("false");
    }

    public static boolean bool(Object value) {
        return (boolean)value;
    }

    public static double num(Object value) {
        return ((Number)value).doubleValue();
    }

    public static Sequence seq(Object value) {
        return (Sequence)value;
    }

    public static double str2num(String value) {
        return Double.parseDouble(value);
    }

    public static boolean str2bool(String value) {
        switch (value) {
            case "true": return true;
            case "false": return false;
            default: throw new RuntimeException();
        }
    }

    public static boolean num2bool(double value, double precision) {
        return !numericEquals(value, 0.0, precision);
    }

    public static double bool2num(boolean value) {
        return value ? 1.0 : 0.0;
    }

    public static boolean numericEquals(double a, double b, double precision) {
        return Math.abs(a - b) < precision;
    }

    public static double protectedDivision(double dividend, double divisor, double precision) {
        return numericEquals(divisor, 0.0, precision) ? 1.0 : dividend / divisor;
    }

    public static Object eq(Object a, Object b, double precision) {
        final Class<?> clazz = a.getClass();
        if (ClassUtils.isNumericType(clazz)) {
            return numericEquals(num(a), num(b), precision);
        } else if (ClassUtils.isSequenceType(clazz)) {
            return seq(a).equals(seq(b), precision);
        } else {
            return a.equals(b);
        }
    }

}
