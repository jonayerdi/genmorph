package org.mu.util;

public final class StringUtils {

    public static boolean isBlank(String s) {
        return s.chars().noneMatch(c -> c != ' ' && c != '\t' && c != '\n');
    }

}
