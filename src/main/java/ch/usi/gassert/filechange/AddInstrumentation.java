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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

/**
 * this class instruments a given class under test and method under test
 * <p>
 * the assertion point is specified by assert(true);
 */
public class AddInstrumentation {

    private static final boolean isEntryEnabled = false;

    private AddInstrumentation() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) {
        System.out.println(instrumentFromFile(args[0], args[1]));
        //instrumentAndOverwriteFile(args[0], args[1]);
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

        private final String enterMethodSignature = "ch.usi.gassert.visitor.GAVisitor.getInstance().enterMethod(";
        private final String assertionPointSignature = "ch.usi.gassert.visitor.GAVisitor.getInstance().assertionPoint(";

        /**
         * current variable names
         */
        private final Set<String> variablesNames = new HashSet<>();
        /**
         * isDone if I am done to do the instrumentation of the assertion point
         */
        private boolean isDone = false;

        /**
         * if true I found assertion point need to check the next statement
         */
        private boolean isCheckNext = false;

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
            // IMPORTANT I have disabled the entry point instrumentation
            if (isEntryEnabled) {
                block.addStatement(getStatementVisitor(enterMethodSignature, n, null));
            }
            recursiveVisitorStatement(block, n.getBody().get(), n.getType(), n, new HashSet<>());
            n.setBody(block);
        }


        private void addAssertionPoint(final BlockStmt block, final Statement s, final MethodDeclaration n) {
            block.addStatement(getStatementVisitor(assertionPointSignature, n, variablesNames));
            block.addStatement(s);
            isDone = true;
        }


        /**
         * this is the recursive function
         *
         * @param block
         * @param orig
         * @param type
         * @param n
         */
        private void recursiveVisitorStatement(final BlockStmt block, final BlockStmt orig, final Type type, final MethodDeclaration n, final Set<String> currentBlockVariablesNames) {


            // scans all statements in the current block
            for (final Statement s : orig.getStatements()) {
                // if already done just add the statement as it is
                if (isDone) {
                    block.addStatement(s);
                    continue;
                }
                // need to check what is the current statement
                if (isCheckNext) {
                    /* we do it manually
                    if (s.isReturnStmt()) {
                        final String returnValue = s.getChildNodes().get(0).toString();

                        if (variablesNames.contains(returnValue)) {
                            variablesNames.remove(returnValue);
                        }
                        block.addStatement(type.toString() + " returnValueGA = " + s.getChildNodes().get(0).toString() + ";");
                        variablesNames.add("returnValueGA");
                        block.addStatement(getStatementVisitor(assertionPointSignature, n, variablesNames));
                        isDone = true;
                        block.addStatement(new ReturnStmt("returnValueGA"));
                        continue;
                        //}
                    }
                    */
                    addAssertionPoint(block, s, n);
                    continue;
                }
// if I am here need to check if is assertion
                if (s.isAssertStmt()) {
                    isCheckNext = true;
                    // I skip it
                    continue;
                }

                if (s.isExpressionStmt()) {
                    final ExpressionStmt exp = s.asExpressionStmt();
                    if (exp.getExpression().isVariableDeclarationExpr()) {
                        for (final VariableDeclarator decl : exp.getExpression().asVariableDeclarationExpr().getVariables()) {
                            variablesNames.add(decl.getNameAsString());
                            currentBlockVariablesNames.add(decl.getNameAsString());
                        }
                    }
                } else if (s.isIfStmt()) {
                    if (s.asIfStmt().hasThenBlock()) {
                        final BlockStmt blockIf = new BlockStmt();
                        final Set<String> currentBlockVariablesNames2 = new HashSet<>();
                        recursiveVisitorStatement(blockIf, s.asIfStmt().getThenStmt().asBlockStmt(), type, n, currentBlockVariablesNames2);
                        variablesNames.removeAll(currentBlockVariablesNames2);
                        s.asIfStmt().setThenStmt(blockIf);
                    }
                    if (s.asIfStmt().hasElseBlock()) {
                        final BlockStmt blockIf = new BlockStmt();
                        final Set<String> currentBlockVariablesNames2 = new HashSet<>();
                        recursiveVisitorStatement(blockIf, s.asIfStmt().getElseStmt().get().asBlockStmt(), type, n, currentBlockVariablesNames2);
                        variablesNames.removeAll(currentBlockVariablesNames2);
                        s.asIfStmt().setElseStmt(blockIf);
                    }
                } else if (s.isForStmt()) {
                    final BlockStmt blockFor = new BlockStmt();
                    final Set<String> currentBlockVariablesNames2 = new HashSet<>();
                    recursiveVisitorStatement(blockFor, s.asForStmt().getBody().asBlockStmt(), type, n, currentBlockVariablesNames2);
                    variablesNames.removeAll(currentBlockVariablesNames2);
                    s.asForStmt().setBody(blockFor);
                } else if (s.isWhileStmt()) {
                    final BlockStmt blockWhile = new BlockStmt();
                    final Set<String> currentBlockVariablesNames2 = new HashSet<>();
                    recursiveVisitorStatement(blockWhile, s.asWhileStmt().getBody().asBlockStmt(), type, n, currentBlockVariablesNames2);
                    variablesNames.removeAll(currentBlockVariablesNames2);
                    s.asWhileStmt().setBody(blockWhile);
                }
                block.addStatement(s);
            }
            // is possible that after the assertion point there is nothing
            if (isCheckNext == true && isDone == false) {
                block.addStatement(getStatementVisitor(assertionPointSignature, n, variablesNames));
                isDone = true;
            }
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



