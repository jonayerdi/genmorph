package org.mu.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Maps {
    public static List<String> sortedIndexMap(Map<String, Integer> map) {
        return map.keySet().stream().sorted(Comparator.comparingInt(map::get)).collect(Collectors.toList());
    }
}
