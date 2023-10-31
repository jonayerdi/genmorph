package ch.usi.gassert.evolutionary;

import ch.usi.gassert.Config;
import ch.usi.gassert.interpreter.CriteriaCompareIndividuals;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Elitism {

    //HashSet<String> isAlreadyMinimized;
    Map<CriteriaCompareIndividuals, List<Individual>> criteria2bestIndividuals;

    public Individual bestOfTheBest;
    public int generation;

    public Elitism() {
        criteria2bestIndividuals = new HashMap<>();
        for (final CriteriaCompareIndividuals criteria : CriteriaCompareIndividuals.values()) {
            criteria2bestIndividuals.put(criteria, new ArrayList<>(Config.sizeForElitism));
        }
        //isAlreadyMinimized = new HashSet<>();
    }

    public List<Individual> getAllBestIndividuals(final int gen) {
        final List<Individual> toSort = new ArrayList<>(criteria2bestIndividuals.size() * Config.sizeForElitism);
        for (final CriteriaCompareIndividuals criteria : criteria2bestIndividuals.keySet()) {
            toSort.addAll(criteria2bestIndividuals.get(criteria));
        }
        if (toSort.isEmpty()) {
            return toSort;
        }
        final Comparator<Individual> comparator = CriteriaCompareIndividuals.FP_FN_complexity.getComparator(gen);
        toSort.sort(comparator);
        final Individual bestOfBestIndividual = toSort.get(0);
        if (bestOfTheBest == null || comparator.compare(bestOfBestIndividual, bestOfTheBest) < 0) {
            bestOfTheBest = bestOfBestIndividual;
            generation = gen;
        }
        return toSort;
    }

    public List<Individual> getBestIndividuals(final int gen) {
        return Elitism.distinctIndividuals(this.getAllBestIndividuals(gen))
                .limit(Config.countBestIndividuals)
                .collect(Collectors.toList());
    }

    public Individual getBestOfBestIndividual(final int gen) {
        this.getAllBestIndividuals(gen);
        return bestOfTheBest;
    }

    public List<Individual> getElite(CriteriaCompareIndividuals criteria) {
        return criteria2bestIndividuals.get(criteria);
    }

    public List<Individual> updateAndGetElitism(final Population population, final int gen) {

        final List<Individual> elite = new ArrayList<>(Config.sizeForElitism * criteria2bestIndividuals.values().size());
        final List<Individual> p = population.getPopulation();
        for (final CriteriaCompareIndividuals criteria : CriteriaCompareIndividuals.values()) {
            final List<Individual> toSort = new ArrayList<>(p.size() + Config.sizeForElitism);
            toSort.addAll(p);
            final List<Individual> currentElite = criteria2bestIndividuals.get(criteria);
            toSort.addAll(currentElite);
            toSort.sort(criteria.getComparator(gen));
            criteria2bestIndividuals.get(criteria).clear();
            criteria2bestIndividuals.put(
                    criteria,
                    Elitism.distinctIndividuals(toSort)
                            .filter(Individual::isEliteValid)
                            .limit(Config.sizeForElitism)
                            .collect(Collectors.toList())
            );
            elite.addAll(criteria2bestIndividuals.get(criteria));
        }

        return elite;
    }

    public void recomputeFitness(final BehaviourManager behaviourManager) {
        for (final List<Individual> best : criteria2bestIndividuals.values()) {
            for (final Individual individual : best) {
                individual.recompute(behaviourManager);
            }
        }
    }

    public static Stream<Individual> distinctIndividuals(final List<Individual> individuals) {
        if (Config.ELITE_UNIQUE_SIGNATURES) {
            final Stream.Builder<Individual> builder = Stream.builder();
            final Set<Integer> signatures = new HashSet<>(256);
            for (final Individual individual : individuals) {
                final int signature = Arrays.hashCode(individual.idsFNGOOD);
                    if (!signatures.contains(signature)) {
                        builder.accept(individual);
                        signatures.add(signature);
                    }
            }
            return builder.build().distinct();
        } else {
            return individuals.stream().distinct();
        }
    }

}
