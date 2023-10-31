package ch.usi.gassert.data.tree;

import ch.usi.gassert.util.LazyMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static ch.usi.gassert.util.CollectionUtils.set;
import static org.junit.Assert.*;

public class TreeUtilsTest {

    final static Map<Tree, Set<String>> trees2variables = new HashMap<>();

    static {
        final Map<String, Class<?>> variableTypes = new LazyMap<>(v -> Integer.class);
        trees2variables.put(TreeReaderGAssert.getTree("false", variableTypes ), set());
        trees2variables.put(TreeReaderGAssert.getTree("-1", variableTypes ), set());
        trees2variables.put(TreeReaderGAssert.getTree("45.353746", variableTypes ), set());
        trees2variables.put(TreeReaderGAssert.getTree("((i_y_s - (i_y_f + 3.1416)) == i_y_f)", variableTypes ), set("i_y_s", "i_y_f"));
        trees2variables.put(TreeReaderGAssert.getTree("(i_y_f >= (i_y_s + 3.1416))", variableTypes ), set("i_y_s", "i_y_f"));
        trees2variables.put(TreeReaderGAssert.getTree("(i_x_f == (i_x_s + 1))", variableTypes ), set("i_x_s", "i_x_f"));
        trees2variables.put(TreeReaderGAssert.getTree("(i_this.PRECISION_s < (((i_x_f / i_this.PRECISION_s) - (i_x_f * i_x_s)) * i_this.PRECISION_s))", variableTypes ), set("i_x_s", "i_x_f", "i_this.PRECISION_s"));
        trees2variables.put(TreeReaderGAssert.getTree("((i_y_s / i_y_f) > 2.7183)", variableTypes ), set("i_y_s", "i_y_f"));
    }

    @Test
    public void testGetUsedVariables() {
        for (Tree tree : trees2variables.keySet()) {
            Set<String> usedVariables = TreeUtils.getUsedVariables(tree);
            assertEquals(trees2variables.get(tree), usedVariables);
        }
    }

}
