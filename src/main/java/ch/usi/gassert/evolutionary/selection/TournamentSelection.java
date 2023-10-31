package ch.usi.gassert.evolutionary.selection;

import ch.usi.gassert.Config;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.interpreter.AssertionManager;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.gassert.util.Pair;

import java.util.List;

public class TournamentSelection implements ISelection {

    @Override
    public Pair<Individual, Individual> select(final List<Individual> population, final AssertionManager.Type type) {
        if (population.isEmpty()) {
            throw new RuntimeException("population is empty");
        }
        return new Pair<>(getParent(population, type), getParent(population, type));
    }


    private Individual getParent(final List<Individual> population, final AssertionManager.Type type) {
        final Individual s1 = MyRandom.getInstance().getRandomElementList(population);
        final Individual s2 = MyRandom.getInstance().getRandomElementList(population);

        final int res;
        if (Config.IS_COMPLEXITY_PENALIZED) {
            res = type.equals(AssertionManager.Type.FN) ? s1.compareFNFPToPENALIZATION(s2) : s1.compareFPFNToPENALIZATION(s2);
        } else {
            res = type.equals(AssertionManager.Type.FN) ? s1.compareFNFPTo(s2) : s1.compareFPFNTo(s2);
        }
        //we want the smallest
        return res == 0 ? (MyRandom.getInstance().nextBoolean() ? s1 : s2) : (res > 0 ? s2 : s1);
    }
}