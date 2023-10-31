package ch.usi.oasis;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

public class FalseNegativeTransformation {

    private CompilationUnit initial_cu;
    private CompilationUnit cu;
    private Node mainNode;
    private int index = 0;
    private int pp = 0;
    private int added = 0;

    private Set<String> staticCommonVarList;
    private Set<String> instanceCommonVarList;

    private HashMap<Node, Integer> beforeLines;
    private HashMap<Node, Integer> afterLines;

    private String methodName;

    public FalseNegativeTransformation(final String fileLocation, final String methodName) throws Exception {
        staticCommonVarList = new HashSet<String>();
        instanceCommonVarList = new HashSet<String>();

        final FileInputStream in = new FileInputStream(fileLocation);
        cu = JavaParser.parse(in);
        this.methodName = methodName;
        setInitial_cu((CompilationUnit) cu.clone());
    }

    public void analyseAssertions() throws Exception {
        for (final TypeDeclaration typeDec : cu.getTypes()) {
            final List<BodyDeclaration> members = typeDec.getMembers();
            if (members != null) {
                for (final BodyDeclaration member : members) {
                    //System.out.println("member:"+member.getClass()+","+member);

                    if (member instanceof FieldDeclaration) {
                        final FieldDeclaration field = (FieldDeclaration) member;

                        if (field.isStatic()) {
                            for (final VariableDeclarator var : field.getVariables()) {
                                staticCommonVarList.add(var.getNameAsString());
                            }
                        }

                        if (!field.isStatic()) {
                            for (final VariableDeclarator var : field.getVariables()) {
                                instanceCommonVarList.add(var.getNameAsString());
                            }
                        }

                    }

                    if (member instanceof ConstructorDeclaration || member instanceof MethodDeclaration) {
                        List<Node> childrenNodes = new ArrayList<Node>();
                        index = 0;

                        final List<String> varList = new ArrayList<String>();

                        boolean isNameCorrect = false;
                        if (member instanceof ConstructorDeclaration) {
                            final ConstructorDeclaration constructor = (ConstructorDeclaration) member;
                            if (constructor.getNameAsString().contains(methodName)) {
                                isNameCorrect = true;
                            }
                            childrenNodes = constructor.getChildNodes();
                            for (final Parameter parameter : ((ConstructorDeclaration) member).getParameters()) {
                                varList.add(parameter.getNameAsString());
                            }
                        }

                        if (member instanceof MethodDeclaration) {
                            final MethodDeclaration method = (MethodDeclaration) member;
                            if (method.getNameAsString().contains(methodName)) {
                                isNameCorrect = true;
                            }
                            childrenNodes = method.getChildNodes();

                            for (final Parameter parameter : ((MethodDeclaration) member).getParameters()) {
                                varList.add(parameter.getNameAsString());
                            }
                        }

                        if (isNameCorrect) {
                            findAssertions(childrenNodes, varList);
                        }

                    }
                }
            }
        }

        //System.out.println(cu);
    }

    private void findAssertions(final List<Node> childrenNodes, final List<String> parameterList) throws Exception {
        final List<Node> complexNodes = new ArrayList<Node>();
        final List<String> varList = new ArrayList<String>(parameterList);

        index = 0;
        for (final Node node : childrenNodes) {
            if ((node instanceof ExpressionStmt) &&
                    (node.getChildNodes().get(0) instanceof VariableDeclarationExpr)) {
                final VariableDeclarationExpr varExpr = (VariableDeclarationExpr) node.getChildNodes().get(0);
                for (final VariableDeclarator varDecl : varExpr.getVariables()) {
                    varList.add(varDecl.getNameAsString());
                }

            } else if (node instanceof AssertStmt) {
                node.setComment(new LineComment("instrumentation"));
                added = 0;

                mainNode = node;
                final AssertStmt assertStmt = (AssertStmt) node;

                addHashMap(mainNode, index);
                index++;

                final Node assertCondition = assertStmt.getCheck();

                decomposeAssertion(assertCondition, index, varList);
                addAssertResultToMap(assertStmt.getCheck());

                //because of added variables in decomposeAssertion
                index = index + added + 1;

                addMaptoMainMap();

                ((BlockStmt) node.getParentNode().get()).getStatements().remove(index);

                index--;
                return;
            } else if (node instanceof IfStmt || node instanceof BlockStmt ||
                    node instanceof TryStmt || node instanceof CatchClause) {
                complexNodes.add(node);
            } else if (node instanceof ReturnStmt) {
                node.setComment(new LineComment("instrumentation"));
            }

            index++;
        }

        for (final Node node : complexNodes) {
            findAssertions(node.getChildNodes(), varList);
        }
    }

    private void decomposeAssertion(final Node assertCondition, final int index, final List<String> varList) {
        for (final String var : staticCommonVarList) {
            final NameExpr clazz = new NameExpr("map" + Integer.toString(pp));
            final MethodCallExpr call = new MethodCallExpr(clazz, "put");
            //ASTHelper.addArgument(call, new StringLiteralExpr("this."+var.toString()));
            //ASTHelper.addArgument(call, new NameExpr("this."+var.toString()));
            call.addArgument(new StringLiteralExpr(var.toString().replace("[]", "")));
            call.addArgument(var.toString().replace("[]", ""));
            //ASTHelper.addStmt(method, call);
            //System.out.println(index + "," + call);
            final ExpressionStmt expressionStmt = new ExpressionStmt(call);
            expressionStmt.setComment(new LineComment("instrumentation"));

            ((BlockStmt) mainNode.getParentNode().get()).getStatements().add(index, expressionStmt);
            //mainNode.getParentNodeForChildren().getChildNodes().add(index, expressionStmt);
            added++;
        }

        for (final String var : instanceCommonVarList) {
            final NameExpr clazz = new NameExpr("map" + Integer.toString(pp));
            final MethodCallExpr call = new MethodCallExpr(clazz, "put");
            call.addArgument(new StringLiteralExpr("this." + var.toString().replace("[]", "")));
            call.addArgument(new NameExpr("this." + var.toString().replace("[]", "")));

            //ASTHelper.addArgument(call, new StringLiteralExpr(var.toString().replace("[]", "")));
            //ASTHelper.addArgument(call, new NameExpr(var.toString().replace("[]", "")));
            //ASTHelper.addStmt(method, call);
            //System.out.println(index + "," + call);
            final ExpressionStmt expressionStmt = new ExpressionStmt(call);
            expressionStmt.setComment(new LineComment("instrumentation"));

            ((BlockStmt) mainNode.getParentNode().get()).getStatements().add(index, expressionStmt);
            //mainNode.getParentNodeForChildren().getChildNodes().add(index, expressionStmt);
            added++;
        }

        for (final String var : varList) {
            final NameExpr clazz = new NameExpr("map" + Integer.toString(pp));
            final MethodCallExpr call = new MethodCallExpr(clazz, "put");
            call.addArgument(new StringLiteralExpr(var.toString().replace("[]", "")));
            call.addArgument(new NameExpr(var.toString().replace("[]", "")));
            //ASTHelper.addStmt(method, call);
            //System.out.println(index + "," + call);
            final ExpressionStmt expressionStmt = new ExpressionStmt(call);
            expressionStmt.setComment(new LineComment("instrumentation"));
            ((BlockStmt) mainNode.getParentNode().get()).getStatements().add(index, expressionStmt);
            //mainNode.getParentNodeForChildren().getChildNodes().add(index, expressionStmt);
            added++;
        }
    }

    public void addMainHashMap() throws Exception {
        addImports();

        for (final Node node : cu.getChildNodes()) {
            if (node instanceof ClassOrInterfaceDeclaration) {
                final ClassOrInterfaceType type = new ClassOrInterfaceType();
                type.setName("Map<Integer, HashMap<String, Object>>");

                final ClassOrInterfaceType cit = new ClassOrInterfaceType();
                cit.setName("HashMap<Integer, HashMap<String, Object>>");

                final ObjectCreationExpr oce = new ObjectCreationExpr(null, cit, new NodeList<Expression>());
                final VariableDeclarator vd = new VariableDeclarator(type, "instrMap", oce);
                final EnumSet<Modifier> modifiers = EnumSet.of(Modifier.STATIC, Modifier.PRIVATE);

                final FieldDeclaration fd = new FieldDeclaration(modifiers, vd);
                fd.setComment(new LineComment("instrumentation"));

                final PrimitiveType primType = new PrimitiveType(PrimitiveType.Primitive.INT);
                final IntegerLiteralExpr intExpr = new IntegerLiteralExpr("0");
                final VariableDeclarator decl = new VariableDeclarator(primType, new SimpleName("jjj"), intExpr);

                final FieldDeclaration fd1 = new FieldDeclaration(modifiers, decl);
                //fd1.setModifiers(10);
                fd1.setComment(new LineComment("instrumentation"));

                final ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) node;
                cid.getMembers().add(0, fd);
                cid.getMembers().add(1, fd1);
            }
        }
    }

    private void addAssertResultToMap(final Expression assertExpression) {
        final NameExpr clazz = new NameExpr("map" + Integer.toString(pp));
        final MethodCallExpr call = new MethodCallExpr(clazz, "put");
        call.addArgument(new StringLiteralExpr("assert_result" + Integer.toString(pp)));
        call.addArgument(assertExpression);
        //ASTHelper.addStmt(method, call);
        final ExpressionStmt expressionStmt = new ExpressionStmt(call);
        expressionStmt.setComment(new LineComment("instrumentation"));

        //mainNode.getParentNodeForChildren().getChildNodes().add(index, expressionStmt);
        ((BlockStmt) mainNode.getParentNode().get()).getStatements().add(index, expressionStmt);
    }

    private void addMaptoMainMap() {
        final NameExpr clazz1 = new NameExpr("instrMap");
        final MethodCallExpr call1 = new MethodCallExpr(clazz1, "put");
        call1.addArgument(new IntegerLiteralExpr("jjj"));
        call1.addArgument(new NameExpr("map" + pp));

        final ExpressionStmt expressionStmt = new ExpressionStmt(call1);
        expressionStmt.setComment(new LineComment("instrumentation"));
        //mainNode.getParentNodeForChildren().getChildNodes().add(index, expressionStmt);
        ((BlockStmt) mainNode.getParentNode().get()).getStatements().add(index, expressionStmt);

        pp++;
        index++;

        //mainNode.getParentNodeForChildren().getChildNodes().add(index, expressionStmt);
        ((BlockStmt) mainNode.getParentNode().get()).getStatements().add(index, getJJJIncrementExpr());
        index++;
    }

    private ExpressionStmt getJJJIncrementExpr() {
        final Expression intExpr = new IntegerLiteralExpr("1");
        final Expression jjjName = new NameExpr("jjj");
        final BinaryExpr addExpr = new BinaryExpr(jjjName, intExpr, BinaryExpr.Operator.PLUS);

        final AssignExpr assignExpr = new AssignExpr(jjjName, addExpr, AssignExpr.Operator.ASSIGN);
        final ExpressionStmt finalExpr = new ExpressionStmt(assignExpr);
        finalExpr.setComment(new LineComment("instrumentation"));

        return finalExpr;
    }

    private void addImports() {
        final NodeList<ImportDeclaration> importList = cu.getImports();
        boolean hasMapImport = false;
        boolean hasHashMapImport = false;

        for (final ImportDeclaration importDeclaration : importList) {
            if (importDeclaration.getName().toString().equals("java.util.Map")) {
                hasMapImport = true;
            }
            if (importDeclaration.getName().toString().equals("java.util.HashMap")) {
                hasHashMapImport = true;
            }
            if (hasMapImport == true && hasHashMapImport == true) {
                break;
            }
        }

        if (hasMapImport == false) {
            importList.add(new ImportDeclaration(new Name("java.util.Map"), false, false));
        }

        if (hasHashMapImport == false) {
            importList.add(new ImportDeclaration(new Name("java.util.HashMap"), false, false));
        }

        cu.setImports(importList);
    }

    public void addHashMap(final Node node, final int index) throws Exception {
        //ReferenceType type = ASTHelper.
        //					createReferenceType("HashMap<String, Object>", 0);

        final ClassOrInterfaceType type = new ClassOrInterfaceType();
        type.setName("Map<String, Object>");

        final ClassOrInterfaceType cit = new ClassOrInterfaceType();
        cit.setName("HashMap<String, Object>");

        final ObjectCreationExpr oce = new ObjectCreationExpr(null, cit, new NodeList<Expression>());

        final VariableDeclarator vd = new VariableDeclarator(cit, "map" + pp, oce);

        final VariableDeclarationExpr vde = new VariableDeclarationExpr(vd);

        final ExpressionStmt expressionStmt = new ExpressionStmt(vde);
        expressionStmt.setComment(new LineComment("instrumentation"));
        ((BlockStmt) node.getParentNode().get()).getStatements().add(index, expressionStmt);
        //node.getParentNodeForChildren().getChildNodes().add(index, expressionStmt);


    }

    public void rewriteJavaFile(final String location) throws Exception {
        final File file = new File(location);
        final FileWriter fw = new FileWriter(file);
        fw.write(cu.toString());
        fw.close();

        try (final PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("fn-instrumentation.java", false)))) {
            out.println(cu.toString());
        } catch (final IOException e) {
            System.err.println(e);
        }
    }

    public HashMap<Node, Integer> getInitialLineNumbers(final CompilationUnit cu) {
        final HashMap<Node, Integer> lineList = new HashMap<Node, Integer>();
        for (final TypeDeclaration typeDec : cu.getTypes()) {
            final List<BodyDeclaration> members = typeDec.getMembers();
            if (members != null) {
                for (final BodyDeclaration member : members) {
                    if (member instanceof ConstructorDeclaration || member instanceof MethodDeclaration) {
                        List<Node> childrenNodes = new ArrayList<Node>();
                        index = 0;

                        final List<String> varList = new ArrayList<String>();

                        if (member instanceof ConstructorDeclaration) {
                            final ConstructorDeclaration constructor = (ConstructorDeclaration) member;
                            childrenNodes = constructor.getChildNodes();
                        }

                        if (member instanceof MethodDeclaration) {
                            final MethodDeclaration method = (MethodDeclaration) member;
                            childrenNodes = method.getChildNodes();
                        }

                        searchNodes(childrenNodes, lineList);
                    }
                }
            }
        }

        return lineList;
    }

    private void searchNodes(final List<Node> childrenNodes, final HashMap<Node, Integer> lineList) {
        for (final Node node : childrenNodes) {
            if (node.getComment() == null || !node.getComment().isPresent() ||
                    node.getComment().get().getContent().trim().equals("instrumentation")) {
                lineList.put(node, node.getBegin().get().line);
            }
            searchNodes(node.getChildNodes(), lineList);
        }
    }

    public String getInstrumentationLines(final String location) throws FileNotFoundException, ParseException {
        final FileInputStream in = new FileInputStream(location);
        final CompilationUnit cu = JavaParser.parse(in);

        setAfterLines(getInitialLineNumbers(cu));

        final List<Integer> instrumentationLines = new ArrayList<Integer>();
        final List<Comment> commentList = cu.getComments();
        for (final Comment comment : commentList) {
            if (comment.isLineComment() && comment.getContent().trim().equals("instrumentation")) {
                instrumentationLines.add(comment.getEnd().get().line + 1);
            }
        }

        String instrLines = instrumentationLines.toString();
        instrLines = instrLines.replace("[", "");
        instrLines = instrLines.replace("]", "");
        instrLines = instrLines.replace(" ", "");

        return instrLines;
    }

    public HashMap<Node, Integer> getBeforeLines() {
        return beforeLines;
    }

    public void setBeforeLines(final HashMap<Node, Integer> beforeLines) {
        this.beforeLines = beforeLines;
    }

    public CompilationUnit getInitial_cu() {
        return initial_cu;
    }

    public void setInitial_cu(final CompilationUnit initial_cu) {
        this.initial_cu = initial_cu;
    }

    public HashMap<Node, Integer> getAfterLines() {
        return afterLines;
    }

    public void setAfterLines(final HashMap<Node, Integer> afterLines) {
        this.afterLines = afterLines;
    }

}