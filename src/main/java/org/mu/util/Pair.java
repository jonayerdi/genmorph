package org.mu.util;

import java.util.Objects;

public class Pair<A, B> {

    public final A a;
    public final B b;

    protected Pair(final A a, final B b) {
        this.a = a;
        this.b = b;
    }

    public static<A, B> Pair<A, B> of(final A a, final B b) {
        return new Pair<>(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(a, pair.a) && Objects.equals(b, pair.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

}
