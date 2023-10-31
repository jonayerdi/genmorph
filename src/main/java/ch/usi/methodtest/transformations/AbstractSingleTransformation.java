package ch.usi.methodtest.transformations;

import java.util.ArrayList;
import java.util.List;

import ch.usi.gassert.util.CollectionUtils;
import ch.usi.methodtest.MethodTest;

public abstract class AbstractSingleTransformation implements MethodTestTransformation {

    @Override
    public List<List<Integer>> findTransformations(final MethodTest source) {
        final List<List<Integer>> transformations = new ArrayList<>();
        for (int i1 = 0 ; i1 < source.methodParameters.length ; ++i1) {
            if (this.transform(CollectionUtils.list(source.methodParameters[i1])) != null) {
                transformations.add(CollectionUtils.list(i1));
            }
        }
        return transformations;
    }

}
