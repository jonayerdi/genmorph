package ch.usi.gassert.evolutionary;

import ch.usi.gassert.Config;
import ch.usi.gassert.interpreter.CriteriaCompareIndividuals;

import java.util.List;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Migration {


    static Map<CriteriaCompareIndividuals, List<Individual>> criteria2bestIndividualsFP;
    static Map<CriteriaCompareIndividuals, List<Individual>> criteria2bestIndividualsFN;

    static {
        criteria2bestIndividualsFN = new ConcurrentHashMap<>();
        criteria2bestIndividualsFP = new ConcurrentHashMap<>();
        for (final CriteriaCompareIndividuals criteria : CriteriaCompareIndividuals.values()) {
            criteria2bestIndividualsFN.put(criteria, new ArrayList<>());
            criteria2bestIndividualsFP.put(criteria, new ArrayList<>());

        }
    }


    static synchronized public void store(final Population population, final int index, final int gen) {
        if (index == 0) {
            storeFPorFN(population, criteria2bestIndividualsFP, gen);
        } else {
            storeFPorFN(population, criteria2bestIndividualsFN, gen);
        }
    }


    static private void storeFPorFN(final Population population, final Map<CriteriaCompareIndividuals,
            List<Individual>> bestIndividuals, final int gen) {
        final List<Individual> p = population.getPopulation();
        for (final CriteriaCompareIndividuals criteria : CriteriaCompareIndividuals.values()) {
            final List<Individual> toSort = new ArrayList<>(p.size());
            toSort.addAll(p);
            Collections.sort(toSort, criteria.getComparator(gen));
            bestIndividuals.get(criteria).clear();
            bestIndividuals.put(criteria, toSort.subList(0, Config.sizeForMigration));
        }
    }


    static synchronized public List<Individual> getEliteFromOtherPopulation(final int index) {
        if (index == 0) {
            return getFPorFN(criteria2bestIndividualsFN);
        } else {
            return getFPorFN(criteria2bestIndividualsFP);
        }
    }


    static private List<Individual> getFPorFN(final Map<CriteriaCompareIndividuals, List<Individual>> bestIndividuals) {
        final List<Individual> elite = new ArrayList<>(Config.sizeForMigration * CriteriaCompareIndividuals.values().length);
        for (final CriteriaCompareIndividuals criteria : CriteriaCompareIndividuals.values()) {
            elite.addAll(bestIndividuals.get(criteria));
        }
        return elite;
    }

    public static void recomputeFitness(final BehaviourManager behaviourManager) {
        for (final List<Individual> best : criteria2bestIndividualsFP.values()) {
            for (final Individual individual : best) {
                individual.recompute(behaviourManager);
            }
        }
        for (final List<Individual> best : criteria2bestIndividualsFN.values()) {
            for (final Individual individual : best) {
                individual.recompute(behaviourManager);
            }
        }
    }
}
