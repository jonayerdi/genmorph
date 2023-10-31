package ch.usi.gassert.evolutionary.crossover.conjunctive;

import ch.usi.gassert.evolutionary.ConjunctiveClausesBehaviourManager;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.crossover.tree.ITreeCrossover;

public abstract class AbstractConjunctiveCrossover implements IConjunctiveCrossover {

    public final ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager;

    protected AbstractConjunctiveCrossover(final ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager) {
        this.conjunctiveBehaviourManager = conjunctiveBehaviourManager;
    }

}
