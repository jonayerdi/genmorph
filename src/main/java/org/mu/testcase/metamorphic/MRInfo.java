package org.mu.testcase.metamorphic;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.util.Objects;

public class MRInfo {

    public static final String SEPARATOR = ",";
    public static final String CSV_HEADER = "mr,source,followup";

    public final String mr;
    public final String source;
    public final String followup;

    public MRInfo(String mr, String source, String followup) {
        this.mr = mr;
        this.source = source;
        this.followup = followup;
    }

    public static MRInfo fromJson(final JsonReader reader) throws IOException {
        String mr = null;
        String source = null;
        String followup = null;
        reader.beginObject();
        while (!reader.peek().equals(JsonToken.END_OBJECT)) {
            switch (reader.nextName()) {
                case "mr":
                    mr = reader.nextString();
                    break;
                case "source":
                    source = reader.nextString();
                    break;
                case "followup":
                    followup = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        if (mr == null || source == null || followup == null) {
            throw new RuntimeException("Invalid MRinfo JSON, missing attribute(s)");
        }
        return new MRInfo(mr, source, followup);
    }

    public static MRInfo fromCsv(final String row) {
        final String[] cells = row.trim().split(SEPARATOR);
        assert cells.length == 3;
        return new MRInfo(cells[0], cells[1], cells[2]);
    }

    public void toJson(final JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("mr").value(mr);
        writer.name("source").value(source);
        writer.name("followup").value(followup);
        writer.endObject();
    }

    public void toCsv(final Writer writer) throws IOException {
        writer.write(mr);
        writer.write(SEPARATOR);
        writer.write(source);
        writer.write(SEPARATOR);
        writer.write(followup);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MRInfo) {
            MRInfo other = (MRInfo)o;
            return this.mr.equals(other.mr)
                    && this.source.equals(other.source)
                    && this.followup.equals(other.followup);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.join(SEPARATOR, mr, source, followup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mr, source, followup);
    }

}
