package org.mu.util;

public final class Conversions {
    public static Boolean char2bool(final char c) {
        switch (c) {
            case '0':
                return false;
            case '1':
                return true;
            default:
                return null;
        }
    }
    public static Boolean string2bool(final String s) {
        switch (s) {
            case "0":
                return false;
            case "1":
                return true;
            default:
                return null;
        }
    }
    public static char bool2char(final boolean b) {
        return b ? '1' : '0';
    }
    public static String bool2string(final boolean b) {
        return "" + bool2char(b);
    }
}
