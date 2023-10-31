import java.util.Arrays;
import static java.lang.Double.NaN;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

public class GuavaClass {

    /**
     * A bit mask which selects the bit encoding ASCII character case.
     */
    private static final char CASE_MASK = 0x20;

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static void checkPositionIndexes(int start, int end, int size) {
        // Carefully optimized for execution by hotspot (explanatory comment above)
        if (start < 0 || end < start || end > size) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static boolean isFinite(double value) {
        return NEGATIVE_INFINITY < value && value < POSITIVE_INFINITY;
    }

    public static String padStart(String string, int minLength, char padChar) {
        // eager for GWT.
        checkNotNull(string);
        if (string.length() >= minLength) {
            return string;
        }
        StringBuilder sb = new StringBuilder(minLength);
        for (int i = string.length(); i < minLength; i++) {
            sb.append(padChar);
        }
        sb.append(string);
        return sb.toString();
    }

    public static String repeat(String string, int count) {
        // eager for GWT.
        checkNotNull(string);
        if (count <= 1) {
            checkArgument(count >= 0, "invalid count: %s", count);
            return (count == 0) ? "" : string;
        }
        // IF YOU MODIFY THE CODE HERE, you must update StringsRepeatBenchmark
        final int len = string.length();
        final long longSize = (long) len * (long) count;
        final int size = (int) longSize;
        if (size != longSize) {
            throw new ArrayIndexOutOfBoundsException("Required array size too large: " + longSize);
        }
        final char[] array = new char[size];
        string.getChars(0, len, array, 0);
        int n;
        for (n = len; n < size - n; n <<= 1) {
            System.arraycopy(array, 0, array, n, n);
        }
        System.arraycopy(array, 0, array, n, size - n);
        return new String(array);
    }

    public static int indexOf(boolean[] array, boolean[] target) {
        checkNotNull(array, "array");
        checkNotNull(target, "target");
        if (target.length == 0) {
            return 0;
        }
        outer: for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte flip(byte b) {
        return (byte) (b ^ 0x80);
    }

    public static void sort(byte[] array, int fromIndex, int toIndex) {
        checkNotNull(array);
        checkPositionIndexes(fromIndex, toIndex, array.length);
        for (int i = fromIndex; i < toIndex; i++) {
            array[i] = flip(array[i]);
        }
        Arrays.sort(array, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            array[i] = flip(array[i]);
        }
    }

    static double calculateNewMeanNonFinite(double previousMean, double value) {
        /*
     * Desired behaviour is to match the results of applying the naive mean formula. In particular,
     * the update formula can subtract infinities in cases where the naive formula would add them.
     *
     * Consequently:
     * 1. If the previous mean is finite and the new value is non-finite then the new mean is that
     *    value (whether it is NaN or infinity).
     * 2. If the new value is finite and the previous mean is non-finite then the mean is unchanged
     *    (whether it is NaN or infinity).
     * 3. If both the previous mean and the new value are non-finite and...
     * 3a. ...either or both is NaN (so mean != value) then the new mean is NaN.
     * 3b. ...they are both the same infinities (so mean == value) then the mean is unchanged.
     * 3c. ...they are different infinities (so mean != value) then the new mean is NaN.
     */
        if (isFinite(previousMean)) {
            // This is case 1.
            return value;
        } else if (isFinite(value) || previousMean == value) {
            // This is case 2. or 3b.
            return previousMean;
        } else {
            // This is case 3a. or 3c.
            return NaN;
        }
    }

    public static double meanOf(int[] values) {
        checkArgument(values.length > 0);
        double mean = values[0];
        for (int index = 1; index < values.length; index++) {
            double value = values[index];
            if (isFinite(value) && isFinite(mean)) {
                // Art of Computer Programming vol. 2, Knuth, 4.2.2, (15)
                mean += (value - mean) / (index + 1);
            } else {
                mean = calculateNewMeanNonFinite(mean, value);
            }
        }
        return mean;
    }

    public static int min(int[] array) {
        checkArgument(array.length > 0);
        int min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public static String join(String separator, boolean[] array) {
        checkNotNull(separator);
        if (array.length == 0) {
            return "";
        }
        // For pre-sizing a builder, just get the right order of magnitude
        StringBuilder builder = new StringBuilder(array.length * 7);
        builder.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            builder.append(separator).append(array[i]);
        }
        return builder.toString();
    }

    public static boolean validSurrogatePairAt(CharSequence string, int index) {
        return index >= 0 && index <= (string.length() - 2) && Character.isHighSurrogate(string.charAt(index)) && Character.isLowSurrogate(string.charAt(index + 1));
    }

    /**
     * Returns the longest string {@code suffix} such that {@code a.toString().endsWith(suffix) &&
     * b.toString().endsWith(suffix)}, taking care not to split surrogate pairs. If {@code a} and
     * {@code b} have no common suffix, returns the empty string.
     *
     * @since 11.0
     */
    public static String commonSuffix(String a, String b) {
        checkNotNull(a);
        checkNotNull(b);
        int maxSuffixLength = Math.min(a.length(), b.length());
        int s = 0;
        while (s < maxSuffixLength && a.charAt(a.length() - s - 1) == b.charAt(b.length() - s - 1)) {
            s++;
        }
        if (validSurrogatePairAt(a, a.length() - s - 1) || validSurrogatePairAt(b, b.length() - s - 1)) {
            s--;
        }
        return a.subSequence(a.length() - s, a.length()).toString();
    }

    public static boolean isUpperCase(char c) {
        return (c >= 'A') && (c <= 'Z');
    }

    public static String toLowerCase(String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (isUpperCase(string.charAt(i))) {
                char[] chars = string.toCharArray();
                for (; i < length; i++) {
                    char c = chars[i];
                    if (isUpperCase(c)) {
                        chars[i] = (char) (c ^ CASE_MASK);
                    }
                }
                return String.valueOf(chars);
            }
        }
        return string;
    }

    public static String truncate(String seq, int maxLength, String truncationIndicator) {
        checkNotNull(seq);
        // length to truncate the sequence to, not including the truncation indicator
        int truncationLength = maxLength - truncationIndicator.length();
        // in this worst case, this allows a maxLength equal to the length of the truncationIndicator,
        // meaning that a string will be truncated to just the truncation indicator itself
        checkArgument(truncationLength >= 0, "maxLength (%s) must be >= length of the truncation indicator (%s)", maxLength, truncationIndicator.length());
        if (seq.length() <= maxLength) {
            String string = seq.toString();
            if (string.length() <= maxLength) {
                return string;
            }
            // if the length of the toString() result was > maxLength for some reason, truncate that
            seq = string;
        }
        return new StringBuilder(maxLength).append(seq, 0, truncationLength).append(truncationIndicator).toString();
    }
}

