package ch.usi.gassert.data.tree;

import ch.usi.methodtest.ConjunctiveMRIP;
import ch.usi.methodtest.ConjunctiveMRIPClause;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import static ch.usi.gassert.util.Assert.assertAlways;

/**
 * Expression composed of a conjunction of boolean clauses over a single variable. Used for MRIPs.
 *
 * Their form should be: [VARIABLE_FOLLOWUP] [OPERATOR] [F(VARIABLE_SOURCE)]
 *
 * These MRIPs cannot be freely modified like a normal Tree, but it can be relaxed by either:
 *  1. Dropping some clauses.
 *  2. Relaxing the OPERATOR from some clauses (e.g. change == to <= or >=).
 */
public class ConjunctiveClausesTree implements ITree {

    // MRIP clauses
    public final ConjunctiveMRIPClause[] clauses;
    // Bitmap indicating whether the clauses are selected
    // Since we only have a single long, ConjunctiveMRIPs may only have up to 64 clauses
    public final long clauseSelected;

    public ConjunctiveClausesTree(final ConjunctiveMRIPClause[] clauses, final long clauseSelected) {
        assertAlways(clauses.length <= Long.SIZE, "Too many clauses");
        assertAlways((clauseSelected & -(1L << clauses.length)) == 0L,
                "Invalid isClauseSelected for " + clauses.length + " clauses: 0x"
                        + Long.toString(clauseSelected, 16).toUpperCase());
        this.clauses = Arrays.copyOf(clauses, clauses.length);
        this.clauseSelected = clauseSelected;
    }

    public ConjunctiveClausesTree(final ConjunctiveMRIPClause[] clauses) {
        this(clauses, (1L << clauses.length) - 1);
    }

    public ConjunctiveClausesTree(final ConjunctiveClausesTree other) {
        this.clauses = new ConjunctiveMRIPClause[other.clauses.length];
        for (int i = 0 ; i < other.clauses.length ; ++i) {
            this.clauses[i] = new ConjunctiveMRIPClause(other.clauses[i]);
        }
        this.clauseSelected = other.clauseSelected;
    }

    @Override
    public Tree asTree() {
        return ConjunctiveMRIP.buildTree(
                IntStream.range(0, this.clauses.length)
                    .filter(index -> (this.clauseSelected & 1L << index) != 0)
                    .mapToObj(index -> this.clauses[index])
                    .toArray(ConjunctiveMRIPClause[]::new)
        );
    }

    @Override
    public ITree cloneTree() {
        return new ConjunctiveClausesTree(this);
    }

    @Override
    public Integer getNumberOfNodes() {
        return Arrays.stream(clauses).mapToInt(c -> c.tree.getNumberOfNodes()).sum();
    }

    @Override
    public Tree.Type getType() {
        return Tree.Type.BOOLEAN;
    }

    @Override
    public String toString() {
        return this.asTree().toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConjunctiveClausesTree)) {
            return false;
        }
        final ConjunctiveClausesTree other = (ConjunctiveClausesTree) o;
        return this.clauseSelected == other.clauseSelected &&
                Arrays.equals(this.clauses, other.clauses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clauseSelected, Arrays.hashCode(clauses));
    }

}
