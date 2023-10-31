package ch.usi.gassert.filechange;

import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.FileUtils;
import ch.usi.gassert.util.StringUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.github.javaparser.JavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.JavaParser.parseStatement;

public class AddInstrumentationMethod {

    private AddInstrumentationMethod() {
        throw new IllegalStateException("Utility class");
    }

    public enum Mode {
        INSTRUMENT, COMMENTS, FORMAT
    }

    public static void main(final String[] args) {
        if (args.length < 5 || args.length > 6) {
            System.err.println("Wrong number of parameters: 5 or 6 arguments expected, got " + args.length);
            System.err.println("Mode (replace|backup|stdout|comments|format)");
            System.err.println("Java source file path");
            System.err.println("System ID");
            System.err.println("Method name");
            System.err.println("Method index");
            System.err.println("[Visitor class (default: NONE)]");
            System.exit(1);
        }
        Iterator<String> arguments = Arrays.stream(args).sequential().iterator();
        final String mode = arguments.next();
        final String javaFilePath = arguments.next();
        final String systemId = arguments.next();
        final String methodName = arguments.next();
        final int methodIndex = Integer.parseInt(arguments.next());
        final String visitorClass = arguments.hasNext() ? arguments.next() : "Not Specified";
        switch (mode) {
            case "replace": instrumentAndOverwriteFile(javaFilePath, systemId, methodName, methodIndex, Mode.INSTRUMENT, visitorClass); break;
            case "backup": instrumentAndBackupFile(javaFilePath, systemId, methodName, methodIndex, Mode.INSTRUMENT, visitorClass); break;
            case "stdout": System.out.println(instrumentFromFile(javaFilePath, systemId, methodName, methodIndex, Mode.INSTRUMENT, visitorClass)); break;
            case "comments": System.out.println(instrumentFromFile(javaFilePath, systemId, methodName, methodIndex, Mode.COMMENTS, visitorClass)); break;
            case "format": instrumentAndOverwriteFile(javaFilePath, systemId, methodName, methodIndex, Mode.FORMAT, visitorClass); break;
            default: throw new RuntimeException("Unsupported mode: \"" + mode + "\"");
        }
    }

    public static void instrumentAndOverwriteFile(final String javaFilePath, final String systemId,
                                                  final String methodName, int methodIndex,
                                                  Mode mode, String visitorClass) {
        FileUtils.overwriteTextOnFile(javaFilePath, instrumentFromFile(javaFilePath, systemId, methodName, methodIndex, mode, visitorClass));
    }

    public static void instrumentAndBackupFile(final String javaFilePath, final String systemId,
                                               final String methodName, int methodIndex,
                                               Mode mode, String visitorClass) {
        final File file = new File(javaFilePath);
        final File backup = new File(javaFilePath + ".bak");
        if (!backup.exists()) {
            try {
                Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Could not move " + file.getPath() + " to " + backup.getPath(), e);
            }
        }
        FileUtils.overwriteTextOnFile(javaFilePath, instrumentFromFile(backup.getPath(), systemId, methodName, methodIndex, mode, visitorClass));
    }

    public static String instrumentFromFile(final String javaFilePath, String systemId,
                                            String methodName, int methodIndex,
                                            Mode mode, String visitorClass) {
        try {
            return instrument(JavaParser.parse(new FileInputStream(javaFilePath)),
                    systemId, methodName, methodIndex, mode, visitorClass);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String instrumentFromString(final String javaFileString, String systemId,
                                              String methodName, int methodIndex,
                                              Mode mode, String visitorClass) {
        return instrument(JavaParser.parse(javaFileString), systemId, methodName, methodIndex, mode, visitorClass);
    }

    public static String instrument(final CompilationUnit cu, String systemId,
                                    String methodName, int methodIndex,
                                    Mode mode, String visitorClass) {
        cu.accept(new MethodCallInstrumentorVisitor(systemId, methodName, methodIndex, mode, visitorClass), null);
        return cu.toString();
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private static class MethodCallInstrumentorVisitor extends VoidVisitorAdapter<Void> {

        private static final Comment commentBegin = new BlockComment(" BEGIN GAssert instrumented method ");
        private static final Comment commentEnd = new BlockComment(" END GAssert instrumented method ");

        private final String enterMethodSignature;
        private final String exitMethodSignature;

        private final String systemId;
        private final String methodName;
        private final int methodIndex;
        private final Mode mode;

        private int methodFoundCount;
        private int returnVarCount;

        public MethodCallInstrumentorVisitor(String systemId, String methodName, int methodIndex,
                                             Mode mode, String visitorClass) {
            this.systemId = systemId;
            this.methodName = methodName;
            this.methodIndex = methodIndex;
            this.mode = mode;
            this.enterMethodSignature = visitorClass + ".enter(";
            this.exitMethodSignature = visitorClass + ".exit(";
            this.methodFoundCount = 0;
            this.returnVarCount = 0;
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

        private boolean isInstrumented(final MethodDeclaration method) {
            try {
                final BlockStmt methodBody = method.getBody().orElseThrow(() -> new NoSuchElementException("Method with no body"));
                return methodBody.getAllContainedComments().stream().anyMatch(c -> c.getContent().equals(commentBegin.getContent()));
                /*
                final Statement firstStatement = methodBody.getStatement(0);
                return firstStatement
                        .asTryStmt().getTryBlock()
                        .getStatement(0).toString()
                        .startsWith(MethodCallInstrumentorVisitor.enterMethodSignature);
                */
            } catch (Exception ignored) {}
            return false;
        }

        private BlockStmt instrumentMethod(final MethodDeclaration method) {
            final BlockStmt originalBlock = method.getBody().orElseThrow(() -> new NoSuchElementException("Method with no body"));
            switch (mode) {
                case FORMAT:
                    return originalBlock;
                case COMMENTS:
                    originalBlock.setComment(commentBegin);
                    originalBlock.addOrphanComment(commentEnd);
                    return originalBlock;
                case INSTRUMENT:
                    // Instrument method body: First statement + returns
                    final String returnType = method.getTypeAsString();
                    final BlockStmt instrumentedBlock = new BlockStmt();
                    final List<String> methodParamsInput = getMethodParameters(method);
                    final List<String> methodParamsOutput = getMethodParameters(method);
                    instrumentedBlock.addStatement(enterMethodCall(methodParamsInput));
                    instrumentBlockStmt(originalBlock, instrumentedBlock, methodParamsOutput, returnType);
                    if (!instrumentedBlock.getStatement(instrumentedBlock.getStatements().size() - 1).isReturnStmt() && returnType.equals("void")) {
                        instrumentedBlock.addStatement(exitMethodCall(methodParamsOutput));
                    }
                    // Wrap method in try/catch block for instrumentation in case the method throws
                    final String exceptionVar = this.makeReturnVar();
                    final Parameter catchParameter = new Parameter(parseClassOrInterfaceType("Exception"), exceptionVar);
                    List<String> exitParams = new ArrayList<>(methodParamsOutput);
                    exitParams.add(StringUtils.quote("return"));
                    exitParams.add(exceptionVar + ".getClass()");
                    exitParams.add(exceptionVar);
                    exitParams.add("false");
                    final BlockStmt catchBody = new BlockStmt();
                    catchBody.addStatement(parseStatement(exitMethodCall(exitParams)));
                    catchBody.addStatement(new ThrowStmt(new NameExpr(exceptionVar)));
                    final CatchClause catchClause = new CatchClause(catchParameter, catchBody);
                    final TryStmt tryStmt = new TryStmt(instrumentedBlock, new NodeList<>(catchClause), null);
                    tryStmt.setComment(commentBegin);
                    final BlockStmt finalBlock = new BlockStmt();
                    finalBlock.addStatement(tryStmt);
                    finalBlock.addOrphanComment(commentEnd);
                    return finalBlock;
                default:
                    throw new IllegalArgumentException("Invalid mode: " + mode);
            }
        }

        private BlockStmt instrumentBlockStmt(final BlockStmt originalBlock, final BlockStmt instrumentedBlock, final List<String> methodParamsOutput, final String returnType) {
            for (final Statement stmt : originalBlock.getStatements()) {
                for (final Statement instrumentedStmt : instrumentStatement(stmt, methodParamsOutput, returnType)) {
                    instrumentedBlock.addStatement(instrumentedStmt);
                }
            }
            return instrumentedBlock;
        }

        private List<Statement> instrumentStatement(final Statement originalStmt, final List<String> methodParamsOutput, final String returnType) {
            if (originalStmt.isBlockStmt()) {
                List<Statement> statements = new ArrayList<>(1);
                statements.add(instrumentBlockStmt(originalStmt.asBlockStmt(), new BlockStmt(), methodParamsOutput, returnType));
                return statements;
            } else if (originalStmt.isIfStmt()) {
                final IfStmt ifStmt = originalStmt.asIfStmt();
                ifStmt.setThenStmt(intoSingleStatement(instrumentStatement(ifStmt.getThenStmt(), methodParamsOutput, returnType)));
                if (ifStmt.getElseStmt().isPresent()) {
                    ifStmt.setElseStmt(intoSingleStatement(instrumentStatement(ifStmt.getElseStmt().get(), methodParamsOutput, returnType)));
                }
            } else if (originalStmt.isDoStmt()) {
                final DoStmt doStmt = originalStmt.asDoStmt();
                doStmt.setBody(intoSingleStatement(instrumentStatement(doStmt.getBody(), methodParamsOutput, returnType)));
            } else if (originalStmt.isWhileStmt()) {
                final WhileStmt whileStmt = originalStmt.asWhileStmt();
                whileStmt.setBody(intoSingleStatement(instrumentStatement(whileStmt.getBody(), methodParamsOutput, returnType)));
            } else if (originalStmt.isForStmt()) {
                final ForStmt forStmt = originalStmt.asForStmt();
                forStmt.setBody(intoSingleStatement(instrumentStatement(forStmt.getBody(), methodParamsOutput, returnType)));
            } else if (originalStmt.isForeachStmt()) {
                final ForeachStmt foreachStmt = originalStmt.asForeachStmt();
                foreachStmt.setBody(intoSingleStatement(instrumentStatement(foreachStmt.getBody(), methodParamsOutput, returnType)));
            } else if (originalStmt.isSwitchStmt()) {
                final SwitchStmt switchStmt = originalStmt.asSwitchStmt();
                for (final SwitchEntryStmt entry : switchStmt.getEntries()) {
                    final List<Statement> instrumentedStatements = new ArrayList<>();
                    for (final Statement s : entry.getStatements()) {
                        instrumentedStatements.addAll(instrumentStatement(s, methodParamsOutput, returnType));
                    }
                    entry.setStatements(new NodeList<>(instrumentedStatements));
                }
            } else if(originalStmt.isTryStmt()) {
                final TryStmt tryStmt = originalStmt.asTryStmt();
                tryStmt.setTryBlock(instrumentBlockStmt(tryStmt.getTryBlock(), new BlockStmt(), methodParamsOutput, returnType));
                for (final CatchClause catchClause : tryStmt.getCatchClauses()) {
                    catchClause.setBody(instrumentBlockStmt(catchClause.getBody(), new BlockStmt(), methodParamsOutput, returnType));
                }
                if (tryStmt.getFinallyBlock().isPresent()) {
                    tryStmt.setFinallyBlock(instrumentBlockStmt(tryStmt.getFinallyBlock().get(), new BlockStmt(), methodParamsOutput, returnType));
                }
            } else if (originalStmt.isReturnStmt()) {
                final Optional<Expression> returnExpr = originalStmt.asReturnStmt().getExpression();
                if (returnExpr.isPresent()) {
                    // Evaluate the return expression into the returnVar variable,
                    // then call serialization, and then return returnVar.
                    // This is to avoid evaluating the return expression twice.
                    final String returnVar = this.makeReturnVar();
                    List<String> exitParams = new ArrayList<>(methodParamsOutput);
                    exitParams.add(StringUtils.quote("return"));
                    exitParams.add(ClassUtils.getTypeWithoutGenerics(returnType) + ".class");
                    exitParams.add(returnVar);
                    exitParams.add("true");
                    List<Statement> statements = new ArrayList<>(3);
                    statements.add(parseStatement("final " + returnType + " " + returnVar + " = " + returnExpr.get() + ";"));
                    statements.add(parseStatement(exitMethodCall(exitParams)));
                    statements.add(parseStatement("return " + returnVar + ";"));
                    return statements;
                } else {
                    List<Statement> statements = new ArrayList<>(2);
                    statements.add(parseStatement(exitMethodCall(methodParamsOutput)));
                    statements.add(originalStmt);
                    return statements;
                }
            }
            List<Statement> statements = new ArrayList<>(1);
            statements.add(originalStmt);
            return statements;
        }

        private Statement intoSingleStatement(final List<Statement> statements) {
            if (statements.size() == 1) {
                return statements.get(0);
            }
            return new BlockStmt(new NodeList<>(statements));
        }

        private String enterMethodCall(final List<String> methodParams) {
            final List<String> args = new ArrayList<>(methodParams.size() + 3);
            args.add(StringUtils.quote(systemId));
            args.add(StringUtils.quote(methodName));
            args.add(Integer.toString(methodIndex));
            args.addAll(methodParams);
            return enterMethodSignature + String.join(",", args) + ");";
        }

        private String exitMethodCall(final List<String> methodParams) {
            return exitMethodSignature + String.join(",", methodParams) + ");";
        }

        private static List<String> getMethodParameters(final MethodDeclaration method) {
            final List<String> params = new ArrayList<>();
            // "this" object
            params.add(StringUtils.quote("this"));
            if (method.isStatic()) {
                params.add(getMethodClass(method) + ".class");
                params.add("null");
            } else {
                params.add("this.getClass()");
                params.add("this");
            }
            params.add("false");
            // Method parameters
            for (final Parameter p : method.getParameters()) {
                params.add(StringUtils.quote(p.getName().getIdentifier()));
                params.add(ClassUtils.getTypeWithoutGenerics(p.getTypeAsString()) + ".class");
                params.add(p.getName().getIdentifier());
                params.add("false");
            }
            return params;
        }

        private static String getMethodClass(final MethodDeclaration method) {
            Optional<Node> currentNode = Optional.of(method);
            do {
                if (currentNode.get() instanceof ClassOrInterfaceDeclaration) {
                    final ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration)currentNode.get();
                    return decl.getNameAsString();
                }
                currentNode = currentNode.get().getParentNode();
            } while (currentNode.isPresent());
            throw new RuntimeException("Method class not found");
        }

    }

}
