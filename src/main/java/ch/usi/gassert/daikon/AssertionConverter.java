package ch.usi.gassert.daikon;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.type.Type;

/**
 * This class converts DAIKON to JAVA
 * Initially it was designed to add primitive and array parameters as old values
 * into the body of the method. However, because of the time issues for now we have
 * decided to keep only Daikon invariants that do not use old values.
 */
public class AssertionConverter {

    private String filePath;
    private String methodName;
    private String oldValuesFile;
    private List<Comment> commentList;
    private final String ensuresTag = "@ ensures ";
    private final String requiresTag = "@ requires ";
    private List<String> preconditionList;
    private List<String> postconditionList;
    private HashSet<String> oldValuesSet;
    private CompilationUnit cu;

    public AssertionConverter(final String filePath, final String methodName, final String oldValuesFile) {
        this.filePath = filePath;
        this.methodName = methodName;
        this.oldValuesFile = oldValuesFile;
        preconditionList = new ArrayList<>();
        postconditionList = new ArrayList<>();
        oldValuesSet = getOldValuesList(oldValuesFile);
    }

    private static HashSet<String> getOldValuesList(final String oldValuesFile) {
        final HashSet<String> oldValueSet = new HashSet<String>();

        try (final BufferedReader br = new BufferedReader(new FileReader(oldValuesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(" ") && line.contains("=")) {
                    final int startIndex = line.indexOf(" ");
                    final int endIndex = line.indexOf("=");
                    final String variableName = line.substring(startIndex, endIndex).trim();
                    oldValueSet.add(variableName);
                }
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return oldValueSet;
    }

    private String getDaikonComments() {
        try {
            cu = JavaParser.parse(new File(filePath));
            commentList = cu.getComments();
            final MethodVisitor visitor = new MethodVisitor(methodName);
            cu.accept(visitor, null);
            rewriteJavaFile(cu);
            return visitor.result;
        } catch (final Exception e) {
            e.printStackTrace();
            return "assert (true);";
        }
        //return "ERROR in ASSETIONCONVERTER";
    }


    private void rewriteJavaFile(final CompilationUnit cu) {
        try {
            final FileWriter file = new FileWriter(new File(filePath));
            file.write(cu.toString());
            file.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * args[0] file
     * args[1] method
     *
     * @param args
     */
    public static void main(final String[] args) {

        final AssertionConverter sc = new AssertionConverter(args[0], args[1], args[2]);
        sc.getDaikonComments();
    }

    public class MethodVisitor extends VoidVisitorAdapter<String> {

        private final String methodName;
        private int commentAreaStart = 0;
        private String result = "";

        public MethodVisitor(final String methodName) {
            this.methodName = methodName;
        }

        @Override
        public void visit(final MethodDeclaration md, final String arg) {
            if (!md.getNameAsString().equals(methodName)) {
                commentAreaStart = md.getEnd().get().line;
                return;
            }
            getCommentsByLineNumbers(md);
        }

        private void getCommentsByLineNumbers(final MethodDeclaration md) {
            final int beginLine = md.getBegin().get().line;
            for (final Comment comment : commentList) {
                final int commentLine = comment.getBegin().get().line;
                if (commentLine > commentAreaStart && commentLine < beginLine) {
                    final String contents = comment.getContent();
                    final String[] comments = contents.split("@");
                    for (final String cnt : comments) {
                        final String content = "@" + cnt;
                        if (content.contains(requiresTag) && !content.contains("\forall") && !content.contains(".getClass().getName()")) {
                            final String assertion = getAssertionFromComment(requiresTag, content, md.getParameters());
                            preconditionList.add(assertion);
                        }

                        if (content.contains(ensuresTag) && !content.contains("\forall") && !content.contains(".getClass().getName()")) {
                            final String assertion = getAssertionFromComment(ensuresTag, content, md.getParameters());
                            postconditionList.add(assertion);
                        }

                    }
                }
            }
            writeFinalString();
        }

        private void writeFinalString() {
            final StringBuilder finalString = new StringBuilder();
            String finalRequires = "";
            String finalEnsures = "";

            int i = 0;

            for (final String precond : preconditionList) {
                if (precond.contains(methodName)) {
                    continue;
                }

                //finalString.append("assert (" + precond + ");");
                //finalString.append(System.getProperty("line.separator"));
                finalRequires = finalRequires + (i == 0 ? " " : " && ") + "(" + precond + ")";
                i++;
            }

            i = 0;

            for (final String postcond : postconditionList) {
                if (postcond.contains("this.theArray == old_this_theArray")) {
                    continue;
                }
                if (postcond.contains(methodName)) {
                    continue;
                }
                if (!finalRequires.isEmpty()) {
                    finalString.append("assert (ch.usi.gassert.util.Implies.implies(" + finalRequires + "," + postcond + "));");
                } else {
                    finalString.append("assert (" + postcond + ");");
                }
                finalString.append(System.getProperty("line.separator"));
                finalEnsures = finalEnsures + (i == 0 ? " " : " && ") + "(" + postcond + ")";
                i++;
            }

            if (i > 1) {
                String finalAssertion = "";

                if (!finalRequires.isEmpty() && !finalEnsures.isEmpty()) {
                    finalAssertion = "assert (ch.usi.gassert.util.Implies.implies(" + finalRequires + "," + finalEnsures + "));";
                } else if (finalRequires.isEmpty() && !finalEnsures.isEmpty()) {
                    finalAssertion = "assert (" + finalEnsures + ");";
                }

                if (!finalAssertion.isEmpty()) {
                    finalString.append(finalAssertion);
                }
            }
            result = finalString.toString().replace("old_segmentFraction", "old_this_segmentFraction").replace(System.getProperty("line.separator") + "" + System.getProperty("line.separator"), System.getProperty("line.separator"));
            System.out.println(result);
        }

        private String getAssertionFromComment(final String tag, final String content, final NodeList<Parameter> paramList) {
            final int beginIndex = content.indexOf(tag) + tag.length();
            final int endIndex = content.lastIndexOf(";");
            String assertionText = content.substring(beginIndex, endIndex);
            assertionText = assertionText.replace("\\result", "result");

            //final Matcher m = Pattern.compile("old([ ,\\S]+)").matcher(assertionText);
            final Matcher m = Pattern.compile("old\\((.*?)\\)").matcher(assertionText);
            while (m.find()) {

                final String oldValue = m.group(1);
                //oldValue = oldValue.substring(1, oldValue.length() - 1);
                /*if (oldValue.contains(")") && !oldValue.contains("(")) {
                    oldValue = oldValue.substring(0, oldValue.indexOf(")"));
                }*/

                /*if (oldValue.contains("this.")) {
                    String newOldValue = oldValue.replace("this.", "old_this_");
                    newOldValue = newOldValue.replace("\\old(" + oldValue + ")", oldValue);
                    assertionText = assertionText.replace("\\old(" + oldValue + ")", newOldValue.replace("()", ""));
                } */

                if (oldValue.contains("daikon.")) {
                    //System.out.println("contains daikon:" + oldValue);
                    final String oldValueRepl = oldValue.replace("(", "(old_");
                    //System.out.println("assertionText before:" + assertionText);
                    assertionText = assertionText.replace("\\old(" + oldValue + ")", oldValueRepl);
                    // System.out.println("assertionText after:" + assertionText);
                }

                assertionText = assertionText.replace("\\old(" + oldValue + ")", "old_" + oldValue.replace("()", ""));
                assertionText = assertionText.replace("old_this.", "old_this_");
                // System.out.println("assertionText final:" + assertionText);

            }

            for (final Parameter param : paramList) {
                final String name = param.getNameAsString();
                assertionText = assertionText.replace("\\old(" + name + ")", "old_" + name.replace("()", ""));
            }

            if (assertionText.contains("<==>")) {
                assertionText = assertionText.replace("<==>", "==");
            }

            if (assertionText.contains("==>")) {
                final int index = assertionText.indexOf("==>");
                final String leftPart = assertionText.substring(0, index);
                final String rightPart = assertionText.substring(index + 3, assertionText.length());
                assertionText = "ch.usi.gassert.util.Implies.implies(" + leftPart + "," + rightPart + ")";
            }

            for (final String oldValue : oldValuesSet) {
                final String replOldValue = oldValue.replace("_", "\\_");
                assertionText = assertionText.replaceAll(replOldValue + "\\(\\)", oldValue);
            }

            return assertionText;
        }

        //Not used for now as we do not consider old values for now
        private void addOldParameter(final MethodDeclaration md, final Type paramType, final String paramName) {
            final NameExpr expr = new NameExpr(paramName);

            VariableDeclarationExpr varDeclExpr = null;
            if (paramType.isPrimitiveType()) {
                final VariableDeclarator varDecl = new VariableDeclarator(paramType, "old_" + paramName.replace(".", "_"), expr);
                varDeclExpr = new VariableDeclarationExpr(varDecl);
            } else if (paramType.isArrayType()) {
                final String declString = "Arrays.copyOf(" + paramName + ", " + paramName + ".length)";
                final MethodCallExpr methodCallExpr = JavaParser.parseExpression(declString);
                final VariableDeclarator varDecl = new VariableDeclarator(paramType, "old_" + paramName.replace(".", "_"), methodCallExpr);
                varDeclExpr = new VariableDeclarationExpr(varDecl);
            } else {


            }

            if (varDeclExpr != null) {
                final ExpressionStmt exprStmt = new ExpressionStmt(varDeclExpr);
                exprStmt.setComment(new LineComment("instrumentation"));
                md.getBody().get().addStatement(0, exprStmt);
            }
        }
    }

}