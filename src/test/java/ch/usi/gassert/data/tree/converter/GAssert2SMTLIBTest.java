package ch.usi.gassert.data.tree.converter;

import ch.usi.gassert.data.tree.TreeReaderGAssert;
import ch.usi.gassert.util.LazyMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class GAssert2SMTLIBTest {

    final static Map<String, String> gassert2smtlib = new HashMap<>();

    static {
        gassert2smtlib.put("false", "false");
        gassert2smtlib.put("1", "1");
        gassert2smtlib.put("45.353746", "45.354");
        gassert2smtlib.put("((i_y_s - (i_y_f + 3.1416)) == i_y_f)", "(= (- i_y_s (+ i_y_f 3.142)) i_y_f)");
        gassert2smtlib.put("(i_y_f >= (i_y_s + 3.1416))", "(>= i_y_f (+ i_y_s 3.142))");
        gassert2smtlib.put("(i_y_f == (i_y_s * 3.1416))", "(= i_y_f (* i_y_s 3.142))");
        gassert2smtlib.put("(i_x_f == (i_x_s + 1))", "(= i_x_f (+ i_x_s 1))");
        gassert2smtlib.put("((i_y_s / i_y_f) > 2.7183)", "(> (/ i_y_s i_y_f) 2.718)");
        gassert2smtlib.put("(i_this.PRECISION_s < (((i_x_f / i_this.PRECISION_s) - (i_x_f * i_x_s)) * i_this.PRECISION_s))", "(< i_this.PRECISION_s (* (- (/ i_x_f i_this.PRECISION_s) (* i_x_f i_x_s)) i_this.PRECISION_s))");
        gassert2smtlib.put("((i_p_s * i_x_f) > (i_this.PRECISION_s / i_x_s))", "(> (* i_p_s i_x_f) (/ i_this.PRECISION_s i_x_s))");
    }

    final static Map<String, Class<?>> variableTypes = new LazyMap<>(v -> Double.class);

    @Test
    public void test() {
        for (String assertion : gassert2smtlib.keySet()) {
            String smtConstraint;
            try {
                smtConstraint = GAssert2SMTLIB.GAssertTree2SMTLIB(TreeReaderGAssert.getTree(assertion, variableTypes));
            } catch (Exception e) {
                throw new RuntimeException("Exception converting assertion: " + assertion, e);
            }
            assertEquals(gassert2smtlib.get(assertion), smtConstraint);
        }
    }

}
