package org.mu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class Vectors {

    public static double distance(final List<Double> v1, final List<Double> v2,
                                  final List<Double> maxDistancesSquared) {
        if (v1.size() == 0) {
            return 0.0;
        }
        double distance = 0.0;
        for (int i = 0 ; i < v1.size() ; ++i) {
            final double diff = v1.get(i) - v2.get(i);
            final double maxDiffSquared = maxDistancesSquared.get(i);
            if (maxDiffSquared == 0.0) {
                if (diff != 0.0) {
                    throw new RuntimeException("Max diff squared for feature " + i + " is 0.0, but actual diff is" + diff);
                }
            } else {
                distance += diff * diff / maxDiffSquared;
            }
        }
        return Math.sqrt(distance / (double)v1.size());
    }

    public static double sumDistances(final List<Double> features,
                                     final Collection<List<Double>> featureVectors,
                                     final List<Double> maxDistancesSquared) {
        return featureVectors.stream()
                .mapToDouble(v -> distance(features, v, maxDistancesSquared))
                .sum();
    }

    public static List<Pair<Double, Double>> getThresholds(final Collection<List<Double>> featureVectors) {
        final int featuresCount = featureVectors.stream().findAny().map(List::size).orElse(0);
        final List<Pair<Double, Double>> thresholds = new ArrayList<>(featuresCount);
        for (int i = 0 ; i < featuresCount ; ++i) {
            thresholds.add(Pair.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        }
        for (final List<Double> featureVector : featureVectors) {
            if (featureVector.size() != featuresCount) {
                throw new RuntimeException("Feature vectors have different sizes");
            }
            for (int i = 0 ; i < featuresCount ; ++i) {
                final Pair<Double, Double> threshold = thresholds.get(i);
                final double value = featureVector.get(i);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    throw new RuntimeException("Invalid value for feature vector: " + value);
                }
                final double lower = Math.min(threshold.a, value);
                final double upper = Math.max(threshold.b, value);
                thresholds.set(i, Pair.of(lower, upper));
            }
        }
        return thresholds;
    }

    public static List<Double> getMaxDistancesSquared(final List<Pair<Double, Double>> thresholds) {
        return thresholds.stream()
                .map(p -> (p.b - p.a) * (p.b - p.a))
                .collect(Collectors.toList());
    }

    public static double diversity(final List<List<Double>> featureVectors) {
        return diversity(featureVectors, getMaxDistancesSquared(getThresholds(featureVectors)));
    }

    public static double diversity(final List<List<Double>> featureVectors, final List<Double> maxDistancesSquared) {
        return featureVectors.stream()
                .mapToDouble(v -> sumDistances(v, featureVectors, maxDistancesSquared))
                .sum();
    }

}
