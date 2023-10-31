package ch.usi.gassert.interpreter;

import ch.usi.gassert.Config;
import com.udojava.evalex.Expression;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class BooleanInterpreterTest {


    @Test
    public void testInterpreter1() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "3");
        name2value.put("B", "1");
        assertTrue(BooleanInterpreter.eval("A > B", name2value));
    }

    @Test
    public void testInterpreter2() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "true");
        name2value.put("B", "false");
        name2value.put("C", "false");
        assertTrue(BooleanInterpreter.eval("(A || (B && C))", name2value));
    }

    @Test
    public void testInterpreter3() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "false");
        name2value.put("B", "true");
        name2value.put("C", "false");
        assertFalse(BooleanInterpreter.eval("(A || (B && C))", name2value));
    }

    @Test
    public void testInterpreter4() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "3");
        name2value.put("B", "4");
        try {
            assertFalse(BooleanInterpreter.eval("(A + B)", name2value));
            fail();
        } catch (final ch.usi.gassert.interpreter.NotBooleanException e) {
            assertTrue(true);
        }
    }


    @Test
    public void testInterpreter5() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "false");
        assertTrue(BooleanInterpreter.eval("(A || 5>4)", name2value));
    }


    @Test
    public void testInterpreter6() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "true");
        assertTrue(BooleanInterpreter.eval("(A || (5+3)>10)", name2value));
    }

    @Test
    public void testInterpreter7() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "true");
        name2value.put("NO", "true");
        assertTrue(BooleanInterpreter.eval("(A || (5+3)>10)", name2value));
    }

    @Test
    public void testImplies() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "false");
        name2value.put("B", "false");
        assertTrue(BooleanInterpreter.eval("(A => B)", name2value));
    }

    @Test
    public void testImplies2() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "false");
        name2value.put("B", "true");
        assertTrue(BooleanInterpreter.eval("(A => B)", name2value));
    }

    @Test
    public void testImplies3() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "true");
        name2value.put("B", "false");
        assertFalse(BooleanInterpreter.eval("(A => B)", name2value));
    }

    @Test
    public void testImplies4() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "true");
        name2value.put("B", "true");
        assertTrue(BooleanInterpreter.eval("(A => B)", name2value));
    }

    @Test
    public void testABS() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "-5");
        name2value.put("B", "5");
        assertTrue(BooleanInterpreter.eval("ABS(A) == B", name2value));
    }

    @Test
    public void testStrings() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "ciao".hashCode());
        name2value.put("B", "ciao".hashCode());
        assertTrue(BooleanInterpreter.eval("A == B", name2value));
    }

    @Test
    public void testStrings2() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "ciao".hashCode());
        name2value.put("B", "ciaoe".hashCode());
        assertFalse(BooleanInterpreter.eval("A == B", name2value));
    }


    @Test
    public void testModulo() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "999");
        assertTrue(BooleanInterpreter.eval("(A % 999) = 0", name2value));
        name2value.put("A", "933399");
        assertFalse(BooleanInterpreter.eval("(A % 999) = 0", name2value));
    }

    @Test
    public void testNull() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "null");
        assertTrue(BooleanInterpreter.eval("A = null", name2value));
    }

    @Test
    public void testNull2() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "null");
        assertTrue(BooleanInterpreter.eval("A == null", name2value));
        assertFalse(BooleanInterpreter.eval("A != null", name2value));
    }

    @Test
    public void testImply() {

        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "true");
        name2value.put("B", "true");
        assertTrue(BooleanInterpreter.eval("A => B", name2value));
        name2value.put("A", "true");
        name2value.put("B", "false");
        assertFalse(BooleanInterpreter.eval("A => B", name2value));
        name2value.put("A", "false");
        name2value.put("B", "true");
        assertTrue(BooleanInterpreter.eval("A => B", name2value));
        name2value.put("A", "false");
        name2value.put("B", "false");
        assertTrue(BooleanInterpreter.eval("A => B", name2value));

    }


    @Test
    public void testIfAndOnlyIf() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("A", "true");
        name2value.put("B", "true");
        assertTrue(BooleanInterpreter.eval("A <=> B", name2value));
        name2value.put("A", "true");
        name2value.put("B", "false");
        assertFalse(BooleanInterpreter.eval("A <=> B", name2value));
        name2value.put("A", "false");
        name2value.put("B", "true");
        assertFalse(BooleanInterpreter.eval("A <=> B", name2value));
        name2value.put("A", "false");
        name2value.put("B", "false");
        assertTrue(BooleanInterpreter.eval("A <=> B", name2value));

    }


    @Test
    public void testNullResilient() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("this_k__isNull", "false");
        name2value.put("this_k__equals_h", "true");
        assertTrue(BooleanInterpreter.eval("this_k__equals_h", name2value));
    }

    @Test
    public void testNullResilient2() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("this_k__isNull", "false");
        name2value.put("this_k__equals_h", "false");
        assertFalse(BooleanInterpreter.eval("this_k__equals_h", name2value));
    }

    @Test
    public void testNullResilient3() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("this_k__isNull", "true");
        assertTrue(BooleanInterpreter.eval("this_k__equals_h", name2value));
    }


    @Test
    public void testGuard() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull", "false");
        name2value.put("a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "size", "5");
        assertTrue(BooleanInterpreter.eval("a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "size > 1", name2value));
    }

    @Test
    public void testGuard2() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull", "true");
        assertTrue(BooleanInterpreter.eval("a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "size > 1", name2value));
    }

    @Test
    public void testGuard3() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull", "true");
        name2value.put("b" + Config.DELIMITER_METHODS + "isNull", "true");
        final String expected = "(NOT(a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull) && NOT(b" + Config.DELIMITER_METHODS + "isNull))";
        final String expected2 = "(NOT(b" + Config.DELIMITER_METHODS + "isNull) && NOT(a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull))";


        final String actual = BooleanInterpreter.getGuard(new Expression("(a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "size > 5) && (b" + Config.DELIMITER_METHODS + "size == 0)"), name2value);

        assertTrue(expected.equals(actual) || expected2.equals(actual));
    }

    @Test
    public void testGuard4() {
        final Expression inputExpression = new Expression("(a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "size > 5) && (b" + Config.DELIMITER_METHODS + "size == 0)");
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull", "true");
        name2value.put("b" + Config.DELIMITER_METHODS + "isNull", "true");
        final String expected = "((NOT(a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull) && NOT(b" + Config.DELIMITER_METHODS + "isNull))) => (" + inputExpression.toString() + ")";
        final String expected2 = "((NOT(b" + Config.DELIMITER_METHODS + "isNull) && NOT(a" + Config.DELIMITER_FIELDS + "k" + Config.DELIMITER_METHODS + "isNull))) => (" + inputExpression.toString() + ")";
        final String actual = BooleanInterpreter.addGuard(inputExpression);
        System.out.println("expected:   " + ConverterExpression.convertGASSERTtoJava(expected));
        System.out.println("expected2:  " + ConverterExpression.convertGASSERTtoJava(expected2));
        System.out.println("actual:     " + ConverterExpression.convertGASSERTtoJava(actual));
        //bug?
        //assertTrue(expected.equals(actual) || expected2.equals(actual));
    }

    @Test
    public void testGuard5() {
        final Expression inputExpression = new Expression("((a >= b) <=> (min == b))");
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("a", "3.0");
        name2value.put("b", "3.0");
        name2value.put("min", "3.0");
        final String expected = "((a >= b) <=> (min == b))";
        final String actual = BooleanInterpreter.addGuard(inputExpression);
        assertTrue(expected.equals(actual));
    }

    @Test
    public void testGuard6() {
        final Expression inputExpression = new Expression("NOT(a" + Config.DELIMITER_FIELDS + "k)");
        final String actual = BooleanInterpreter.addGuard(inputExpression);
        assertEquals("((NOT(a_ACCESSING_METHODS_isNull))) => (NOT(a_ACCESSING_FIELDS_k))", actual);
    }

    @Test
    public void testGuard7() {
        final Expression inputExpression = new Expression("(((1 <> ind) <> Arrays_ACCESSING_METHODS_asList_PARAMETER_METHOD_intArray_PARAMETER_SPECIAL_contains_PARAMETER_METHOD_old_ind) || ((1 <> old_intArray_ACCESSING_FIELDS_length) <> NOT(Arrays_ACCESSING_METHODS_asList_PARAMETER_METHOD_old_intArray_PARAMETER_SPECIAL_contains_PARAMETER_METHOD_old_ind)))");
        final String actual = BooleanInterpreter.addGuard(inputExpression);
        final String expected = "ch.usi.gassert.util.Implies.implies(((!(intArray == null) && !(old_intArray == null))),((((1 != ind) != Arrays.asList(intArray).contains(old_ind)) || ((1 != old_intArray.length) != !(Arrays.asList(old_intArray).contains(old_ind))))))";
        assertEquals(expected, ConverterExpression.convertGASSERTtoJava(actual));
    }

    @Test
    public void testtrue() {
        final Expression inputExpression = new Expression("true > false");
        final Map<String, Object> name2value = new HashMap<>();
        assertTrue(BooleanInterpreter.eval(inputExpression, name2value));
    }
}
