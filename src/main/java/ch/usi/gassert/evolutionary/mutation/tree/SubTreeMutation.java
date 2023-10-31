package ch.usi.gassert.evolutionary.mutation.tree;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.selector.ITreeSelector;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.random.MyRandom;

public class SubTreeMutation extends AbstractTreeMutation {

    public SubTreeMutation(final TreeBehaviourManager treeBehaviourManager) {
        super(treeBehaviourManager);
    }

    @Override
    public Tree mutate(final Tree tree, final Individual individual, final TreeFactory treeFactory) {
        //create the mutant which is the copy of the original tree
        final Tree mutant = new Tree(tree);
        final ITreeSelector.ParentAndTree selectedParentAndNode = treeBehaviourManager.getTreeSelector().selectTree(mutant, individual);
        //create the list to choose a random element
        final Tree parentNode = selectedParentAndNode.getParent();
        final Tree selectedNode = selectedParentAndNode.getTree();
        // be sure is the same type
        if (parentNode == null) {
            // it means I selected the root I generate a random tree
            return treeFactory.buildTree(tree.getType(), treeBehaviourManager.GetTreeDepth.get(), treeBehaviourManager.ProbConstant);
        }
        final int depthTree = MyRandom.getInstance().nextFloat() <= Config.PROB_SUBTREE_LEAF_MUTATION ? 1 : MyRandom.getInstance().nextInt(3) + 2;
        parentNode.substitute(selectedNode, treeFactory.buildTree(selectedNode.getType(), depthTree, treeBehaviourManager.ProbConstant));
        return mutant;
    }

}
