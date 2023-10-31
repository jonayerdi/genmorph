package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Bitmap;
import com.udojava.evalex.Expression;

import java.util.*;

/**
 * We check only the faulty test executions
 * <p>
 * we have a FN if the assertion returns true for a test
 * <p>
 * The fitness function is 1.0 if in all tests the assertion returns true
 * The fitness function is 0.0 is in all tests the assertion returns false
 */
public class NumberFNFitnessFunction implements IFitnessFunction {

    final IDataManager dataManager;

    public NumberFNFitnessFunction(final IDataManager dataManager) {
        this.dataManager = dataManager;
    }

    protected void computeFitnessWithExecutions(final Individual sol,
                                                final List<ITestExecution> incorrectTestExecutions,
                                                final int computedCountStates, double computedCountFN) {
        if (!incorrectTestExecutions.isEmpty()) {
            final Tree tree = sol.getTree().asTree();
            final long[] testFaultyIdsFN = Bitmap.create(dataManager.getIncorrectTestExecutions().size());
            // Copy precomputed values from sol.idsFNGOOD
            Bitmap.copyBuckets(sol.idsFNGOOD, testFaultyIdsFN, computedCountStates);
            // Need to count how many because we might have to remove some for equivalences
            int countNumberStates = computedCountStates;
            // Check faulty test executions for FNs
            for (final ITestExecution testExecution : incorrectTestExecutions) {
                boolean compliant = dataManager.getAssertionEvaluator().eval(tree, testExecution.getVariables().getValues());
                if (compliant) computedCountFN += 1.0;
                else Bitmap.set(testFaultyIdsFN, countNumberStates);
                ++countNumberStates;
            }
            // Save results
            sol.fitnessValueFN = countNumberStates == 0 ? 0.0 : computedCountFN / (double)countNumberStates;
            sol.idsFNGOOD = testFaultyIdsFN;
            sol.setLastComputedIncorrectTestExecutionsSize(countNumberStates);
        }
    }

    @Override
    public void computeFitness(final Individual sol) {
        this.computeFitnessWithExecutions(sol, dataManager.getIncorrectTestExecutions(), 0, 0.0);
    }

    @Override
    public void recomputeFitness(Individual sol) {
        final int lastComputedIncorrectTestExecutionsSize = sol.getLastComputedIncorrectTestExecutionsSize();
        this.computeFitnessWithExecutions(sol,
                dataManager.getIncorrectTestExecutions()
                        .subList(lastComputedIncorrectTestExecutionsSize, dataManager.getIncorrectTestExecutions().size()),
                lastComputedIncorrectTestExecutionsSize,
                (double) Math.round(sol.fitnessValueFN * (double)lastComputedIncorrectTestExecutionsSize));
    }

}
