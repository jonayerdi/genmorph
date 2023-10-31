package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.evolutionary.Individual;

public class ComplexityValidator implements IValidator {

    public final int maxComplexity;

    public ComplexityValidator(final int maxComplexity) {
        this.maxComplexity = maxComplexity;
    }

    @Override
    public boolean validate(Individual sol) {
        // The expression's complexity cannot be higher than MAX_COMPLEXITY
        return sol.complexity <= this.maxComplexity;
    }

}
