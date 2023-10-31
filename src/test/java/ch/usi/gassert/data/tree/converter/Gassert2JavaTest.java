package ch.usi.gassert.data.tree.converter;

import ch.usi.gassert.data.tree.TreeReaderGAssert;
import ch.usi.gassert.util.LazyMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Gassert2JavaTest {

    final static Map<String, String> gassert2java = new HashMap<>();

    static {
        gassert2java.put("false", "false");
        gassert2java.put("1", "1.0");
        gassert2java.put("45.353746", "45.3537");
        gassert2java.put("((i_y_s - (i_y_f + 3.1416)) == i_y_f)", "(Math.abs((((double) i_y_s) - (((double) i_y_f) + 3.1416)) - ((double) i_y_f)) < 1.0E-4)");
        gassert2java.put("(i_y_f >= (i_y_s + 3.1416))", "(((double) i_y_f) >= (((double) i_y_s) + 3.1416))");
        gassert2java.put("(i_y_f == (i_y_s * 3.1416))", "(Math.abs(((double) i_y_f) - (((double) i_y_s) * 3.1416)) < 1.0E-4)");
        gassert2java.put("(i_x_f == (i_x_s + 1))", "(Math.abs(((double) i_x_f) - (((double) i_x_s) + 1.0)) < 1.0E-4)");
        gassert2java.put("((i_y_s / i_y_f) > 2.7183)", "(((Math.abs(((double) i_y_f)) < 1.0E-4) ? 1.0 : (((double) i_y_s) / ((double) i_y_f))) > 2.7183)");
    }

    final static Map<String, Class<?>> variableTypes = new LazyMap<>(v -> Double.class);

    @Test
    public void test() {
        for (String assertion : gassert2java.keySet()) {
            String javaExpression;
            try {
                javaExpression = GAssert2Java.GAssertTree2Java(TreeReaderGAssert.getTree(assertion, variableTypes));
            } catch (Exception e) {
                throw new RuntimeException("Exception converting assertion: " + assertion, e);
            }
            assertEquals(gassert2java.get(assertion), javaExpression);
        }
    }

}
