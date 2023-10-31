package ch.usi.gassert.data.state;

import org.mu.testcase.Util;

/**
 * Metamorphic test case pairs
 */
public class TestExecutionPair implements ITestExecution {

    private final TestExecution source;
    private final TestExecution followup;
    private final Variables variables;

    public TestExecutionPair(final TestExecution source, final TestExecution followup) {
        this.source = source;
        this.followup = followup;
        this.variables = VariablesHelper.makeMetamorphic(source.getVariables(), followup.getVariables());
    }

    public String getSystemId() {
        return source.getSystemId();
    }

    @Override
    public String getTestId() {
        return Util.joinSourceFollowup(source.getTestId(), followup.getTestId());
    }

    public Variables getVariables() {
        return variables;
    }

    public TestExecution getSource() {
        return source;
    }

    public TestExecution getFollowup() {
        return followup;
    }

}
