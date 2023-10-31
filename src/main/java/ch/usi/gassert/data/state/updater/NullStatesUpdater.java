package ch.usi.gassert.data.state.updater;

import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.evolutionary.Individual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NullStatesUpdater implements IStatesUpdater {

    public static final NullStatesUpdater INSTANCE = new NullStatesUpdater();
    public static final List<ITestExecution> EMPTY_STATES_LIST = new ArrayList<>(0);

    public NullStatesUpdater() {

    }

    public NullStatesUpdater(final List<String> args) {

    }

    @Override
    public void writeIndividuals(Collection<Individual> individuals, int timeBudget) {
        // Do nothing
    }

    @Override
    public List<ITestExecution> readStates() {
        // Do nothing
        return EMPTY_STATES_LIST;
    }

}
