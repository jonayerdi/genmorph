package ch.usi.gassert.util;

import ch.usi.gassert.data.state.TestExecution;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * variables that have changed between the mutated and not mutated
 */
@Deprecated
public class GetCriticalVars {

    private static Map<Integer, List<String>> mutTestIdsToDifferenceVars;

    static {
        mutTestIdsToDifferenceVars = new ConcurrentHashMap<>();
    }

    public static Map<Integer, List<String>> getMutTestIdsToDifferenceVars() {
        return mutTestIdsToDifferenceVars;
    }

    /**
     * find diff between the original version and mutated version
     */
    public static void computeCriticalVars(final Map<String, TestExecution> originalTestExecutions) {
        throw new RuntimeException("Not implemented");
        /*
        int countNumberStates = 0;
        for (final List<TestExecution> mutTests : TestExecutionStates.finalMutationToTestExecutions) {
            for (final TestExecution test : mutTests) {
                // I need the original one not the unique ones
                if (originalTestExecutions.containsKey(test.getTestId())) {
                    final List<String> list = test.getInputs().getDifferentVariables(originalTestExecutions.get(test.getTestId()).getInputs());
                    mutTestIdsToDifferenceVars.put(countNumberStates, list);
                }
                countNumberStates++;
            }
        } */
    }
}
