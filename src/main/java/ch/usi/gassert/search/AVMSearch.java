package ch.usi.gassert.search;

public class AVMSearch {
    public static void search(VariablesList variables, int plateauSteps) {
        boolean hasImproved;
        do {
            hasImproved = false;
            for (int i = 0 ; i < variables.size() ; ++i) {
                // Selected delta, 0 means both directions
                int delta = 0;
                // Find out the direction for the local optimum (add or sub)
                Objective objInit = variables.evaluate();
                Objective objAdd = variables.evaluateWithDelta(i, 1);
                Objective objSub = variables. evaluateWithDelta(i, -1);
                int diffAdd = objInit.compareTo(objAdd);
                int diffSub = objInit.compareTo(objSub);
                if (diffAdd < 0 && diffSub < 0) {
                    // Variable already in local optimum
                    continue;
                }
                if (diffAdd >= 0) {
                    ++delta;
                }
                if (diffSub >= 0) {
                    --delta;
                }
                Object oldValue = variables.getValue(i);
                boolean improved;
                if (delta == 0) {
                    boolean improvedAdd = searchVariableValue(variables, plateauSteps, i, 1);
                    Object valueAdd = variables.getValue(i);
                    objAdd = variables.evaluate();
                    variables.setValue(i, oldValue);
                    boolean improvedSub = searchVariableValue(variables, plateauSteps, i, -1);
                    Object valueSub = variables.getValue(i);
                    objSub = variables.evaluate();
                    improved = improvedAdd | improvedSub;
                    if (improved) {
                        if (objAdd.compareTo(objSub) < 0) {
                            variables.setValue(i, valueAdd);
                        } else {
                            variables.setValue(i, valueSub);
                        }
                    } else {
                        variables.setValue(i, oldValue);
                    }
                } else {
                    improved = searchVariableValue(variables, plateauSteps, i, delta);
                }
                if (improved) {
                    hasImproved = true;
                } else {
                    variables.setValue(i, oldValue);
                }
            }
        } while (hasImproved);
    }
    private static boolean searchVariableValue(VariablesList variables, int plateauSteps, int variableIndex, int delta) {
        // Progress tracking
        boolean hasImproved = false;
        int plateau = 0;
        boolean updated = true;
        // Initial objective values
        Objective objPrevious = variables.evaluate();
        Objective objNext = variables. evaluateWithDelta(variableIndex, delta);
        // Do we start improving, or in a plateau?
        int progress = objPrevious.compareTo(objNext);
        if (progress > 0) {
            hasImproved = true;
        } else if (progress < 0) {
            return false;
        } else {
            ++plateau;
        }
        // Update value with first step
        if (plateau <= plateauSteps) {
            variables.updateValue(variableIndex, delta);
        } else {
            return false;
        }
        // Keep updating value until we reach the local optimum
        while (updated) {
            while (updated) {
                delta *= 2;
                updated = variables.updateValue(variableIndex, delta);
                if (updated) {
                    objPrevious = objNext;
                    objNext = variables.evaluate();
                    progress = objPrevious.compareTo(objNext);
                    if (progress > 0) {
                        hasImproved = true;
                        plateau = 0;
                    } else {
                        if (progress < 0) {
                            // Undo the last variables.updateValue, which overshot the local optimum
                            variables.updateValue(variableIndex, -delta);
                        }
                        // No progress, break to outer loop
                        break;
                    }
                }
            }
            if (progress == 0 && plateau < plateauSteps) {
                // Stuck in plateau
                ++plateau;
            } else {
                // Reached local optimum, skip to next variable
                break;
            }
        }
        return hasImproved;
    }
}
