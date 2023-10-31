package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.util.Bitmap;
import com.udojava.evalex.Expression;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * We check only the correct test executions
 * <p>
 * we have a FP if the assertion returns false for a test
 * <p>
 * The fitness function is 1.0 if in all tests the assertion returns false
 * The fitness function is 0.0 is in all tests the assertion returns true
 */
public class NumberFPFitnessFunction implements IFitnessFunction {

    final IDataManager dataManager;

    public NumberFPFitnessFunction(final IDataManager dataManager) {
        this.dataManager = dataManager;
    }

    protected void computeFitnessWithExecutions(final Individual sol,
                                                final List<ITestExecution> correctTestExecutions,
                                                final int computedCountStates, double computedCountFP) {
        if (!correctTestExecutions.isEmpty()) {
            final Tree tree = sol.getTree().asTree();
            final long[] testCorrectIdsFP = Bitmap.create(dataManager.getCorrectTestExecutions().size());
            // Copy precomputed values from sol.idsFPGOOD
            Bitmap.copyBuckets(sol.idsFPGOOD, testCorrectIdsFP, computedCountStates);
            // Need to count how many because we might have to remove some for equivalences
            int countNumberStates = computedCountStates;
            // Check correct test executions for FPs
            for (final ITestExecution testExecution : correctTestExecutions) {
                boolean compliant = dataManager.getAssertionEvaluator().eval(tree, testExecution.getVariables().getValues());
                if (!compliant) computedCountFP += 1.0;
                else Bitmap.set(testCorrectIdsFP, countNumberStates);
                ++countNumberStates;
            }
            // Save results
            sol.fitnessValueFP = countNumberStates == 0 ? 0.0 : computedCountFP / (double)countNumberStates;
            sol.idsFPGOOD = testCorrectIdsFP;
            sol.setLastComputedCorrectTestExecutionsSize(countNumberStates);
        }
    }

    @Override
    public void computeFitness(final Individual sol) {
        this.computeFitnessWithExecutions(sol, dataManager.getCorrectTestExecutions(), 0, 0.0);
    }

    @Override
    public void recomputeFitness(Individual sol) {
        final int lastComputedCorrectTestExecutionsSize = sol.getLastComputedCorrectTestExecutionsSize();
        this.computeFitnessWithExecutions(sol,
                dataManager.getCorrectTestExecutions()
                        .subList(lastComputedCorrectTestExecutionsSize, dataManager.getCorrectTestExecutions().size()),
                lastComputedCorrectTestExecutionsSize,
                (double) Math.round(sol.fitnessValueFP * (double)lastComputedCorrectTestExecutionsSize));
    }

}
