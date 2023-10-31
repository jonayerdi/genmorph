package ch.usi.gassert.data.manager;

import ch.usi.gassert.Mode;
import ch.usi.gassert.data.state.ITestExecution;
import ch.usi.gassert.data.state.IVariablesManager;
import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.data.tree.ITree;
import ch.usi.gassert.data.tree.template.TreeGroup;
import ch.usi.gassert.data.tree.template.TreeTemplate;
import ch.usi.gassert.evaluator.IEvaluator;
import ch.usi.gassert.evolutionary.BehaviourManager;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IDataManager {

    // Args
    DataManagerArgs getArgs();

    // Mode
    Mode getMode();

    // Template for the generated expressions
    TreeTemplate getTreeTemplate();
    void setTreeTemplate(TreeTemplate treeTemplate);

    // Evaluator for the generated expressions
    IEvaluator getAssertionEvaluator();
    void setAssertionEvaluator(IEvaluator evaluator);

    // SUTs and test cases
    List<String> getSystemIds();
    List<String> getTestIds();

    // Correct/Incorrect test executions
    List<ITestExecution> getCorrectTestExecutions();
    List<ITestExecution> getIncorrectTestExecutions();

    // StatesUpdater
    IStatesUpdater getStatesUpdater();
    void addCorrectStates(List<ITestExecution> correctTestExecutions);

    // Variables
    Set<String> getInputs();
    Set<String> getOutputs();
    IVariablesManager getEvaluationVariablesManager();
    default Map<String, Class<?>> getEvaluationVariableTypes() {
        return getEvaluationVariablesManager().getVariableTypes();
    }

    // BehaviourManager for the evolutionary algorithm
    BehaviourManager getBehaviourManager();

    // Metamorphic Testing
    default ITree getInputRelation(final TreeGroup tg) {
        throw new UnsupportedOperationException();
    }
    default ITree getOutputRelation(final TreeGroup tg) {
        throw new UnsupportedOperationException();
    }

}
