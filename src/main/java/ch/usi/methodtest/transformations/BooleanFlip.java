package ch.usi.methodtest.transformations;

import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.CollectionUtils;
import ch.usi.gassert.util.MR;
import ch.usi.methodtest.ConjunctiveMRIPClause;
import ch.usi.methodtest.MethodParameter;

import static ch.usi.gassert.util.Assert.assertAlways;

import java.util.List;

public class BooleanFlip extends AbstractSingleTransformation {

    public BooleanFlip(final String[] parameters) {
        assertAlways(parameters.length == 0, "BooleanFlip does not take any parameters");
    }

    public BooleanFlip() {

    }

    @Override
    public List<String> getParameters() {
        return CollectionUtils.list();
    }

    @Override
    public List<MethodParameter> transform(final List<MethodParameter> parameters) {
        final MethodParameter p = parameters.get(0);
        if (ClassUtils.isBooleanType(p.clazz)) {
            return CollectionUtils.list(
                new MethodParameter(p.name, p.clazz, !ClassUtils.booleanAsBoolean(p.value))
            );
        }
        return null;
    }

    @Override
    public List<ConjunctiveMRIPClause> makeClauses(final List<String> parameters) {
        return CollectionUtils.list(ConjunctiveMRIPClause.buildSimple(
            MR.VARIABLE_TO_FOLLOWUP(parameters.get(0)),
            "!=",
            MR.VARIABLE_TO_SOURCE(parameters.get(0))
        ));
    }

}
