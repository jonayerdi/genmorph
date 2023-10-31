package ch.usi.gassert.util.random;

import ch.usi.gassert.util.Pair;
import org.jfree.threads.ReaderWriterLock;

import java.util.ArrayList;
import java.util.List;

/**
 * This class selects the object based on the weight (probability)
 *
 * The weights can be updated after construction
 *
 * @param <E>
 */
public class DynamicWeightedMap<E> implements IRandomSelector<E> {
    private final List<Pair<E, Weight>> elements = new ArrayList<>();
    private final ReaderWriterLock elementsLock = new ReaderWriterLock();

    private List<Pair<E, Double>> elementsFrozen = new ArrayList<>();
    private Double totalWeightFrozen = 0.0;
    private final ReaderWriterLock frozenLock = new ReaderWriterLock();

    public DynamicWeightedMap<E> add(final Weight weight, final E result) {
        this.elementsLock.lockWrite();
        weight.observers.add(this);
        this.elements.add(Pair.of(result, weight));
        this.elementsLock.unlock();
        return this;
    }
    public DynamicWeightedMap<E> add(final double weight, final E result) {
        if (weight > 0.0) {
            this.add(new Weight(weight), result);
        }
        return this;
    }
    public E next() {
        this.frozenLock.lockRead();
        final double selectedValue = MyRandom.getInstance().nextDouble() * this.totalWeightFrozen;
        Pair<E, Double> selected = null;
        double currentValue = 0.0;
        for (Pair<E, Double> e : this.elementsFrozen) {
            selected = e;
            currentValue += selected.snd;
            if (currentValue > selectedValue) {
                break;
            }
        }
        this.frozenLock.unlock();
        return selected != null ? selected.fst : null;
    }

    public DynamicWeightedMap<E> update() {
        this.elementsLock.lockRead();
        List<Pair<E, Double>> newElementsFrozen = new ArrayList<>(this.elements.size());
        double newTotalWeightFrozen = 0.0;
        for (Pair<E, Weight> e : this.elements) {
            double weight = e.snd.get();
            newElementsFrozen.add(new Pair<>(e.fst, weight));
            newTotalWeightFrozen += weight;
        }
        this.elementsLock.unlock();
        this.frozenLock.lockWrite();
        this.elementsFrozen = newElementsFrozen;
        this.totalWeightFrozen = newTotalWeightFrozen;
        this.frozenLock.unlock();
        return this;
    }

    public static class Weight implements Comparable<Weight> {
        private final List<DynamicWeightedMap<?>> observers = new ArrayList<>();
        private Double weight;
        public Weight(double weight) {
            this.weight = weight;
        }
        public double get() {
            return this.weight;
        }
        public void update(double weight) {
            this.weight = weight;
            this.observers.forEach(DynamicWeightedMap::update);
        }
        @Override
        public int compareTo(Weight o) {
            return this.weight.compareTo(o.weight);
        }
    }
}
