package org.mu.util.streams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileStreamLoader implements IStreamLoader {

    private final Path path;

    public FileStreamLoader(final String path) {
        this.path = new File(path).toPath();
    }

    @Override
    public List<String> entries() {
        List<String> files = new ArrayList<>();
        for (File child : Objects.requireNonNull(path.toFile().listFiles())) {
            if (child.isFile()) {
                files.add(child.getName());
            }
        }
        return files;
    }

    @Override
    public BufferedReader load(String name) {
        try {
            return new BufferedReader(new FileReader(path.resolve(name).toFile()));
        } catch (Exception e) {
            throw new RuntimeException("Could not load file: " + name, e);
        }
    }

    @Override
    public void close() {}

}
