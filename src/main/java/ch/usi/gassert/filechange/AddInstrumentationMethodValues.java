package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;
import ch.usi.gassert.util.StringUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.github.javaparser.JavaParser.parseStatement;

public class AddInstrumentationMethodValues {

    public static final String VISITOR_CLASS = "ch.usi.gassert.serialization.ValuesSerializingMethodVisitor";

    private AddInstrumentationMethodValues() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) {
        if (args.length != 5) {
            System.err.println("Wrong number of parameters: 5 arguments expected, got " + args.length);
            System.err.println("Mode (replace|backup|stdout)");
            System.err.println("Java source file path");
            System.err.println("System ID");
            System.err.println("Method name");
            System.err.println("Method index");
            System.exit(1);
        }
        Iterator<String> arguments = Arrays.stream(args).sequential().iterator();
        final String mode = arguments.next();
        final String javaFilePath = arguments.next();
        final String systemId = arguments.next();
        final String methodName = arguments.next();
        final int methodIndex = Integer.parseInt(arguments.next());
        final String visitorClass = VISITOR_CLASS;
        switch (mode) {
            case "replace": instrumentAndOverwriteFile(javaFilePath, systemId, methodName, methodIndex, visitorClass); break;
            case "backup": instrumentAndBackupFile(javaFilePath, systemId, methodName, methodIndex, visitorClass); break;
            case "stdout": System.out.println(instrumentFromFile(javaFilePath, systemId, methodName, methodIndex, visitorClass)); break;
            default: throw new RuntimeException("Unsupported mode: \"" + mode + "\"");
        }
    }

    public static void instrumentAndOverwriteFile(final String javaFilePath, final String systemId,
                                                  final String methodName, int methodIndex,
                                                  String visitorClass) {
        FileUtils.overwriteTextOnFile(javaFilePath, instrumentFromFile(javaFilePath, systemId, methodName, methodIndex, visitorClass));
    }

    public static void instrumentAndBackupFile(final String javaFilePath, final String systemId,
                                               final String methodName, int methodIndex,
                                               String visitorClass) {
        final File file = new File(javaFilePath);
        final File backup = new File(javaFilePath + ".bak");
        if (!backup.exists()) {
            try {
                Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Could not move " + file.getPath() + " to " + backup.getPath(), e);
            }
        }
        FileUtils.overwriteTextOnFile(javaFilePath, instrumentFromFile(backup.getPath(), systemId, methodName, methodIndex, visitorClass));
    }

    public static String instrumentFromFile(final String javaFilePath, String systemId,
                                            String methodName, int methodIndex,
                                            String visitorClass) {
        try {
            return instrument(JavaParser.parse(new FileInputStream(javaFilePath)),
                    systemId, methodName, methodIndex, visitorClass);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String instrumentFromString(final String javaFileString, String systemId,
                                              String methodName, int methodIndex,
                                              String visitorClass) {
        return instrument(JavaParser.parse(javaFileString), systemId, methodName, methodIndex, visitorClass);
    }

    public static String instrument(final CompilationUnit cu, String systemId,
                                    String methodName, int methodIndex,
                                    String visitorClass) {
        cu.accept(new MethodValuesInstrumentorVisitor(systemId, methodName, methodIndex, visitorClass), null);
        return cu.toString();
    }

    private static class MethodValuesInstrumentorVisitor extends VoidVisitorAdapter<Void> {

        private static final Comment commentBegin = new BlockComment(" BEGIN GAssert instrumented method ");
        private static final Comment commentEnd = new BlockComment(" END GAssert instrumented method ");

        private final String visitorClass;
        private final String enterMethodSignature;
        private final String exitMethodSignature;
        private final String foundLiteralSignature;
        private final String foundVariableSignature;

        private final String systemId;
        private final String methodName;
        private final int methodIndex;

        private int methodFoundCount;
        private int returnVarCount;
        private int varCount;

        public MethodValuesInstrumentorVisitor(String systemId, String methodName, int methodIndex, String visitorClass) {
            this.systemId = systemId;
            this.methodName = methodName;
            this.methodIndex = methodIndex;
            this.visitorClass = visitorClass;
            this.enterMethodSignature = visitorClass + ".enter(";
            this.exitMethodSignature = visitorClass + ".exit(";
            this.foundLiteralSignature = visitorClass + ".foundLiteral(";
            this.foundVariableSignature = visitorClass + ".foundVariable(";
            this.methodFoundCount = 0;
            this.returnVarCount = 0;
            this.varCount = 0;
        }

        @Override
        public void visit(final MethodDeclaration method, final Void arg) {
            if (this.methodName.equals(method.getName().getIdentifier())) {
                if (methodIndex == methodFoundCount && !isInstrumented(method)) {
                    final BlockStmt instrumentedBlock = instrumentMethod(method);
                    method.setBody(instrumentedBlock);
                }
                this.methodFoundCount += 1;
            }
        }

        private String makeReturnVar() {
            return "__gassert__retval" + returnVarCount++;
        }

        private int makeVariableId() {
            return varCount++;
        }

        private boolean isInstrumented(final MethodDeclaration method) {
            try {
                final BlockStmt methodBody = method.getBody().orElseThrow(() -> new NoSuchElementException("Method with no body"));
                return methodBody.getAllContainedComments().stream().anyMatch(c -> c.getContent().equals(commentBegin.getContent()));
            } catch (Exception ignored) {}
            return false;
        }

        private BlockStmt instrumentMethod(final MethodDeclaration method) {
            // Instrument method body
            final BlockStmt originalBlock = method.getBody().orElseThrow(() -> new NoSuchElementException("Method with no body"));
            final String returnType = method.getTypeAsString();
            final BlockStmt instrumentedBlock = new BlockStmt();
            final Statement enterStmt = JavaParser.parseStatement(enterMethodCall());
            enterStmt.setComment(commentBegin);
            instrumentedBlock.addStatement(enterStmt);
            instrumentBlockStmt(originalBlock, instrumentedBlock, returnType);
            if (!instrumentedBlock.getStatement(instrumentedBlock.getStatements().size() - 1).isReturnStmt() && returnType.equals("void")) {
                instrumentedBlock.addStatement(exitMethodCall());
            }
            instrumentedBlock.addOrphanComment(commentEnd);
            return instrumentedBlock;
        }

        private BlockStmt instrumentBlockStmt(final BlockStmt originalBlock, final BlockStmt instrumentedBlock, final String returnType) {
            for (final Statement stmt : originalBlock.getStatements()) {
                for (final Statement instrumentedStmt : instrumentStatement(stmt, returnType)) {
                    instrumentedBlock.addStatement(instrumentedStmt);
                }
            }
            return instrumentedBlock;
        }

        private List<Statement> instrumentStatement(final Statement originalStmt, final String returnType) {
            if (originalStmt.isBlockStmt()) {
                List<Statement> statements = new ArrayList<>(1);
                statements.add(instrumentBlockStmt(originalStmt.asBlockStmt(), new BlockStmt(), returnType));
                return statements;
            } else if (originalStmt.isIfStmt()) {
                final IfStmt ifStmt = originalStmt.asIfStmt();
                ifStmt.setCondition(instrumentExpression(ifStmt.getCondition()));
                ifStmt.setThenStmt(intoSingleStatement(instrumentStatement(ifStmt.getThenStmt(), returnType)));
                if (ifStmt.getElseStmt().isPresent()) {
                    ifStmt.setElseStmt(intoSingleStatement(instrumentStatement(ifStmt.getElseStmt().get(), returnType)));
                }
            } else if (originalStmt.isDoStmt()) {
                final DoStmt doStmt = originalStmt.asDoStmt();
                doStmt.setCondition(instrumentExpression(doStmt.getCondition()));
                doStmt.setBody(intoSingleStatement(instrumentStatement(doStmt.getBody(), returnType)));
            } else if (originalStmt.isWhileStmt()) {
                final WhileStmt whileStmt = originalStmt.asWhileStmt();
                whileStmt.setCondition(instrumentExpression(whileStmt.getCondition()));
                whileStmt.setBody(intoSingleStatement(instrumentStatement(whileStmt.getBody(), returnType)));
            } else if (originalStmt.isForStmt()) {
                final ForStmt forStmt = originalStmt.asForStmt();
                final List<Expression> initialization = forStmt.getInitialization();
                for (int i = 0 ; i < initialization.size() ; ++i) {
                    initialization.set(i, instrumentExpression(initialization.get(i)));
                }
                forStmt.getCompare().ifPresent(compare -> forStmt.setCompare(instrumentExpression(compare)));
                final List<Expression> update = forStmt.getUpdate();
                for (int i = 0 ; i < update.size() ; ++i) {
                    update.set(i, instrumentExpression(update.get(i)));
                }
                forStmt.setBody(intoSingleStatement(instrumentStatement(forStmt.getBody(), returnType)));
            } else if (originalStmt.isForeachStmt()) {
                final ForeachStmt foreachStmt = originalStmt.asForeachStmt();
                foreachStmt.setBody(intoSingleStatement(instrumentStatement(foreachStmt.getBody(), returnType)));
            } else if (originalStmt.isSwitchStmt()) {
                final SwitchStmt switchStmt = originalStmt.asSwitchStmt();
                for (final SwitchEntryStmt entry : switchStmt.getEntries()) {
                    // Cannot instrument label because it needs to be a constant expression
                    //entry.getLabel().ifPresent(label -> entry.setLabel(instrumentExpression(label)));
                    final List<Statement> instrumentedStatements = new ArrayList<>();
                    for (final Statement s : entry.getStatements()) {
                        instrumentedStatements.addAll(instrumentStatement(s, returnType));
                    }
                    entry.setStatements(new NodeList<>(instrumentedStatements));
                }
            } else if(originalStmt.isTryStmt()) {
                final TryStmt tryStmt = originalStmt.asTryStmt();
                tryStmt.setTryBlock(instrumentBlockStmt(tryStmt.getTryBlock(), new BlockStmt(), returnType));
                for (final CatchClause catchClause : tryStmt.getCatchClauses()) {
                    catchClause.setBody(instrumentBlockStmt(catchClause.getBody(), new BlockStmt(), returnType));
                }
                if (tryStmt.getFinallyBlock().isPresent()) {
                    tryStmt.setFinallyBlock(instrumentBlockStmt(tryStmt.getFinallyBlock().get(), new BlockStmt(), returnType));
                }
            } else if (originalStmt.isReturnStmt()) {
                final ReturnStmt returnStmt = originalStmt.asReturnStmt();
                final Optional<Expression> returnExpr = returnStmt.getExpression();
                if (returnExpr.isPresent()) {
                    // Evaluate the return expression into the returnVar variable (with instrumentation),
                    // then do the exitMethodCall, and then return returnVar.
                    // This is to avoid evaluating the return expression twice.
                    List<Statement> statements = new ArrayList<>(3);
                    final String returnVar = this.makeReturnVar();
                    statements.add(parseStatement("final " + returnType + " " + returnVar + " = " + instrumentExpression(returnExpr.get()) + ";"));
                    statements.add(parseStatement(exitMethodCall()));
                    statements.add(parseStatement("return " + returnVar + ";"));
                    return statements;
                } else {
                    List<Statement> statements = new ArrayList<>(2);
                    statements.add(parseStatement(exitMethodCall()));
                    statements.add(originalStmt);
                    return statements;
                }
            } else if (originalStmt.isExpressionStmt()) {
                final List<Statement> statements = new ArrayList<>(1);
                final Expression expression = originalStmt.asExpressionStmt().getExpression();
                statements.add(new ExpressionStmt(instrumentExpression(expression)));
                return statements;
            }
            List<Statement> statements = new ArrayList<>(1);
            statements.add(originalStmt);
            return statements;
        }

        private Expression instrumentExpression(final Expression originalExpr) {
            //System.out.println(originalExpr + "\t\t" + originalExpr.getClass().getSimpleName());
            if (originalExpr.toString().startsWith(visitorClass)) {
                // Avoid double-instrumenting the expression
                return originalExpr;
            }
            if (originalExpr.isLiteralExpr()) {
                final String originalExprString = originalExpr.toString();
                if (originalExprString.equals("true") || originalExprString.equals("false") || originalExprString.equals("null")) {
                    // Avoid instrumenting boolean literals, they are handled specially in conditionals
                    // We also do not care about null
                    return originalExpr;
                } else {
                    return JavaParser.parseExpression(foundLiteralCall(originalExprString));
                }
            } else if (originalExpr.isNameExpr() || originalExpr.isFieldAccessExpr()) {
                return JavaParser.parseExpression(foundVariableCall(originalExpr.toString()));
            } else if (originalExpr.isAssignExpr()) {
                final AssignExpr assignExpr = originalExpr.asAssignExpr();
                return new AssignExpr(assignExpr.getTarget(), instrumentExpression(assignExpr.getValue()), assignExpr.getOperator());
            } else if(originalExpr.isVariableDeclarationExpr()) {
                final VariableDeclarationExpr variableDeclarationExpr = originalExpr.asVariableDeclarationExpr();
                final NodeList<VariableDeclarator> newVariables = new NodeList<>(variableDeclarationExpr.getVariables());
                for (final VariableDeclarator vd : newVariables) {
                    vd.getInitializer().ifPresent(expression -> vd.setInitializer(instrumentExpression(expression)));
                }
                return new VariableDeclarationExpr(
                        variableDeclarationExpr.getTokenRange().orElse(null),
                        variableDeclarationExpr.getModifiers(),
                        variableDeclarationExpr.getAnnotations(),
                        newVariables);
            } else if(originalExpr.isUnaryExpr()) {
                final UnaryExpr unaryExpr = originalExpr.asUnaryExpr();
                final UnaryExpr.Operator operator = unaryExpr.getOperator();
                final boolean requiresAssignable = operator.asString().length() > 1; // ++ and --
                return requiresAssignable // Avoid instrumenting ++ and --, since they require a VARIABLE, not just a VALUE
                        ? unaryExpr
                        : new UnaryExpr(
                        unaryExpr.getTokenRange().orElse(null),
                        instrumentExpression(unaryExpr.getExpression()),
                        unaryExpr.getOperator()
                );
            } else if (originalExpr.isBinaryExpr()) {
                final BinaryExpr binaryExpr = originalExpr.asBinaryExpr();
                return new BinaryExpr(
                        instrumentExpression(binaryExpr.getLeft()),
                        instrumentExpression(binaryExpr.getRight()),
                        binaryExpr.getOperator()
                );
            } else if (originalExpr.isEnclosedExpr()) {
                final EnclosedExpr enclosedExpr = originalExpr.asEnclosedExpr();
                return new EnclosedExpr(enclosedExpr.getTokenRange().orElse(null), instrumentExpression(enclosedExpr.getInner()));
            } else if (originalExpr.isMethodCallExpr()) {
                final MethodCallExpr methodCallExpr = originalExpr.asMethodCallExpr();
                final NodeList<Expression> instrumentedArguments = new NodeList<>(methodCallExpr.getArguments());
                for (int i = 0 ; i < instrumentedArguments.size() ; ++i) {
                    instrumentedArguments.set(i, instrumentExpression(instrumentedArguments.get(i)));
                }
                return new MethodCallExpr(
                    methodCallExpr.getTokenRange().orElse(null),
                    methodCallExpr.getScope().orElse(null),
                    methodCallExpr.getTypeArguments().orElse(null),
                    methodCallExpr.getName(),
                    instrumentedArguments
                );
            } else {
                return originalExpr;
                //throw new RuntimeException("Unhandled expression type: "
                //        + originalExpr + " (" + originalExpr.getClass().getSimpleName() + ")");
            }
        }

        private Statement intoSingleStatement(final List<Statement> statements) {
            if (statements.size() == 1) {
                return statements.get(0);
            }
            return new BlockStmt(new NodeList<>(statements));
        }

        private String enterMethodCall() {
            final List<String> args = new ArrayList<>(3);
            args.add(StringUtils.quote(systemId));
            args.add(StringUtils.quote(methodName));
            args.add(Integer.toString(methodIndex));
            return enterMethodSignature + String.join(",", args) + ");";
        }

        private String exitMethodCall() {
            final List<String> args = new ArrayList<>(3);
            args.add(StringUtils.quote(systemId));
            args.add(StringUtils.quote(methodName));
            args.add(Integer.toString(methodIndex));
            return exitMethodSignature + String.join(",", args) + ");";
        }

        private String foundLiteralCall(final String expression) {
            return foundLiteralSignature + expression + ")";
        }

        private String foundVariableCall(final String expression) {
            return foundVariableSignature + makeVariableId() + ", " + expression + ")";
        }

    }

}
