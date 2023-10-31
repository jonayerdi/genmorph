package org.mu.testcase.evaluation;

import org.mu.testcase.TabularData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TestEvaluationResults extends TabularData<TestResult> {

    public static final BiConsumer<Writer, TestResult> writeValue = (w, v) -> {
        try {
            w.write(v.toChar());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
    public static final Function<String, TestResult> readValue = TestResult::fromString;
    public static final TestResult[] ARRAY1 = new TestResult[0];
    public static final TestResult[][] ARRAY2 = new TestResult[0][0];

    public TestEvaluationResults(TabularData<TestResult> src) {
        super(src);
    }

    public TestEvaluationResults(String oracleId, Map<String, Integer> testIds, Map<String, Integer> systemIds, TestResult[][] data) {
        super(oracleId, testIds, systemIds, data);
    }

    public static TestEvaluationResults readFrom(final BufferedReader reader) throws IOException {
        return new TestEvaluationResults(TabularData.readFrom(reader, readValue, ARRAY1, ARRAY2));
    }

    @Override
    public void writeTo(final Writer writer) throws IOException {
        this.writeTo(writer, writeValue);
    }

}
