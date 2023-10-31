package ch.usi.gassert.data.manager.method;

import ch.usi.gassert.Config;
import ch.usi.gassert.Tool;
import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.manager.IDataManager;
import ch.usi.gassert.data.manager.LoaderUtils;
import ch.usi.gassert.data.state.*;
import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.data.state.updater.NullStatesUpdater;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.selector.*;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.data.tree.builder.GeneratedTreeBuilder;
import ch.usi.gassert.evaluator.BasicEvaluator;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.evolutionary.TreeBehaviourManager;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.crossover.ICrossover;
import ch.usi.gassert.evolutionary.crossover.SingleCrossover;
import ch.usi.gassert.evolutionary.crossover.tree.ITreeCrossover;
import ch.usi.gassert.evolutionary.crossover.tree.MergingTreeCrossover;
import ch.usi.gassert.evolutionary.crossover.tree.RandomTreeCrossover;
import ch.usi.gassert.evolutionary.fitness.ComplexityValidator;
import ch.usi.gassert.evolutionary.fitness.NullValidator;
import ch.usi.gassert.evolutionary.fitness.NumberFNFitnessFunction;
import ch.usi.gassert.evolutionary.fitness.NumberFPFitnessFunction;
import ch.usi.gassert.evolutionary.mutation.SingleMutation;
import ch.usi.gassert.evolutionary.mutation.tree.ConstantValueMutation;
import ch.usi.gassert.evolutionary.mutation.IMutation;
import ch.usi.gassert.evolutionary.mutation.tree.ITreeMutation;
import ch.usi.gassert.evolutionary.mutation.tree.SingleNodeTreeMutation;
import ch.usi.gassert.evolutionary.mutation.tree.SubTreeMutation;
import ch.usi.gassert.evolutionary.selection.BestMatchSelection;
import ch.usi.gassert.evolutionary.selection.ISelection;
import ch.usi.gassert.evolutionary.selection.RandomSelection;
import ch.usi.gassert.evolutionary.selection.TournamentSelection;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.DynamicWeightedMap;
import ch.usi.gassert.util.random.IRandomSelector;
import ch.usi.gassert.util.random.WeightedMap;
import com.google.gson.stream.JsonReader;
import org.mu.testcase.classification.TestClassifications;
import org.mu.util.streams.IStreamLoader;
import org.mu.util.streams.StreamLoaderFactory;

import java.io.BufferedReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.usi.gassert.util.FileUtils.SEPARATORS;

public abstract class MethodDataManager implements IDataManager {

    /**
     * Data manager args
     */
    public final DataManagerArgs dargs;

    /**
     * Selected configuration for the EvolutionaryAlgorithm
     */
    public final Tool tool;

    /**
     * Template for the generated expressions
     */
    protected TreeTemplate treeTemplate;

    /**
     * Evaluator for the generated expressions
     */
    protected IEvaluator assertionEvaluator;

    /**
     * Lists of system and test case IDs
     */
    protected final List<String> systemIds;
    protected final List<String> testIds;

    /**
     * Variables
     */
    protected final Set<String> inputs;
    protected final Set<String> outputs;
    protected final IVariablesManager evaluationVariablesManager;

    /**
     * Correct and Incorrect execution data
     */
    protected final List<ITestExecution> correctTestExecutions;
    protected final List<ITestExecution> incorrectTestExecutions;

    /**
     * StatesUpdater
     */
    protected final IStatesUpdater statesUpdater;

    /**
     * BehaviourManager single instance
     */
    protected final BehaviourManager behaviourManager;

    public static String getTestExecutionEntry(final String systemId, final String testId) {
        return systemId + SEPARATORS[0] + testId + ".state.json";
    }

    public static TestExecution loadTestExecution(final BufferedReader bufReader) {
        try (final JsonReader reader = new JsonReader(bufReader)) {
            final TestExecution testExecution = TestExecution.fromJson(reader);
            final boolean hasInvalidValue = testExecution.getVariables().getValues().values().stream()
                    .anyMatch(v -> v == null || ClassUtils.isErrorType(v.getClass()));
            if (hasInvalidValue) {
                throw new RuntimeException("TestExecution has an invalid value!");
            }
            return testExecution;
        } catch (Exception e) {
            throw new RuntimeException("Error loading TestExecution");
        }
    }

    public static Map<String, Map<String, TestExecution>> loadAllTestExecutions(final IStreamLoader statesDataSource) {
        // Load inputs
        final Map<String, Map<String, TestExecution>> testExecutions = new HashMap<>();
        for (String entry : statesDataSource.entries()) {
            if (entry.endsWith(".state.json")) {
                try (final JsonReader reader = new JsonReader(statesDataSource.load(entry))) {
                    final TestExecution testExecution = TestExecution.fromJson(reader);
                    final boolean hasInvalidValue = testExecution.getVariables().getValues().values().stream()
                            .anyMatch(v -> v == null || ClassUtils.isErrorType(v.getClass()));

                    if (hasInvalidValue) {
                        throw new RuntimeException(entry + " has an invalid value!");
                    }
                    testExecutions.putIfAbsent(testExecution.getSystemId(), new HashMap<>());
                    final TestExecution previous = testExecutions.get(testExecution.getSystemId())
                            .put(testExecution.getTestId(), testExecution);
                    if (previous != null) {
                        throw new RuntimeException("Duplicate TestExecution for systemId=" + testExecution.getSystemId()
                                + " testId=" + testExecution.getTestId());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error loading inputs entry: " + entry, e);
                }
            }
        }
        return testExecutions;
    }

    public MethodDataManager(final DataManagerArgs dargs, final IStatesUpdater statesUpdater) {
        // Parse args
        this.dargs = dargs;
        final String[] args = dargs.args;
        tool = Tool.fromString(args[0]);
        // StatesUpdater
        this.statesUpdater = statesUpdater;
        this.statesUpdater.setDataManager(this);
        // Load test executions + classifications
        final IStreamLoader classificationsDataSource = StreamLoaderFactory.forPath(args[2]);
        final Map<String, TestClassifications> classifications = LoaderUtils.loadClassifications(classificationsDataSource);
        classificationsDataSource.close();
        // Init correct/incorrect executions
        final IStreamLoader statesDataSource = StreamLoaderFactory.forPath(args[1]);
        final Pair<List<ITestExecution>, List<ITestExecution>> correctIncorrectExecutions
                = initCorrectIncorrectExecutions(dargs, classifications, statesDataSource);
        statesDataSource.close();
        correctTestExecutions = correctIncorrectExecutions.fst;
        incorrectTestExecutions = correctIncorrectExecutions.snd;
        // Init systemIds and testIds
        systemIds = classifications.keySet().stream().sorted().collect(Collectors.toList());
        testIds = Stream.concat(correctTestExecutions.stream(), incorrectTestExecutions.stream())
                .reduce(new HashSet<String>(), (acc, e) -> {
                    acc.add(e.getTestId());
                    return acc;
                }, (s1, s2) -> {
                    s1.addAll(s2);
                    return s1;
                })
                .stream()
                .sorted()
                .collect(Collectors.toList());
        // Init assertion evaluator
        assertionEvaluator = new BasicEvaluator();
        // Init variable types + inputs/outputs
        ITestExecution testExecution = correctTestExecutions.stream().findAny()
                .orElse(incorrectTestExecutions.stream().findAny().orElse(null));
        if (testExecution != null) {
            final Variables variables = testExecution.getVariables();
            inputs = variables.getInputs();
            outputs = variables.getOutputs();
            evaluationVariablesManager = VariablesManager.fromVariableValues(variables.getValues());
        } else {
            inputs = new HashSet<>();
            outputs = new HashSet<>();
            evaluationVariablesManager = new VariablesManager(new HashMap<>(0));
        }
        // Init Tree template
        treeTemplate = initTreeTemplate(args);
        treeTemplate.memoizeGeneratedNodes();
        // Init BehaviourManager
        behaviourManager = initBehaviourManager();
    }

    protected abstract Pair<List<ITestExecution>, List<ITestExecution>>
    initCorrectIncorrectExecutions(final DataManagerArgs dargs,
                                   final Map<String, TestClassifications> classifications,
                                   final IStreamLoader statesDataSource);

    protected TreeTemplate initTreeTemplate(final String[] args) {
        return new TreeTemplate(
                new GeneratedTreeBuilder(
                        initTreeBehaviourManager(Config.MAX_DEPTH_TREE), new TreeFactory(evaluationVariablesManager), Tree.Type.BOOLEAN
                ),
                Tree.Type.BOOLEAN, TreeTemplate.Mode.GENERATED
        );
    }

    @Override
    public DataManagerArgs getArgs() {
        return dargs;
    }

    @Override
    public TreeTemplate getTreeTemplate() {
        return treeTemplate;
    }

    @Override
    public void setTreeTemplate(TreeTemplate treeTemplate) {
        this.treeTemplate = treeTemplate;
    }

    @Override
    public IEvaluator getAssertionEvaluator() {
        return assertionEvaluator;
    }

    @Override
    public void setAssertionEvaluator(IEvaluator assertionEvaluator) {
        this.assertionEvaluator = assertionEvaluator;
    }

    @Override
    public List<String> getSystemIds() {
        return systemIds;
    }

    @Override
    public List<String> getTestIds() {
        return testIds;
    }

    @Override
    public List<ITestExecution> getCorrectTestExecutions() {
        return correctTestExecutions;
    }

    @Override
    public List<ITestExecution> getIncorrectTestExecutions() {
        return incorrectTestExecutions;
    }

    @Override
    public IStatesUpdater getStatesUpdater() {
        return this.statesUpdater;
    }

    @Override
    public void addCorrectStates(final List<ITestExecution> correctTestExecutions) {
        this.correctTestExecutions.addAll(correctTestExecutions);
    }

    @Override
    public Set<String> getInputs() {
        return inputs;
    }

    @Override
    public Set<String> getOutputs() {
        return outputs;
    }

    @Override
    public IVariablesManager getEvaluationVariablesManager() {
        return evaluationVariablesManager;
    }

    @Override
    public BehaviourManager getBehaviourManager() {
        return this.behaviourManager;
    }

    protected BehaviourManager initBehaviourManager() {
        BehaviourManager behaviourManager = new BehaviourManager();

        behaviourManager.setValidator(new ComplexityValidator(Config.MAX_COMPLEXITY));
        behaviourManager.setEliteValidator(NullValidator.INSTANCE);
        behaviourManager.setFitnessFP(new NumberFPFitnessFunction(this));
        behaviourManager.setFitnessFN(new NumberFNFitnessFunction(this));

        IRandomSelector<ISelection> selection = null;

        if (tool.equals(Tool.GASSERT)) {
            selection = new WeightedMap<ISelection>()
                    .add(0, new RandomSelection())
                    .add(50, new TournamentSelection())
                    .add(50, new BestMatchSelection());
        } else if (tool.equals(Tool.NAIVE_SEARCH_BASED) || tool.equals(Tool.RANDOM)) {
            if (tool.equals(Tool.RANDOM)) {
                selection = new WeightedMap<ISelection>()
                        .add(100, new RandomSelection())
                        .add(0, new TournamentSelection())
                        .add(0, new BestMatchSelection());
                behaviourManager.IS_ELITISM_ENABLED = false;
                behaviourManager.IS_MIGRATION_ENABLED = false;
            } else {
                selection = new WeightedMap<ISelection>()
                        .add(0, new RandomSelection())
                        .add(100, new TournamentSelection())
                        .add(0, new BestMatchSelection());
            }
        }

        IRandomSelector<ITreeTemplateSelector> treeTemplateSelector = new WeightedMap<ITreeTemplateSelector>()
                .add(100, new RandomTreeTemplateSelector());
        IRandomSelector<ICrossover> crossovers = new WeightedMap<ICrossover>()
                .add(100, new SingleCrossover(behaviourManager));
        IRandomSelector<IMutation> mutations = new WeightedMap<IMutation>()
                .add(100, new SingleMutation(behaviourManager));

        behaviourManager.setSelection(selection);
        behaviourManager.setTreeTemplateSelector(treeTemplateSelector);
        behaviourManager.setCrossover(crossovers);
        behaviourManager.setMutation(mutations);

        return behaviourManager;
    }

    protected TreeBehaviourManager initTreeBehaviourManager(final int treeComplexity) {
        TreeBehaviourManager treeBehaviourManager = new TreeBehaviourManager(treeComplexity);

        IRandomSelector<ITreeSelector> treeSelector = new WeightedMap<ITreeSelector>()
                .add(100, new RandomTreeSelector())
                .add(0, new PopularityBasedTreeSelector())
                .add(0, new MutStateDiffTreeSelector());
        IRandomSelector<ITreeMutation> mutation = new DynamicWeightedMap<ITreeMutation>()
                .add(50, new SingleNodeTreeMutation(treeBehaviourManager))
                .add(50, new SubTreeMutation(treeBehaviourManager))
                .add(treeBehaviourManager.ConstantValueMutationWeight, new ConstantValueMutation(treeBehaviourManager, 0.1, true))
                .update();
        IRandomSelector<ITreeCrossover>  crossover = new WeightedMap<ITreeCrossover>()
                .add(0, new MergingTreeCrossover(treeBehaviourManager))
                .add(100, new RandomTreeCrossover(treeBehaviourManager));

        treeBehaviourManager.setTreeSelector(treeSelector);
        treeBehaviourManager.setCrossover(crossover);
        treeBehaviourManager.setMutation(mutation);

        return treeBehaviourManager;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "_" + tool.toString();
    }

}
