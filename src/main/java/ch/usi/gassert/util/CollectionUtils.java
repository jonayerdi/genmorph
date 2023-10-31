package ch.usi.gassert.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CollectionUtils {

    @SafeVarargs
    public static <E> Set<E> set(E... elements) {
        return Arrays.stream(elements).collect(Collectors.toSet());
    }

    @SafeVarargs
    public static <E> List<E> list(E... elements) {
        return Arrays.stream(elements).collect(Collectors.toList());
    }

    @SafeVarargs
    public static <E> E[] array(E... elements) {
        return elements;
    }

    public static <K,V> Map<K,V> map() {
        final Map<K,V> map = new HashMap<>(0);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1) {
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2) {
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3) {
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

}
