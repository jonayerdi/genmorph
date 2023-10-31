package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * this class instruments a given class under test and method under test
 */
public class KeepOnlyFailingTests {


    private KeepOnlyFailingTests() {
        throw new IllegalStateException("Utility class");
    }


    public static void main(final String[] args) {
        addIgnoreAndOverwriteFile(args[0], args[1]);
    }

    public static void addIgnoreAndOverwriteFile(final String javaFilePath, final String testNames) {

        if (!new File(javaFilePath).isDirectory()) {
            if (javaFilePath.endsWith(".java") && javaFilePath.contains("GAssert/subjects") && !javaFilePath.toLowerCase().contains("manual")
                    && !javaFilePath.contains("CreateTestRunnerMainForDaikon")) {
                try {
                    FileUtils.overwriteTextOnFile(javaFilePath, addIgnoreAndOverwriteCU(JavaParser.parse(new File(javaFilePath)), testNames));
                } catch (final FileNotFoundException e) {
                    e.printStackTrace();
                }
                return;
            }
        } else {
            for (final File f : new File(javaFilePath).listFiles()) {
                addIgnoreAndOverwriteFile(f.getAbsolutePath(), testNames);
            }
        }
    }


    public static String addIgnoreAndOverwriteCU(final CompilationUnit cu, final String testNames) {
        cu.accept(new AddIgnoreVisitor(testNames), null);
        return cu.toString().replaceAll("@Test\n", "@Test(timeout=1000)\n");
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class AddIgnoreVisitor extends ModifierVisitor<Void> {

        private List<String> testNames;

        public AddIgnoreVisitor(final String fileNames) {

            testNames = new ArrayList<String>();

            final File file = new File(fileNames);
            if (file.exists()) {
                try {
                    testNames = Files.readAllLines(file.toPath(), Charset.defaultCharset());
                } catch (final IOException ex) {
                    ex.printStackTrace();
                }

            }
            System.out.println("failing tests in file");

            System.out.println(testNames);
        }

        @Override
        public Visitable visit(final MethodDeclaration n, final Void arg) {
            if (n.getName().toString().startsWith("test") && n.getBody().isPresent()) {
                for (final String testName : testNames) {
                    final String classMethodName = ((ClassOrInterfaceDeclaration) n.getParentNode().get()).getName() + "." + n.getName();
                    if (testName.endsWith(classMethodName)) {
                        return super.visit(n, arg);
                    }
                }
            }
            return null;
        }

    }

}



