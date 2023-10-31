package ch.usi.gassert.data.state.updater;

import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.evolutionary.Individual;

import java.util.Collection;
import java.util.List;

public interface IStatesUpdater {

    void writeIndividuals(Collection<Individual> individuals, int timeBudget);
    List<ITestExecution> readStates();

    default void setDataManager(final IDataManager dataManager) {
        // Do nothing
    }
    default List<ITestExecution> sync(Collection<Individual> individuals, int timeBudget) {
        this.writeIndividuals(individuals, timeBudget);
        return readStates();
    }

}
