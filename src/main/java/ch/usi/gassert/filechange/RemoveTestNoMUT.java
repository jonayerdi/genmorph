package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * this class instruments a given class under test and method under test
 */
public class RemoveTestNoMUT {


    private RemoveTestNoMUT() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(String args[]) {
        removeNoMUTTestsAndOverwriteFile(args[0], args[1]);
    }

    public static void removeNoMUTTestsAndOverwriteFile(String javaFilePath, String methodName) {

        if (!new File(javaFilePath).isDirectory()) {
            if (javaFilePath.endsWith(".java") && javaFilePath.contains("GAssert/subjects") && !javaFilePath.toLowerCase().contains("manual")) {
                try {
                    FileUtils.overwriteTextOnFile(javaFilePath, removeNoMUTTestsAndOverwriteCU(JavaParser.parse(new File(javaFilePath)), methodName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return;
            }
        } else {
            for (File f : new File(javaFilePath).listFiles()) {
                removeNoMUTTestsAndOverwriteFile(f.getAbsolutePath(), methodName);
            }
        }
    }


    public static String removeNoMUTTestsAndOverwriteCU(CompilationUnit cu, String methodName) {
        cu.accept(new RemoveTestVisitor(methodName), null);
        return cu.toString().replaceAll("@Test\n", "@Test(timeout=1000)\n");
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class RemoveTestVisitor extends ModifierVisitor<Void> {

        private final String methodName;

        public RemoveTestVisitor(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public Visitable visit(MethodDeclaration n, Void arg) {
            if (n.getName().toString().startsWith("test") && n.getBody().isPresent() && !n.getBody().get().toString().contains("." + methodName + "(")) {
                return null;
            }
            return super.visit(n, arg);
        }

    }

}



