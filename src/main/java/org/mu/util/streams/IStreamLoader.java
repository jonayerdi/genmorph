package org.mu.util.streams;

import java.io.BufferedReader;
import java.util.List;

public interface IStreamLoader {
    List<String> entries();
    BufferedReader load(String name);
    void close();
}
