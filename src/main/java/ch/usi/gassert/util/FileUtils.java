package ch.usi.gassert.util;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {

    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    // Separators we use for file/directory naming schemes.
    // Some characters like '$' or '%' can break tools like Maven on Linux.
    // Keep in sync with the definitions for Python: scripts/config.py
    public static final String[] SEPARATORS = new String[] { "@", "?" };

    public static String[] splitExtension(final String filename) {
        final String[] parts = filename.split("\\.");
        if (parts.length < 2) {
            return new String[] { filename, "" };
        } else {
            return new String[] {
                Arrays.stream(parts, 0, parts.length - 1).collect(Collectors.joining(".")),
                "." + parts[parts.length - 1] 
            };
        }
    }

    public static void mkdirsFile(final File file) {
        File directory = file.getParentFile();
        if (directory != null) {
            directory.mkdirs();
        }
    }

    public static void mkdirsFile(final String pathFile) {
        mkdirsFile(new File(pathFile));
    }

    public static void mkdirs(final String pathFile) {
        new File(pathFile).mkdirs();
    }

    public static void appendTextOnFile(final String pathFile, final String text) {
        writeTextOnFile(pathFile, text, true, false);
    }

    public static void overwriteTextOnFile(final String pathFile, final String text) {
        writeTextOnFile(pathFile, text, false, false);
    }

    public static void writeTextOnFile(final String pathFile, final String text, final boolean append, final boolean mkdirs) {
        PrintWriter out = null;
        try {
            if (mkdirs) {
                mkdirsFile(pathFile);
            }
            out =  new PrintWriter(new BufferedWriter(new FileWriter(pathFile, append)));
            out.println(text);
        } catch(final IOException e) {
            System.err.println(e);
        } finally {
            try {
                out.close();
            } catch(Exception ignored) {}
        }
    }

    public static String readContentFile(final String path) {
        try {
            return readContentFile(new File(path));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String readContentFile(final File f) throws IOException {
        return String.join("\n", Files.readAllLines(Paths.get(f.getAbsolutePath())));
    }

    public static List<String> readContentFileList(final File f) throws IOException {
        return Files.readAllLines(Paths.get(f.getAbsolutePath()));
    }

    public static List<String> readContentFileList(final String path) {
        try {
            return readContentFileList(new File(path));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void delete(final File file) {
        try {
            final File[] contents = file.listFiles();
            if (contents != null) {
                for (final File child : contents) {
                    delete(child);
                }
            }
            file.delete();
        } catch(Exception ignored) {}
    }

    public static void delete(final String pathFile) {
        delete(new File(pathFile));
    }

    public static String joinClasspaths(String... classpaths) {
        return joinClasspaths(Arrays.stream(classpaths));
    }

    public static String joinClasspaths(Stream<String> classpaths) {
        final StringBuilder sb = new StringBuilder();
        classpaths.forEach(part -> sb.append(part).append(PATH_SEPARATOR));
        return sb.length() == 0 ? sb.toString() : sb.substring(0, sb.length() - PATH_SEPARATOR.length());
    }

    public static Stream<File> getFilesFromURLs(final Stream<URL> urls) {
        Stream.Builder<File> sb = Stream.builder();
        urls.forEach(url -> {
            try {
                sb.add(new File(url.toURI()));
            } catch (Exception ignored) {}
        });
        return sb.build();
    }

    public static String escapeNonAlphanum(final String s, final char replacement) {
        return s.chars()
            .mapToObj(c -> Character.isLetterOrDigit(c) ? c : replacement)
            .collect(
                () -> new StringBuilder(),
                (acc, c) -> acc.append((char)(int) c),
                (acc, other) -> acc.append(other)
            )
            .toString();
    }

}
