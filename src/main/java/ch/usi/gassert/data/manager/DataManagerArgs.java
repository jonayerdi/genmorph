package ch.usi.gassert.data.manager;

public class DataManagerArgs {

    public final Integer maxCorrectExecutions;
    public final Integer maxIncorrectExecutions;
    public final String[] args;

    public DataManagerArgs(Integer maxCorrectExecutions, Integer maxIncorrectExecutions, String[] args) {
        this.maxCorrectExecutions = maxCorrectExecutions;
        this.maxIncorrectExecutions = maxIncorrectExecutions;
        this.args = args;
    }

    public DataManagerArgs(String[] args) {
        this(null, null, args);
    }

}
