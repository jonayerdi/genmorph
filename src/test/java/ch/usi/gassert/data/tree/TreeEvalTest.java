package ch.usi.gassert.data.tree;

import org.junit.Test;

import ch.usi.gassert.data.types.Sequence;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static ch.usi.gassert.util.CollectionUtils.*;

public class TreeEvalTest {

    public static Object eval(final String expr) {
        return eval(expr, new HashMap<>(0));
    }

    public static Object eval(final String expr, final Map<String, Object> vars) {
        final Map<String, Class<?>> varTypes = new HashMap<>(vars.size());
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            varTypes.put(e.getKey(), e.getValue().getClass());
        }
        final Tree tree = TreeReaderGAssert.getTree(expr, varTypes);
        return TreeEval.eval(tree, vars, 0.0001);
    }

    @Test
    public void testConstants() {
        assertEquals(false, eval("false"));
        assertEquals(true, eval("true"));
        assertEquals(0.0, eval("0.0"));
        assertEquals(12.3, (double)eval("12.3"), 0.0001);
        assertEquals(12.3, (double)eval("+12.3"), 0.0001);
        assertEquals(-66.6, (double)eval("-66.6"), 0.0001);
        assertEquals(-6.0, (double)eval("6 - 12"), 0.0001);
        assertEquals(true, eval("true || false"));
        assertEquals(true, eval("NOT(false)"));
        assertEquals(66.6, (double)eval("ABS(66.6)"), 0.0001);
    }

    @Test
    public void testVariables() {
        assertEquals(false, eval("true && (1 + x) < 2", map("x", 1)));
        assertEquals(0.0, (double)eval("2.0 + 1 + x", map("x", -3)), 0.0001);
        assertEquals(2.0 / 3.0, (double)eval("x * y / z", map("x", 1, "y", 2, "z", 3)), 0.0001);
    }

    @Test
    public void testFunctions() {
        assertEquals(true, eval("NOT((1 >= 2.0) || false)"));
        assertEquals(false, eval("NOT((1 >= 2.0) || true)"));
        IntStream.range(-10, 10).forEach(x -> {
            assertEquals(Math.abs(4*x), (double)eval("ABS((2 * x) * -2)", map("x", x)), 0.0001);
        });
    }

    @Test
    public void testStringSequences() {
        assertEquals("45", ((Sequence)eval("string(45)")).getValue());
        assertEquals("45", ((Sequence)eval("string(45.72)")).getValue());
        assertEquals("-45", ((Sequence)eval("string(-45)")).getValue());
        assertEquals("54", ((Sequence)eval("flip(string(45))")).getValue());
        assertEquals("4", ((Sequence)eval("remove(flip(string(45)), 0)")).getValue());
        assertEquals("5", ((Sequence)eval("remove(flip(string(45)), 1)")).getValue());
        assertEquals("54", ((Sequence)eval("remove(flip(string(45)), 2)")).getValue());
        assertEquals('1' + '2', (double)eval("sum(string(12))"), 0.0001);
        assertEquals("12345678", ((Sequence)eval("string(x)", map("x", 12345678))).getValue());
        assertEquals(8, (double)eval("length(string(x))", map("x", 12345678)), 0.0001);
        assertEquals(true, eval("remove(flip(string(45)), 1) == string(5)"));
        assertEquals(true, eval("remove(flip(string(45)), 1) != string(4)"));
        assertEquals("12", ((Sequence)eval("truncate(string(1234), 2)")).getValue());
    }

    @Test
    public void testArraySequences() {
        final Object a = array(1, 2, 3, 4);
        final Object b = array(1, 2, 3, 4);
        assertEquals(false, a.equals(b));
        assertEquals(true, Sequence.fromValue(a).equals(Sequence.fromValue(b)));
        assertArrayEquals(array(1, 2, 3, 4), (Integer[])((Sequence)eval("x", map("x", Sequence.fromValue(array(1, 2, 3, 4))))).getValue());
        assertArrayEquals(array(4, 3, 2, 1), (Integer[])((Sequence)eval("flip(x)", map("x", Sequence.fromValue(array(1, 2, 3, 4))))).getValue());
        assertEquals(true, eval("flip(x) != x", map("x", Sequence.fromValue(array(1, 2, 3, 4)))));
        assertEquals(true, eval("remove(flip(x), length(x) - 2) == flip(remove(x, 1))", map("x", Sequence.fromValue(array(1, 2, 3, 4)))));
        assertEquals(true, eval("x == flip(flip(x))", map("x", Sequence.fromValue(array(Sequence.fromValue(array(1, 2, 3, 4)))))));
        assertEquals(Sequence.fromValue(list(1, 2)), eval("truncate(x, -2)", map("x", Sequence.fromValue(list(1, 2, 1, 2)))));
        assertEquals(Sequence.fromValue(list(1, 2)), eval("truncate(x, 2)", map("x", Sequence.fromValue(list(1, 2, 1, 2)))));
        assertEquals(true, eval("(truncate(x, -2) == truncate(x, 2)) && (truncate(x, -1) != truncate(x, 1))", map("x", Sequence.fromValue(array(1, 2, 1, 2)))));
        final Sequence v1 = Sequence.fromValue(array(1.0, 2.0));
        final Sequence v2 = Sequence.fromValue(array(1.0, 2.0 + 0.00001));
        final Sequence v3 = Sequence.fromValue(array(1.0, 2.0 + 0.001));
        assertEquals(true, eval("x == y", map("x", v1, "y", v2)));
        assertEquals(false, eval("x == y", map("x", v1, "y", v3)));
    }

    @Test
    public void testListSequences() {
        final Object a = list(1, 2, 3, 4);
        final Object b = list(1, 2, 3, 4);
        assertEquals(true, a.equals(b));
        assertEquals(list(1, 2, 3, 4), ((Sequence)eval("x", map("x", Sequence.fromValue(list(1, 2, 3, 4))))).getValue());
        assertEquals(list(4, 3, 2, 1), ((Sequence)eval("flip(x)", map("x", Sequence.fromValue(list(1, 2, 3, 4))))).getValue());
        assertEquals(list(1), ((Sequence)eval("truncate(x, 1)", map("x", Sequence.fromValue(list(1, 2, 3, 4))))).getValue());
        final Sequence v1 = Sequence.fromValue(list(1.0, 2.0));
        final Sequence v2 = Sequence.fromValue(list(1.0, 2.0 + 0.00001));
        final Sequence v3 = Sequence.fromValue(list(1.0, 2.0 + 0.001));
        assertEquals(true, eval("x == y", map("x", v1, "y", v2)));
        assertEquals(true, eval("x <> y", map("x", v1, "y", v3)));
    }

}
