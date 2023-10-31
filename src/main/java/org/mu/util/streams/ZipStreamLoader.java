package org.mu.util.streams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipStreamLoader implements IStreamLoader {

    private final ZipFile zipFile;

    public ZipStreamLoader(String path) {
        try {
            this.zipFile = new ZipFile(path);
        } catch (Exception e) {
            throw new RuntimeException("Could not load zip file: " + path, e);
        }
    }

    @Override
    public List<String> entries() {
        List<String> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> entriesList = zipFile.entries();
        while (entriesList.hasMoreElements()) {
            ZipEntry entry = entriesList.nextElement();
            entries.add(entry.getName());
        }
        return entries;
    }

    @Override
    public BufferedReader load(String name) {
        try {
            ZipEntry entry = zipFile.getEntry(name);
            return new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
        } catch (Exception e) {
            throw new RuntimeException("Could not load zip entry: " + name, e);
        }
    }

    @Override
    public void close() {
        try {
            zipFile.close();
        } catch (Exception ignored) {}
    }

}
