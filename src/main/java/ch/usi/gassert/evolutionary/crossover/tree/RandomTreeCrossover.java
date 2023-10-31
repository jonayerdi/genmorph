package ch.usi.gassert.evolutionary.crossover.tree;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.selector.ITreeSelector;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Pair;

/**
 * This class impements the crossover between two trees
 * <p>
 * choose randomly two nodes in the two trees and swap them creating two offsprings
 */
public class RandomTreeCrossover extends AbstractTreeCrossover {

    private static final int MAX_TRIES = 10;

    public RandomTreeCrossover(final TreeBehaviourManager treeBehaviourManager) {
        super(treeBehaviourManager);
    }

    public Pair<Tree, Tree> crossover(final Tree mother, final Tree father, final Individual individualMother, final Individual individualFather) {
        final Tree child1 = new Tree(mother);
        final Tree child2 = new Tree(father);

        int iter = 0; // this is a safe net
        do {
            iter++;
            if (iter >= MAX_TRIES) {
                return new Pair<>(child1, child2);
            }
            final ITreeSelector.ParentAndTree motherSelect = treeBehaviourManager.getTreeSelector().selectTree(child1, individualMother);
            final ITreeSelector.ParentAndTree fatherSelect = treeBehaviourManager.getTreeSelector().selectTree(child2, individualFather);
            final Tree nodeMother = motherSelect.getTree();
            final Tree nodeFather = fatherSelect.getTree();
            final Tree parentMother = motherSelect.getParent();
            final Tree parentFather = fatherSelect.getParent();

            //put this otherwise I might get the root
            if (parentFather == null || parentMother == null) {
                continue;
            }
            if (nodeMother.getType().equals(nodeFather.getType())) {
                // if is compatible I can do the crossover
                parentFather.substitute(nodeFather, nodeMother);
                parentMother.substitute(nodeMother, nodeFather);
                return new Pair<>(child1, child2);
            }
        } while (true);
    }
}
