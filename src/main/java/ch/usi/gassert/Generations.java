package ch.usi.gassert;

import ch.usi.gassert.evolutionary.Elitism;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.evolutionary.Population;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Generations {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.00000");

    private static Generations instance = null;

    public static final class Data {
        public boolean foundBetter;
        public double bestFP;
        public double bestFN;
        public double avgFitnessFP = 0.0;
        public double avgFitnessFN = 0.0;
        public double avgComplexity = 0.0;
        public double avgFitnessFPplusFN = 0.0;
        public double avgFitnessFPtimesFN = 0.0;
        public Data() {
            foundBetter = false;
            bestFP = -1.0;
            bestFN = -1.0;
        }
        public Data(Population population, Elitism elitism, boolean foundBetter, int generation) {
            final Individual bestOfTheBest = elitism.getBestOfBestIndividual(generation);
            bestFP = bestOfTheBest.fitnessValueFP;
            bestFN = bestOfTheBest.fitnessValueFN;
            for (Individual individual : population.getPopulation()) {
                double FP = individual.getFitnessValueFP();
                double FN = individual.getFitnessValueFN();
                avgFitnessFP += FP;
                avgFitnessFN += FN;
                avgComplexity += individual.complexity;
                avgFitnessFPplusFN += (FP + FN);
                avgFitnessFPtimesFN += (FP * FN);
            }
            int size = population.getPopulation().size();
            avgFitnessFP /= size;
            avgFitnessFN /= size;
            avgComplexity /= size;
            avgFitnessFPplusFN /= size;
            avgFitnessFPtimesFN /= size;
            this.foundBetter = foundBetter;
        }
    }

    private final List<List<Data>> generationStats;

    public static void init (int evolveInstances) {
        instance = new Generations(evolveInstances);
    }

    private Generations(int evolveInstances) {
        generationStats = new ArrayList<>(evolveInstances);
        for (int i = 0 ; i < evolveInstances ; ++i) {
            generationStats.add(new ArrayList<>());
        }
    }

    public static Generations getInstance() {
        return instance;
    }

    public void addGeneration(int evolveInstanceIndex, Population population, Elitism elitism, boolean newBest, int generation) {
        Data data = new Data();
        try {
            data = new Data(population, elitism, newBest, generation);
        } catch (Exception ignore) {}
        generationStats.get(evolveInstanceIndex).add(data);
    }

    public List<List<Data>> getGenerationStats() {
        return generationStats;
    }

    public void writeStats(PrintStream out) {
        assert generationStats.size() > 0;
        int maxGeneration = generationStats.stream()
                .mapToInt(List::size)
                .max()
                .orElseThrow(() -> new RuntimeException("Empty Generations instance"));
        final String header = "newBestIndividual[%d],bestFP[%d],bestFN[%d],avgFitnessFP[%d],avgFitnessFN[%d],avgComplexity[%d],avgFitnessFPplusFN[%d],avgFitnessFPtimesFN[%d],";
        for (int evolveInstance = 0 ; evolveInstance < generationStats.size() ; ++evolveInstance) {
            assert generationStats.get(evolveInstance).size() == maxGeneration;
            out.printf(header, evolveInstance, evolveInstance, evolveInstance, evolveInstance, evolveInstance, evolveInstance, evolveInstance, evolveInstance);
        }
        out.println();
        final String datarow = "%s,%s,%s,%s,%s,%s,%s,%s,";
        for (int generation = 0 ; generation < maxGeneration ; ++generation) {
            for (List<Data> generationStat : generationStats) {
                try {
                    Data data = generationStat.get(generation);
                    String newBestIndividual = data.foundBetter ? "1" : "0";
                    String bestFP = DECIMAL_FORMAT.format(data.bestFP);
                    String bestFN = DECIMAL_FORMAT.format(data.bestFN);
                    String avgFitnessFP = DECIMAL_FORMAT.format(data.avgFitnessFP);
                    String avgFitnessFN = DECIMAL_FORMAT.format(data.avgFitnessFN);
                    String avgComplexity = DECIMAL_FORMAT.format(data.avgComplexity);
                    String avgFitnessFPplusFN = DECIMAL_FORMAT.format(data.avgFitnessFPplusFN);
                    String avgFitnessFPtimesFN = DECIMAL_FORMAT.format(data.avgFitnessFPtimesFN);
                    out.printf(datarow, newBestIndividual, bestFP, bestFN, avgFitnessFP, avgFitnessFN, avgComplexity, avgFitnessFPplusFN, avgFitnessFPtimesFN);
                } catch (Exception ignored) {
                    // FIXME: Sometimes the stats for the last generation of some Evolve instance are missing,
                    //  probably because of the way the task was terminated
                    out.printf(datarow, "?", "?", "?", "?", "?", "?", "?", "?");
                }
            }
            out.println();
        }
    }

}
