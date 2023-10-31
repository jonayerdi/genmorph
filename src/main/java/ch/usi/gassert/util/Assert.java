package ch.usi.gassert.util;

import java.util.function.Predicate;

public final class Assert {

    public static class AssertionFailedException extends RuntimeException {
        public AssertionFailedException(final String message) {
            super(message);
        }
    }

    public static void assertAlways(final boolean predicate, final String message) {
        if (!predicate) {
            throw new AssertionFailedException(message);
        }
    }

    public static <T> T assertProperty(final T obj, final Predicate<T> predicate, final String message) {
        assertAlways(predicate.test(obj), message);
        return obj;
    }

}
