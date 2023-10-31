package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.io.File;

/**
 * this class simply remove the assertions from the test generated we need this because some tests might fail.
 * remove both junit and java assertions
 */
public class RemoveAssertionsFromTests {

    private RemoveAssertionsFromTests() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) {
        removeAssertionsAndOverwriteFile(args[0]);
    }

    public static void removeAssertionsAndOverwriteFile(final String javaFilePath) {

        if (!new File(javaFilePath).isDirectory()) {
            if (javaFilePath.endsWith(".java") && javaFilePath.contains("GAssert/subjects")) {
                FileUtils.overwriteTextOnFile(javaFilePath, removeAssertions(javaFilePath));
                return;
            }
        } else {
            for (final File f : new File(javaFilePath).listFiles()) {
                removeAssertionsAndOverwriteFile(f.getAbsolutePath());
            }
        }
    }


    protected static String removeAssertions(final String javaFilePath) {
        try {
            return removeAssertions(JavaParser.parse(new File(javaFilePath)));
        } catch (final Exception e) {
            //System.exit(-1);
            e.printStackTrace();
        }
        return "";
    }


    private static String removeAssertions(final CompilationUnit cu) {
        cu.accept(new RemoveJUnitAssertionsVisitor(), null);
        return cu.toString();
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class RemoveJUnitAssertionsVisitor extends ModifierVisitor<Void> {

        public RemoveJUnitAssertionsVisitor() {

        }

        @Override
        public Visitable visit(final MethodCallExpr n, final Void arg) {
            if (n.getName().asString().startsWith("assert") || (n.getScope().isPresent() && n.getScope().get().toString().equals("org.junit.Assert"))) {
                return null;
            }
            return super.visit(n, arg);
        }

        @Override
        public Visitable visit(final AssertStmt n, final Void arg) {
            return null;
        }
    }
}