package ch.usi.gassert.data.manager.method;

import ch.usi.gassert.Config;
import ch.usi.gassert.Mode;
import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.state.VariablesManager;
import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeFactory;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.data.tree.builder.GeneratedTreeBuilder;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.crossover.ICrossover;
import ch.usi.gassert.evolutionary.crossover.SingleCrossover;
import ch.usi.gassert.evolutionary.crossover.SwappingCrossover;
import ch.usi.gassert.evolutionary.fitness.IValidator;
import ch.usi.gassert.evolutionary.fitness.InputRelationSatisfactionValidator;
import ch.usi.gassert.evolutionary.fitness.IsMetamorphicOutputRelationValidator;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.IRandomSelector;
import ch.usi.gassert.util.random.WeightedMap;
import org.mu.testcase.classification.TestClassifications;
import org.mu.testcase.metamorphic.MRInfo;
import org.mu.testcase.metamorphic.MRInfoDB;
import org.mu.util.streams.IStreamLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Instantiated by DataManagerFactory
@SuppressWarnings("unused")
public class MethodMetamorphicFullDataManager extends MethodMetamorphicDataManager {

    /**
     * args = new String[] {
     *                         "GASSERT",
     *                         "states/states.zip",
     *                         "states/classifications.zip",
     *                         ["states/MRInfoMandatory.csv"]
     *                 }
     */
    public MethodMetamorphicFullDataManager(DataManagerArgs dargs, final IStatesUpdater statesUpdater) {
        super(dargs, statesUpdater);
    }

    @Override
    public Mode getMode() {
        return Mode.METAMORPHIC_FULL;
    }

    @Override
    protected Pair<List<ITestExecution>, List<ITestExecution>>
    initCorrectIncorrectExecutions(DataManagerArgs dargs,
                                   Map<String, TestClassifications> classifications,
                                   IStreamLoader statesDataSource) {
        final List<MRInfo> mrinfosMandatory;
        if (dargs.args.length > 3) {
            final String mrinfosMandatoryFile = dargs.args[3];
            try (final BufferedReader reader = new BufferedReader(new FileReader(mrinfosMandatoryFile))) {
                mrinfosMandatory = MRInfoDB.fromCsv(reader).getMRInfos();
            } catch (Exception e) {
                throw new RuntimeException("Error loading " + mrinfosMandatoryFile, e);
            }
        } else {
            mrinfosMandatory = new ArrayList<>();
        }
        return initCorrectIncorrectExecutionsAllCombinations(dargs, mrinfosMandatory, classifications, statesDataSource);
    }

    @Override
    protected TreeTemplate initTreeTemplate(final String[] args) {
        // Init VariablesManager with only inputs for the input relation
        final VariablesManager inputsVariablesManager = VariablesManager.fromVariableTypes(
                evaluationVariablesManager.getVariableTypes().entrySet().stream()
                        .filter(v -> inputs.contains(v.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        // Input relation IMPLIES output relation
        return new TreeTemplate("=>",
                new TreeTemplate(
                        new GeneratedTreeBuilder(
                                initTreeBehaviourManager(Config.MAX_DEPTH_IR_TREE), new TreeFactory(inputsVariablesManager), Tree.Type.BOOLEAN
                        ),
                        Tree.Type.BOOLEAN, TreeTemplate.Mode.GENERATED
                ),
                new TreeTemplate(
                        new GeneratedTreeBuilder(
                                initTreeBehaviourManager(Config.MAX_DEPTH_OR_TREE), new TreeFactory(evaluationVariablesManager), Tree.Type.BOOLEAN
                        ),
                        Tree.Type.BOOLEAN, TreeTemplate.Mode.GENERATED
                ),
                Tree.Type.BOOLEAN, TreeTemplate.Mode.STATIC
        );
    }

    @Override
    protected BehaviourManager initBehaviourManager() {
        BehaviourManager behaviourManager = super.initBehaviourManager();

        // Set validator
        TreeTemplate irTemplate = treeTemplate.getLeft();
        TreeTemplate orTemplate = treeTemplate.getRight();
        IValidator irSatisfaction = new InputRelationSatisfactionValidator(irTemplate,
                correctTestExecutions, assertionEvaluator,
                Config.MR_INPUT_RELATION_SATISFACTION_THRESHOLD_MIN,
                Config.MR_INPUT_RELATION_SATISFACTION_THRESHOLD_MAX);
        IValidator irComplexity =
                sol -> sol.getTreeGroup().mappings.get(irTemplate).getNumberOfNodes() <= Config.MAX_IR_COMPLEXITY;
        IValidator orComplexity =
                sol -> sol.getTreeGroup().mappings.get(orTemplate).getNumberOfNodes() <= Config.MAX_OR_COMPLEXITY;
        IValidator orIsMetamorphic = new IsMetamorphicOutputRelationValidator(orTemplate);
        behaviourManager.setValidator(sol -> irComplexity.validate(sol) && orComplexity.validate(sol) && irSatisfaction.validate(sol));
        behaviourManager.setEliteValidator(orIsMetamorphic);

        // Add SwappingCrossover
        IRandomSelector<ICrossover> crossovers = new WeightedMap<ICrossover>()
                .add(50, new SingleCrossover(behaviourManager))
                .add(50, new SwappingCrossover(behaviourManager));
        behaviourManager.setCrossover(crossovers);

        return behaviourManager;
    }

    @Override
    public ITree getInputRelation(final TreeGroup tg) {
        // Left side of the root is Input Relation
        return tg.mappings.get(treeTemplate.getLeft());
    }

    @Override
    public ITree getOutputRelation(final TreeGroup tg) {
        // Right side of the root is Output Relation
        return tg.mappings.get(treeTemplate.getRight());
    }

}
