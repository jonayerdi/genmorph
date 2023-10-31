package ch.usi.gassert.data.manager.method;

import ch.usi.gassert.Mode;
import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.util.Cache;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.MyRandom;
import org.mu.testcase.classification.Classification;
import org.mu.testcase.classification.TestClassifications;
import org.mu.util.streams.IStreamLoader;

import java.util.*;
import java.util.stream.Collectors;

// Instantiated by DataManagerFactory
@SuppressWarnings("unused")
public class MethodRegularDataManager extends MethodDataManager {

    /**
     * args = new String[] {
     *                         "GASSERT",
     *                         "states/states.zip",
     *                         "states/classifications.zip"
     *                 }
     */
    public MethodRegularDataManager(final DataManagerArgs dargs, final IStatesUpdater statesUpdater) {
        super(dargs, statesUpdater);
    }

    @Override
    public Mode getMode() {
        return Mode.REGULAR_ASSERTIONS;
    }

    public static Pair<List<ITestExecution>, List<ITestExecution>>
    sampleAndLoadStates(final IStreamLoader statesDataSource,
                       List<String[]> correct,
                       List<String[]> incorrect,
                       Integer maxCorrectExecutions,
                       Integer maxIncorrectExecutions) {
        final List<String[]> correctSelected;
        final List<String[]> incorrectSelected;
        // Sample states if needed
        if (maxCorrectExecutions != null && correct.size() > maxCorrectExecutions) {
            final Set<Integer> selected = MyRandom.getInstance().getRandomIndices(correct.size(), maxCorrectExecutions);
            correctSelected = selected.stream().map(correct::get).collect(Collectors.toList());
        } else {
            correctSelected = correct;
        }
        if (maxIncorrectExecutions != null && incorrect.size() > maxIncorrectExecutions) {
            final Set<Integer> selected = MyRandom.getInstance().getRandomIndices(incorrect.size(), maxIncorrectExecutions);
            incorrectSelected = selected.stream().map(incorrect::get).collect(Collectors.toList());
        } else {
            incorrectSelected = incorrect;
        }
        // Load sampled TestExecutions
        final Cache<String, TestExecution> testExecutionsCache = new Cache<>(entry -> {
            try {
                return loadTestExecution(statesDataSource.load(entry));
            } catch (Exception e) {
                throw new RuntimeException("Error for entry: " + entry, e);
            }
        });
        final List<ITestExecution> correctExecutions = new ArrayList<>(correctSelected.size() * 2);
        for (final String[] id : correctSelected) {
            correctExecutions.add(testExecutionsCache.get(getTestExecutionEntry(id[0], id[1])));
        }
        final List<ITestExecution> incorrectExecutions = new ArrayList<>(incorrectSelected.size() * 2);
        for (final String[] id : incorrectSelected) {
            incorrectExecutions.add(testExecutionsCache.get(getTestExecutionEntry(id[0], id[1])));
        }
        return Pair.of(correctExecutions, incorrectExecutions);
    }

    @Override
    protected Pair<List<ITestExecution>, List<ITestExecution>>
    initCorrectIncorrectExecutions(DataManagerArgs dargs,
                                   Map<String, TestClassifications> classifications,
                                   IStreamLoader statesDataSource) {
        final int size = classifications.size() * classifications.values().stream().findAny().map(c -> c.rowIds.size()).orElse(0);
        final List<String[]> correct = new ArrayList<>(size);
        final List<String[]> incorrect = new ArrayList<>(size);
        final int columnIndex = 0;
        classifications.keySet().forEach(systemId -> {
            final TestClassifications systemClassifications = classifications.get(systemId);
            systemClassifications.rowIds.forEach((testId, rowIndex) -> {
                final Classification cls = classifications.get(systemId).get(rowIndex, columnIndex);
                switch (cls) {
                    case CORRECT:
                        correct.add(new String[] { systemId, testId });
                        break;
                    case INCORRECT:
                        incorrect.add(new String[] { systemId, testId });
                        break;
                }
            });
        });
        return sampleAndLoadStates(statesDataSource, correct, incorrect,
                dargs.maxCorrectExecutions, dargs.maxIncorrectExecutions);
    }

}
