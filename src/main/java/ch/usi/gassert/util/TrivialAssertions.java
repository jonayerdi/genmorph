package ch.usi.gassert.util;

import ch.usi.gassert.evolutionary.Individual;
import com.udojava.evalex.Expression;

import java.util.HashSet;
import java.util.regex.Pattern;

public class TrivialAssertions {

    public static Pattern trivialPattern = Pattern.compile("^\\s*\\(*\\s*(\\d*\\.?\\d*|true|false)\\s*\\)*\\s*$");

    /*
    public static HashSet<String> trivial;

    static {
        trivial = new HashSet<>();
        trivial.add("true");
        trivial.add("false");
        trivial.add("1");
        trivial.add("0");
        trivial.add("(true)");
        trivial.add("(false)");
        trivial.add("(1)");
        trivial.add("(0)");
        trivial.add("(true > false)");
        trivial.add("( true > false )");
        trivial.add("true > false");
        trivial.add("(false > true)");
        trivial.add("( false > true )");
        trivial.add("false > true");
        trivial.add("(true )");
        trivial.add("( false )");
        trivial.add("( 1 )");
        trivial.add("( 0 )");
    }
    */

    public static boolean isTrivial(final String assertion) {
        //return trivial.contains(assertion);
        return trivialPattern.matcher(assertion).matches();
    }

    public static boolean isTrivial(final Individual individual) {
        return isTrivial(individual.getAssertionAsString());
    }

}
