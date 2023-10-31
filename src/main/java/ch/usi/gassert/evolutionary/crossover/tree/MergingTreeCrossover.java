package ch.usi.gassert.evolutionary.crossover.tree;

import ch.usi.gassert.Config;
import ch.usi.gassert.Functions;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.selector.ITreeSelector;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.gassert.util.Pair;


public class MergingTreeCrossover extends AbstractTreeCrossover {

    private static final int MAX_TRIES = 10;

    public MergingTreeCrossover(final TreeBehaviourManager treeBehaviourManager) {
        super(treeBehaviourManager);
    }

    public Pair<Tree, Tree> crossover(final Tree mother, final Tree father, final Individual individualMother, final Individual individualFather) {
        Tree.Type treeType = mother.getType();
        if (treeType == Tree.Type.SEQUENCE) {
            // There is no fn(SEQUENCE, SEQUENCE) -> SEQUENCE
            return new Pair<>(mother, father);
        }
        assert treeType == father.getType();
        int iter = 0; // this is a safe net
        do {
            iter++;
            if (iter >= MAX_TRIES) {
                return new Pair<>(mother, father);
            }

            final Tree nodeMother;
            final Tree nodeFather;
            if (father.getNumberOfNodes() + mother.getNumberOfNodes() < Config.MAX_COMPLEXITY && MyRandom.getInstance().nextFloat() < Config.PROB_MERGE_CROSSOVER_AS_IT_IS) {
                nodeMother = mother;
                nodeFather = father;
            } else {
                final ITreeSelector.ParentAndTree motherSelect = treeBehaviourManager.getTreeSelector().selectTree(mother, individualMother);
                final ITreeSelector.ParentAndTree fatherSelect = treeBehaviourManager.getTreeSelector().selectTree(father, individualFather);
                nodeMother = motherSelect.getTree();
                nodeFather = fatherSelect.getTree();
            }

            //put this otherwise I might get the root
            if (nodeMother.getType().equals(nodeFather.getType())
                    && nodeFather.getType().equals(treeType)
                    && !nodeMother.isLeaf() && !nodeFather.isLeaf()
            ) {
                // if is compatible I can do the crossover
                final Tree child1, child2;
                switch (treeType) {
                    case BOOLEAN:
                        child1 = new Tree(Functions.functionsBooleanReturnsBoolean.get(0)
                                , nodeMother, nodeFather, Tree.Type.BOOLEAN);
                        child2 = new Tree(Functions.functionsBooleanReturnsBoolean.get(1)
                                , nodeMother, nodeFather, Tree.Type.BOOLEAN);
                        break;
                    case NUMBER:
                        int op1 = MyRandom.getInstance().nextInt(4);
                        int op2;
                        do {
                            op2 = MyRandom.getInstance().nextInt(4);
                        } while (op1 == op2);
                        child1 = new Tree(Functions.functionsMathReturnsMath.get(op1)
                                , nodeMother, nodeFather, Tree.Type.NUMBER);
                        child2 = new Tree(Functions.functionsMathReturnsMath.get(op2)
                                , nodeMother, nodeFather, Tree.Type.NUMBER);
                        break;
                    default:
                        throw new RuntimeException("Unknown tree type: " + treeType);
                }

                return new Pair<>(child1, child2);
            }
        } while (true);


    }
}
