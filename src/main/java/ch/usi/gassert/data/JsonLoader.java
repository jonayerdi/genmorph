package ch.usi.gassert.data;

import com.google.gson.stream.JsonReader;

import java.util.List;

public interface JsonLoader {
    List<String> entries();
    JsonReader load(String name);
}
