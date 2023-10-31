package ch.usi.gassert.util.random;

import ch.usi.gassert.Config;

import java.util.*;

import static ch.usi.gassert.util.Assert.assertAlways;

public class MyRandom extends Random {

    private static final long serialVersionUID = 1L;

    static private MyRandom instance;

    private MyRandom() {
        super(Config.seed);
    }

    public static MyRandom getInstance() {
        if (instance == null) {
            instance = new MyRandom();
        }
        return instance;
    }

    public <E> int getRandomPositionList(final List<E> l) {
        if (l.size() == 0) {
            return -1;
        }
        return nextInt(l.size());
    }

    public <E> E getRandomElementList(final List<E> l) {
        return l.get(nextInt(l.size()));
    }

    public <E> E getRandomElementCollection(final Collection<E> s) {
        final int position = nextInt(s.size());
        int i = 0;
        for (final E o : s) {
            if (i == position) {
                return o;
            }
            i++;
        }
        return null;
    }

    public <E> E getRandomElementArray(final E array[]) {
        return array[nextInt(array.length)];
    }

    public <E> List<E> getRandomFirstSegment(final List<E> l) {
        return new LinkedList<E>(l.subList(0, getRandomPositionList(l)));
    }

    public <E> List<E> getRandomSecondSegment(final List<E> l) {
        return new LinkedList<E>(l.subList(getRandomPositionList(l), l.size() - 1));
    }

    public Set<Integer> getRandomIndices(final int collectionSize, final int indicesCount, final Set<Integer> alreadySelected) {
        assertAlways(indicesCount + alreadySelected.size() <= collectionSize, "collectionSize=" + collectionSize + ", alreadySelectedSize=" + alreadySelected.size() + ", indicesCount=" + indicesCount);
        final Set<Integer> indices = new HashSet<>(Math.max(alreadySelected.size(), indicesCount));
        indices.addAll(alreadySelected);
        while (indices.size() < indicesCount) {
            final int index = nextInt(collectionSize);
            indices.add(index);
        }
        return indices;
    }

    public Set<Integer> getRandomIndices(final int collectionSize, final int indicesCount) {
        return getRandomIndices(collectionSize, indicesCount, new HashSet<>());
    }

    /**
     * wheel based on fitnessvalue
     **/
    public int choosePositionListOnWeight(final List<Double> list) {

        double completeWeight = 0.0;
        for (final Double d : list) {
            completeWeight += d + 0.01;
        }
        final double r = nextDouble() * completeWeight;
        double countWeight = 0.0;
        int i = 0;
        for (final Double d : list) {
            countWeight += d + 0.01;
            if (countWeight >= r) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Something is wrong");
    }

    private int getRandomIntegerInRange(final int min, final int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return nextInt((max - min) + 1) + min;
    }

}
