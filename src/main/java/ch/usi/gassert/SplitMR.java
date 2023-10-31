package ch.usi.gassert;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeReaderGAssert;
import ch.usi.gassert.util.FileUtils;
import ch.usi.gassert.util.LazyMap;
import ch.usi.gassert.util.Pair;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import static ch.usi.gassert.util.Assert.assertAlways;

/**
 * Extract the Input Relation and Output Relation from a given Metamorphic Relation
 */
public class SplitMR {

    public static Pair<Tree, Tree> splitMR(final Tree fullMR) {
        assertAlways(fullMR.getValue().equals("=>"), "Root node of a full MR should be \"=>\"");
        return Pair.of(fullMR.getLeft(), fullMR.getRight());
    }

    public static Pair<String, String> splitMR(final String fullMR) {
        return splitMR(TreeReaderGAssert.getTree(fullMR, new LazyMap<>(v -> Integer.class)))
                .map(Object::toString, Object::toString);
    }

    public static void writeSplitMRFile(final String fullMRFilename, final String irFilename, final String orFilename) {
        splitMR(FileUtils.readContentFile(fullMRFilename)).forEach(
                ir -> FileUtils.writeTextOnFile(irFilename, ir, false, true),
                or -> FileUtils.writeTextOnFile(orFilename, or, false, true)
        );
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Wrong number of parameters: 3 arguments expected, got " + args.length);
            System.err.println("Full MR file");
            System.err.println("Input Relation file");
            System.err.println("Output Relation file");
            System.exit(1);
        }
        final Iterator<String> argsIter = Arrays.stream(args).iterator();
        final String fullMRFilename = argsIter.next();
        final String irFilename = argsIter.next();
        final String orFilename = argsIter.next();
        writeSplitMRFile(fullMRFilename, irFilename, orFilename);
    }

}
