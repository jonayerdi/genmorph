package org.mu.testcase.classification;

import org.mu.testcase.TabularData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TestClassifications extends TabularData<Classification> {

    public static final BiConsumer<Writer, Classification> writeValue = (w, v) -> {
        try {
            w.write(v.toChar());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
    public static final Function<String, Classification> readValue = Classification::fromString;
    public static final Classification[] ARRAY1 = new Classification[0];
    public static final Classification[][] ARRAY2 = new Classification[0][0];

    public TestClassifications(TabularData<Classification> src) {
        super(src);
    }

    public TestClassifications(String systemId, Map<String, Integer> testIds, Map<String, Integer> variables, Classification[][] data) {
        super(systemId, testIds, variables, data);
    }

    public static TestClassifications readFrom(final BufferedReader reader) throws IOException {
        return new TestClassifications(TabularData.readFrom(reader, readValue, ARRAY1, ARRAY2));
    }

    @Override
    public void writeTo(final Writer writer) throws IOException {
        this.writeTo(writer, writeValue);
    }

}
