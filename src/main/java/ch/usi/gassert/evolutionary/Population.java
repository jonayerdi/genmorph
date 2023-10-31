package ch.usi.gassert.evolutionary;

import ch.usi.gassert.Config;
import ch.usi.gassert.Stats;
import ch.usi.gassert.Time;
import ch.usi.gassert.interpreter.AssertionManager;
import ch.usi.gassert.util.Statistics;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Population {

    List<Individual> population;

    private final int maxSize;

    public Population() {
        this(Config.POPULATION_SIZE);
    }

    public Population(final int maxSize) {
        this.population = new ArrayList<>(maxSize);
        this.maxSize = maxSize;
    }

    public Population(final Population copy) {
        this.population = new ArrayList<>(copy.population);
        this.maxSize = copy.maxSize;
    }

    public Population(final List<Individual> population, final int maxSize) {
        this.population = population;
        this.maxSize = maxSize;
    }

    public List<Individual> getPopulation() {
        return population;
    }

    public int size() {
        return population.size();
    }

    public boolean isFull() {
        return size() >= maxSize;
    }


    public void add(Individual sol, final BehaviourManager behaviourManager) {
        try {
            // Caching
            Individual solCached = null;
            if (Config.IS_CACHE_ENABLED) {
                solCached = AssertionManager.getStringToSolution().get(sol.getAssertionAsString());
                if (solCached != null) {
                    Stats.getInstance().increment(Stats.KeysCounter.numberCacheHitAssertion);
                    sol = solCached;
                }
            }
            if (solCached == null) {
                Stats.getInstance().increment(Stats.KeysCounter.numberCacheMissAssertion);
                Time.getInstance().start(Time.KeysCounter.computeFitnessFunction);
                sol.compute(behaviourManager);
                Time.getInstance().stop(Time.KeysCounter.computeFitnessFunction);
            }
            if (sol.isValid()) {
                population.add(sol);
            }
        } catch (final ArithmeticException e) { //TODO is necessary?
        }

    }


    public void addAll(final List<Individual> sol) {
        population.addAll(sol);
    }

    public void addPartition(final Population pop) {
        population.addAll(pop.population);
    }

    public Population clone() {
        return new Population(this);
    }

    public void clear() {
        population.clear();
    }

    public void recomputeFitness(final BehaviourManager behaviourManager) {
        for (final Individual individual : population) {
            individual.recompute(behaviourManager);
        }
    }

    public enum TypeStatistics {
        FN, FP, FPplusFN, FPmultipyFN, complexity;
    }

    public Map<TypeStatistics, Statistics> getStatisticsPopulation() {
        final DescriptiveStatistics summaryFN = new DescriptiveStatistics();
        final DescriptiveStatistics summaryFP = new DescriptiveStatistics();
        final DescriptiveStatistics summaryFPplusFN = new DescriptiveStatistics();
        final DescriptiveStatistics summaryFPmultiplyFN = new DescriptiveStatistics();
        final DescriptiveStatistics summaryComplexity = new DescriptiveStatistics();

        for (final Individual p : population) {
            summaryFN.addValue(p.getFitnessValueFN());
            summaryFP.addValue(p.getFitnessValueFP());
            summaryFPplusFN.addValue(p.getFitnessValueFN() + p.getFitnessValueFP());
            summaryFPmultiplyFN.addValue(p.getFitnessValueFN() * p.getFitnessValueFP());
            summaryComplexity.addValue(p.complexity);
        }
        final Map<TypeStatistics, Statistics> summaries = new HashMap<>();
        summaries.put(TypeStatistics.FN, new Statistics(summaryFN));
        summaries.put(TypeStatistics.FP, new Statistics(summaryFP));
        summaries.put(TypeStatistics.FPmultipyFN, new Statistics(summaryFPmultiplyFN));
        summaries.put(TypeStatistics.FPplusFN, new Statistics(summaryFPplusFN));
        summaries.put(TypeStatistics.complexity, new Statistics(summaryComplexity));
        return summaries;
    }


}