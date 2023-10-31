package ch.usi.methodtest.transformations;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.CollectionUtils;
import ch.usi.gassert.util.MR;
import ch.usi.methodtest.ConjunctiveMRIPClause;
import ch.usi.methodtest.MethodParameter;

import static ch.usi.gassert.util.Assert.assertAlways;
import static ch.usi.methodtest.MethodTestTransformerConfig.NUMERIC_FORMAT;

import java.util.List;

public class NumericMultiplication extends AbstractSingleTransformation {

    public final double number;

    public NumericMultiplication(final String[] parameters) {
        this(Double.parseDouble(parameters[0]));
        assertAlways(parameters.length == 1, "NumericMultiplication takes exactly one parameter");
    }

    public NumericMultiplication(final double number) {
        this.number = number;
    }

    @Override
    public List<String> getParameters() {
        return CollectionUtils.list(String.format(NUMERIC_FORMAT, this.number));
    }

    @Override
    public List<MethodParameter> transform(final List<MethodParameter> parameters) {
        final MethodParameter p = parameters.get(0);
        if (ClassUtils.isNumericType(p.clazz)) {
            return CollectionUtils.list(
                new MethodParameter(
                    p.name, p.clazz,
                    ClassUtils.numericWithClass(p.clazz, ClassUtils.numericAsDouble(p.value) * this.number)
                )
            );
        }
        return null;
    }

    @Override
    public List<ConjunctiveMRIPClause> makeClauses(final List<String> parameters) {
        return CollectionUtils.list(new ConjunctiveMRIPClause(
                new Tree(
                        "==",
                        new Tree(MR.VARIABLE_TO_FOLLOWUP(parameters.get(0)), Tree.Type.NUMBER),
                        new Tree(
                                "*",
                                new Tree(MR.VARIABLE_TO_SOURCE(parameters.get(0)), Tree.Type.NUMBER),
                                new Tree(this.number, Tree.Type.NUMBER),
                                Tree.Type.NUMBER
                        ),
                        Tree.Type.BOOLEAN
                )
        ));
    }

}
