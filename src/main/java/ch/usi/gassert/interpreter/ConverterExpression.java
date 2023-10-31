package ch.usi.gassert.interpreter;

import ch.usi.gassert.Config;

import java.util.*;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeReaderJava;
import ch.usi.gassert.util.Pair;
import com.udojava.evalex.Expression;

public class ConverterExpression {
    private static String IMPLY = "=>";

    public static String convertGASSERTtoJava(final String s) {

        String result = "";
        try {
            result = handleImply(s);
        } catch (final StackOverflowError t) {
            // more general: catch(Error t)
            // anything: catch(Throwable t)
            System.err.println(s);
            t.printStackTrace();
            //System.exit(-1);
        }

        final Expression expr = new Expression(result);
        final Iterator<Expression.Token> expressionTokenizer = expr.getExpressionTokenizer();

        while (expressionTokenizer.hasNext()) {
            final Expression.Token token = expressionTokenizer.next();
            final String tokenConverted = replaceDelimiters(token.surface.toString());
            result = result.replace(token.surface.toString(), tokenConverted);
        }

        result = replaceDelimiters(result);
        result = result.replace("_PARAMETER_SPECIAL_", ").");

        if (result.contains("<=>")) {
            result = replaceEquality(result);
        }

        if (result.contains("NOT")) {
            result = replaceNot(result);
        }

        if (result.contains("<>")) {
            result = replaceInequality(result);
        }
        return result;
    }


    public static String handleImply(final String s) {
        String result = s;
        final Expression expr1 = new Expression(result);
        final Iterator<Expression.Token> expressionTokenizer1 = expr1.getExpressionTokenizer();
        final ArrayList<String> tokenList = new ArrayList<String>();
        final ArrayList<String> tokenTypeList = new ArrayList<String>();

        while (expressionTokenizer1.hasNext()) {
            final Expression.Token token = expressionTokenizer1.next();
            tokenList.add(token.surface.toString());
            tokenTypeList.add(token.type.toString());
        }

        int i = 0;
        String leftPart = "";
        String rightPart = "";
        boolean prevActiveLeft = false;
        boolean prevActiveRight = false;
        boolean hadImply = false;
        int firstImplyIndex = -1;
        int closeParentCount = 0;

        for (final String token : tokenList) {

            if (token.equals(IMPLY) && !hadImply) {
                prevActiveLeft = true;
                prevActiveRight = true;
                hadImply = true;
                firstImplyIndex = i;
            }

            //getLeftPart of imply
            if (prevActiveLeft && !tokenList.get(i - 1).equals(")")) {
                leftPart = tokenList.get(i - 1);
            } else if (prevActiveLeft && tokenList.get(i - 1).equals(")")) {

                leftpartloop:
                for (int j = i - 1; j >= 0; j--) {
                    if (tokenList.get(j).equals("(")) {
                        closeParentCount--;
                    } else if (tokenList.get(j).equals(")")) {
                        closeParentCount++;
                    }

                    if (closeParentCount != -1) {
                        leftPart = getReplacement(tokenList.get(j), tokenTypeList.get(j)) + leftPart;
                    } else {
                        closeParentCount = 0;
                        break leftpartloop;
                    }
                }
            }

            prevActiveLeft = false;

            if (prevActiveRight && tokenList.get(i).equals("(")) {
                closeParentCount++;
            } else if (prevActiveRight && tokenList.get(i).equals(")")) {
                closeParentCount--;
            }

            if (prevActiveRight && (i != firstImplyIndex)) {
                rightPart = rightPart + getReplacement(tokenList.get(i), tokenTypeList.get(i));
            }

            if (prevActiveRight && closeParentCount == -1) {

                leftPart = leftPart.replace(", ", ",");
                rightPart = rightPart.replace(", ", ",");
                result = result.replace(", ", ",");
                //System.out.println("leftPart:" + leftPart);
                //System.out.println("rightPart" + rightPart);
                final String implyAssertion = convertGASSERTwithImply(leftPart, rightPart);
                //System.out.println("to replace:" + leftPart + " => " + rightPart);
                //System.out.println("result:" + result + " " + result.contains(leftPart + "=>" + rightPart));
                result = result.replace(leftPart + " => " + rightPart, implyAssertion);
                //System.out.println("replaced:" + result);
                prevActiveRight = false;

                if (rightPart.contains(IMPLY)) {
                    final String newRightPart = handleImply(rightPart);
                    result = result.replace(rightPart, newRightPart);
                }

                if (leftPart.contains(IMPLY)) {
                    final String newLeftPart = handleImply(leftPart);
                    result = result.replace(leftPart, newLeftPart);
                }

                if (result.contains(IMPLY)) {
                    //System.out.println("calling now");
                    result = handleImply(result);
                }
            } else if (i == tokenList.size() - 1) {
                final String implyAssertion = convertGASSERTwithImply(leftPart, rightPart);
                result = result.replace(leftPart + " => " + rightPart, implyAssertion);
            }

            i++;
        }

        return result;
    }

    private static String getReplacement(final String s, final String type) {
        if (type.equals("OPERATOR") && !s.equals(".")) {
            return " " + s + " ";
        }

        if (type.equals("UNARY_OPERATOR")) {
            return s.replace("u", "");
        }

        return s;
    }

    public static String replaceDelimiters(final String s) {
        String adapted = s;
        adapted = adapted.replaceAll(Config.DELIMITER_FIELDS, ".");
        if (adapted.contains(Config.DELIMITER_METHODS)) {

            if (adapted.contains(Config.DELIMITER_METHODS + "isNull")) {
                adapted = adapted.replace(Config.DELIMITER_METHODS + "isNull", " == null");
                adapted = adapted.replace(Config.DELIMITER_METHODS, ".");
                return adapted;
            }

            adapted = adapted.replace(Config.DELIMITER_METHODS, ".");

            if (adapted.contains(Config.DELIMITER_PARAMETERS_METHODS)) {
                adapted = adapted.replace(Config.DELIMITER_PARAMETERS_METHODS, "(");
                adapted = adapted + ")";
            } else {
                adapted = adapted + "()";
            }
        }
        return adapted;
    }

    public static String replaceNot(final String s) {
        final String newAssertion = s;
        return newAssertion.replace("NOT", "!");
    }

    public static String replaceInequality(final String s) {
        final String newAssertion = s;
        return newAssertion.replace("<>", "!=");
    }

    public static String replaceEquality(final String s) {
        final String newAssertion = s;
        return newAssertion.replace("<=>", "==");
    }

    public static String convertGASSERTwithImply(final String leftPart, final String rightPart) {
        String newAssertion = null;

        final long countRightOpen = getCount(rightPart, '(');
        final long countRightClose = getCount(rightPart, ')');
        final long countLeftOpen = getCount(leftPart, '(');
        final long countLeftClose = getCount(leftPart, ')');

        if ((countRightOpen != countRightClose) && (countLeftOpen != countLeftClose)) {
            newAssertion = "ch.usi.gassert.util.Implies.implies" + leftPart + "," + rightPart;
        } else {
            newAssertion = "ch.usi.gassert.util.Implies.implies(" + leftPart + "," + rightPart + ")";
        }

        return newAssertion;
    }

    private static long getCount(final String s, final char symbol) {
        return s.chars().filter(ch -> ch == symbol).count();
    }

    public static String convertGASSERTtoJava(final Expression exp) {
        return replaceDelimiters(exp.toString());
    }

    public boolean equals(final Object x) {
        return true;
    }

    public static class CreateAssertStatements {

        public static List<String> convert(final String assertion) {
            if (assertion.replace(" ", "").replace("(", "").startsWith("ch.usi.gassert.util.Implies")) {
                final Pair<String, String> impliesArgs = SplitImplies.splitImplies(assertion);

                final Tree tree = TreeReaderJava.getTree(impliesArgs.getValue());
                final CreateAssertStatements ga = new CreateAssertStatements();
                ga.recursionImplies(tree, impliesArgs.getKey());
                if (ga.assertions.size() > 1) {
                    ga.assertions.add("assert(" + assertion + ");");
                }
                return ga.assertions;
            } else {
                final Tree tree = TreeReaderJava.getTree(assertion);
                final CreateAssertStatements ga = new CreateAssertStatements();
                ga.recursion(tree);
                if (ga.assertions.size() > 1) {
                    ga.assertions.add("assert(" + assertion + ");");
                }
                return ga.assertions;
            }
        }

        final List<String> assertions = new LinkedList<>();

        public void recursionImplies(final Tree t, final String implies) {
            if (t.getValue().equals("&&")) {
                recursionImplies(t.getLeft(), implies);
                recursionImplies(t.getRight(), implies);
            } else {
                assertions.add("assert(ch.usi.gassert.util.Implies.implies(" + implies + "," + t.toString() + "));");
            }
        }

        public void recursion(final Tree t) {
            if (t.getValue().equals("&&")) {
                recursion(t.getLeft());
                recursion(t.getRight());
            } else {
                assertions.add("assert(" + t.toString() + ");");
            }
        }
    }
}


