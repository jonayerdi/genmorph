package ch.usi.gassert.interpreter;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeReaderGAssert;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.AssertStmt;

@Deprecated
public class JavaToGAssertConverter {

    private static final LinkedHashMap<String, String> implyReplacementMap = new LinkedHashMap<>();

    public static String convert(final String s) {
        String finalString = JavaParser.parseExpression(s).toString();
        implyReplacementMap.clear();
        finalString = finalString.replace("assert (", "assert(");
        final Matcher m = Pattern.compile("assert\\((.*?)\\);").matcher(finalString);
        if (m.find()) {
            finalString = m.group(1);
        }
        finalString = finalString.replace("ch.usi.gassert.util.Implies.implies", "implies");
        finalString = replaceFieldDelimiters(finalString, getExpressionFromString(finalString).findAll(FieldAccessExpr.class));
        finalString = replaceMethodAccessAndParameters(finalString, getExpressionFromString(finalString).findAll(MethodCallExpr.class));
        finalString = replaceNotAndNotEqual(finalString);
        finalString = finalString.replaceAll("\\.", "_ACCESSING_FIELDS_");

        final Matcher m2 = Pattern.compile("Arrays_ACCESSING_METHODS_asList_PARAMETER_METHOD_(.*?)_ACCESSING_METHODS_").matcher(finalString);
        while (m2.find()) {
            final String found = m2.group();
            finalString = finalString.replace(found, found.substring(0, found.length() - "_ACCESSING_METHODS_".length()) + Config.DELIMITER_SPECIAL);
        }
        //System.out.println("before sanity check:" + finalString);
        return sanityCheckIssue(finalString);
    }

    // avoid issue with == that is used for both integers and boolean
    private static String sanityCheckIssue(final String finalString) {
        final Tree tree = TreeReaderGAssert.getTree(finalString);
        tree.fixAmbiguity();
        return tree.toString();
    }

    private static Expression getExpressionFromString(final String s) {
        final String assertStr = "assert " + s + ";";
        final AssertStmt assertStmt = JavaParser.parseStatement(assertStr).asAssertStmt();
        return assertStmt.getCheck();
    }

    private static String replaceMethodAccessAndParameters(String finalString, final List<MethodCallExpr> mcList) {
        final List<MethodCallExpr> orderedMcList = new LinkedList<MethodCallExpr>();
        for (final MethodCallExpr mcExpr : mcList) {

            if (mcExpr.toString().contains("implies")) {
                orderedMcList.add(0, mcExpr);
            } else {
                orderedMcList.add(mcExpr);
            }
        }

        //VariableManager.getNumbersVars().add("this_ACCESSING_FIELDS_theArray_ACCESSING_METHODS_size");
        for (final MethodCallExpr mcExpr : orderedMcList) {
            if (!mcExpr.getNameAsString().equals("asList")) {
                final String methodCall = mcExpr.toString();
                //System.out.println("method call:" + methodCall);
                String methodCallRepl = methodCall.replace(".", "_ACCESSING_METHODS_");

                if (methodCall.contains("implies")) {
                    finalString = handleImply(mcExpr, finalString);
                }

                //System.out.println("mcExpr:" + mcExpr.toString());
                /*
                if (mcExpr.toString().startsWith("daikon_ACCESSING_FIELDS_Quant.size")) {
                    //System.out.println("here is the size");
                    final String paramName = mcExpr.getArgument(0).toString();
                    final String arrayNameLength = paramName + "_ACCESSING_FIELDS_length";
                    final String setNameSize = paramName + "_ACCESSING_METHODS_size";
                    if (VariableManager.getNumbersVars().contains(arrayNameLength)) {
                        finalString = finalString.replace(mcExpr.toString(), arrayNameLength);
                    } else if (VariableManager.getNumbersVars().contains(setNameSize)) {
                        finalString = finalString.replace(mcExpr.toString(), setNameSize);
                    }
                }
                */

                //replace parameter
                //TODO: check more than one parameter
                if (!methodCallRepl.contains("()")) {
                    methodCallRepl = methodCallRepl.replace("(", "_PARAMETER_METHOD_");
                    methodCallRepl = methodCallRepl.replace(")", "");
                } else {
                    methodCallRepl = methodCallRepl.replace("()", "");
                }
                finalString = finalString.replace(methodCall, methodCallRepl);
            }

        }
        return finalString;
    }

    private static String handleImply(final MethodCallExpr mcExpr, String finalString) {
        final NodeList<Expression> argList = mcExpr.getArguments();
        String firstArgument = argList.get(0).toString();
        String secondArgument = argList.get(1).toString();
        finalString = finalString.replace(mcExpr.toString(), "implies(" + firstArgument + "," + secondArgument + ")");
        if (firstArgument.contains("implies") || secondArgument.contains("implies")) {
            for (final String impl : implyReplacementMap.keySet()) {
                firstArgument = firstArgument.replace(", ", ",");
                secondArgument = secondArgument.replace(", ", ",");

                if (firstArgument.contains(impl)) {
                    firstArgument = firstArgument.replace(impl, implyReplacementMap.get(impl));
                }

                secondArgument = secondArgument.replace(", ", ",");
                if (secondArgument.contains(impl)) {
                    secondArgument = secondArgument.replace(impl, implyReplacementMap.get(impl));
                }
            }
        }

        String origImply = "implies(" + firstArgument + "," + secondArgument + ")";
        //String origImply = mcExpr.toString();
        for (final String impl : implyReplacementMap.keySet()) {
            origImply = origImply.replace(impl, implyReplacementMap.get(impl));
        }

        origImply = origImply.replace(", ", ",");
        final String utilReplacement = "(" + firstArgument + ") => (" + secondArgument + ")";
        finalString = finalString.replace(", ", ",");
        finalString = finalString.replace("implies( ", "implies(");
        finalString = finalString.replace(origImply, utilReplacement);
        implyReplacementMap.put(origImply, utilReplacement);
        implyReplacementMap.put(mcExpr.toString().replace("), ", "),"), utilReplacement);

        return finalString;
    }

    private static String replaceFieldDelimiters(String finalString, final List<FieldAccessExpr> faList) {
        for (final FieldAccessExpr faExpr : faList) {
            final String fieldCall = faExpr.toString();
            String fieldCallRepl = fieldCall;

            fieldCallRepl = fieldCall.replace(".", "_ACCESSING_FIELDS_");
            finalString = finalString.replace(fieldCall, fieldCallRepl);
        }
        return finalString;
    }

    //TODO: check that only not gets replace
    public static String replaceNotAndNotEqual(final String s) {
        String tempS = s;
        tempS = tempS.replace("!=", "notequal123");
        tempS = tempS.replace("!", "NOT");
        tempS = tempS.replace("notequal123", "<>");
        return tempS;
    }
}
