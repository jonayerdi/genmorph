package ch.usi.methodtest.transformations;

import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.CollectionUtils;
import ch.usi.gassert.util.MR;
import ch.usi.methodtest.ConjunctiveMRIPClause;
import ch.usi.methodtest.MethodParameter;

import java.util.List;

import static ch.usi.gassert.util.Assert.assertAlways;

public class SwitchParams extends AbstractDoubleTransformation {

    public SwitchParams(final String[] parameters) {
        assertAlways(parameters.length == 0, "SwitchParams does not take any parameters");
    }

    public SwitchParams() {

    }

    @Override
    public List<String> getParameters() {
        return CollectionUtils.list();
    }

    public boolean isSupportedType(final Class<?> clazz) {
        return ClassUtils.isBooleanType(clazz)
            || ClassUtils.isNumericType(clazz)
            || ClassUtils.isSequenceType(clazz);
    }

    @Override
    public List<MethodParameter> transform(final List<MethodParameter> parameters) {
        final MethodParameter p1 = parameters.get(0);
        final MethodParameter p2 = parameters.get(1);
        if (p1.clazz.equals(p2.clazz) && isSupportedType(p1.clazz)) {
            return CollectionUtils.list(
                new MethodParameter(p1.name, p1.clazz, p2.value),
                new MethodParameter(p2.name, p2.clazz, p1.value)
            );
        }
        return null;
    }

    @Override
    public List<ConjunctiveMRIPClause> makeClauses(final List<String> parameters) {
        return CollectionUtils.list(
                ConjunctiveMRIPClause.buildSimple(
                    MR.VARIABLE_TO_FOLLOWUP(parameters.get(0)),
                    "==",
                    MR.VARIABLE_TO_SOURCE(parameters.get(1))
                ),
                ConjunctiveMRIPClause.buildSimple(
                    MR.VARIABLE_TO_FOLLOWUP(parameters.get(1)),
                    "==",
                    MR.VARIABLE_TO_SOURCE(parameters.get(0))
                )
        );
    }

}
