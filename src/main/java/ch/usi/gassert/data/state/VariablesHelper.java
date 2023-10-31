package ch.usi.gassert.data.state;

import ch.usi.gassert.util.MR;
import ch.usi.gassert.util.Utils;

import java.util.*;

public class VariablesHelper {

    public static Variables makeMetamorphic(final Variables sourceVariables, final Variables followupVariables) {
        Map<String, Object> sourceVariableValues = sourceVariables.getValues();
        Map<String, Object> followupVariableValues = followupVariables.getValues();
        Set<String> sourceInputs = sourceVariables.getInputs();
        Set<String> sourceOutputs = sourceVariables.getOutputs();

        Map<String, Object> variableValues = new HashMap<>(sourceVariableValues.size() * 2);
        Set<String> inputs = new HashSet<>(sourceInputs.size() * 2);
        Set<String> outputs = new HashSet<>(sourceOutputs.size() * 2);

        for (String var : sourceVariables.getNames()) {
            String sourceVar = MR.VARIABLE_TO_SOURCE(var);
            String followVar = MR.VARIABLE_TO_FOLLOWUP(var);
            Object sourceValue = Objects.requireNonNull(sourceVariableValues.get(var),
                    "Source variable " + var + " not found");
            Object followValue = Objects.requireNonNull(followupVariableValues.get(var),
                    "Followup variable " + var + " not found");
            if (sourceInputs.contains(var)) {
                inputs.add(sourceVar);
                inputs.add(followVar);
            }
            if (sourceOutputs.contains(var)) {
                outputs.add(sourceVar);
                outputs.add(followVar);
            }
            Utils.requireNull(variableValues.put(sourceVar, sourceValue),
                    "Duplicate variable " + sourceVar);
            Utils.requireNull(variableValues.put(followVar, followValue),
                    "Duplicate variable " + followVar);
        }

        return new Variables(variableValues, inputs, outputs);
    }

    public static Map<String, Object> makeMetamorphic(final Map<String, Object> sourceVariableValues,
                                                      final Map<String, Object> followupVariableValues) {
        Map<String, Object> variableValues = new HashMap<>(sourceVariableValues.size() * 2);

        for (String var : sourceVariableValues.keySet()) {
            String sourceVar = MR.VARIABLE_TO_SOURCE(var);
            String followVar = MR.VARIABLE_TO_FOLLOWUP(var);
            Object sourceValue = Objects.requireNonNull(sourceVariableValues.get(var),
                    "Source variable " + var + " not found");
            Object followValue = Objects.requireNonNull(followupVariableValues.get(var),
                    "Followup variable " + var + " not found");
            Utils.requireNull(variableValues.put(sourceVar, sourceValue),
                    "Duplicate variable " + sourceVar);
            Utils.requireNull(variableValues.put(followVar, followValue),
                    "Duplicate variable " + followVar);
        }

        return variableValues;
    }

}
