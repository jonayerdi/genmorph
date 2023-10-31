package ch.usi.gassert;


import ch.usi.gassert.util.AtomicBigInteger;
import ch.usi.gassert.util.FileUtils;
import ch.usi.gassert.util.MyGson;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Stats {

    static final Set<KeysCounter> KEYS_INIT_WITH_NULL;
    private static final Stats instance;

    public enum KeysCounter {
        timestampStart,
        numberOfTests,
        numberOfMutants,
        numberOfMutantTests,
        numberOfUniqueAssertionEvaluated,
        numberOfIterations,
        numberOfGenerations,
        numberOfBooleanVariables,
        numberOfNumberVariables,
        numberCacheMissAssertion,
        numberCacheHitAssertion,
        numberMutations,
        minimizationImprovments,
        minimizations,
        numberCrossover,
        generationBestSolution,
        generationGoodSolution,
        timestampGoodSolution,
    }

    static {
        KEYS_INIT_WITH_NULL = new HashSet<>();
        KEYS_INIT_WITH_NULL.add(KeysCounter.generationGoodSolution);
        KEYS_INIT_WITH_NULL.add(KeysCounter.timestampGoodSolution);
        instance = new Stats();
    }

    private final Map<KeysCounter, AtomicBigInteger> key2counterStats;

    private Stats() {
        super();
        key2counterStats = new ConcurrentHashMap<>();
        for (final KeysCounter key : KeysCounter.values()) {
            if (KEYS_INIT_WITH_NULL.contains(key)) {
                key2counterStats.put(key, new AtomicBigInteger(null));
            } else {
                key2counterStats.put(key, new AtomicBigInteger(new BigInteger("0")));
            }
        }
    }

    public static Stats getInstance() {
        return instance;
    }

    public Map<KeysCounter, AtomicBigInteger> getKey2counterStats() {
        return key2counterStats;
    }

    public void setIfMissing(final KeysCounter key, final long value) {
        key2counterStats.get(key).setIfMissing(value);
    }

    public void increment(final KeysCounter key) {
        key2counterStats.get(key).increment();
    }

    public void set(final KeysCounter key, final int value) {
        key2counterStats.get(key).set(value);
    }

    public void set(final KeysCounter key, final long value) {
        key2counterStats.get(key).set(value);
    }

    public BigInteger get(final KeysCounter key) {
        return key2counterStats.get(key).get();
    }

    public void print() {
        System.out.println(key2counterStats);
        //FileUtils.overwriteTextOnFile(Config.GASSERT_HOME + /*"/subjects/" + subjectFolder + */  "/output/evolution-iter" + iteration + ".info", MyGson.getInstance().toJson(key2counterStats));
    }

    public void writeStats(Writer out) throws IOException {
        JsonWriter writer = new JsonWriter(out);
        writer.beginObject();
        for (final KeysCounter key : KeysCounter.values()) {
            writer.name(key.name()).value(key2counterStats.get(key).get());
        }
        writer.endObject();
    }

}
