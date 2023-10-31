package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.evolutionary.Individual;

public interface IFitnessFunction {
    void computeFitness(Individual sol);
    default void recomputeFitness(Individual sol) {
        computeFitness(sol);
    }
}
