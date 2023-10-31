package ch.usi.gassert.util;

import java.util.Objects;

public class Tuple3<A, B, C> {
        public final A a;
        public final B b;
        public final C c;

        public Tuple3(final A a, final B b, final C c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public String toString() {
            return "Tuple3[" + a + "," + b + "," + c + "]";
        }

        private static boolean equals(Object x, Object y) {
            return (x == null && y == null) || (x != null && x.equals(y));
        }

        public boolean equals(Object other) {
            return
                    other instanceof Tuple3 &&
                            equals(a, ((Tuple3) other).a) &&
                            equals(b, ((Tuple3) other).b) &&
                            equals(c, ((Tuple3) other).c);
        }

        public int hashCode() {
            return Objects.hash(a, b, c);
        }

        public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
            return new Tuple3<>(a, b, c);
        }
}
