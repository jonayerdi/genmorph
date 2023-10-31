package ch.usi.gassert.search;

public abstract class Objective implements Comparable<Objective> {
    /**
     * Negative value means this is better than other.
     */
    @Override
    public abstract int compareTo(Objective other);
}
