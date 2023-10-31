package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class simply remove the assertions from the test generated we need this because some tests might fail.
 * remove both junit and java assertions
 */
public class WriteAssertionFile {

    private WriteAssertionFile() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) throws FileNotFoundException {
        addAssertion(args[0], args[1], args[2]);
    }

    public static void addAssertion(final String javaFilePath, final String methodName, final String assertion) throws FileNotFoundException {
        System.out.println("assertion in input for the WriteAssertion " + assertion);


        if (!new File(javaFilePath).isDirectory()) {
            if (javaFilePath.endsWith(".java") && javaFilePath.contains("GAssert/subjects")) {
                final CompilationUnit cu = JavaParser.parse(new File(javaFilePath));
                cu.accept(new AddAssertionsVisitor(assertion, methodName), null);
                FileUtils.overwriteTextOnFile(javaFilePath, cu.toString());
                return;
            }
        } else {
            for (final File f : new File(javaFilePath).listFiles()) {
                addAssertion(f.getAbsolutePath(), methodName, assertion);
            }
        }
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class AddAssertionsVisitor extends VoidVisitorAdapter<Void> {
        private final String assertion;
        private final String methodName;
        private boolean isAdded = false;

        public AddAssertionsVisitor(final String assertion, final String methodName) {
            this.assertion = assertion;
            this.methodName = methodName;
        }

        @Override
        public void visit(final MethodDeclaration n, final Void arg) {
            // skip it if is not the method I want
            if (!n.getNameAsString().equals(methodName)) {
                return;
            }
            final BlockStmt block = new BlockStmt();
            recursiveVisitorStatement(block, n.getBody().get(), n.getType(), n);
            n.setBody(block);
        }

        private void recursiveVisitorStatement(final BlockStmt block, final BlockStmt orig, final Type type, final MethodDeclaration n) {


            for (final Statement s : orig.getStatements()) {
                if (isAdded) {
                    block.addStatement(s);
                    continue;
                }

                if (s.isAssertStmt()) {
                    if (assertion.trim().contains(";")) {
                        for (final String a : assertion.trim().split(";")) {
                            System.out.println(a.trim() + ";");
                            block.addStatement(a.trim() + ";\n");
                        }
                        isAdded = true;
                        continue;
                    }
                    if (!assertion.startsWith("assert (") && !assertion.startsWith("assert(")) {
                        block.addStatement("assert(" + assertion + ");\n");
                    } else {
                        block.addStatement(assertion + "\n");
                    }
                    isAdded = true;
                    continue;
                } else if (s.toString().contains("ch.usi.gassert.visitor.GAVisitor.getInstance().assertionPoint(")) {
                    block.addStatement(s);


                    if (!assertion.startsWith("assert (") && !assertion.startsWith("assert(")) {
                        block.addStatement("assert(" + assertion + ");\n");
                    } else {
                        block.addStatement(assertion + "\n");
                    }
                    isAdded = true;
                    continue;
                }
                block.addStatement(s);

            }
        }
    }
}