package ch.usi.gassert.evolutionary;

import ch.usi.gassert.evolutionary.crossover.conjunctive.IConjunctiveCrossover;
import ch.usi.gassert.evolutionary.mutation.conjunctive.IConjunctiveMutation;
import ch.usi.gassert.util.random.IRandomSelector;

public class ConjunctiveClausesBehaviourManager {

    // EvolutionaryAlgorithm variables

    public void updateGen(int gen) {
        // Nothing here yet
    }

    // EvolutionaryAlgorithm operators

    public IRandomSelector<IConjunctiveCrossover> crossovers;
    public IRandomSelector<IConjunctiveMutation> mutations;

    public void setCrossover(final IRandomSelector<IConjunctiveCrossover> crossovers) {
        this.crossovers = crossovers;
    }

    public void setMutation(final IRandomSelector<IConjunctiveMutation> mutations) {
        this.mutations = mutations;
    }

    public IConjunctiveCrossover getCrossover() {
        return crossovers.next();
    }

    public IConjunctiveMutation getMutation() {
        return mutations.next();
    }

}
