package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.evolutionary.Individual;

public interface IValidator {
    boolean validate(Individual sol);
    default boolean revalidate(Individual sol, Boolean valid) {
        // WARNING: We assume that most IValidator do not need revalidating
        return valid == null ? this.validate(sol) : valid;
    }
}
