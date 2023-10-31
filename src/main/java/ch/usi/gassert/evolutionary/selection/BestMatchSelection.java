package ch.usi.gassert.evolutionary.selection;

import ch.usi.gassert.Config;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.interpreter.AssertionManager;
import ch.usi.gassert.util.Bitmap;
import ch.usi.gassert.util.random.MyRandom;
import ch.usi.gassert.util.Pair;
import ch.usi.gassert.util.random.WeightedMap;

import java.util.List;

public class BestMatchSelection implements ISelection {

    @Override
    public Pair<Individual, Individual> select(final List<Individual> population, final AssertionManager.Type type) {
        if (population.isEmpty()) {
            throw new RuntimeException("population is empty");
        }
        // get the first at random
        final Individual s1 = MyRandom.getInstance().getRandomElementList(population);
        final Individual s2 = getBestMatch(s1, type);
        return new Pair<>(s1, s2);
    }


    private Individual getBestMatch(final Individual parent, AssertionManager.Type type) {
        type = MyRandom.getInstance().nextFloat() < Config.PROB_BEST_SELECTION_SAME_TYPE ? type : (type == AssertionManager.Type.FP ? AssertionManager.Type.FN : AssertionManager.Type.FP);
        // synchronized (AssertionManager.getDataToSolutions(type)) {
        final AssertionManager.Data bestData = findBestMatch(parent, type);
        return MyRandom.getInstance().getRandomElementList(AssertionManager.getDataToSolutions(type).get(bestData));
        //}
    }

    /**
     * return the best Data that maximize coverage
     * <p>
     * <p>
     * TODO too expensive
     *
     * @param parent
     * @param type
     * @return
     */
    private AssertionManager.Data findBestMatch(final Individual parent, final AssertionManager.Type type) {
        final AssertionManager.Data dataParent = new AssertionManager.Data(type.equals(AssertionManager.Type.FN)
                ? parent.idsFNGOOD : parent.idsFPGOOD);
        final WeightedMap<AssertionManager.Data> selector = new WeightedMap<>();
        for (final AssertionManager.Data data : AssertionManager.getDataToSolutions(type).keySet()) {
            final int countNewIds = countElementsInBNotInA(dataParent.getIds(), data.getIds());
            // consider only those that improve something
            if (countNewIds > 0) {
                selector.add(countNewIds, data);
            }
        }
        // select based on the probability
        return !selector.isEmpty() ? selector.next() : MyRandom.getInstance().getRandomElementCollection(AssertionManager.getDataToSolutions(type).keySet());
    }


    /**
     * thi is much faster than create a new list everytime
     *
     * @param aSet
     * @param bSet
     * @return
     */
    private int countElementsInBNotInA(final long[] aSet, final long[] bSet) {
        int count = 0;
        assert aSet.length == bSet.length;
        for(int i = 0 ; i < aSet.length ; ++i) {
            count += Bitmap.countSetBits(aSet[i] & ~bSet[i]);
        }
        return count;
    }
}
