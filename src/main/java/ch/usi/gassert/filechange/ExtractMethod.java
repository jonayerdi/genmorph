package ch.usi.gassert.filechange;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;

/**
 * this class simply remove the assertions from the test generated we need this because some tests might fail.
 * remove both junit and java assertions
 */
public class ExtractMethod {

    private ExtractMethod() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(String args[]) {
        extractMethod(args[0], args[1]);
    }

    public static void extractMethod(String javaFilePath, String methodName) {
        try {
            JavaParser.parse(new File(javaFilePath)).accept(new ExtractMethodVisitor(methodName), null);
        } catch (Exception e) {
            System.exit(-1);
            e.printStackTrace();
        }
    }


    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class ExtractMethodVisitor extends VoidVisitorAdapter<Void> {

        private final String methodName;

        public ExtractMethodVisitor(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            // skip it if is not the method I want
            if (!n.getNameAsString().equals(methodName)) {
                return;
            }
            //this is essential do not remove
            System.out.println(n.getBody().get());
        }
    }
}