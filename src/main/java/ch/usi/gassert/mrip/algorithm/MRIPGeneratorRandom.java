package ch.usi.gassert.mrip.algorithm;

import ch.usi.gassert.data.state.IVariablesManager;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.mrip.MRIP;
import ch.usi.gassert.mrip.MRIPGenerator;
import ch.usi.gassert.mrip.MRIPGroup;
import ch.usi.gassert.util.LogUtils;

import java.util.Map;
import java.util.function.Supplier;

public class MRIPGeneratorRandom extends MRIPGenerator {

    public MRIPGeneratorRandom(int mripCount, double minCoveragePercent, double maxCoveragePercent,
                               int maxComplexity, int timeBudgetMinutes,
                               final BehaviourManager behaviourManager, final TreeTemplate treeTemplate,
                               final IVariablesManager variablesManager,
                               final IEvaluator evaluator, final Map<String, Map<String, Object>> testInputs) {
        super(mripCount, minCoveragePercent, maxCoveragePercent, maxComplexity, timeBudgetMinutes,
                behaviourManager, treeTemplate, variablesManager, evaluator, testInputs);
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
        if (timeBudgetExpired.get()) {
            throw new RuntimeException("Time budget expired while generating initial population");
        }
        LogUtils.log().info("Finished generating initial population");
        LogUtils.log().info("Current test case coverage: " + solution.coverage.size() + "/" + testInputs.size());
        int previousLoggedCoverage = solution.coverage.size();
        // Iterate until we run out of time budget
        while (!timeBudgetExpired.get()) {
            // Generate new candidate
            final MRIP newMRIP = generateMRIP(timeBudgetExpired);
            if (newMRIP != null && isGoodMrip(newMRIP)) {
                // Test replacing every MRIP in the current solution with the new one
                final MRIPGroup[] candidateSolutions = solution.replaceMRIP(newMRIP);
                for (int i = 0 ; i < solution.mrips.length ; ++i) {
                    if (isBetterSolution(candidateSolutions[i], solution, i)) {
                        solution = candidateSolutions[i];
                    }
                }
                // Log progress
                if (solution.coverage.size() != previousLoggedCoverage) {
                    previousLoggedCoverage = solution.coverage.size();
                    LogUtils.log().info("Current test case coverage: " + solution.coverage.size() + "/" + testInputs.size());
                }
            }
        }
        // Time budget expired
        LogUtils.log().info("Time budget expired, exiting...");
    }

}
