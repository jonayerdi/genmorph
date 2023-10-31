package ch.usi.gassert.data.tree.converter;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeEval;
import ch.usi.gassert.util.Pair;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Converts GAssert expressions to SMT-LIB 2 syntax.
 *
 * ISSUES:
 *      - Numeric equality: GAssert converts (A == B) into (abs(A - B) < EPSILON), but SMT cannot. We round numbers to 3 decimals to work around this.
 */
@Deprecated
public class GAssert2SMTLIB {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");
    static final Map<String, String> GAssert2SMTLIBOperators = new HashMap<>(8);
    static {
        /*
         *      (NOT a) -> (not a)
         *      (|| a b) -> (or a b)
         *      (&& a b) -> (and a b)
         *      (== a b) -> (= a b)
         *      (<=> a b) -> (= a b)
         */
        GAssert2SMTLIBOperators.put("NOT", "not");
        GAssert2SMTLIBOperators.put("||", "or");
        GAssert2SMTLIBOperators.put("&&", "and");
        GAssert2SMTLIBOperators.put("==", "=");
        GAssert2SMTLIBOperators.put("<=>", "=");
    }

    /**
     * Convert the given GAssert expression tree to RPN:
     *
     * For instance, the expression:
     * a + ((b - c) * 2)
     * Will be converted to the following token sequence:
     * + a * - b c 2
     *
     * Some operators are also converted to be compatible with SMT-LIB (e.g. "||" becomes "or").
     *
     * @param tree The GAssert expression tree
     * @param rpn The RPN representation of the given Tree
     */
    public static void Tree2RPN(final Tree tree, final Deque<RPNToken> rpn) {
        /*
         * From ch.usi.gassert.Functions:
         *      functionsMathReturnsMath = Arrays.asList("+", "*", "-", "/");
         *      functionsMathReturnsBoolean = Arrays.asList("==", "<", ">", "<=", ">=", "<>");
         *      functionsBooleanReturnsBoolean = Arrays.asList("&&", "||", "<>", "=>", "<=>");
         *      functionsUnaryBooleanReturnsBoolean =Arrays.asList("NOT");
         */
        final String value = tree.getValue().toString();
        if (tree.isLeaf()) {
            if (value.startsWith("NOT(")) {
                // NOT is currently always a leaf node: Use splitUnaryFunction to get the inner expression;
                final Pair<String, String> fn = TreeEval.splitUnaryFunction(value);
                rpn.add(RPNToken.of("not", 1));
                rpn.add(RPNToken.of(fn.snd, 0));
            } else if(TreeEval.isNumericLiteral(value)) {
                // Format numeric literals
                rpn.add(RPNToken.of(DECIMAL_FORMAT.format(Double.parseDouble(value)), 0));
            } else {
                // Just add the token as it is
                rpn.add(RPNToken.of(value, 0));
            }
        } else {
            if (value.equals("<>") || value.equals("!=")) {
                // (<> a b) -> (not (= a b))
                rpn.add(RPNToken.of("not", 1));
                rpn.add(RPNToken.of("=", 2));
            } else {
                // Simple operator syntax conversion or no conversion at all
                rpn.add(RPNToken.of(GAssert2SMTLIBOperators.getOrDefault(value, value), 2));
            }
            Tree2RPN(tree.getLeft(), rpn);
            Tree2RPN(tree.getRight(), rpn);
        }
    }

    /**
     * Convert the given GAssert expression tree to RPN:
     *
     * For instance, the expression:
     * a + ((b - c) * 2)
     * Will be converted to the following token sequence:
     * + a * - b c 2
     *
     * Some operators are also converted to be compatible with SMT-LIB (e.g. "||" becomes "or").
     *
     * @param tree The GAssert expression tree
     * @return The RPN representation of the given Tree
     */
    public static Deque<RPNToken> Tree2RPN(final Tree tree) {
        final Deque<RPNToken> rpn = new ArrayDeque<>(tree.getNumberOfNodes());
        Tree2RPN(tree, rpn);
        return rpn;
    }

    /**
     * Convert the given RPN to an SMT-LIB compatible constraint expression
     *
     * For instance, the expression:
     * a + ((b - c) * 2)
     * Which corresponds with the token sequence:
     * + a * - b c 2
     * Will be converted to the following expression String:
     * (+ a (* (- b c) 2))
     *
     * @param rpn The RPN representation of the expression
     * @return  The SMT-LIB compatible constraint expression
     */
    public static String RPN2SMTLIB(final Deque<RPNToken> rpn) {
        final Stack<Integer> closingParentheses = new Stack<>();
        final StringBuilder sb = new StringBuilder();
        // Traverse RPN
        while (!rpn.isEmpty()) {
            RPNToken token = rpn.removeFirst();
            if (token.childrenCount > 0) {
                // Add parentheses for tokens which have children
                sb.append("(");
                // Defer closing the parenthesis after the children
                closingParentheses.push(token.childrenCount);
            }
            sb.append(token.value);
            if (token.childrenCount > 0) {
                // Add space to separate the following token (first child)
                sb.append(" ");
            } else {
                // Here, I use num = 0 as a sentinel value indicating that closingParentheses is empty, which
                // should only happen if rpn is also empty (otherwise there should always be parentheses to close).
                int num = closingParentheses.isEmpty() ? 0 : closingParentheses.pop();
                // We might need to close multiple levels of parentheses, hence the "while"
                while (num == 1) {
                    sb.append(")");
                    num = closingParentheses.isEmpty() ? 0 : closingParentheses.pop();
                }
                if (num > 1) {
                    // Add space to separate the following token (sibling)
                    closingParentheses.push(num - 1);
                    sb.append(" ");
                } else if (!rpn.isEmpty()) {
                    // If we finished closing all the parentheses, then
                    // there should also be no tokens remaining in the RPN
                    throw new RuntimeException("Invalid state: closingParentheses stack empty\nRPN: " + rpn);
                }
            }
        }
        // Check that closingParentheses to detect possible errors
        if (!closingParentheses.isEmpty()) {
            throw new RuntimeException("Invalid state: closingParentheses stack not empty after processing RPN");
        }
        return sb.toString();
    }

    /**
     * Convert the given GAssert Tree to an SMT-LIB compatible constraint expression
     *
     * For instance, the expression:
     * a + ((b - c) * 2)
     * Which corresponds with the token sequence:
     * + a * - b c 2
     * Will be converted to the following expression String:
     * (+ a (* (- b c) 2))
     *
     * @param tree The GAssert Tree representing the expression
     * @return  The SMT-LIB compatible constraint expression
     */
    public static String GAssertTree2SMTLIB(final Tree tree) {
        return RPN2SMTLIB(Tree2RPN(tree));
    }

    static class RPNToken {
        // The String representation of the token
        final String value;
        // The number of children the token should have
        // For binary operators ("and", "+", etc), this should be 2
        // For unary operators ("not"), this should be 1
        // For terminal tokens (variable names or constants), this should be 0
        final int childrenCount;
        public RPNToken(String value, int childrenCount) {
            this.value = value;
            this.childrenCount = childrenCount;
        }
        public static RPNToken of(String value, int childrenCount) {
            return new RPNToken(value, childrenCount);
        }
        @Override
        public String toString() {
            return '(' + value + ',' + childrenCount + ')';
        }
    }

}
