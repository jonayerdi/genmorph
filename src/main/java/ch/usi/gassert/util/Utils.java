package ch.usi.gassert.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Utils {
    public static <T> T requireNull(T obj) {
        return requireNull(obj, "Unexpected non-null value");
    }
    public static <T> T requireNull(T obj, String msg) {
        if (obj != null)
            throw new RuntimeException(msg);
        return null;
    }
    public static <T> Set<T> union(final Set<T> s1, final Set<T> s2) {
        final Set<T> s = new HashSet<>(s1.size() + s2.size());
        s.addAll(s1);
        s.addAll(s2);
        return s;
    }
    public static <T> Set<T> unionWith(Set<T> s1, final Set<T> s2) {
        s1.addAll(s2);
        return s1;
    }
    public static <T> T repeatUntil(Supplier<T> supplier, Predicate<T> condition) {
        while (true) {
            final T t = supplier.get();
            if (condition.test(t)) {
                return t;
            }
        }
    }
}
