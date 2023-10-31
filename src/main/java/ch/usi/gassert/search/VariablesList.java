package ch.usi.gassert.search;

public abstract class VariablesList {

    public abstract int size();
    public abstract Object getValue(int variableIndex);
    public abstract void setValue(int variableIndex, Object value);
    public abstract boolean updateValue(int variableIndex, int delta);
    public abstract Objective evaluate();
    public Objective evaluateWithDelta(int variableIndex, int delta) {
        this.updateValue(variableIndex, delta);
        Objective obj = this.evaluate();
        this.updateValue(variableIndex, -delta);
        return obj;
    }

}
