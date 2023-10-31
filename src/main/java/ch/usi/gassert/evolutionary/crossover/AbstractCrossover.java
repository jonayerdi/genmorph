package ch.usi.gassert.evolutionary.crossover;

import ch.usi.gassert.evolutionary.BehaviourManager;

public abstract class AbstractCrossover implements ICrossover {

    public final BehaviourManager behaviourManager;

    protected AbstractCrossover(final BehaviourManager behaviourManager) {
        this.behaviourManager = behaviourManager;
    }

}
