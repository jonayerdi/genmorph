package ch.usi.gassert.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class ArrayUtils {

    public static <T> Stream<T> merge(final T[] array1, int startIndex1, int stopIndex1,
                                final T[] array2, int startIndex2, int stopIndex2) {
        return Stream.concat(
                Arrays.stream(array1).skip(startIndex1).limit(stopIndex1 - startIndex1),
                Arrays.stream(array2).skip(startIndex2).limit(stopIndex2 - startIndex2)
        );
    }

    public static Object newArrayWithElements(final Class<?> itemsClass, final int length, Iterator<?> elements) {
        final Object newArray = Array.newInstance(itemsClass, length);
        for (int i = 0 ; i < length ; ++i) {
            Array.set(newArray, i, elements.next());
        }
        return newArray;
    }

    public static Object newArrayWithElements(final Class<?> itemsClass, final int length, Stream<?> elements) {
        return newArrayWithElements(itemsClass, length, elements.iterator());
    }

}
