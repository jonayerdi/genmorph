package ch.usi.gassert.evolutionary.crossover;

import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.builder.ITreeBuilder;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

public class SingleCrossover extends AbstractCrossover {

    public SingleCrossover(BehaviourManager behaviourManager) {
        super(behaviourManager);
    }

    @Override
    public Pair<TreeGroup, TreeGroup> crossover(TreeGroup mother, TreeGroup father, final Individual individualMother, final Individual individualFather) {
        // Select a random subtree and do the crossover there.
        TreeGroup childMother = new TreeGroup(mother);
        TreeGroup childFather = new TreeGroup(father);
        TreeTemplate subTree = behaviourManager.getTreeTemplateSelector().selectTree(mother);
        ITreeBuilder<?> treeBuilder = subTree.getTreeBuilder();
        ITree motherTree = mother.mappings.get(subTree);
        ITree fatherTree = father.mappings.get(subTree);
        Pair<? extends ITree, ? extends ITree> newTrees = treeBuilder.crossover(motherTree, fatherTree, individualMother, individualFather);
        childMother.mappings.put(subTree, newTrees.fst);
        childFather.mappings.put(subTree, newTrees.snd);
        return Pair.of(childMother, childFather);
    }

}
