package ch.usi.gassert.util;

public final class Bitmap {

    public static int countSetBits(long bucketValue) {
        int count = 0;
        for(int i = 0 ; i < Long.SIZE ; ++i) {
            count += bucketValue % 2;
            bucketValue >>= 1;
        }
        return count;
    }

    public static int bucketsForSize(final int size) {
        return (size + Long.SIZE - 1) / Long.SIZE;
    }

    public static int bucketForIndex(final int index) {
        return index / Long.SIZE;
    }

    public static int bitForIndex(final int index) {
        return index % Long.SIZE;
    }

    public static long[] create(final int size) {
        return new long[bucketsForSize(size)];
    }

    public static boolean isSet(final long[] bitmap, final int index) {
        return (bitmap[bucketForIndex(index)] & (1L << bitForIndex(index))) != 0;
    }

    public static void set(final long[] bitmap, final int index) {
        bitmap[bucketForIndex(index)] |= (1L << bitForIndex(index));
    }

    public static void unset(final long[] bitmap, final int index) {
        bitmap[bucketForIndex(index)] &= ~(1L << bitForIndex(index));
    }

    public static void copyBuckets(final long[] src, final long[] dst, int length) {
        if (length > 0) {
            // WARNING: We copy the entire last bucket even if length % Long.SIZE != 0
            System.arraycopy(src, 0, dst, 0, bucketsForSize(length));
        }
    }

}
