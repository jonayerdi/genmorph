package ch.usi.gassert;


import ch.usi.gassert.util.AtomicBigInteger;
import ch.usi.gassert.util.FileUtils;
import ch.usi.gassert.util.MyGson;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Time {

    private static Time instance;

    public enum KeysCounter {
        crossOver, selection, mutation, minimization, elitism, computeFitnessFunction, loadState, initialPopulation
    }

    static {
        instance = new Time();
    }

    private Map<KeysCounter, AtomicBigInteger> key2counterTime;
    private Map<Long, Map<KeysCounter, Long>> thread2startTime;


    private Time() {
        super();
        thread2startTime = new ConcurrentHashMap<>();
        key2counterTime = new ConcurrentHashMap<>();
        for (final KeysCounter key : KeysCounter.values()) {
            key2counterTime.put(key, new AtomicBigInteger(new BigInteger("0")));
        }
    }

    public static Time getInstance() {
        return instance;
    }

    public Map<KeysCounter, AtomicBigInteger> getKey2counterTime() {
        return key2counterTime;
    }

    public void start(final KeysCounter key) {
        thread2startTime.putIfAbsent(Thread.currentThread().getId(), new HashMap<>());
        thread2startTime.get(Thread.currentThread().getId()).put(key, System.currentTimeMillis());
    }

    public void stop(final KeysCounter key) {
        if (thread2startTime.containsKey(Thread.currentThread().getId())) {
            if (thread2startTime.get(Thread.currentThread().getId()).containsKey(key)) {
                key2counterTime.get(key).addValue(System.currentTimeMillis() - thread2startTime.get(Thread.currentThread().getId()).get(key));
            }
        }
    }

    public AtomicBigInteger get(final KeysCounter key) {
        if (key2counterTime.containsKey(key)) {
            return key2counterTime.get(key);
        }
        return new AtomicBigInteger(BigInteger.ZERO);
    }

    public void print(final int iteration) {
        System.out.println(key2counterTime);
        //FileUtils.overwriteTextOnFile(Config.GASSERT_HOME + /*"/subjects/" + subjectFolder + */  "/output/time-iter" + iteration + ".info", MyGson.getInstance().toJson((key2counterTime)));
    }
}
