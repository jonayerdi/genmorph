package ch.usi.methodtest;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.util.LazyMap;
import ch.usi.gassert.util.Pair;

import java.io.*;
import java.util.*;

import static ch.usi.gassert.util.Assert.assertAlways;

/**
 Represents a MRIP with conjunctive form (AND of clauses over individual variables).
 */
public class ConjunctiveMRIP {

    public final ConjunctiveMRIPClause[] clauses;

    public ConjunctiveMRIP(final ConjunctiveMRIPClause[] clauses) {
        this.clauses = clauses;
    }

    public ConjunctiveMRIP(final ConjunctiveMRIP other) {
        this.clauses = Arrays.stream(other.clauses).map(ConjunctiveMRIPClause::new).toArray(ConjunctiveMRIPClause[]::new);
    }

    public static ConjunctiveMRIP read(final BufferedReader reader, final Map<String, Class<?>> variableTypes) throws IOException {
        return new ConjunctiveMRIP(readClauses(reader, variableTypes));
    }

    public static ConjunctiveMRIPClause[] readClauses(final BufferedReader reader, final Map<String, Class<?>> variableTypes) throws IOException {
        final List<ConjunctiveMRIPClause> clauses = new ArrayList<>();
        String line = reader.readLine();
        while (line != null) {
            final String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                clauses.add(ConjunctiveMRIPClause.read(trimmedLine, variableTypes));
            }
            line = reader.readLine();
        }
        return clauses.toArray(new ConjunctiveMRIPClause[0]);
    }

    public static ConjunctiveMRIP read(final BufferedReader reader) throws IOException {
        return read(reader, new LazyMap<>(k -> Integer.class));
    }

    public void write(final Writer writer) throws IOException {
        writeClauses(writer, this.clauses);
    }

    public static void writeClauses(final Writer writer, final Collection<ConjunctiveMRIPClause> clauses) throws IOException {
        for (final ConjunctiveMRIPClause clause : clauses) {
            clause.write(writer);
            writer.write("\n");
        }
    }

    public static void writeClauses(final Writer writer, final ConjunctiveMRIPClause[] clauses) throws IOException {
        for (final ConjunctiveMRIPClause clause : clauses) {
            clause.write(writer);
            writer.write("\n");
        }
    }

    public Tree buildTree() {
        return buildTree(this.clauses);
    }

    public static Tree buildTree(final ConjunctiveMRIPClause[] clauses) {
        switch (clauses.length) {
            case 0:
                return new Tree("true", null, null, Tree.Type.BOOLEAN);
            case 1:
                return clauses[0].getTree();
            default:
                final Iterator<ConjunctiveMRIPClause> clausesIter = Arrays.stream(clauses).iterator();
                Tree tree = new Tree(
                        "&&",
                        clausesIter.next().getTree(),
                        clausesIter.next().getTree(),
                        Tree.Type.BOOLEAN
                );
                while (clausesIter.hasNext()) {
                    tree = new Tree(
                            "&&",
                            tree,
                            clausesIter.next().getTree(),
                            Tree.Type.BOOLEAN
                    );
                }
                return tree;
        }
    }

    @Override
    public String toString() {
        return this.buildTree().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConjunctiveMRIP) {
            final ConjunctiveMRIP other = (ConjunctiveMRIP) obj;
            return this.toString().equals(other.toString());
            // The following does not work :(
            //return this.clauses.equals(other.clauses);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.clauses.hashCode();
    }

    public static void writeMRIPs(final Iterator<Pair<String, ConjunctiveMRIP>> mrips, final Writer writer) throws IOException {
        while (mrips.hasNext()) {
            final Pair<String, ConjunctiveMRIP> pair = mrips.next();
            writer.write(pair.fst);
            writer.write('\n');
            writer.write(pair.snd.toString());
            writer.write('\n');
        }
    }

    public static void generateMRIPs(final File inDir, final File outFile) {
        final Iterator<Pair<String, ConjunctiveMRIP>> mrips =
                Arrays.stream(Objects.requireNonNull(inDir.list((dir, name) -> name.endsWith(".cmrip"))))
                    .map(filename -> {
                        try {
                            final File filepath = inDir.toPath().resolve(filename).toFile();
                            final String mripName = filename.substring(0, filename.length() - ".cmrip".length());
                            return Pair.of(mripName, ConjunctiveMRIP.read(new BufferedReader(new FileReader(filepath))));
                        } catch (IOException e) {
                            throw new RuntimeException("Exception while handling file: " + filename, e);
                        }
                    }).iterator();
        try (final Writer outWriter = new FileWriter(outFile)) {
            writeMRIPs(mrips, outWriter);
        } catch (IOException e) {
            throw new RuntimeException("Exception while writing to file: " + outFile, e);
        }
    }

    public static void generateMRIPs(Iterator<String> args) {
        final File inDir = new File(args.next());
        final File outFile = new File(args.next());
        outFile.getParentFile().mkdirs();
        assertAlways(!args.hasNext(), "Too many arguments passed");
        generateMRIPs(inDir, outFile);
    }

    public static void main(String[] args) {
        final Iterator<String> argsIter = Arrays.stream(args).iterator();
        final String command = argsIter.next();
        switch (command.toLowerCase()) {
            case "generatemrips":
                generateMRIPs(argsIter);
                break;
            default:
                System.err.println("Unknown command: " + command);
                System.exit(1);
        }
    }

}
