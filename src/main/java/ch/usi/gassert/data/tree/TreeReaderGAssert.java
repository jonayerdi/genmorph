package ch.usi.gassert.data.tree;


import ch.usi.gassert.Functions;
import ch.usi.gassert.util.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TreeReaderGAssert {

    public static Tree getTree(final String expressionString) {
        return getTree(expressionString, new HashMap<>(0));
    }

    public static Tree getTree(final String expressionString, final Map<String, Class<?>> variableTypes) {
        return constructTree(Parser.toRPN(expressionString), variableTypes);
    }

    // Returns root of constructed tree for given RPN
    static Tree constructTree(final List<Parser.Token> rpn, final Map<String, Class<?>> variableTypes) {
        final Stack<Tree> st = new Stack<>();
        // Traverse through every token of the RPN
        rpn.forEach(token -> {
            Tree t, t1, t2;
            final Tree.Type treeType;
            switch (token.type) {
                case LITERAL:
                case HEX_LITERAL:
                    st.push(new Tree(Double.parseDouble(token.surface), Tree.Type.NUMBER));
                    break;
                case VARIABLE:
                    if (TreeEval.isBooleanLiteral(token.surface)) {
                        st.push(new Tree(Boolean.parseBoolean(token.surface), Tree.Type.BOOLEAN));
                    } else {
                        final Class<?> varType = variableTypes.get(token.surface);
                        if (varType == null) {
                            throw new RuntimeException("Undefined variable: " + token.surface);
                        }
                        if (ClassUtils.isBooleanType(varType)) {
                            treeType = Tree.Type.BOOLEAN;
                        } else if (ClassUtils.isNumericType(varType)) {
                            treeType = Tree.Type.NUMBER;
                        } else if (ClassUtils.isSequenceType(varType)) {
                            treeType = Tree.Type.SEQUENCE;
                        } else {
                            throw new RuntimeException("Unknown type: " + varType);
                        }
                        st.push(new Tree(token.surface, treeType));
                    }
                    break;
                case UNARY_OPERATOR:
                    if (token.surface.equals("-u")) {
                        t = new Tree("-", new Tree(0.0, Tree.Type.NUMBER), st.pop(), Tree.Type.NUMBER);
                        st.push(t);
                    } else if (!token.surface.equals("+u")) {
                        throw new RuntimeException("Unsupported unary operator: " + token.surface);
                    }
                    break;
                case OPERATOR:
                    // Pop two top nodes
                    t1 = st.pop();
                    t2 = st.pop();
                    // Make them children
                    t = new Tree(token.surface, t2, t1, Functions.isBooleanOp(token.surface) ? Tree.Type.BOOLEAN : Tree.Type.NUMBER);
                    // Add this subexpression to stack
                    st.push(t);
                    break;
                case FUNCTION:
                    if (Functions.isUnaryReturnsBoolean(token.surface)) {
                        t = new Tree(token.surface, st.pop(), null, Tree.Type.BOOLEAN);
                    } else if (Functions.isUnaryReturnsMath(token.surface)
                            || Functions.isUnarySequenceReturnsMath(token.surface)) {
                        t = new Tree(token.surface, st.pop(), null, Tree.Type.NUMBER);
                    } else if (Functions.isUnarySequenceReturnsSequence(token.surface) 
                            || Functions.isUnaryMathReturnsSequence(token.surface)) {
                        t = new Tree(token.surface, st.pop(), null, Tree.Type.SEQUENCE);
                    } else if (Functions.isBinarySequenceReturnsBoolean(token.surface)) {
                        t1 = st.pop();
                        t2 = st.pop();
                        t = new Tree(token.surface, t2, t1, Tree.Type.BOOLEAN);
                    } else if (Functions.isBinarySequenceReturnsSequence(token.surface)) {
                        t1 = st.pop();
                        t2 = st.pop();
                        t = new Tree(token.surface, t2, t1, Tree.Type.SEQUENCE);
                    } else {
                        // object.metod()
                        final Class<?> varType = variableTypes.get(token.surface);
                        if (varType == null) {
                            throw new RuntimeException("Undefined function: " + token.surface);
                        }
                        if (ClassUtils.isBooleanType(varType)) {
                            treeType = Tree.Type.BOOLEAN;
                        } else if (ClassUtils.isNumericType(varType)) {
                            treeType = Tree.Type.NUMBER;
                        } else {
                            throw new RuntimeException("Unknown type: " + varType);
                        }
                        t = new Tree(token.surface + "()", treeType);
                    }
                    st.push(t);
                    break;
            }
        });
        //  Take the root of expression tree
        return st.peek();
    }

}
