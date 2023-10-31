package ch.usi.gassert.util;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class AtomicBigIntegerTest {


    @Test
    public void increment() {
        final AtomicBigInteger number = new AtomicBigInteger(BigInteger.ZERO);
        number.increment();
        assertEquals(BigInteger.ONE, number.get());
    }

    @Test
    public void addValue() {
        final AtomicBigInteger number = new AtomicBigInteger(BigInteger.ONE);
        number.addValue(10l);
        assertEquals(new BigInteger("11"), number.get());
    }


    @Test
    public void set() {
        final AtomicBigInteger number = new AtomicBigInteger(BigInteger.ONE);
        number.addValue(10l);
        assertEquals(new BigInteger("11"), number.get());
        number.set(100);
        assertEquals(new BigInteger("100"), number.get());
    }


    @Test
    public void concurrencyTest() throws InterruptedException {
        final int size = 100;
        final AtomicBigInteger number = new AtomicBigInteger(BigInteger.ZERO);

        final WorkerThread[] workers = new WorkerThread[size];
        for (int i = 0; i < size; i++) {
            workers[i] = new WorkerThread(number);
            workers[i].start();

        }
        for (int i = 0; i < size; i++) {
            workers[i].join();
        }
        final int expected = size * 100000;
        assertEquals(new BigInteger(String.valueOf(expected)), number.get());
    }

    class WorkerThread extends Thread {
        private AtomicBigInteger number;

        public WorkerThread(final AtomicBigInteger number) {
            this.number = number;
        }

        @Override
        public void run() {
            for (int i = 0; i < 100000; i++) {
                number.increment();
            }
        }
    }
}