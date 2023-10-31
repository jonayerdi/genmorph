package ch.usi.gassert.mrip.algorithm;

import ch.usi.gassert.data.state.IVariablesManager;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.mrip.MRIP;
import ch.usi.gassert.mrip.MRIPGenerator;
import ch.usi.gassert.mrip.MRIPGroup;
import ch.usi.gassert.util.LogUtils;
import ch.usi.gassert.util.Utils;

import java.util.Map;
import java.util.function.Supplier;

import static ch.usi.gassert.util.Assert.assertAlways;

public class MRIPGeneratorHillClimbing extends MRIPGenerator {

    final int plateauSize;

    public MRIPGeneratorHillClimbing(int mripCount, double minCoveragePercent, double maxCoveragePercent,
                                     int maxComplexity, int timeBudgetMinutes,
                                     final BehaviourManager behaviourManager, final TreeTemplate treeTemplate,
                                     final IVariablesManager variablesManager,
                                     final IEvaluator evaluator, final Map<String, Map<String, Object>> testInputs,
                                     int plateauSize) {
        super(mripCount, minCoveragePercent, maxCoveragePercent, maxComplexity, timeBudgetMinutes,
                behaviourManager, treeTemplate, variablesManager, evaluator, testInputs);
        assertAlways(plateauSize > 0, "Plateau size must be larger than 0");
        this.plateauSize = plateauSize;
    }

    public void run() {
        LogUtils.log().info("MRIPGenerator started with a time budget of " + timeBudgetMinutes + " minutes");
        final long startTime = System.currentTimeMillis();
        // Utility functions
        final Supplier<Long> elapsedMinutes = () -> (System.currentTimeMillis() - startTime) / 60000L;
        final Supplier<Boolean> timeBudgetExpired = () -> elapsedMinutes.get() >= timeBudgetMinutes;
        // Generate initial set of MRIPs
        LogUtils.log().info("Generating initial MRIP population...");
        solution = generateRandomSolution(timeBudgetExpired);
        // Current candidate MRIP and plateau
        MRIP candidate = generateMRIP(timeBudgetExpired);
        int currentPlateau = 0;
        if (timeBudgetExpired.get()) {
            throw new RuntimeException("Time budget expired while generating initial population");
        }
        LogUtils.log().info("Finished generating initial population");
        LogUtils.log().info("Current test case coverage: " + solution.coverage.size() + "/" + testInputs.size());
        int previousLoggedCoverage = solution.coverage.size();
        // Iterate until we run out of time budget
        while (!timeBudgetExpired.get()) {
            // Check candidate
            if (isGoodMrip(candidate)) {
                // Test replacing every MRIP in the current solution with the new one
                final MRIPGroup[] candidateSolutions = solution.replaceMRIP(candidate);
                for (int i = 0 ; i < solution.mrips.length ; ++i) {
                    if (isBetterSolution(candidateSolutions[i], solution, i)) {
                        // Set new best solution + reset currentPlateau
                        solution = candidateSolutions[i];
                        currentPlateau = 0;
                    }
                }
            } else {
                // No improvement, increase currentPlateau
                ++currentPlateau;
            }
            if (currentPlateau < plateauSize) {
                // Mutate candidate while checking exit condition
                final TreeGroup treeGroup = candidate.treeGroup;
                final TreeGroup newTree = Utils.repeatUntil(
                        () -> behaviourManager.getMutation().mutate(treeGroup, null),
                        tree -> isGoodTree(tree) || timeBudgetExpired.get()
                );
                if (timeBudgetExpired.get()) {
                    break;
                } else {
                    candidate = new MRIP(newTree, evaluator, testInputs, maxCoverage);
                }
            } else {
                // Plateau: Generate new candidate from scratch and reset plateau
                candidate = generateMRIP(timeBudgetExpired);
                currentPlateau = 0;
                if (timeBudgetExpired.get()) {
                    break;
                }
            }
            // Log progress
            if (solution.coverage.size() != previousLoggedCoverage) {
                previousLoggedCoverage = solution.coverage.size();
                LogUtils.log().info("Current test case coverage: " + solution.coverage.size() + "/" + testInputs.size());
            }
        }
        // Time budget expired
        LogUtils.log().info("Time budget expired, exiting...");
    }

}
