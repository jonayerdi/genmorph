package ch.usi.methodtest.transformations;

import java.util.ArrayList;
import java.util.List;

import ch.usi.gassert.util.CollectionUtils;
import ch.usi.methodtest.MethodTest;

public abstract class AbstractDoubleTransformation implements MethodTestTransformation {

    @Override
    public List<List<Integer>> findTransformations(final MethodTest source) {
        final List<List<Integer>> transformations = new ArrayList<>();
        for (int i1 = 0 ; i1 < source.methodParameters.length ; ++i1) {
            for (int i2 = i1 + 1 ; i2 < source.methodParameters.length ; ++i2) {
                if (this.transform(CollectionUtils.list(source.methodParameters[i1], source.methodParameters[i2])) != null) {
                    transformations.add(CollectionUtils.list(i1, i2));
                }
            }
        }
        return transformations;
    }

}
