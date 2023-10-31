package ch.usi.oasis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.UnaryExpr.Operator;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FalsePositiveTransformation {
    private CompilationUnit cu;
    private Node mainNode;
    private List<Integer> lineList;
    private String fileLocation;
    private String cutType;
    private String methodName;
    //private String inputRelationFile;
    //private String outputRelationFile;
    private List<String> inputRelationList;
    private List<String> outputRelationList;

    //public FalsePositiveTransformation(String fileLocation, String methodName,
    //                                   String inputRelationFile, String outputRelationFile) {
    public FalsePositiveTransformation(String fileLocation, String methodName,
                List<String> inputRelationFile, List<String> outputRelationFile) {
        try {
            this.fileLocation = fileLocation;
            final String fileName = new File(fileLocation).getName();
            this.cutType = fileName.substring(0, fileName.length() - ".java".length());
            this.inputRelationList = inputRelationFile;
            this.outputRelationList = outputRelationFile;
            FileInputStream in = new FileInputStream(fileLocation);
            setCu(JavaParser.parse(in));
            setLineList(new ArrayList<Integer>());
            this.methodName = methodName;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void createHarnessMethod() throws IOException {
        System.out.println("starting harness");

//        String codePW = "java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileOutputStream(new java.io.File(\"" + execLogFile + "\"), true));";
//        System.out.println("codePW:" + codePW);
//        Statement pwStatement = JavaParser.parseStatement(codePW);
//        System.out.println("pwStatement:" + pwStatement);

        FileInputStream in = new FileInputStream(fileLocation);
        setCu(JavaParser.parse(in));
        //getCu().getImports().add(new ImportDeclaration(null, JavaParser.parseName("java.io.IOException"), false, false));
        MethodDeclaration harnessMethodDecl = new MethodDeclaration();

        NodeList<ReferenceType> excList = new NodeList<ReferenceType>();

        NodeList<Parameter> harnessParameters = new NodeList<Parameter>();

        for (TypeDeclaration typeDec : getCu().getTypes()) {
            List<BodyDeclaration> members = typeDec.getMembers();
            for (BodyDeclaration member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration md = (MethodDeclaration) member;
//                    ObjectCreationExpr exception = new ObjectCreationExpr();
//                    exception.setType(JavaParser.parseClassOrInterfaceType("java.io.IOException"));

                    System.out.println("method name: " + md.getNameAsString());

                    if (md.getNameAsString().equals(methodName)) {
                        System.out.println("inside harness");
                        String sutMethodName = md.getNameAsString();
                        String harnessMethodName = sutMethodName + "_mr_test_harness";
                        harnessMethodDecl.setName(harnessMethodName);

                        NodeList<Parameter> parameters = md.getParameters();
                        for (Parameter parameter:parameters) {
                            System.out.println(parameter.getName() + " " + parameter.getType().asString());
                            Parameter harnessSourceParameter = new Parameter();
                            harnessSourceParameter.setType(parameter.getType());
                            harnessSourceParameter.setName("i_" + parameter.getName() + "_s");

                            Parameter harnessFollowupParameter = new Parameter();
                            harnessFollowupParameter.setType(parameter.getType());
                            harnessFollowupParameter.setName("i_" + parameter.getName() + "_f");

                            //out.write("entered" + i_n_s + " "  + i_n_f + " " + i_k_s + " " + i_k_f + "\n");

                            harnessParameters.add(harnessSourceParameter);
                            harnessParameters.add(harnessFollowupParameter);
                        }

                        harnessMethodDecl.setParameters(harnessParameters);
                        harnessMethodDecl.setType("void");
                        harnessMethodDecl.setStatic(true);
                        harnessMethodDecl.setPublic(true);

                        MethodCallExpr methodCallExprSource = new MethodCallExpr(cutType + "." + methodName);
                        MethodCallExpr methodCallExprFollowup = new MethodCallExpr(cutType + "." + methodName);

                        BlockStmt harnessMethodBody = new BlockStmt();
                        //harnessMethodBody.addStatement(pwStatement);

                        String paramString = "o_return_s + \" \" + o_return_f";
                        for (Parameter parameter : parameters) {
                            methodCallExprSource.addArgument("o_" + parameter.getName() + "_s");
                            methodCallExprFollowup.addArgument("o_" + parameter.getName() + "_f");

                            harnessMethodBody.addStatement(getVarDeclExpr(parameter.getType().asString(),
                                    "o_" + parameter.getName() + "_s", new MethodCallExpr("ch.usi.gassert.util.ClassUtils.cloneValue", new NameExpr("i_" + parameter.getName() + "_s"))));
                            harnessMethodBody.addStatement(getVarDeclExpr(parameter.getType().asString(),
                                    "o_" + parameter.getName() + "_f", new MethodCallExpr("ch.usi.gassert.util.ClassUtils.cloneValue", new NameExpr("i_" + parameter.getName() + "_f"))));
                            
                            paramString = paramString + " + \" \"  + i_" + parameter.getName() + "_s + \" \" +  i_" + parameter.getName() + "_f";
                        }

//                        System.out.println("out.write(\"entered\" + " + paramString.replace("o_return_s + \" \" + o_return_f +", "") + " + \"\\n\");");
//                        harnessMethodBody.addStatement(JavaParser.parseStatement("out.write(\"entered\"  + " + paramString.replace("o_return_s + \" \" + o_return_f +", "") + " + \"\\n\");"));
                        //harnessMethodBody.addStatement(getVarDeclExpr(cutType, "i_this_s", new NullLiteralExpr()));
                        //harnessMethodBody.addStatement(getVarDeclExpr(cutType, "i_this_f", new NullLiteralExpr()));

                        System.out.println("paramString:" + paramString);
                        String returnType = md.getTypeAsString();
                        if (!returnType.equals("void")) {
                            harnessMethodBody.addStatement(getVarDeclExpr(returnType, "o_return_s", methodCallExprSource));
                            harnessMethodBody.addStatement(getVarDeclExpr(returnType, "o_return_f", methodCallExprFollowup));
                        } else {
                            harnessMethodBody.addStatement(methodCallExprSource);
                            harnessMethodBody.addStatement(methodCallExprFollowup);
                        }

                        //harnessMethodBody.addStatement(getVarDeclExpr(cutType, "o_this_s", new NullLiteralExpr()));
                        //harnessMethodBody.addStatement(getVarDeclExpr(cutType, "o_this_f", new NullLiteralExpr()));

                        for (int i = 0;  i < inputRelationList.size(); i++) {
                            IfStmt inputConditionStmt = new IfStmt();
                            inputConditionStmt.setCondition(new NameExpr(inputRelationList.get(i)));

                            BlockStmt ifStmtBody = new BlockStmt();

//                            System.out.println("out.write(\"input_condition_done\" + " + paramString + " + \"\\n\");");
//                            ifStmtBody.addStatement(JavaParser.parseStatement("out.write(\"input_condition_done\" + " + paramString + " + \"\\n\");"));

                            MethodCallExpr methodCallExpr = new MethodCallExpr();
                            //methodCallExpr.setName("org.junit.Assert.assertTrue");
                            methodCallExpr.setName("assert");
                            methodCallExpr.addArgument(outputRelationList.get(i));
                            ifStmtBody.addStatement(methodCallExpr);
                            inputConditionStmt.setThenStmt(ifStmtBody);

                            harnessMethodBody.addStatement(inputConditionStmt);
                        }

//                        Statement closeStatement = JavaParser.parseStatement("out.close();");
//                        harnessMethodBody.addStatement(closeStatement);
                        harnessMethodDecl.setBody(harnessMethodBody);

                        System.out.println("ending harness" + harnessMethodDecl.toString());
                        break;
                    }
                }
            }

            //harnessMethodDecl = harnessMethodDecl.addThrownException((new java.io.IOException()).getClass());
            System.out.println("first hmd:" + harnessMethodDecl);
            typeDec.addMember(harnessMethodDecl);
        }
    }

    public void writeHarnessedFile(String location) {
        File file = new File(location);
        System.out.println("harnessedFile" + location);
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(cu.toString());
            //System.out.println(cu.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findAssertions(int type) throws Exception {
        FileInputStream in = new FileInputStream(fileLocation);
        setCu(JavaParser.parse(in));
        for (TypeDeclaration typeDec : getCu().getTypes()) {
            List<BodyDeclaration> members = typeDec.getMembers();
            for (BodyDeclaration member : members) {
                if (member instanceof ConstructorDeclaration || member instanceof MethodDeclaration) {

                    boolean isCorrectName = false;
                    if (member instanceof ConstructorDeclaration) {
                        ConstructorDeclaration constructor = (ConstructorDeclaration) member;
                        mainNode = constructor.getBody();

                        //if (constructor.getName().contains(methodName)) {
                        if (methodName.equals("init")) {
                            isCorrectName = true;
                        }
                    }

                    if (member instanceof MethodDeclaration) {
                        MethodDeclaration method = (MethodDeclaration) member;
                        mainNode = method.getBody().get();

                        if (method.getNameAsString().equals(methodName + "_mr_test_harness")) {
                            isCorrectName = true;
                            System.out.println("method nm:" + method.getNameAsString());
                        }
                    }

                    System.out.println("isCorrectName:" + isCorrectName);
                    System.out.println("type:" + type);
                    if (isCorrectName) {
                        if (type == 1) {
                            decompose(mainNode);
                        } else {
                            decomposeForAddedAssertions(mainNode);
                        }
                    }
                }
            }
        }
    }

    private void decompose(Node node) {
        System.out.println("normal decompose");
        for (Node childNode : node.getChildNodes()) {
            if (childNode instanceof AssertStmt) {
                System.out.println("found assertStmt");
                AssertStmt assertStmt = (AssertStmt) childNode;

                Expression expr = assertStmt.getCheck();

                EnclosedExpr enclosedExpr = new EnclosedExpr(expr);
                UnaryExpr unaryExpr = new UnaryExpr(enclosedExpr, Operator.LOGICAL_COMPLEMENT);
                IfStmt ifStmt = new IfStmt();
                ifStmt.setCondition(unaryExpr);

                ifStmt.setThenStmt(getThenStmt());

                int index = childNode.getParentNode().get().getChildNodes().indexOf(childNode);

                BlockStmt parentBlock = (BlockStmt) childNode.getParentNode().get();
                parentBlock.getStatements().remove(index);
                parentBlock.getStatements().add(index, ifStmt);
                return;
            }
            decompose(childNode);
        }
    }

    private void decomposeForAddedAssertions(Node node) {
        for (Node childNode : node.getChildNodes()) {
            if (childNode instanceof AssertStmt) {
                lineList.add(childNode.getBegin().get().line);
            }
            decomposeForAddedAssertions(childNode);
        }
    }

    public BlockStmt getThenStmt() {
        BlockStmt thenStmt = new BlockStmt();
        PrimitiveType primType = new PrimitiveType(PrimitiveType.Primitive.INT);
        VariableDeclarator varDecl = new VariableDeclarator(primType, "mm", new IntegerLiteralExpr("2"));

        VariableDeclarationExpr declExpr = new VariableDeclarationExpr(varDecl);
        ExpressionStmt exprStmt = new ExpressionStmt(declExpr);
        //System.out.println(exprStmt);
        AssertStmt assertStmt = new AssertStmt();
        BinaryExpr bExpr = new BinaryExpr(new IntegerLiteralExpr("3"), new NameExpr("mm"), BinaryExpr.Operator.GREATER);
        EnclosedExpr expr = new EnclosedExpr(bExpr);
        assertStmt.setCheck(expr);
        //System.out.println(assertStmt);
        thenStmt.getStatements().add(exprStmt);
        thenStmt.getStatements().add(assertStmt);

        //System.out.println(thenStmt);
        return thenStmt;
    }

    public void rewriteFile() {
        File file = new File(fileLocation);

        try {
            FileWriter fw = new FileWriter(file);
            fw.write(getCu().toString());
            //System.out.println(cu.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void getAssertLines() {

        try {
            FileInputStream in = new FileInputStream(fileLocation);
            setCu(JavaParser.parse(in));
            try {
                findAssertions(2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getFileContent(String filePath) throws IOException {
        return new String (Files.readAllBytes( Paths.get(filePath)));
    }

    private VariableDeclarationExpr getVarDeclExpr(String type, String name, Expression initValue) {
        VariableDeclarator thisSourceDecl = new VariableDeclarator();
        thisSourceDecl.setType(type);
        thisSourceDecl.setName(name);
        thisSourceDecl.setInitializer(initValue);
        VariableDeclarationExpr thisSource = new VariableDeclarationExpr(thisSourceDecl);
        return thisSource;
    }

//    public static void main(String[] args) {
//        String fileLocation = args[0];
//        FalsePositiveTransformation rawi = new FalsePositiveTransformation(fileLocation, args[1], "", "");
//        try {
//            rawi.findAssertions(1);
//            rawi.rewriteFile();
//            rawi.getAssertLines();
//            String lineList = rawi.getLineList().toString();
//            lineList = lineList.replace("[", "");
//            lineList = lineList.replace("]", "");
//            lineList = lineList.replace(" ", "");
//            System.out.println(lineList);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * @return the lineList
     */
    public List<Integer> getLineList() {
        return lineList;
    }

    /**
     * @param lineList the lineList to set
     */
    public void setLineList(List<Integer> lineList) {
        this.lineList = lineList;
    }

    public CompilationUnit getCu() {
        return cu;
    }

    public void setCu(CompilationUnit cu) {
        this.cu = cu;
    }
}
