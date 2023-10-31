package ch.usi.gassert.evolutionary.selection;

import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.interpreter.AssertionManager;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.gassert.util.Pair;

import java.util.List;

public class RandomSelection implements ISelection {

    @Override
    public Pair<Individual, Individual> select(final List<Individual> population, final AssertionManager.Type type) {
        if (population.isEmpty()) {
            throw new RuntimeException("population is empty");
        }
        return new Pair<>(MyRandom.getInstance().getRandomElementList(population),
                MyRandom.getInstance().getRandomElementList(population));
    }
}
