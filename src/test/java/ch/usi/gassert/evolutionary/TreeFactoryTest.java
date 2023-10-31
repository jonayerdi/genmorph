package ch.usi.gassert.evolutionary;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.interpreter.BooleanInterpreter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

public class TreeFactoryTest {

    /**
     * stress testing
     */
    @Test
    public void test1() {
        final Map<String, Object> name2value = new HashMap<>();
        name2value.put("a", "true");
        name2value.put("b", "false");
        name2value.put("x", "3");
        name2value.put("y", "4");
        name2value.put("z", "0.4");
        Map<String, Class<?>> variableTypes = new HashMap<>();
        variableTypes.put("a", Boolean.class);
        variableTypes.put("b", Boolean.class);
        variableTypes.put("x", Integer.class);
        variableTypes.put("y", Integer.class);
        variableTypes.put("z", Integer.class);
        final TreeFactory factory = new TreeFactory(VariablesManager.fromVariableTypes(variableTypes));
        for (int depth = 1; depth <= 15; depth++) {
            for (int i = 0; i <= 10; i++) {
                final Tree tree = factory.buildTree(Tree.Type.BOOLEAN, depth, Config.PROB_CONSTANT_MIN);
                try {
                    BooleanInterpreter.eval(tree.toString(), name2value);
                } catch (final java.lang.ArithmeticException e) {
                    // ignore division by zero
                }
            }
        }
    }

}