package org.mu.testcase.metamorphic;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.mu.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MRInfoDB {

    private final List<MRInfo> mrinfos;

    public MRInfoDB() {
        this(new ArrayList<>());
    }

    public MRInfoDB(List<MRInfo> mrinfos) {
        this.mrinfos = mrinfos;
    }

    public static MRInfoDB fromJson(final JsonReader reader) throws IOException {
        List<MRInfo> mrinfos = new ArrayList<>();
        reader.beginArray();
        while (!reader.peek().equals(JsonToken.END_ARRAY)) {
            mrinfos.add(MRInfo.fromJson(reader));
        }
        reader.endArray();
        return new MRInfoDB(mrinfos);
    }

    public static MRInfoDB fromCsv(final BufferedReader reader) throws IOException {
        List<MRInfo> mrinfos = new ArrayList<>();
        String line = reader.readLine();
        assert line.equals(MRInfo.CSV_HEADER);
        line = reader.readLine();
        while (line != null) {
            if (!StringUtils.isBlank(line)) {
                mrinfos.add(MRInfo.fromCsv(line));
            }
            line = reader.readLine();
        }
        return new MRInfoDB(mrinfos);
    }

    public void toJson(final JsonWriter writer) throws IOException {
        writer.beginArray();
        for (MRInfo mrinfo : this.mrinfos) {
            mrinfo.toJson(writer);
        }
        writer.endArray();
    }

    public void toCsv(final Writer writer) throws IOException {
        writer.write(MRInfo.CSV_HEADER);
        writer.write('\n');
        for (MRInfo mrinfo : this.mrinfos) {
            mrinfo.toCsv(writer);
            writer.write('\n');
        }
    }

    public List<MRInfo> getMRInfos() {
        return this.mrinfos;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MRInfoDB) {
            MRInfoDB other = (MRInfoDB)o;
            return this.mrinfos.equals(other.mrinfos);
        }
        return false;
    }

}
