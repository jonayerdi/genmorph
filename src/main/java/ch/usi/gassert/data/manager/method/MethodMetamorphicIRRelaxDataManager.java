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
import ch.usi.gassert.data.tree.builder.ConjunctiveClausesTreeBuilder;
import ch.usi.gassert.data.tree.builder.GeneratedTreeBuilder;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.ConjunctiveClausesBehaviourManager;
import ch.usi.gassert.evolutionary.crossover.ICrossover;
import ch.usi.gassert.evolutionary.crossover.SingleCrossover;
import ch.usi.gassert.evolutionary.crossover.SwappingCrossover;
import ch.usi.gassert.evolutionary.crossover.conjunctive.IConjunctiveCrossover;
import ch.usi.gassert.evolutionary.crossover.conjunctive.SinglePointConjunctiveCrossover;
import ch.usi.gassert.evolutionary.fitness.ConjunctiveClauseSelectedValidator;
import ch.usi.gassert.evolutionary.fitness.IValidator;
import ch.usi.gassert.evolutionary.fitness.InputRelationSatisfactionValidator;
import ch.usi.gassert.evolutionary.fitness.IsMetamorphicOutputRelationValidator;
import ch.usi.gassert.evolutionary.mutation.conjunctive.ClauseSelectionConjunctiveMutation;
import ch.usi.gassert.evolutionary.mutation.conjunctive.IConjunctiveMutation;
import ch.usi.gassert.evolutionary.mutation.conjunctive.RelaxOperatorConjunctiveMutation;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.IRandomSelector;
import ch.usi.gassert.util.random.WeightedMap;
import ch.usi.methodtest.ConjunctiveMRIP;
import org.mu.testcase.classification.TestClassifications;
import org.mu.testcase.metamorphic.MRInfo;
import org.mu.testcase.metamorphic.MRInfoDB;
import org.mu.util.streams.IStreamLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Instantiated by DataManagerFactory
@SuppressWarnings("unused")
public class MethodMetamorphicIRRelaxDataManager extends MethodMetamorphicDataManager {

    /**
     * args = new String[] {
     *                         "GASSERT",
     *                         "states/states.zip",
     *                         "states/classifications.zip",
     *                         "MRIP1.cmrip",
     *                         ["states/MRInfoMandatory.csv"]
     *                 }
     */
    public MethodMetamorphicIRRelaxDataManager(DataManagerArgs dargs, final IStatesUpdater statesUpdater) {
        super(dargs, statesUpdater);
    }

    @Override
    public Mode getMode() {
        return Mode.METAMORPHIC_INPUT_RELATION_RELAX;
    }

    @Override
    protected Pair<List<ITestExecution>, List<ITestExecution>>
    initCorrectIncorrectExecutions(DataManagerArgs dargs,
                                   Map<String, TestClassifications> classifications,
                                   IStreamLoader statesDataSource) {
        final List<MRInfo> mrinfosMandatory;
        if (dargs.args.length > 4) {
            final String mrinfosMandatoryFile = dargs.args[4];
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
        // Init original MRIP
        final ConjunctiveMRIP mrip;
        try (BufferedReader reader = new BufferedReader(new FileReader(args[3]))) {
            mrip = ConjunctiveMRIP.read(reader, evaluationVariablesManager.getVariableTypes());
        } catch (IOException e) {
            throw new RuntimeException("Error loading MRIP from " + args[3], e);
        }
        // Init VariablesManager with only inputs for the input relation
        final VariablesManager inputsVariablesManager = VariablesManager.fromVariableTypes(
                evaluationVariablesManager.getVariableTypes().entrySet().stream()
                        .filter(v -> inputs.contains(v.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        // Input relation IMPLIES output relation
        return new TreeTemplate("=>",
                new TreeTemplate(
                        new ConjunctiveClausesTreeBuilder(initConjunctiveBehaviourManager(), mrip.clauses),
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
                0.0,
                Config.MR_INPUT_RELATION_SATISFACTION_THRESHOLD_MAX);
        IValidator irComplexity = new ConjunctiveClauseSelectedValidator(irTemplate);
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

    protected ConjunctiveClausesBehaviourManager initConjunctiveBehaviourManager() {
        ConjunctiveClausesBehaviourManager conjunctiveBehaviourManager = new ConjunctiveClausesBehaviourManager();

        IRandomSelector<IConjunctiveCrossover> crossover = new WeightedMap<IConjunctiveCrossover>()
                .add(100, new SinglePointConjunctiveCrossover(conjunctiveBehaviourManager));
        IRandomSelector<IConjunctiveMutation> mutation = new WeightedMap<IConjunctiveMutation>()
                .add(50, new ClauseSelectionConjunctiveMutation(conjunctiveBehaviourManager))
                .add(50, new RelaxOperatorConjunctiveMutation(conjunctiveBehaviourManager));
        conjunctiveBehaviourManager.setCrossover(crossover);
        conjunctiveBehaviourManager.setMutation(mutation);

        return conjunctiveBehaviourManager;
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
