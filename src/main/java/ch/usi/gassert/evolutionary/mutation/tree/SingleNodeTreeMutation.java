package ch.usi.gassert.evolutionary.mutation.tree;

import ch.usi.gassert.Functions;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.random.MyRandom;

/**
 * This mutation changes a single node in the tree
 */
public class SingleNodeTreeMutation extends AbstractTreeMutation {

    public SingleNodeTreeMutation(final TreeBehaviourManager treeBehaviourManager) {
        super(treeBehaviourManager);
    }

    protected void mutateNode(final Tree selectedNode, final TreeFactory treeFactory) {
        final MyRandom rng = MyRandom.getInstance();
        if (selectedNode.isLeaf()) {
            selectedNode.setValue(treeFactory.createLeaf(selectedNode.getType(),
                    rng.nextFloat() > treeBehaviourManager.ProbConstant));
        } else {
            final String value = selectedNode.getValue().toString();
            final String newValue;
            if (Functions.isMathReturnsBoolean(value)) {
                newValue = rng.getRandomElementList(Functions.functionsMathReturnsBoolean);
            } else if (Functions.isMathReturnsMath(value)) {
                newValue = rng.getRandomElementList(Functions.functionsMathReturnsMath);
            } else if (Functions.isBooleanReturnsBoolean(value)) {
                newValue = rng.getRandomElementList(Functions.functionsBooleanReturnsBoolean);
            } else if (Functions.isUnarySequenceReturnsMath(value)) {
                newValue = rng.getRandomElementList(Functions.functionsUnarySequenceReturnsMath);
            } else if (Functions.isBinarySequenceReturnsBoolean(value)) {
                newValue = rng.getRandomElementList(Functions.functionsBinarySequenceReturnsBoolean);
            } else {
                mutateNode(selectedNode.getRight() == null || rng.nextBoolean() 
                    ? selectedNode.getLeft() : selectedNode.getRight(), treeFactory);
                return;
            }
            selectedNode.setValue(newValue);
        }
    }

    @Override
    public Tree mutate(final Tree tree, final Individual individual, final TreeFactory treeFactory) {
        //create the mutant which is the copy of the original tree
        final Tree mutant = new Tree(tree);
        final Tree selectedNode = treeBehaviourManager.getTreeSelector().selectTree(mutant, individual).getTree();
        this.mutateNode(selectedNode, treeFactory);
        return mutant;
    }

}
