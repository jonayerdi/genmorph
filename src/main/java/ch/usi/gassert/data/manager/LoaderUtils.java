package ch.usi.gassert.data.manager;

import org.mu.testcase.classification.TestClassifications;
import org.mu.util.streams.IStreamLoader;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class LoaderUtils {

    public static Map<String, TestClassifications> loadClassifications(final IStreamLoader dataSource) {
        return loadClassifications(dataSource, e -> e.endsWith(".classifications.csv"));
    }

    public static Map<String, TestClassifications> loadClassifications(final IStreamLoader dataSource,
                                                                       final Predicate<String> isClassificationsFile) {
        Map<String, TestClassifications> systemClassifications = new HashMap<>();
        for (String entry : dataSource.entries()) {
            if (isClassificationsFile.test(entry)) {
                try {
                    BufferedReader reader = dataSource.load(entry);
                    TestClassifications classifications = TestClassifications.readFrom(reader);
                    reader.close();
                    systemClassifications.put(classifications.id, classifications);
                } catch (Exception e) {
                    throw new RuntimeException("Error loading classification entry: " + entry, e);
                }
            }
        }
        return systemClassifications;
    }

}
