package ch.usi.methodtest;

import ch.usi.gassert.data.tree.Tree;
import ch.usi.gassert.data.tree.TreeReaderGAssert;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class ConjunctiveMRIPClause {

    public final Tree tree;

    public ConjunctiveMRIPClause(final Tree tree) {
        this.tree = tree;
    }

    public ConjunctiveMRIPClause(final ConjunctiveMRIPClause other) {
        this.tree = new Tree(other.getTree());
    }

    public static ConjunctiveMRIPClause read(final String line, final Map<String, Class<?>> variableTypes) throws IOException {
        return new ConjunctiveMRIPClause(TreeReaderGAssert.getTree(line, variableTypes));
    }

    public static ConjunctiveMRIPClause buildSimple(String left, String operator, String right) {
        return new ConjunctiveMRIPClause(new Tree(
                operator,
                new Tree(left, Tree.Type.BOOLEAN),
                new Tree(right, Tree.Type.BOOLEAN),
                Tree.Type.BOOLEAN
        ));
    }

    public void write(final Writer writer) throws IOException {
        writer.write(this.tree.toString());
    }

    public Tree getTree() {
        return this.tree;
    }

    @Override
    public String toString() {
        return this.tree.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConjunctiveMRIPClause) {
            final ConjunctiveMRIPClause other = (ConjunctiveMRIPClause) obj;
            return this.tree.equals(other.tree);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.tree.hashCode();
    }

}
