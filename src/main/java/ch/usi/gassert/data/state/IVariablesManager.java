package ch.usi.gassert.data.state;

import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public interface IVariablesManager {
    Map<String, Class<?>> getVariableTypes();
    Set<String> getNumericVars();
    Set<String> getBooleanVars();
    Set<String> getSequenceVars();
    IVariablesManager filterVariables(BiPredicate<String, Class<?>> f);
    IVariablesManager makeMetamorphic();
}
