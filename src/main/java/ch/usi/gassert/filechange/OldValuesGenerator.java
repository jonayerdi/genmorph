package ch.usi.gassert.filechange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class OldValuesGenerator {

    private String classFilePath;
    private String methodName;
    private CompilationUnit cu;
    private Map<String, Type> oldVariablesMap;
    private Set<VariableDeclarationExpr> varDeclExprSet;

    public OldValuesGenerator(final String classFilePath, final String methodName) {
        this.classFilePath = classFilePath;
        this.methodName = methodName;
        oldVariablesMap = new HashMap<>();
        try {
            cu = JavaParser.parse(new File(classFilePath));
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void getFieldDeclarations() {
        for (final FieldDeclaration fieldDecl : cu.findAll(FieldDeclaration.class)) {
            if (!fieldDecl.isStatic() && !fieldDecl.isFinal()) {
                for (final VariableDeclarator varDecl : fieldDecl.getVariables()) {
                    final Type type = varDecl.getType();
                    if (isSuiteableType(type)) {
                        oldVariablesMap.put(varDecl.getNameAsString(), varDecl.getType());
                    }
                }
            }
        }
    }

    public void writeOldValuesToFile() {
        getFieldDeclarations();
        varDeclExprSet = new HashSet<VariableDeclarationExpr>();
        cu.accept(new MethodVisitor(methodName), null);
        // rewriteJavaFile(cu);
    }


    public static void main(final String[] args) {
        //final AssertionConverter sc = new AssertionConverter(args[0], args[1]);
        final String filePath = args[0];//"/Users/usi/GAssert/GAssert/subjects/simple-examples/build/classes/java/main/examples/TestExample.java";
        final OldValuesGenerator sc = new OldValuesGenerator(filePath, args[1]);
        sc.writeOldValuesToFile();
    }

    private boolean isSuiteableType(final Type type) {
        if (type.isPrimitiveType() || type.toString().equals("String") || type.isArrayType()) {
            return true;
        }

        return false;
    }

    public class MethodVisitor extends VoidVisitorAdapter<String> {
        private final String methodName;

        public MethodVisitor(final String methodName) {
            this.methodName = methodName;
        }

        @Override
        public void visit(final MethodDeclaration md, final String arg) {
            if (!md.getNameAsString().equals(methodName)) {
                return;
            }

            for (final Parameter param : md.getParameters()) {
                getOldParameterInit(md, param.getType(), param.getNameAsString());
            }

            for (final String varName : oldVariablesMap.keySet()) {
                getOldParameterInit(md, oldVariablesMap.get(varName), "this." + varName);
            }

            addInitsToMethods(md);

        }

        private void getOldParameterInit(final MethodDeclaration md, final Type paramType, final String paramName) {
            final NameExpr expr = new NameExpr(paramName);
            VariableDeclarationExpr varDeclExpr = null;
            if (paramType.isPrimitiveType() || paramType.toString().equals("String") || paramType.toString().equals("Integer")) {
                final VariableDeclarator varDecl = new VariableDeclarator(paramType, "old_" + paramName.replace(".", "_"), expr);
                varDeclExpr = new VariableDeclarationExpr(varDecl);
                varDeclExprSet.add(varDeclExpr);
            } else if (paramType.isArrayType()) {
                final String declString = "Arrays.copyOf(" + paramName + ", " + paramName + ".length)";
                final MethodCallExpr methodCallExpr = JavaParser.parseExpression(declString);
                final VariableDeclarator varDecl = new VariableDeclarator(paramType, "old_" + paramName.replace(".", "_"), methodCallExpr);
                varDeclExpr = new VariableDeclarationExpr(varDecl);
                varDeclExprSet.add(varDeclExpr);
            }
        }


        private void addInitsToMethods(final MethodDeclaration md) {


            final File newFile = new File("ignore-old-values-hard-coded.txt");
            //if (!newFile.exists()) {
            final StringBuilder sb = new StringBuilder();
            for (final VariableDeclarationExpr varDeclExpr : varDeclExprSet) {
                sb.append(varDeclExpr.toString() + ";\n");
            }
            System.out.println("the things I want to add is" + sb.toString());
            AddStatementsBeginningMethod.addAndOverwriteFile(classFilePath, methodName, sb.toString());
            // }

        }

    }
}
