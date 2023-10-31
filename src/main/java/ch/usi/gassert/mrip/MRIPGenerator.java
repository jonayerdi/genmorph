package ch.usi.gassert.mrip;

import ch.usi.gassert.Config;
import ch.usi.gassert.Mode;
import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.manager.DataManagerFactory;
import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.state.IVariablesManager;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.builder.GeneratedTreeBuilder;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evaluator.BasicEvaluator;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.mrip.algorithm.MRIPGeneratorHillClimbing;
import ch.usi.gassert.mrip.algorithm.MRIPGeneratorRandom;
import ch.usi.gassert.util.LogUtils;
import ch.usi.gassert.util.Utils;
import org.mu.testcase.metamorphic.MRInfo;
import org.mu.testcase.metamorphic.MRInfoDB;
import org.mu.util.Pair;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ch.usi.gassert.util.Assert.assertAlways;

public abstract class MRIPGenerator implements Runnable {

    // Config
    public final int mripCount;
    public final int minCoverage;
    public final int maxCoverage;
    public final int maxComplexity;
    public final int timeBudgetMinutes;
    // Dataset
    public final BehaviourManager behaviourManager;
    public final TreeTemplate treeTemplate;
    public final IVariablesManager variablesManager;
    public final IEvaluator evaluator;
    public final Map<String, Map<String, Object>> testInputs;
    // Results
    protected MRIPGroup solution;

    public MRIPGenerator(int mripCount, double minCoveragePercent, double maxCoveragePercent,
                         int maxComplexity, int timeBudgetMinutes,
                         final BehaviourManager behaviourManager, final TreeTemplate treeTemplate,
                         final IVariablesManager variablesManager,
                         final IEvaluator evaluator, final Map<String, Map<String, Object>> testInputs) {
        // Config
        this.mripCount = mripCount;
        this.minCoverage = Math.min(Math.max((int)Math.round(minCoveragePercent * (double)testInputs.size()), 1), testInputs.size());
        this.maxCoverage = Math.min(Math.max((int)Math.round(maxCoveragePercent * (double)testInputs.size()), this.minCoverage), testInputs.size());
        this.maxComplexity = maxComplexity;
        this.timeBudgetMinutes = timeBudgetMinutes;
        // Dataset
        this.behaviourManager = behaviourManager;
        this.treeTemplate = treeTemplate;
        this.variablesManager = variablesManager;
        this.evaluator = evaluator;
        this.testInputs = testInputs;
        // Results
        this.solution = null;
    }

    public MRIPGroup getSolution() {
        return this.solution;
    }

    public TreeGroup generateNewTree() {
            return new TreeGroup(treeTemplate);
    }

    public MRIPGroup generateRandomSolution(Supplier<Boolean> stopCondition) {
        final MRIP[] mrips = new MRIP[mripCount];
        for (int i = 0 ; i < mripCount ; ++i) {
            mrips[i] = Utils.repeatUntil(this::generateMRIP, mrip -> isGoodMrip(mrip) || stopCondition.get());
        }
        return new MRIPGroup(mrips);
    }

    public MRIP generateMRIP() {
        return new MRIP(
                Utils.repeatUntil(this::generateNewTree, this::isGoodTree),
                evaluator, testInputs, maxCoverage
        );
    }

    public MRIP generateMRIP(Supplier<Boolean> stopCondition) {
        try {
            return new MRIP(
                    Utils.repeatUntil(this::generateNewTree, tree -> isGoodTree(tree) || stopCondition.get()),
                    evaluator, testInputs, maxCoverage
            );
        } catch (NullPointerException ignored) {
            // Stop condition reached while generating Tree
            return null;
        }
    }

    public boolean isGoodTree(final TreeGroup treeGroup) {
        return treeGroup.getTotalNumberOfNodes() <= maxComplexity;
    }

    public boolean isGoodMrip(final MRIP mrip) {
        return mrip.coveredTestCases.size() >= minCoverage && mrip.coveredTestCases.size() <= maxCoverage;
    }

    public static boolean isBetterMRIP(final MRIP newMRIP, final MRIP oldMRIP) {
        final int coverageDiff = newMRIP.coveredTestCases.size() - oldMRIP.coveredTestCases.size();
        if (coverageDiff != 0) {
            // First, favor candidates with a better test set coverage
            return coverageDiff > 0;
        } else if (newMRIP.selectedPairs.size() != oldMRIP.selectedPairs.size()) {
            // In case of identical test set coverage, minimize the number of test pairs (to minimize redundant pairings)
            return newMRIP.selectedPairs.size() < oldMRIP.selectedPairs.size();
        } else {
            // Finally, favor MRIPs that have a lower complexity in their expression
            return newMRIP.tree.getNumberOfNodes() < oldMRIP.tree.getNumberOfNodes();
        }
    }

    public static boolean isBetterSolution(final MRIP newMRIP, final MRIP oldMRIP, final int newCoverage, final int oldCoverage) {
        // First, ensure global fitness (solution coverage) improves or stays the same
        // If new candidate does not change the global fitness, check the fitness for each MRIP
        return newCoverage > oldCoverage
                || (newCoverage == oldCoverage
                && isBetterMRIP(newMRIP, oldMRIP));
    }

    public static boolean isBetterSolution(final MRIPGroup newSolution, final MRIPGroup oldSolution, int newMRIPIndex) {
        return isBetterSolution(newSolution.mrips[newMRIPIndex], oldSolution.mrips[newMRIPIndex],
                newSolution.coverage.size(), oldSolution.coverage.size());
    }

    public static void main(String[] args) {
        // Parse args
        if (args.length != 10) {
            System.err.println("Wrong number of parameters: 10 arguments expected, got " + args.length);
            System.err.println("Manager class");
            System.err.println("Manager args");
            System.err.println("Algorithm (random|hillclimbing)");
            System.err.println("MRIP count");
            System.err.println("Min test case coverage percent per MRIP");
            System.err.println("Max test case coverage percent per MRIP");
            System.err.println("Random seed (integer)");
            System.err.println("Time budget in minutes");
            System.err.println("Output MRIPs file");
            System.err.println("Output MRInfos file");
            System.exit(-1);
        }
        Iterator<String> arguments = Arrays.stream(args).sequential().iterator();
        final String managerClass = arguments.next();
        final String[] managerArgs = arguments.next().split(";");
        final String algorithm = arguments.next();
        final int mripCount = Integer.parseInt(arguments.next());
        assertAlways(mripCount > 0, "MRIP count must be 1 or more");
        final double minCoveragePercent = Double.parseDouble(arguments.next());
        assertAlways(minCoveragePercent > 0.0 && minCoveragePercent <= 1.0, "Min MRIP coverage must be a decimal in the range (0.0, 1.0]");
        final double maxCoveragePercent = Double.parseDouble(arguments.next());
        assertAlways(maxCoveragePercent > 0.0 && maxCoveragePercent <= 1.0, "Max MRIP coverage must be a decimal in the range (0.0, 1.0]");
        final int randomSeed = Integer.parseInt(arguments.next());
        final int timeBudgetMinutes = Integer.parseInt(arguments.next());
        final String outputMRIPs = arguments.next();
        final String outputMRInfo = arguments.next();
        // Load dataset
        Config.seed = randomSeed;
        final int sampledCorrectStates = Config.DATASET_CORRECT_STATES_SAMPLED(timeBudgetMinutes);
        final int sampledIncorrectStates = Config.DATASET_INCORRECT_STATES_SAMPLED(timeBudgetMinutes);
        final DataManagerArgs dataManagerArgs = new DataManagerArgs(sampledCorrectStates, sampledIncorrectStates, managerArgs);
        final IDataManager dataManager = DataManagerFactory.load(managerClass, dataManagerArgs);
        assertAlways(dataManager.getMode() == Mode.REGULAR_ASSERTIONS, "Manager class must operate in REGULAR_ASSERTIONS mode");
        // Init VariablesManager, BehaviourManager, and Evaluator
        final IVariablesManager variablesManager = dataManager
                .getEvaluationVariablesManager()
                .filterVariables((name, clazz) -> dataManager.getInputs().contains(name))
                .makeMetamorphic();
        assertAlways(!(variablesManager.getNumericVars().isEmpty() && variablesManager.getBooleanVars().isEmpty()),
                "No variables to use for MRIP generation");
        final BehaviourManager behaviourManager = dataManager.getBehaviourManager();
        LogUtils.log().info("Using variables: " + variablesManager.getVariableTypes().keySet());
        final IEvaluator evaluator = new BasicEvaluator();
        // Get TreeTemplate and modify its TreeFactory to use only input variables + the max Tree depth and complexity
        final int maxComplexity = Config.MAX_IR_COMPLEXITY;
        final TreeTemplate treeTemplate = dataManager.getTreeTemplate();
        final GeneratedTreeBuilder treeBuilder = (GeneratedTreeBuilder) treeTemplate.getTreeBuilder();
        treeBuilder.treeBehaviourManager.setMaxTreeDepth(Config.MAX_DEPTH_IR_TREE);
        treeBuilder.treeFactory = new TreeFactory(variablesManager);
        // Init test inputs
        final List<ITestExecution> executions = dataManager.getCorrectTestExecutions();
        final Set<String> inputVars = dataManager.getInputs();
        final Map<String, Map<String, Object>> testInputs  = new HashMap<>(executions.size());
        for (final ITestExecution execution : executions) {
            testInputs.put(
                    execution.getTestId(),
                    execution.getVariables().getValues().entrySet().stream()
                            .filter(v -> inputVars.contains(v.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }
        LogUtils.log().info("Loaded " + executions.size() + " test executions");
        // Generate MRIPs
        MRIPGenerator generator;
        switch (algorithm.toLowerCase()) {
            case "random":
                generator = new MRIPGeneratorRandom(mripCount, minCoveragePercent, maxCoveragePercent,
                        maxComplexity, timeBudgetMinutes,
                        behaviourManager, treeTemplate, variablesManager,
                        evaluator, testInputs);
                break;
            case "hillclimbing":
                generator = new MRIPGeneratorHillClimbing(mripCount, minCoveragePercent, maxCoveragePercent,
                        maxComplexity, timeBudgetMinutes,
                        behaviourManager, treeTemplate, variablesManager,
                        evaluator, testInputs,
                        32);
                break;
            default:
                throw new RuntimeException("Unsupported algorithm: " + algorithm);
        }
        LogUtils.log().info("Coverage threshold: [" + generator.minCoverage + ", " + generator.maxCoverage + "]");
        generator.run();
        final MRIPGroup solution = generator.getSolution();
        // Output results
        List<MRInfo> mrinfos = new ArrayList<>();
        Writer outMrinfos;
        PrintStream outMrips;
        try {
            new File(outputMRIPs).getParentFile().mkdirs();
            outMrips = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputMRIPs)));
        } catch (Exception e) {
            throw new RuntimeException("Error opening output MRIPs file", e);
        }
        LogUtils.log().info("Writing MRIPs to: " + outputMRIPs);
        try {
            new File(outputMRInfo).getParentFile().mkdirs();
            outMrinfos = new BufferedWriter(new FileWriter(outputMRInfo));
        } catch (Exception e) {
            throw new RuntimeException("Error opening output MRInfos file", e);
        }
        LogUtils.log().info("Writing MRInfos to: " + outputMRInfo);
        int i = 0;
        for (final MRIP mrip : solution.mrips) {
            final String mripName = "MRIP" + (i++);
            outMrips.println(mripName);
            outMrips.println(mrip.tree.toString());
            for (final Pair<String, String> pair : mrip.selectedPairs) {
                mrinfos.add(new MRInfo(mripName, pair.a, pair.b));
            }
        }
        outMrips.close();
        final MRInfoDB mrinfodb = new MRInfoDB(mrinfos);
        try {
            mrinfodb.toCsv(outMrinfos);
        } catch (Exception e) {
            throw new RuntimeException("Error writing to MRInfos file", e);
        } finally {
            try {
                outMrinfos.close();
            } catch (Exception ignored) {}
        }
    }

}
