package ch.usi.gassert.evolutionary;

import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.data.tree.selector.ITreeTemplateSelector;
import ch.usi.gassert.evolutionary.crossover.ICrossover;
import ch.usi.gassert.evolutionary.fitness.IFitnessFunction;
import ch.usi.gassert.evolutionary.fitness.IValidator;
import ch.usi.gassert.evolutionary.mutation.IMutation;
import ch.usi.gassert.evolutionary.selection.ISelection;
import ch.usi.gassert.util.random.IRandomSelector;

public class BehaviourManager {

    // EvolutionaryAlgorithm configuration parameters

    public boolean IS_ELITISM_ENABLED = true;
    public boolean IS_MIGRATION_ENABLED = true;

    // EvolutionaryAlgorithm Individual evaluation

    public IValidator validator;
    public IValidator eliteValidator;
    public IFitnessFunction fitnessFP;
    public IFitnessFunction fitnessFN;

    public void setValidator(final IValidator validator) {
        this.validator = validator;
    }

    public void setEliteValidator(final IValidator eliteValidator) {
        this.eliteValidator = eliteValidator;
    }

    public void setFitnessFP(final IFitnessFunction fitnessFP) {
        this.fitnessFP = fitnessFP;
    }

    public void setFitnessFN(final IFitnessFunction fitnessFN) {
        this.fitnessFN = fitnessFN;
    }

    public IValidator getValidator() {
        return this.validator;
    }

    public IValidator getEliteValidator() {
        return this.eliteValidator;
    }

    public IFitnessFunction getFitnessFP() {
        return this.fitnessFP;
    }

    public IFitnessFunction getFitnessFN() {
        return this.fitnessFN;
    }

    // EvolutionaryAlgorithm operators

    public IRandomSelector<ISelection> selections;
    public IRandomSelector<ITreeTemplateSelector> treeTemplateSelector;
    public IRandomSelector<ICrossover> crossovers;
    public IRandomSelector<IMutation> mutations;

    public void setSelection(final IRandomSelector<ISelection> selections) {
        this.selections = selections;
    }

    public void setTreeTemplateSelector(final IRandomSelector<ITreeTemplateSelector> treeTemplateSelector) {
        this.treeTemplateSelector = treeTemplateSelector;
    }

    public void setCrossover(final IRandomSelector<ICrossover> crossovers) {
        this.crossovers = crossovers;
    }

    public void setMutation(final IRandomSelector<IMutation> mutations) {
        this.mutations = mutations;
    }

    public ISelection getSelection() {
        return selections.next();
    }

    public ITreeTemplateSelector getTreeTemplateSelector() {
        return treeTemplateSelector.next();
    }

    public ICrossover getCrossover() {
        return crossovers.next();
    }

    public IMutation getMutation() {
        return mutations.next();
    }

}
