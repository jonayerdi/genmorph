package ch.usi.gassert.util.random;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * This class selects the object based on the weight (probability)
 *
 * @param <E>
 */
public class WeightedMap<E> implements IRandomSelector<E> {
    protected final NavigableMap<Double, E> map = new TreeMap<>();
    protected double total = 0.0;

    public WeightedMap<E> add(final double weight, final E result) {
        if (weight <= 0.0) {
            return this;
        }
        total += weight;
        map.put(total, result);
        return this;
    }

    public E next() {
        final double value = MyRandom.getInstance().nextDouble() * total;
        return map.higherEntry(value).getValue();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }
}

