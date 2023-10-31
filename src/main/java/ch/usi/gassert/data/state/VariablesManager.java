package ch.usi.gassert.data.state;

import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.MR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class VariablesManager implements IVariablesManager {

    public static final VariablesManager EMPTY = new VariablesManager(new HashMap<>(0), new HashSet<>(0), new HashSet<>(0), new HashSet<>(0));

    protected final Map<String, Class<?>> variableTypes;
    protected final Set<String> numericVars;
    protected final Set<String> booleanVars;
    protected final Set<String> sequenceVars;

    public VariablesManager(final Map<String, Class<?>> variableTypes, final Set<String> numericVars, final Set<String> booleanVars, final Set<String> sequenceVars) {
        this.variableTypes = variableTypes;
        this.numericVars = numericVars;
        this.booleanVars = booleanVars;
        this.sequenceVars = sequenceVars;
    }

    public VariablesManager(final Map<String, Class<?>> variableTypes) {
        this.variableTypes = variableTypes;
        this.numericVars = new HashSet<>();
        this.booleanVars = new HashSet<>();
        this.sequenceVars = new HashSet<>();
        getTypeVars(variableTypes, this.numericVars, this.booleanVars, this.sequenceVars);
    }

    public static VariablesManager fromVariableTypes(final Map<String, Class<?>> variableTypes) {
        return new VariablesManager(variableTypes);
    }

    public static VariablesManager fromVariableValues(final Map<String, Object> variables) {
        return new VariablesManager(getVariableTypesFromValues(variables));
    }

    @Override
    public Map<String, Class<?>> getVariableTypes() {
        return this.variableTypes;
    }

    @Override
    public Set<String> getNumericVars() {
        return this.numericVars;
    }

    @Override
    public Set<String> getBooleanVars() {
        return this.booleanVars;
    }

    @Override
    public Set<String> getSequenceVars() {
        return this.sequenceVars;
    }

    @Override
    public IVariablesManager filterVariables(BiPredicate<String, Class<?>> f) {
        return VariablesManager.fromVariableTypes(variableTypes.entrySet().stream()
                .filter(e -> f.test(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public IVariablesManager makeMetamorphic() {
        final Map<String, Class<?>> metamorphicVariableTypes = new HashMap<>(variableTypes.size() * 2);
        for (Map.Entry<String, Class<?>> var : variableTypes.entrySet()) {
            metamorphicVariableTypes.put(MR.VARIABLE_TO_SOURCE(var.getKey()), var.getValue());
            metamorphicVariableTypes.put(MR.VARIABLE_TO_FOLLOWUP(var.getKey()), var.getValue());
        }
        return VariablesManager.fromVariableTypes(metamorphicVariableTypes);
    }

    public static void getTypeVars(final Map<String, Class<?>> variableTypes, Set<String> numericVars, Set<String> booleanVars, Set<String> sequenceVars) {
        for (Map.Entry<String, Class<?>> entry : variableTypes.entrySet()) {
            if (ClassUtils.isNumericType(entry.getValue())) {
                numericVars.add(entry.getKey());
            } else if (ClassUtils.isBooleanType(entry.getValue())) {
                booleanVars.add(entry.getKey());
            } else if (ClassUtils.isSequenceType(entry.getValue())) {
                sequenceVars.add(entry.getKey());
            }
        }
    }

    public static Map<String, Class<?>> getVariableTypesFromValues(final Map<String, Object> variables) {
        final Map<String, Class<?>> variableTypes = new HashMap<>(variables.size());
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            variableTypes.put(entry.getKey(), entry.getValue().getClass());
        }
        return variableTypes;
    }

}
