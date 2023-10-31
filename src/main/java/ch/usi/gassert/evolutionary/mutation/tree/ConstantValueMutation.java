package ch.usi.gassert.evolutionary.mutation.tree;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.TreeUtils;
import ch.usi.gassert.data.tree.selector.ITreeSelector;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.evolutionary.mutation.tree.AbstractTreeMutation;
import ch.usi.gassert.util.random.MyRandom;

import java.util.List;

import static ch.usi.gassert.Functions.functionsBooleanReturnsBoolean;

/**
 * This mutation changes a single node in the tree
 */
public class ConstantValueMutation extends AbstractTreeMutation {

    private final boolean insertConstant;
    private final Double delta;

    public ConstantValueMutation(final TreeBehaviourManager treeBehaviourManager, final Double delta, final boolean insertConstant) {
        super(treeBehaviourManager);
        this.delta = delta;
        this.insertConstant = insertConstant;
    }

    public ConstantValueMutation(final TreeBehaviourManager treeBehaviourManager) {
        this(treeBehaviourManager, null, true);
    }

    @Override
    public Tree mutate(final Tree tree, final Individual individual, final TreeFactory treeFactory) {
        //create the mutant which is the copy of the original tree
        Tree mutant = new Tree(tree);
        MyRandom rng = MyRandom.getInstance();
        List<Tree> constantNodes = TreeUtils.getConstantNodes(mutant);
        if (!constantNodes.isEmpty()) {
            // Modify the value of a randomly selected constant node
            Tree node = rng.getRandomElementList(constantNodes);
            if (node.getType() == Tree.Type.NUMBER) {
                Number newValue;
                if (delta != null) {
                    newValue = Double.parseDouble(node.getValue().toString()) + (rng.nextDouble() * delta * (rng.nextBoolean() ? 1 : -1));
                } else {
                    newValue = treeFactory.getRandomNumber();
                }
                node.setValue(newValue);
            } else {
                node.setValue(rng.nextBoolean());
            }
        } else if(this.insertConstant) {
            // No constant nodes found, inject one into a random position
            ITreeSelector.ParentAndTree insertInto = treeBehaviourManager.getTreeSelector().selectTree(tree, individual);
            Tree parent = insertInto.getParent();
            Tree child = insertInto.getTree();
            Tree newNode = treeFactory.createLeaf(tree.getType(), false);
            String operator;
            switch (tree.getType()) {
                case NUMBER:
                    // Only perform addition of small numbers
                    operator = "+";
                    newNode.setValue(treeFactory.getRandomSmallNumber());
                    break;
                case BOOLEAN:
                    operator = rng.getRandomElementList(functionsBooleanReturnsBoolean);
                    break;
                default:
                    throw new RuntimeException("Unsupported tree type");
            }
            if (parent == null) {
                // Root node selected
                mutant = new Tree(operator, mutant, newNode, tree.getType());
            } else {
                parent.substitute(child, new Tree(operator, child, newNode, tree.getType()));
            }
        }
        return mutant;
    }

}
