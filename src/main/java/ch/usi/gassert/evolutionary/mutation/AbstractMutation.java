package ch.usi.gassert.evolutionary.mutation;

import ch.usi.gassert.evolutionary.BehaviourManager;

public abstract class AbstractMutation implements IMutation {

    public final BehaviourManager behaviourManager;

    protected AbstractMutation(final BehaviourManager behaviourManager) {
        this.behaviourManager = behaviourManager;
    }

}
