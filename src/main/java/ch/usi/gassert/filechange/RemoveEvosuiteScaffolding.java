package ch.usi.gassert.filechange;

import ch.usi.gassert.util.FileUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * this class simply remove the assertions from the test generated we need this because some tests might fail.
 * remove both junit and java assertions
 */
public class RemoveEvosuiteScaffolding {

    private RemoveEvosuiteScaffolding() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(final String[] args) {
        if (args.length == 0) {
            System.err.println("no evosuite scaffolding file");
            return;
        }
        removeEvosuiteScaffolding(args[0]);
    }

    public static void removeEvosuiteScaffolding(final String javaFilePath) {

        if (!new File(javaFilePath).isDirectory()) {
            if (javaFilePath.endsWith(".java") && javaFilePath.contains("GAssert/subjects")) {
                FileUtils.overwriteTextOnFile(javaFilePath, removeEvosuite(javaFilePath));
            }
        } else {
            for (final File f : new File(javaFilePath).listFiles()) {
                removeEvosuiteScaffolding(f.getAbsolutePath());
            }
        }
    }


    protected static String removeEvosuite(final String javaFilePath) {
        try {
            return removeEvosuite(Files.readAllLines(Paths.get(javaFilePath), Charset.defaultCharset()));
        } catch (final Exception e) {
            System.exit(-1);
            e.printStackTrace();
        }
        return "";
    }


    private static String removeEvosuite(final List<String> lines) {
        final StringBuilder sb = new StringBuilder();
        boolean done = false;
        for (final String l : lines) {
            if (!done) {
                if (l.startsWith("@RunWith(EvoRunner.class)")
                        || l.startsWith("@EvoRunnerParameters(") || l.startsWith("import org.evosuite.")) {
                    continue;
                }
                if (l.contains("extends ") && l.contains("_scaffolding")) {
                    sb.append(l.split(" extends")[0]);
                    sb.append("{");
                    sb.append(System.lineSeparator());
                    done = true;
                    continue;
                }
            }
            sb.append(l);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}




