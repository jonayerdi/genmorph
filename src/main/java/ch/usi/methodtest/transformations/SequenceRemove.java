package ch.usi.methodtest.transformations;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.CollectionUtils;
import ch.usi.gassert.util.MR;
import ch.usi.methodtest.ConjunctiveMRIPClause;
import ch.usi.methodtest.MethodParameter;

import static ch.usi.gassert.util.Assert.assertAlways;

import java.util.List;

public class SequenceRemove extends AbstractSingleTransformation {
    
    public final int index;

    public SequenceRemove(final String[] parameters) {
        this(Integer.parseInt(parameters[0]));
        assertAlways(parameters.length == 1, "SequenceRemove takes exactly one parameter");
    }

    public SequenceRemove(final int index) {
        this.index = index;
    }

    @Override
    public List<String> getParameters() {
        return CollectionUtils.list(String.format("%d", this.index));
    }

    @Override
    public List<MethodParameter> transform(final List<MethodParameter> parameters) {
        final MethodParameter p = parameters.get(0);
        if (ClassUtils.isSequenceType(p.clazz)) {
            return CollectionUtils.list(
                new MethodParameter(
                    p.name, p.clazz,
                    ClassUtils.sequenceAsSequence(p.value).remove(this.index).getValue()
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
                        new Tree(MR.VARIABLE_TO_FOLLOWUP(parameters.get(0)), Tree.Type.SEQUENCE),
                        new Tree(
                                "remove",
                                new Tree(MR.VARIABLE_TO_SOURCE(parameters.get(0)), Tree.Type.SEQUENCE),
                                new Tree(this.index, Tree.Type.NUMBER),
                                Tree.Type.SEQUENCE
                        ),
                        Tree.Type.BOOLEAN
                )
        ));
    }

}
