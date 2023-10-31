package ch.usi.gassert.data.tree;

import ch.usi.gassert.Functions;
import ch.usi.gassert.interpreter.ConverterExpression;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;

public class TreeReaderJava {

    public static Tree getTree(final String expression) {
        CompilationUnit cu = null;
        try {
            cu = JavaParser.parse(encapsulateInsideClassAndMethod(ConverterExpression.convertGASSERTtoJava(expression)));
        } catch (final Exception e) {
            System.err.println(expression);
            e.printStackTrace();
        }
        final Expression exp = findOuterMostExpression(cu.getChildNodes().get(0).getChildNodes().get(1).getChildNodes().get(4));
        return createTree(exp, Tree.Type.BOOLEAN);
    }

    private static Tree createTree(final Node n, final Tree.Type typeChildrenParent) {
        if (n instanceof BinaryExpr) {
            final BinaryExpr expr = (BinaryExpr) n;
            final String op = expr.getOperator().asString();
            final Tree.Type type = Functions.isMathReturnsMath(op) ? Tree.Type.NUMBER : Tree.Type.BOOLEAN;
            final Tree.Type typeChildren = (Functions.isMathReturnsMath(op) || Functions.isMathReturnsBoolean(op)) ? Tree.Type.NUMBER : Tree.Type.BOOLEAN;
            return new Tree(expr.getOperator().asString(), createTree(expr.getLeft(), typeChildren), createTree(expr.getRight(), typeChildren), type);
        } else if (n instanceof UnaryExpr /* && n.toString().startsWith(("!"))*/) {
            return new Tree(n.toString(), null, null, typeChildrenParent);
        } else if (n.getChildNodes().size() == 1) {
            return createTree(n.getChildNodes().get(0), typeChildrenParent);
        } else {
            return new Tree(n.toString(), null, null, typeChildrenParent);
        }
    }

    private static Expression findOuterMostExpression(final Node n) {
        if (n instanceof Expression) {
            return (Expression) n;
        }
        for (final Node node : n.getChildNodes()) {
            final Expression exp = findOuterMostExpression(node);
            if (exp != null) {
                return exp;
            }
        }
        return null;
    }

    /**
     * create a syntetic class
     *
     * @param body
     * @return
     */
    private static String encapsulateInsideClassAndMethod(final String body) {
        final StringBuilder s = new StringBuilder();
        s.append("public class Post");
        s.append("{");
        s.append(System.lineSeparator());
        s.append("public static void main(final String args[]) throws RuntimeException {");
        s.append(System.lineSeparator());
        s.append("assert(");
        s.append(body);
        s.append(");");
        s.append(System.lineSeparator());
        s.append("}");
        s.append(System.lineSeparator());
        s.append("}");
        return s.toString();
    }


}
