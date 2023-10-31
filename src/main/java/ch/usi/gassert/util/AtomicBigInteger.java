package ch.usi.gassert.util;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class AtomicBigInteger {

    private final AtomicReference<BigInteger> valueHolder = new AtomicReference<>();

    public AtomicBigInteger(final BigInteger bigInteger) {
        valueHolder.set(bigInteger);
    }

    public boolean setIfMissing(final long value) {
        return valueHolder.compareAndSet(null, new BigInteger(String.valueOf(value)));
    }

    public void increment() {
        for (; ; ) {
            final BigInteger current = valueHolder.get();
            final BigInteger next = current.add(BigInteger.ONE);
            if (valueHolder.compareAndSet(current, next)) {
                break;
            }
        }
    }

    public void addValue(final long value) {
        for (; ; ) {
            final BigInteger current = valueHolder.get();
            final BigInteger next = current.add(new BigInteger(String.valueOf(value)));
            if (valueHolder.compareAndSet(current, next)) {
                break;
            }
        }
    }

    public BigInteger get() {
        return valueHolder.get();
    }

    public BigInteger set(final Integer value) {
        for (; ; ) {
            final BigInteger current = valueHolder.get();
            final BigInteger next = new BigInteger(String.valueOf(value));
            if (valueHolder.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    public BigInteger set(final Long value) {
        for (; ; ) {
            final BigInteger current = valueHolder.get();
            final BigInteger next = new BigInteger(String.valueOf(value));
            if (valueHolder.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    @Override
    public String toString() {
        return valueHolder.get().toString();
    }
}

