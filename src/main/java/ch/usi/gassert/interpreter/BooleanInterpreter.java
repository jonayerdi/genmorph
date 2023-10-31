package ch.usi.gassert.interpreter;


import ch.usi.gassert.Config;
import ch.usi.gassert.data.ProgramState;
import ch.usi.gassert.data.state.TestExecution;
import com.google.common.collect.Sets;
import com.udojava.evalex.Expression;

import java.math.BigInteger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * this is a boolean interpreter
 */
@Deprecated
public class BooleanInterpreter {

    private static BigInteger countEval = BigInteger.ZERO;

    private BooleanInterpreter() {
        throw new IllegalStateException("Utility class");
    }

    public static BigInteger getCountEval() {
        return countEval;
    }

    public static boolean eval(final String expression, final Map<String, Object> name2value) {
        return eval(new Expression(expression), name2value);
    }

    public static boolean eval(final Expression expression, final ProgramState state) {
        return eval(expression, state.getIdentifier2value());
    }

    public static boolean eval(final Expression exp, final TestExecution exec) {
        return eval(exp, exec.getVariables().getValues());
    }

    /**
     * evaluation with the guard in case of missing/undefined variables
     *
     * @param exp
     * @param name2value
     * @return
     */
    protected static boolean eval(final Expression exp, final Map<String, Object> name2value) {
        //TODO INPUT implies (O_followup < exp)
        countEval = countEval.add(BigInteger.ONE);
        final Expression expGuard = new Expression(getGuard(exp, name2value));
        final int resGuard = expGuard.eval(name2value).intValue();
        if (resGuard != 0 && resGuard != 1) {
            throw new NotBooleanException(expGuard.toString());
        }
        if (resGuard == 1) {
            // if the guard is true I return the result of the expression (we are doing imply)
            final int res = exp.eval(name2value).intValue();
            if (res != 0 && res != 1) {
                throw new NotBooleanException(exp.toString());
            }
            if (Config.DEBUG) {
                System.out.println("expression : " + exp.toString());
                System.out.println("expression with guard : " + expGuard.toString());
                System.out.println("result : " + (res == 1 ? "T" : "F"));
                System.out.println("state : " + name2value.toString());
                System.out.println("_______________________________");
                System.out.println();
            }
            return res == 1;
        }
        // if the guard is false I return true
        // because we are doing imply
        return true;

    }

    /**
     * add a guard of the expression
     * <p>
     * check if there are unknown operator and add the null check
     *
     * @param exp
     * @return TRUE if no guard is not needed
     * return the guard if is needed
     */
    protected static String getGuard(final Expression exp, final Map<String, Object> name2value) {

        // get only the difference, means the undefined variables
        final Set<String> usedVars = exp.getUsedVariablesSet();
        final Set<String> availableVars = name2value.keySet();
        final Set<String> guardsVariables = new HashSet<>();
        for (final String varMissing : Sets.difference(usedVars, availableVars)) {
            for (final String varAvailable : availableVars) {
                if (varAvailable.endsWith("isNull") && varMissing.startsWith(varAvailable.substring(0, varAvailable.length() - "isNull".length()))) {
                    guardsVariables.add(varAvailable);
                } else if (varAvailable.endsWith("isNull") && varMissing.contains(varAvailable.split(Config.DELIMITER_METHODS)[0])) {
                    guardsVariables.add(varAvailable);
                }
            }

        }

        if (guardsVariables.isEmpty()) {
            // guard no needed
            return "TRUE";
        }

        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String guard : guardsVariables) {
            if (!first) {
                sb.append(" && ");
            }
            first = false;
            sb.append("NOT(" + guard + ")");
        }
        return "(" + sb.toString() + ")";
    }

    /**
     * this adds the gaurd for every isNull that are used
     *
     * @param expression
     * @return
     */
    public static String addGuard(final String expression) {
        return addGuard(new Expression(expression));
    }

    public static String addGuard(final Expression expression) {
        // this add to work for every variables that it requires no matter if we have or no
        final Map<String, Object> nameTovariables = new HashMap<>();
        final Set<String> usedVars = expression.getUsedVariablesSet();
        // I create fake variables of isNull so I can use the oroginal getGuard function
        for (final String usedVar : usedVars) {
            if (usedVar.contains(Config.DELIMITER_FIELDS)) {
                nameTovariables.put(usedVar.split(Config.DELIMITER_FIELDS)[0] + Config.DELIMITER_METHODS + "isNull", new Object());
            }
            if (usedVar.contains(Config.DELIMITER_METHODS)) {
                if (usedVar.contains(Config.DELIMITER_SPECIAL)) {
                    nameTovariables.put(usedVar.split(Config.DELIMITER_SPECIAL)[0].split(Config.DELIMITER_PARAMETERS_METHODS)[1] + Config.DELIMITER_METHODS + "isNull", new Object());
                } else {
                    nameTovariables.put(usedVar.split(Config.DELIMITER_METHODS)[0] + Config.DELIMITER_METHODS + "isNull", new Object());
                }
            }
        }
        final String guard = getGuard(expression, nameTovariables);
        return guard.equals("TRUE") ? expression.toString() : "(" + guard + ") => (" + expression.toString() + ")";
    }


}

class NotBooleanException extends RuntimeException {
    NotBooleanException(final String message) {
        super("the expression" + message + "is not a boolean expression");
    }
}
