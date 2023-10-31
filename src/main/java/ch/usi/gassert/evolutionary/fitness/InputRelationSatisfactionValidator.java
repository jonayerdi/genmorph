package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.evolutionary.Individual;

import java.util.List;
import java.util.stream.Stream;

import static ch.usi.gassert.util.Assert.assertAlways;

/**
 * Function to determine if an Individual is a valid solution for full MR generation.
 */
public class InputRelationSatisfactionValidator implements IValidator {

    final TreeTemplate inputRelationTemplate;
    final List<ITestExecution> testExecutions;
    final IEvaluator evaluator;
    final double inputRelationSatisfactionThresholdMin;
    final double inputRelationSatisfactionThresholdMax;

    public InputRelationSatisfactionValidator(final TreeTemplate inputRelationTemplate, final List<ITestExecution> testExecutions,
                                              final IEvaluator evaluator,
                                              double inputRelationSatisfactionThresholdMin,
                                              double inputRelationSatisfactionThresholdMax) {
        assertAlways(inputRelationSatisfactionThresholdMin >= 0.0 && inputRelationSatisfactionThresholdMin <= 1.0,
                "inputRelationSatisfactionThresholdMin must be a value within [0,1], but is "
                        + inputRelationSatisfactionThresholdMax);
        assertAlways(inputRelationSatisfactionThresholdMax >= 0.0 && inputRelationSatisfactionThresholdMax <= 1.0,
                "inputRelationSatisfactionThresholdMax must be a value within [0,1], but is "
                        + inputRelationSatisfactionThresholdMax);
        assertAlways(inputRelationSatisfactionThresholdMin <= inputRelationSatisfactionThresholdMax,
                "Invalid inputRelationSatisfactionThresholds: ["
                        + inputRelationSatisfactionThresholdMin + ", " + inputRelationSatisfactionThresholdMax + "]");
        this.inputRelationTemplate = inputRelationTemplate;
        this.testExecutions = testExecutions;
        this.evaluator = evaluator;
        this.inputRelationSatisfactionThresholdMin = inputRelationSatisfactionThresholdMin;
        this.inputRelationSatisfactionThresholdMax = inputRelationSatisfactionThresholdMax;
    }

    protected boolean validateWithExecutions(Individual sol, Stream<ITestExecution> executions, long computedSatisfactionCount) {
        // The input relation must be satisfied for a number of TestExecutions
        // within the inputRelationSatisfactionThresholds
        final Tree inputRelation = sol.getTreeGroup().mappings.get(inputRelationTemplate).asTree();
        final long satisfactionCount = computedSatisfactionCount + executions
                .filter(e -> evaluator.eval(inputRelation, e.getVariables().getValues()))
                .count();
        // Store information for next revalidation
        sol.setLastValidatedTestExecutionsSize(testExecutions.size());
        sol.setLastValidatedSatisfactionCount(satisfactionCount);
        // Validate satisfactionCount
        return satisfactionCount >= Math.round((double) testExecutions.size() * inputRelationSatisfactionThresholdMin)
                && satisfactionCount <= Math.round((double) testExecutions.size() * inputRelationSatisfactionThresholdMax);
    }

    @Override
    public boolean validate(Individual sol) {
        return validateWithExecutions(sol, testExecutions.stream(), 0);
    }

    @Override
    public boolean revalidate(Individual sol, Boolean valid) {
        return validateWithExecutions(sol,
                testExecutions.stream().skip(sol.getLastValidatedTestExecutionsSize()),
                sol.getLastValidatedSatisfactionCount());
    }

}
