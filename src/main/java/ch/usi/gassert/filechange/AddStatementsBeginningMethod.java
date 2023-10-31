package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.util.Set;

/**
 * this class instruments a given class under test and method under test
 * <p>
 * the assertion point is specified by assert(true);
 */
public class AddStatementsBeginningMethod {
    private AddStatementsBeginningMethod() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) {
        addAndOverwriteFile(args[0], args[1], args[2]);
    }

    public static void addAndOverwriteFile(final String javaFilePath, final String methodName, final String lines) {
        FileUtils.overwriteTextOnFile(javaFilePath, instrumentFromFile(javaFilePath, methodName, lines));
    }

    public static String instrumentFromFile(final String javaFilePath, final String methodName, final String lines) {
        try {
            return instrument(JavaParser.parse(new FileInputStream(javaFilePath)), methodName, lines);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String instrument(final CompilationUnit cu, final String methodName, final String lines) {
        cu.accept(new InstrumentorVisitor(methodName, lines), null);
        return cu.toString();
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class InstrumentorVisitor extends VoidVisitorAdapter<Void> {

        private String oldValuesList = "old-values-initialization.list";


        private final String methodName;
        private final String lines;
        /**
         * the parameter string is cached for performance issues
         */
        private String parameterStringCache;

        private final String enterMethodSignature = "ch.usi.gassert.visitor.GAVisitorForOldValues.getInstance().enterMethod(";

        public InstrumentorVisitor(final String methodName, final String lines) {
            this.methodName = methodName;
            this.lines = lines;
        }

        @Override
        public void visit(final MethodDeclaration n, final Void arg) {
            // skip it if is not the method I want
            if (!n.getNameAsString().equals(methodName)) {
                return;
            }
            final BlockStmt block = new BlockStmt();
            try {
                final File file = new File(oldValuesList);
                final FileReader fr = new FileReader(file);
                final BufferedReader br = new BufferedReader(fr);
                String fileLine;
                while ((fileLine = br.readLine()) != null && fileLine.contains("=")) {
                    block.addStatement(fileLine.replace("old_this.", "old_this_"));
                    block.getStatements().get(block.getStatements().size() - 1).setComment(new LineComment("instrumentation"));
                }
                br.close();
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
            if (lines.contains("\n")) {
                for (final String s : lines.split("\n")) {
                    if (block.getStatements().toString().contains(s)) {
                        continue;
                    }
                    block.addStatement(s.replace("old_this.", "old_this_"));
                    block.getStatements().get(block.getStatements().size() - 1).setComment(new LineComment("instrumentation"));
                }
            }
            for (final Statement s : n.getBody().get().getStatements()) {
                block.addStatement(s);
            }
            n.setBody(block);
        }
    }


}



