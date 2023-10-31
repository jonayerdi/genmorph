package ch.usi.gassert.util;

public class Implies {

    public static boolean implies(final boolean a, final boolean b) {
        if (a) {
            return b;
        }
        return true;
    }
}
