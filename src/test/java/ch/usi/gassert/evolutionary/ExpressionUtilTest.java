package ch.usi.gassert.evolutionary;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.udojava.evalex.Expression;
import org.junit.Test;

public class ExpressionUtilTest {


    @Test
    public void test1() {
        System.out.println(new Expression("A > B").with("A", "4").and("B", "3").eval());
        System.out.println(new Expression("A && B").with("A", "true").and("B", "true").eval());
        CompilationUnit cu = JavaParser.parse(encapsulateInsideClassAndMethod("assert(a>5 && b<4);"));

    }

    private static String encapsulateInsideClassAndMethod(String body) {
        StringBuilder s = new StringBuilder();
        s.append("public class Post");
        s.append("{");
        s.append(System.lineSeparator());
        s.append("public static void main(final String args[]) throws RuntimeException {");
        s.append(System.lineSeparator());
        s.append(body);
        s.append(System.lineSeparator());
        s.append("}");
        s.append(System.lineSeparator());
        s.append("}");
        return s.toString();
    }
}