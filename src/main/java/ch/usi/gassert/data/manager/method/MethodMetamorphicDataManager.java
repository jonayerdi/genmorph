package ch.usi.gassert.data.manager.method;

import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.state.TestExecution;
import ch.usi.gassert.data.state.TestExecutionPair;
import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.util.Cache;
import ch.usi.gassert.util.MR;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.MyRandom;
import org.mu.testcase.classification.Classification;
import org.mu.testcase.classification.TestClassifications;
import org.mu.testcase.metamorphic.MRInfo;
import org.mu.util.streams.IStreamLoader;

import java.util.*;
import java.util.stream.Collectors;

import static ch.usi.gassert.util.FileUtils.SEPARATORS;

public abstract class MethodMetamorphicDataManager extends MethodDataManager {

    public MethodMetamorphicDataManager(DataManagerArgs dargs, final IStatesUpdater statesUpdater) {
        super(dargs, statesUpdater);
    }

    public static Pair<List<ITestExecution>, List<ITestExecution>>
    sampleAndLoadMetamorphicStates(final IStreamLoader statesDataSource,
                                   List<String[]> correct,
                                   List<String[]> incorrect,
                                   Integer maxCorrectExecutions,
                                   Integer maxIncorrectExecutions,
                                   final List<MRInfo> mrinfosMandatory) {
        final Set<String> mandatoryTestPairs = mrinfosMandatory.stream()
                .map(mt -> MR.JOIN_SOURCE_FOLLOWUP(mt.source, mt.followup))
                .collect(Collectors.toSet());
        final List<String[]> correctSelected;
        final List<String[]> incorrectSelected;
        // Sample states if needed
        if (maxCorrectExecutions != null && correct.size() > maxCorrectExecutions) {
            Set<Integer> selected = new HashSet<>(maxCorrectExecutions);
            for (int i = 0 ; i < correct.size() ; ++i) {
                final String[] id = correct.get(i);
                if (mandatoryTestPairs.contains(MR.JOIN_SOURCE_FOLLOWUP(id[1], id[2]))) {
                    selected.add(i);
                }
            }
            selected = MyRandom.getInstance().getRandomIndices(correct.size(), maxCorrectExecutions, selected);
            correctSelected = selected.stream().map(correct::get).collect(Collectors.toList());
        } else {
            correctSelected = correct;
        }
        if (maxIncorrectExecutions != null && incorrect.size() > maxIncorrectExecutions) {
            Set<Integer> selected = new HashSet<>(maxIncorrectExecutions);
            for (int i = 0 ; i < incorrect.size() ; ++i) {
                final String[] id = incorrect.get(i);
                if (mandatoryTestPairs.contains(MR.JOIN_SOURCE_FOLLOWUP(id[1], id[2]))) {
                    selected.add(i);
                }
            }
            selected = MyRandom.getInstance().getRandomIndices(incorrect.size(), maxIncorrectExecutions, selected);
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
            final TestExecution source = testExecutionsCache.get(getTestExecutionEntry(id[0], id[1]));
            final TestExecution followup = testExecutionsCache.get(getTestExecutionEntry(id[0], id[2]));
            correctExecutions.add(new TestExecutionPair(source, followup));
        }
        final List<ITestExecution> incorrectExecutions = new ArrayList<>(incorrectSelected.size() * 2);
        for (final String[] id : incorrectSelected) {
            final TestExecution source = testExecutionsCache.get(getTestExecutionEntry(id[0], id[1]));
            final TestExecution followup = testExecutionsCache.get(getTestExecutionEntry(id[0], id[2]));
            incorrectExecutions.add(new TestExecutionPair(source, followup));
        }
        return Pair.of(correctExecutions, incorrectExecutions);
    }

    public static Pair<List<ITestExecution>, List<ITestExecution>>
    initCorrectIncorrectExecutionsAllCombinations(DataManagerArgs dargs,
                                                  List<MRInfo> mrinfosMandatory,
                                                  Map<String, TestClassifications> classifications,
                                                  IStreamLoader statesDataSource) {
        final int size = classifications.size() * 32; // Assume 32 correct and 32 incorrect executions per systemId
        final List<String[]> correct = new ArrayList<>(size);
        final List<String[]> incorrect = new ArrayList<>(size);
        // Enumerate all possible states
        System.out.println("enumerate all states");
        final int columnIndex = 0;
        classifications.keySet().forEach(systemId -> {
            final TestClassifications systemClassifications = classifications.get(systemId);
            // Include every possible test pair combination (in both ways)
            systemClassifications.rowIds.forEach((testId1, rowIndex1) -> {
                systemClassifications.rowIds.forEach((testId2, rowIndex2) -> {
                    if (!testId1.equals(testId2)) {
                        final Classification cls1 = classifications.get(systemId).get(rowIndex1, columnIndex);
                        final Classification cls2 = classifications.get(systemId).get(rowIndex2, columnIndex);
                        if (cls1 == Classification.CORRECT && cls2 == Classification.CORRECT) {
                            // A pair where both executions are correct is a correct sample
                            correct.add(new String[]{ systemId, testId1, testId2 });
                            correct.add(new String[]{ systemId, testId2, testId1 });
                        } else if (cls1 == Classification.INCORRECT || cls2 == Classification.INCORRECT) {
                            // A pair where at least one execution is incorrect is an incorrect sample
                            incorrect.add(new String[]{ systemId, testId1, testId2 });
                            incorrect.add(new String[]{ systemId, testId2, testId1 });
                        }
                    }
                });
            });
        });
        System.out.println("metamorphic states: correct=" + correct.size() + ", incorrect=" + incorrect.size());
        System.out.println("call sampleAndLoadMetamorphicStates");
        return sampleAndLoadMetamorphicStates(statesDataSource, correct, incorrect,
                dargs.maxCorrectExecutions, dargs.maxIncorrectExecutions, mrinfosMandatory);
    }

    public static Pair<List<ITestExecution>, List<ITestExecution>>
    initCorrectIncorrectExecutionsMRInfo(DataManagerArgs dargs,
                                         List<MRInfo> selectedPairs, List<MRInfo> mrinfosMandatory,
                                         Map<String, TestClassifications> classifications,
                                         IStreamLoader statesDataSource) {
        final int size = classifications.size() * 32; // Assume 32 correct and 32 incorrect executions per systemId
        final List<String[]> correct = new ArrayList<>(size);
        final List<String[]> incorrect = new ArrayList<>(size);
        final int columnIndex = 0;
        classifications.keySet().forEach(systemId -> {
            final TestClassifications systemClassifications = classifications.get(systemId);
            for (final MRInfo pair : selectedPairs) {
                final Integer sourceClassificationIndex = systemClassifications.rowIds.get(pair.source);
                final Integer followupClassificationIndex = systemClassifications.rowIds.get(pair.followup);
                if (sourceClassificationIndex != null && followupClassificationIndex != null) {
                    final Classification sourceClassification = classifications.get(systemId).get(sourceClassificationIndex, columnIndex);
                    final Classification followupClassification = classifications.get(systemId).get(followupClassificationIndex, columnIndex);
                    final Classification classification = Classification.metamorphic(sourceClassification, followupClassification);
                    if (classification == null) {
                        System.err.println("Invalid classification pair: " + systemId + SEPARATORS[0] + pair.source + ":" + pair.followup);
                        continue;
                    }
                    switch (classification) {
                        case CORRECT:
                            correct.add(new String[]{ systemId, pair.source, pair.followup });
                            break;
                        case INCORRECT:
                            incorrect.add(new String[]{ systemId, pair.source, pair.followup });
                            break;
                    }
                }
            }
        });
        return sampleAndLoadMetamorphicStates(statesDataSource, correct, incorrect,
                dargs.maxCorrectExecutions, dargs.maxIncorrectExecutions, mrinfosMandatory);
    }

}
