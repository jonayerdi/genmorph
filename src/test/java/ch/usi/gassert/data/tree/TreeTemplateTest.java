package ch.usi.gassert.data.tree;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.tree.builder.GeneratedTreeBuilder;
import ch.usi.gassert.data.tree.selector.ITreeSelector;
import ch.usi.gassert.data.tree.selector.RandomTreeSelector;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.crossover.tree.ITreeCrossover;
import ch.usi.gassert.evolutionary.crossover.tree.RandomTreeCrossover;
import ch.usi.gassert.evolutionary.mutation.tree.ConstantValueMutation;
import ch.usi.gassert.evolutionary.mutation.tree.ITreeMutation;
import ch.usi.gassert.evolutionary.mutation.tree.SingleNodeTreeMutation;
import ch.usi.gassert.evolutionary.mutation.tree.SubTreeMutation;
import ch.usi.gassert.util.random.DynamicWeightedMap;
import ch.usi.gassert.util.random.IRandomSelector;
import ch.usi.gassert.util.random.WeightedMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TreeTemplateTest {

    public static VariablesManager makeVariables() {
        final Map<String, Class<?>> variableTypes = new HashMap<>();
        variableTypes.put("b1", Boolean.class);
        variableTypes.put("b2", Boolean.class);
        variableTypes.put("b3", Boolean.class);
        variableTypes.put("i1", Integer.class);
        variableTypes.put("d1", Double.class);
        variableTypes.put("l1", Long.class);
        return VariablesManager.fromVariableTypes(variableTypes);
    }

    public static TreeBehaviourManager makeTreeBehaviourManager() {
        TreeBehaviourManager treeBehaviourManager = new TreeBehaviourManager(Config.MAX_DEPTH_TREE);

        IRandomSelector<ITreeSelector> treeSelector = new WeightedMap<ITreeSelector>()
                .add(100, new RandomTreeSelector());
        IRandomSelector<ITreeMutation> mutation = new DynamicWeightedMap<ITreeMutation>()
                .add(33, new SingleNodeTreeMutation(treeBehaviourManager))
                .add(33, new SubTreeMutation(treeBehaviourManager))
                .add(33, new ConstantValueMutation(treeBehaviourManager, 0.1, true))
                .update();
        IRandomSelector<ITreeCrossover>  crossover = new WeightedMap<ITreeCrossover>()
                .add(100, new RandomTreeCrossover(treeBehaviourManager));

        treeBehaviourManager.setTreeSelector(treeSelector);
        treeBehaviourManager.setCrossover(crossover);
        treeBehaviourManager.setMutation(mutation);

        return treeBehaviourManager;
    }

    @Test
    public void testBuildTree1() {
        final TreeTemplate basicBoolean = new TreeTemplate(
                new GeneratedTreeBuilder(makeTreeBehaviourManager(), new TreeFactory(makeVariables()), Tree.Type.BOOLEAN),
                Tree.Type.BOOLEAN, TreeTemplate.Mode.GENERATED);
        final TreeGroup tgBool = new TreeGroup(basicBoolean);
        final Tree tBool = basicBoolean.buildTree(tgBool);
        assertEquals(tBool.getType(), basicBoolean.getType());
    }

    @Test
    public void testBuildTree2() {
        final TreeTemplate basicNumeric = new TreeTemplate(
                new GeneratedTreeBuilder(makeTreeBehaviourManager(), new TreeFactory(makeVariables()), Tree.Type.NUMBER),
                Tree.Type.NUMBER, TreeTemplate.Mode.GENERATED);
        final TreeGroup tgNum = new TreeGroup(basicNumeric);
        final Tree tNum = tgNum.buildTree();
        assertEquals(tNum.getType(), basicNumeric.getType());
    }

    @Test
    public void testBuildTree3() {
        final TreeTemplate impliesTemplate = new TreeTemplate("<=>",
                new TreeTemplate(
                        new GeneratedTreeBuilder(makeTreeBehaviourManager(), new TreeFactory(makeVariables()), Tree.Type.BOOLEAN),
                        Tree.Type.BOOLEAN,TreeTemplate.Mode.GENERATED),
                new TreeTemplate(
                        new GeneratedTreeBuilder(makeTreeBehaviourManager(), new TreeFactory(makeVariables()), Tree.Type.BOOLEAN),
                        Tree.Type.BOOLEAN, TreeTemplate.Mode.GENERATED),
                Tree.Type.BOOLEAN, TreeTemplate.Mode.STATIC
        );
        for (int i = 0 ; i < 20 ; ++i) {
            final TreeGroup tgImplies = new TreeGroup(impliesTemplate);
            final Tree tImplies = tgImplies.buildTree();
            assertEquals(tImplies.getType(), impliesTemplate.getType());
            assertEquals(tImplies.getValue(), "<=>");
            assertEquals(tImplies.getLeft().getType(), Tree.Type.BOOLEAN);
            assertEquals(tImplies.getRight().getType(), Tree.Type.BOOLEAN);
        }
    }

    @Test
    public void testMatch1() {
        final TreeTemplate comparisonTemplate = new TreeTemplate("<=",
                new TreeTemplate("d1", Tree.Type.NUMBER,TreeTemplate.Mode.STATIC),
                new TreeTemplate(
                        new GeneratedTreeBuilder(makeTreeBehaviourManager(), new TreeFactory(makeVariables()), Tree.Type.NUMBER),
                        Tree.Type.NUMBER, TreeTemplate.Mode.GENERATED),
                Tree.Type.BOOLEAN, TreeTemplate.Mode.STATIC
        );
        final TreeGroup tgc = comparisonTemplate.match(
                TreeReaderGAssert.getTree("d1 <= (2 * (i1 + l1))", makeVariables().getVariableTypes())
        );
        assertEquals(tgc.buildTree(),
                new Tree(
                    "<=",
                    new Tree("d1", Tree.Type.NUMBER),
                    new Tree(
                            "*",
                            new Tree("2", Tree.Type.NUMBER),
                            new Tree(
                                    "+",
                                    new Tree("i1", Tree.Type.NUMBER),
                                    new Tree("l1", Tree.Type.NUMBER),
                                    Tree.Type.NUMBER
                            ),
                            Tree.Type.NUMBER
                    ),
                    Tree.Type.BOOLEAN
                )
        );
        try {
            // Wrong comparison operator
            comparisonTemplate.match(
                    TreeReaderGAssert.getTree("d1 >= i1", makeVariables().getVariableTypes())
            );
            fail("Expected exception from non-matching template");
        } catch (RuntimeException e) {
            assertEquals("Cannot match template", e.getMessage());
        }
        try {
            // Right side of the expression is boolean instead of numeric
            comparisonTemplate.match(
                    TreeReaderGAssert.getTree("d1 <= (i1 <= l1)", makeVariables().getVariableTypes())
            );
            fail("Expected exception from non-matching template");
        } catch (RuntimeException e) {
            assertEquals("Cannot match template", e.getMessage());
        }
    }

    @Test
    public void testMatch2() {
        final TreeTemplate impliesTemplate = new TreeTemplate("=>",
                new TreeTemplate(
                        new GeneratedTreeBuilder(makeTreeBehaviourManager(), new TreeFactory(makeVariables()), Tree.Type.BOOLEAN),
                        Tree.Type.BOOLEAN,TreeTemplate.Mode.GENERATED),
                new TreeTemplate(
                        new GeneratedTreeBuilder(makeTreeBehaviourManager(), new TreeFactory(makeVariables()), Tree.Type.BOOLEAN),
                        Tree.Type.BOOLEAN, TreeTemplate.Mode.GENERATED),
                Tree.Type.BOOLEAN, TreeTemplate.Mode.STATIC
        );
        final TreeGroup tgc = impliesTemplate.match(
                TreeReaderGAssert.getTree("((d1 + i1) < 5) => (b2 || b3)", makeVariables().getVariableTypes())
        );
        assertEquals(tgc.buildTree(),
                new Tree(
                        "=>",
                        new Tree(
                                "<",
                                new Tree(
                                        "+",
                                        new Tree("d1", Tree.Type.NUMBER),
                                        new Tree("i1", Tree.Type.NUMBER),
                                        Tree.Type.NUMBER
                                ),
                                new Tree("5", Tree.Type.NUMBER),
                                Tree.Type.BOOLEAN
                        ),
                        new Tree(
                                "||",
                                new Tree("b2", Tree.Type.BOOLEAN),
                                new Tree("b3", Tree.Type.BOOLEAN),
                                Tree.Type.BOOLEAN
                        ),
                        Tree.Type.BOOLEAN
                )
        );
        try {
            // Missing implies operator
            impliesTemplate.match(
                    TreeReaderGAssert.getTree("(b1 && b2) && b3", makeVariables().getVariableTypes())
            );
            fail("Expected exception from non-matching template");
        } catch (RuntimeException e) {
            assertEquals("Cannot match template", e.getMessage());
        }
        try {
            // Right side of the expression is numeric instead of boolean
            impliesTemplate.match(
                    TreeReaderGAssert.getTree("((b1 || b3) => (b2 || b3)) => i1", makeVariables().getVariableTypes())
            );
            fail("Expected exception from non-matching template");
        } catch (RuntimeException e) {
            assertEquals("Cannot match template", e.getMessage());
        }
    }

}
