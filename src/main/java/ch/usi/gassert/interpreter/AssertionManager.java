package ch.usi.gassert.interpreter;


import ch.usi.gassert.Config;
import ch.usi.gassert.evolutionary.Individual;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class store evaluation information
 * <p>
 * 1) to enable smart mutation/selection and crossover
 * 2) cache results for performance reasons
 */
public class AssertionManager {

    public enum Type {
        FP, FN
    }

    /**
     * cached solutions with a maximum size
     */
    private static Map<String, Individual> stringToSolution;
    private static Map<Data, List<Individual>> fnDataToSolutions;
    private static Map<Data, List<Individual>> fpDataToSolutions;

    static {
        fnDataToSolutions = new ConcurrentHashMap<>();
        fpDataToSolutions = new ConcurrentHashMap<>();
        stringToSolution = new ConcurrentHashMap<>();
    }

    /**
     * cache data for ids
     *
     * @param type
     * @param individual
     */
    public static void cacheIdsData(final Type type, final Individual individual, final int gen) {
        final Data data = new Data(type.equals(Type.FN) ? individual.idsFNGOOD : individual.idsFPGOOD);
        final Map<Data, List<Individual>> dataToAssertions = getListBasedOn(type);
        dataToAssertions.putIfAbsent(data, new ArrayList<>());
        dataToAssertions.get(data).add(individual);
        if (dataToAssertions.size() >= Config.MAX_SIZE_FOR_BEST_MATCHING_LIST) {
            Collections.sort(dataToAssertions.get(data), CriteriaCompareIndividuals.FNplusFP_complexity.getComparator(gen));
            dataToAssertions.remove(dataToAssertions.size() - 1);
        }
    }

    public static void cacheAssertion(final Individual individual) {
        if (stringToSolution.size() < Config.MAX_SIZE_CACHE) {
            stringToSolution.putIfAbsent(individual.getAssertionAsString(), individual);
        }
    }

    public static void getCacheAssertion(final String assertion) {
        if (stringToSolution.containsKey(assertion)) {
            stringToSolution.get(assertion);
        }
    }

    public static boolean containsAssertion(final String assertion) {
        return stringToSolution.containsKey(assertion);
    }

    public static Map<String, Individual> getStringToSolution() {
        return stringToSolution;
    }

    public static Map<Data, List<Individual>> getDataToSolutions(final Type type) {
        return getListBasedOn(type);
    }


    private static Map<Data, List<Individual>> getListBasedOn(final Type type) {
        return type.equals(Type.FN) ? fnDataToSolutions : fpDataToSolutions;
    }

    public static int getNumberCachedAssertions() {
        return stringToSolution.size();
    }

    public static void clearAll() {
        fnDataToSolutions.clear();
        fpDataToSolutions.clear();
        stringToSolution.clear();
    }

    /**
     * For debugging purposes
     */
    public static void checkIntegrity() {
        for (final String assertionString : stringToSolution.keySet()) {
            final Individual individual = stringToSolution.get(assertionString);
            if (!individual.getAssertionAsString().equals(assertionString)) {
                throw new RuntimeException("Cached data corrupted:" +
                        "\nK: " + assertionString +
                        "\nV: " + individual.getAssertionAsString());
            }
        }
    }

    public static class Data {
        private final long[] ids;

        public Data(final long[] ids) {
            this.ids = ids;
        }


        public long[] getIds() {
            return ids;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Data)) {
                return false;
            }
            final Data data = (Data) o;
            return Arrays.equals(getIds(), data.getIds());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getIds());
        }
    }
}




