package ch.usi.methodtest.transformations;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.usi.gassert.util.FileUtils.SEPARATORS;

public class MethodTestTransformationFactory {

    public static String toString(final MethodTestTransformation transformation) {
        return transformation.getClass().getSimpleName()
            + SEPARATORS[1]
            + transformation.getParameters().stream().collect(Collectors.joining(SEPARATORS[0]));
    }

    public static String toString(final MethodTestTransformation transformation, final List<Integer> indices) {
        return MethodTestTransformationFactory.toString(transformation)
            + SEPARATORS[1]
            + indices.stream().map(Object::toString).collect(Collectors.joining(SEPARATORS[0]));
    }

    public static Transformation fromString(final String serialized, final boolean params) {
        try {
            if (params) {
                final int paramsIndex = serialized.lastIndexOf(SEPARATORS[1]);
                final Transformation transformation = MethodTestTransformationFactory.fromString(
                    serialized.substring(0, paramsIndex),
                    false
                );
                transformation.params = Arrays
                    .stream(serialized.substring(paramsIndex + 1).split(Pattern.quote(SEPARATORS[0])))
                    .map(Integer::parseInt).collect(Collectors.toList());
                return transformation;
            } else {
                final String[] split = serialized.split(Pattern.quote(SEPARATORS[1]));
                final String clazzName = split[0];
                final String[] args = Arrays.stream(split).skip(1).toArray(String[]::new);
                final Class<?> clazz = Class.forName("ch.usi.methodtest.transformations." + clazzName);
                return new Transformation((MethodTestTransformation) clazz.getConstructor(String[].class).newInstance((Object) args));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading MethodTestTransformation: " + serialized, e);
        }
    }

    public static class Transformation {
        public MethodTestTransformation transformation;
        public List<Integer> params = null;
        public Transformation(final MethodTestTransformation transformation) {
            this.transformation = transformation;
        }
        public Transformation(final MethodTestTransformation transformation, final List<Integer> params) {
            this.transformation = transformation;
            this.params = params;
        }
    }

}
