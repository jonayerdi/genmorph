package ch.usi.gassert.interpreter;

import ch.usi.gassert.Functions;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.util.Pair;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;


public class SplitImplies {

    public static Pair<String, String> splitImplies(final String expression) {
        CompilationUnit cu = null;
        try {
            cu = JavaParser.parse(encapsulateInsideClassAndMethod(ConverterExpression.convertGASSERTtoJava(expression)));
        } catch (final Exception e) {
            System.err.println(expression);
            e.printStackTrace();
        }
        final Expression exp = findOuterMostExpression(cu.getChildNodes().get(0).getChildNodes().get(1).getChildNodes().get(4));
        return readTree(exp);
    }

    private static Pair<String, String> readTree(final Node n) {
        if (n instanceof MethodCallExpr && ((MethodCallExpr) n).getScope().isPresent() && ((MethodCallExpr) n).getScope().get().toString().equals("ch.usi.gassert.util.Implies")) {
            return new Pair(((MethodCallExpr) n).getArguments().get(0).toString(), ((MethodCallExpr) n).getArguments().get(1).toString());
        } else {
            return readTree(n.getChildNodes().get(0));
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
