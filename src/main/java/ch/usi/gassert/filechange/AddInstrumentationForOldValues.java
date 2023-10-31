package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

/**
 * this class instruments a given class under test and method under test
 * <p>
 * the assertion point is specified by assert(true);
 */
public class AddInstrumentationForOldValues {

    private AddInstrumentationForOldValues() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) {
        instrumentAndOverwriteFile(args[0], args[1]);
    }

    public static void instrumentAndOverwriteFile(final String javaFilePath, final String methodName) {
        FileUtils.overwriteTextOnFile(javaFilePath, instrumentFromFile(javaFilePath, methodName));
    }

    public static String instrumentFromFile(final String javaFilePath, final String methodName) {
        try {
            return instrument(JavaParser.parse(new FileInputStream(javaFilePath)), methodName);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String instrumentFromString(final String javaFileString, final String methodName) throws FileNotFoundException {
        return instrument(JavaParser.parse(javaFileString), methodName);
    }

    public static String instrument(final CompilationUnit cu, final String methodName) {
        cu.accept(new InstrumentorVisitor(methodName), null);
        return cu.toString();
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class InstrumentorVisitor extends VoidVisitorAdapter<Void> {

        private final String methodName;
        /**
         * the parameter string is cached for performance issues
         */
        private String parameterStringCache;

        private final String enterMethodSignature = "ch.usi.gassert.visitor.GAVisitorForOldValues.getInstance().enterMethod(";

        public InstrumentorVisitor(final String methodName) {
            this.methodName = methodName;
        }

        @Override
        public void visit(final MethodDeclaration n, final Void arg) {
            // skip it if is not the method I want
            if (!n.getNameAsString().equals(methodName)) {
                return;
            }
            final BlockStmt block = new BlockStmt();
            block.addStatement(getStatementVisitor(enterMethodSignature, n, null));
            for (final Statement s : n.getBody().get().getStatements()) {
                block.addStatement(s);
            }
            n.setBody(block);
        }

        private String getStatementVisitor(final String signature, final MethodDeclaration n, final Set<String> variablesNames) {
            final StringBuilder sb = new StringBuilder();
            sb.append(signature);
            sb.append("\"" + n.getNameAsString() + "\",");
            sb.append(getMethodParameters(n));
            if (variablesNames != null && !variablesNames.isEmpty()) {
                sb.append(getVariables(variablesNames));
            }
            sb.append(");");
            sb.append(System.lineSeparator());
            return sb.toString().replace(",,", ",");
        }


        private String getVariables(final Set<String> variablesNames) {
            final StringBuilder sb = new StringBuilder();
            int count = 0;
            sb.append(",");
            for (final String name : variablesNames) {
                sb.append("\"");
                sb.append(name);
                sb.append("\",");
                sb.append(name);
                count++;
                if (count != variablesNames.size()) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }

        private String getMethodParameters(final MethodDeclaration n) {
            if (parameterStringCache != null) {
                return parameterStringCache;
            }
            final StringBuilder sb = new StringBuilder();
            if (!n.isStatic()) {
                sb.append("\"this\",");
                sb.append("this");
            }
            int count = 0;
            for (final Parameter p : n.getParameters()) {
                if (count == 0) { // do it only if there are parameters
                    sb.append(",");
                }
                sb.append("\"");
                sb.append(p.getName());
                sb.append("\",");
                sb.append(p.getName());
                count++;
                if (count != n.getParameters().size()) {
                    sb.append(",");
                }
            }
            parameterStringCache = sb.toString();
            return parameterStringCache;
        }
    }

}



