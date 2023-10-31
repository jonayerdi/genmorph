package org.mu.testcase.input;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.mu.util.ISerializable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class TestParameters implements ISerializable {

    protected static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public final String id;
    private final Map<?, ?> params;

    public TestParameters(final String id, final Map<?, ?> params) {
        this.id = id;
        this.params = params;
    }

    public Object get(String... path) {
        Object node = this.params;
        for (Object key : path) {
            node = ((Map<?, ?>)node).get(key);
        }
        return node;
    }

    public static TestParameters fromJson(final String id, final JsonReader reader) throws IOException {
        Map<?, ?> map = GSON.fromJson(reader, Map.class);
        return new TestParameters(id, map);
    }

    public void toJson(final JsonWriter writer) throws IOException {
        GSON.toJson(this.params, Map.class, writer);
    }

    public static TestParameters readFrom(final String id, final BufferedReader reader) throws IOException {
        return fromJson(id, new JsonReader(reader));
    }

    @Override
    public void writeTo(Writer writer) throws IOException {
        this.toJson(new JsonWriter(writer));
    }
}
