package ch.usi.gassert.evolutionary.fitness;

import ch.usi.gassert.evolutionary.Individual;

public class NullValidator implements IValidator {

    public static final NullValidator INSTANCE = new NullValidator();

    protected NullValidator() {

    }

    @Override
    public boolean validate(Individual sol) {
        return true;
    }
}
