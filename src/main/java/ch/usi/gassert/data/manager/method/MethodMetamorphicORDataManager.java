package ch.usi.gassert.data.manager.method;

import ch.usi.gassert.Config;
import ch.usi.gassert.Mode;
import ch.usi.gassert.data.manager.DataManagerArgs;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.TreeReaderGAssert;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evolutionary.BehaviourManager;
import ch.usi.gassert.evolutionary.fitness.ComplexityValidator;
import ch.usi.gassert.evolutionary.fitness.IValidator;
import ch.usi.gassert.evolutionary.fitness.IsMetamorphicOutputRelationValidator;
import ch.usi.gassert.mrip.MRIPComposer;
import ch.usi.gassert.util.Lazy;
import ch.usi.gassert.util.Pair;
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
public class MethodMetamorphicORDataManager extends MethodMetamorphicDataManager {

    // Cached input relation
    protected Lazy<MethodMetamorphicORDataManager, ITree> inputRelation = new Lazy<>(thiz -> {
        final String mripsFile = thiz.getArgs().args[4];
        final String mrip = thiz.getArgs().args[5];
        return TreeReaderGAssert.getTree(MRIPComposer.readInputRelation(mripsFile, mrip), getEvaluationVariableTypes());
    });

    /**
     * args = new String[] {
     *                         "GASSERT",
     *                         "states/states.zip",
     *                         "states/classifications.zip",
     *                         "states/MRInfo.csv",
     *                         "states/MRIP.csv",
     *                         "MRIP1",
     *                         ["states/MRInfoMandatory.csv"]
     *                 }
     */
    public MethodMetamorphicORDataManager(final DataManagerArgs dargs, final IStatesUpdater statesUpdater) {
        super(dargs, statesUpdater);
    }

    @Override
    public Mode getMode() {
        return Mode.METAMORPHIC_OUTPUT_RELATION;
    }

    @Override
    protected Pair<List<ITestExecution>, List<ITestExecution>>
    initCorrectIncorrectExecutions(DataManagerArgs dargs,
                                   Map<String, TestClassifications> classifications,
                                   IStreamLoader statesDataSource) {
        final String mrinfosFile = dargs.args[3];
        final String mrip = dargs.args[5];
        final List<MRInfo> mrinfos;
        try (final BufferedReader reader = new BufferedReader(new FileReader(mrinfosFile))) {
            mrinfos = MRInfoDB.fromCsv(reader)
                    .getMRInfos()
                    .stream()
                    .filter(mrinfo -> mrinfo.mr.equals(mrip))
                    .collect(Collectors.toList());;
        } catch (Exception e) {
            throw new RuntimeException("Error loading " + mrinfosFile, e);
        }
        final List<MRInfo> mrinfosMandatory;
        if (dargs.args.length > 6) {
            final String mrinfosMandatoryFile = dargs.args[6];
            try (final BufferedReader reader = new BufferedReader(new FileReader(mrinfosMandatoryFile))) {
                mrinfosMandatory = MRInfoDB.fromCsv(reader).getMRInfos();
            } catch (Exception e) {
                throw new RuntimeException("Error loading " + mrinfosMandatoryFile, e);
            }
        } else {
            mrinfosMandatory = new ArrayList<>();
        }
        return initCorrectIncorrectExecutionsMRInfo(dargs, mrinfos, mrinfosMandatory, classifications, statesDataSource);
    }

    @Override
    protected BehaviourManager initBehaviourManager() {
        BehaviourManager behaviourManager = super.initBehaviourManager();

        // Set validator
        TreeTemplate orTemplate = treeTemplate;
        IValidator orIsMetamorphic = new IsMetamorphicOutputRelationValidator(orTemplate);
        behaviourManager.setValidator(new ComplexityValidator(Config.MAX_OR_COMPLEXITY));
        behaviourManager.setEliteValidator(orIsMetamorphic);

        return behaviourManager;
    }

    @Override
    public ITree getInputRelation(final TreeGroup tg) {
        // Cached input relation
        return this.inputRelation.get(this);
    }

    @Override
    public ITree getOutputRelation(final TreeGroup tg) {
        // Whole Tree is Output Relation
        return tg.buildTree();
    }

}
