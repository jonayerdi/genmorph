package org.mu.util.streams;

import java.io.File;

public class StreamLoaderFactory {

    public static IStreamLoader forPath(final String path) {
        final File file = new File(path);
        if (file.isDirectory()) {
            return new FileStreamLoader(path);
        } else if (file.isFile()) {
            return new ZipStreamLoader(path);
        } else {
            throw new RuntimeException("Path not found: " + path);
        }
    }

}
