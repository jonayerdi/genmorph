package ch.usi.gassert.evolutionary.crossover;

import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

import java.util.Set;

import static ch.usi.gassert.util.Assert.assertAlways;

public class SwappingCrossover extends AbstractCrossover {

    public SwappingCrossover(BehaviourManager behaviourManager) {
        super(behaviourManager);
    }

    @Override
    public Pair<TreeGroup, TreeGroup> crossover(TreeGroup mother, TreeGroup father, Individual individualMother, Individual individualFather) {
        // Select a random subtree and swap it between mother and father.
        TreeGroup childMother = new TreeGroup(mother);
        TreeGroup childFather = new TreeGroup(father);;
        Set<TreeTemplate> generatedTrees = mother.mappings.keySet();
        assertAlways(generatedTrees.size() > 1, "SwappingCrossover should only be used for 2 or more generated subtrees");
        TreeTemplate subTree = behaviourManager.getTreeTemplateSelector().selectTree(generatedTrees);
        ITree motherTree = childMother.mappings.get(subTree);
        ITree fatherTree = childFather.mappings.get(subTree);
        childMother.mappings.put(subTree, fatherTree);
        childFather.mappings.put(subTree, motherTree);
        return Pair.of(childMother, childFather);
    }

}
