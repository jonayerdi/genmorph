package ch.usi.methodtest.transformations;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.util.ClassUtils;
import ch.usi.gassert.util.CollectionUtils;
import ch.usi.gassert.util.MR;
import ch.usi.methodtest.ConjunctiveMRIPClause;
import ch.usi.methodtest.MethodParameter;

import static ch.usi.gassert.util.Assert.assertAlways;

import java.util.List;

public class SequenceFlip extends AbstractSingleTransformation {
    
    public SequenceFlip(final String[] parameters) {
        assertAlways(parameters.length == 0, "SequenceFlip does not take any parameters");
    }

    public SequenceFlip() {
        
    }

    @Override
    public List<String> getParameters() {
        return CollectionUtils.list();
    }

    @Override
    public List<MethodParameter> transform(final List<MethodParameter> parameters) {
        final MethodParameter p = parameters.get(0);
        if (ClassUtils.isSequenceType(p.clazz)) {
            return CollectionUtils.list(
                new MethodParameter(
                    p.name, p.clazz,
                    ClassUtils.sequenceAsSequence(p.value).flip().getValue()
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
                                "flip",
                                new Tree(MR.VARIABLE_TO_SOURCE(parameters.get(0)), Tree.Type.SEQUENCE),
                                null,
                                Tree.Type.SEQUENCE
                        ),
                        Tree.Type.BOOLEAN
                )
        ));
    }

}
