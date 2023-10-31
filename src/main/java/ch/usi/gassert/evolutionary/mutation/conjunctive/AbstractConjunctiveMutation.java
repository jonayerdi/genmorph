package ch.usi.gassert.evolutionary.mutation.conjunctive;

import ch.usi.gassert.evolutionary.ConjunctiveClausesBehaviourManager;

public abstract class AbstractConjunctiveMutation implements IConjunctiveMutation {

    public final ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager;

    protected AbstractConjunctiveMutation(final ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager) {
        this.conjunctiveBehaviourManager = conjunctiveBehaviourManager;
    }

}
