package ch.usi.gassert.data.tree;

import com.udojava.evalex.Expression;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static ch.usi.gassert.util.CollectionUtils.map;

public class TreeReaderTest {

    @Test
    public void test1() {
        final Tree tree = TreeReaderJava.getTree("(a + b)");
        final String treeString = tree.toString();
        System.out.println(treeString);
        assertEquals("(a + b)", treeString);
    }

    @Test
    public void test2() {
        final Tree tree = TreeReaderGAssert.getTree("(a + b)", map("a", Short.class, "b", Short.class));
        final String treeString = tree.toString();
        System.out.println(treeString);
        assertEquals("(a + b)", treeString);
    }

    @Test
    public void test3() {
        final String input = "((((NOT(old_intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_old_ind) || intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_ind) && intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_ind) == old_intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_old_ind) => (old_intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_old_ind || NOT(old_intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_old_ind)))";
        final Tree tree = TreeReaderGAssert.getTree(input, map(
                "old_intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_old_ind", Boolean.class,
                "intArray_ACCESSING_METHODS_contains_PARAMETER_METHOD_ind", Boolean.class
        ));
        final String treeString = tree.toString();
        System.out.println(treeString);
        assertEquals(input, treeString);
    }

    @Test
    public void test4() {
        final String input = "NOT(a)";
        final Tree tree = TreeReaderGAssert.getTree(input, map("a", Short.class));
        final String treeString = tree.toString();
        System.out.println(treeString);
        assertEquals(input, treeString);
    }

    @Test
    public void test5() {
        final String input = "(y < (-1 - x))";
        final String expected = "(y < ((0 - 1) - x))";
        final Tree tree = TreeReaderGAssert.getTree(input, map("x", Float.class, "y", Double.class));
        final String treeString = tree.toString();
        System.out.println(treeString);
        assertEquals(expected, treeString);
    }

    @Test
    public void test6() {
        final String input = "((x == y) || (ch.usi.gassert.util.Implies.implies(!(Arrays.asList(array2).contains(y)),!(Arrays.asList(array).contains(x)))))";
        final Tree tree = TreeReaderJava.getTree(input);
        final String treeString = tree.toString();
        System.out.println(treeString);
        assertEquals(input, treeString);
    }

    @Test
    public void test7() {
        final String input = "(x || (y && (a || c)))";
        final Tree tree = TreeReaderGAssert.getTree(input, map("x", Float.class,
                "y", Double.class, "a", Long.class, "c", Byte.class));
        final String treeString = tree.toString();
        System.out.println(treeString);
        assertEquals(input, treeString);
    }

    @Test
    public void test8() {
        final String input = "(x || (y && (a || c)))";
        final Tree tree = TreeReaderJava.getTree(input);
        final String treeString = tree.toString();
        assertEquals(input, treeString);
    }

    @Test
    public void test9() {
        final String input = "((x && y) || (a || c))";
        final Tree tree = TreeReaderJava.getTree(input);
        final String treeString = tree.toString();
        assertEquals(input, treeString);
    }

    @Test
    public void test10() {
        final String input = "(y < (-1 - x))";
        final Tree tree = TreeReaderJava.getTree(input);
        final String treeString = tree.toString();
        assertEquals(input, treeString);
    }


    @Test
    public void test11() {
        final String input = "((((result >= 1) && (false == i)) || (false == (old_x + result))) && ((result == old_x) || (i > (i * x))))";
        final Tree tree = TreeReaderGAssert.getTree(input, map("result", Integer.class, "i",
                Integer.class, "old_x", Integer.class, "x", Integer.class));
        final Expression exp = new Expression(input);
        exp.setVariable("result", "9");
        exp.setVariable("old_x", "9");
        exp.setVariable("i", "9");
        exp.setVariable("x", "9");
        final Integer result = exp.eval().intValue();
        final String treeString = tree.toString();
    }


    @Test
    public void test13() {
        final String input = "a == b";
        final Tree tree = TreeReaderGAssert.getTree(input, map("a", Integer.class, "b", Integer.class));
        final String treeString = tree.toString();
    }


    @Test
    public void test14() {
        final String input = "(false == (5 + 4))";
        final Tree tree = TreeReaderGAssert.getTree(input);
        assertTrue(tree.isCorrupted());
    }

    @Test
    public void test15() {
        final String input = "((x == y) || (ch.usi.gassert.util.Implies.implies(!(Arrays.asList(array2).contains(y)),!(Arrays.asList(array).contains(x)))))";
        final Tree tree = TreeReaderJavaForComplexityJava.getTree(input);
        final String treeString = tree.toString();
        System.out.println(tree.getNumberOfNodes());
    }

    @Test
    public void test16() {
        final String input = "(a.length == 9)";
        final Tree tree = TreeReaderGAssert.getTree(input, map("a.length", Integer.class));
        assertEquals(input, tree.toString());
    }

    // JAVA -> TREE

    // tree initial population
    // random trees
    // mutation of the assertions in input (java in input)
    // mutation
    // crossover

}