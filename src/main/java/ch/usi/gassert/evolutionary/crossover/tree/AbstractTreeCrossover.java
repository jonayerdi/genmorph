package ch.usi.gassert.evolutionary.crossover.tree;

import ch.usi.gassert.evolutionary.TreeBehaviourManager;

public abstract class AbstractTreeCrossover implements ITreeCrossover {

    public final TreeBehaviourManager treeBehaviourManager;

    protected AbstractTreeCrossover(final TreeBehaviourManager treeBehaviourManager) {
        this.treeBehaviourManager = treeBehaviourManager;
    }

}
