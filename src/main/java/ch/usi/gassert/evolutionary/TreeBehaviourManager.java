package ch.usi.gassert.evolutionary;

import ch.usi.gassert.Config;
import ch.usi.gassert.data.tree.selector.ITreeSelector;
import ch.usi.gassert.evolutionary.crossover.tree.ITreeCrossover;
import ch.usi.gassert.evolutionary.mutation.tree.ITreeMutation;
import ch.usi.gassert.util.random.DynamicWeightedMap;
import ch.usi.gassert.util.random.IRandomSelector;
import ch.usi.gassert.util.random.MyRandom;

import java.util.function.Supplier;

/**
 * Evolutionary algorithm behaviour for generated Trees
 */
public class TreeBehaviourManager {

    public TreeBehaviourManager(final int maxDepthTree) {
        this.GetTreeDepth = () -> MyRandom.getInstance().nextInt(maxDepthTree);
    }

    // EvolutionaryAlgorithm variables

    public Supplier<Integer> GetTreeDepth;
    public void setMaxTreeDepth(final int maxDepthTree) {
        this.GetTreeDepth = () -> MyRandom.getInstance().nextInt(maxDepthTree);
    }

    public float ProbConstant = Config.PROB_CONSTANT_MIN;
    public final DynamicWeightedMap.Weight ConstantValueMutationWeight =
            new DynamicWeightedMap.Weight(Config.CONSTANT_VALUE_MUTATION_WEIGHT_MIN);
    public void updateGen(int gen) {
        this.ProbConstant = Config.COMPUTE_PROB_CONSTANT(gen);
        this.ConstantValueMutationWeight.update(Config.COMPUTE_CONSTANT_VALUE_MUTATION_WEIGHT(gen));
    }

    // EvolutionaryAlgorithm operators

    public IRandomSelector<ITreeSelector> treeSelector;
    public IRandomSelector<ITreeCrossover> crossovers;
    public IRandomSelector<ITreeMutation> mutations;

    public void setTreeSelector(final IRandomSelector<ITreeSelector> treeSelector) {
        this.treeSelector = treeSelector;
    }

    public void setCrossover(final IRandomSelector<ITreeCrossover> crossovers) {
        this.crossovers = crossovers;
    }

    public void setMutation(final IRandomSelector<ITreeMutation> mutations) {
        this.mutations = mutations;
    }

    public ITreeSelector getTreeSelector() {
        return treeSelector.next();
    }

    public ITreeCrossover getCrossover() {
        return crossovers.next();
    }

    public ITreeMutation getMutation() {
        return mutations.next();
    }

}
