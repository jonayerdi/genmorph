package ch.usi.gassert.util;

import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.HashMap;
import java.util.Map;

public class Statistics {

    public enum Values {
        mean, min, q1, median, q3, max, sdv, geometricMean, skewness, kurtosis
    }

    public Map<Values, Double> mapValuesToDouble;

    public Statistics(final DescriptiveStatistics stats) {
        mapValuesToDouble = new HashMap<>();
        mapValuesToDouble.put(Values.max, stats.getMax());
        mapValuesToDouble.put(Values.min, stats.getMin());
        mapValuesToDouble.put(Values.kurtosis, stats.getKurtosis());
        mapValuesToDouble.put(Values.mean, stats.getMean());
        mapValuesToDouble.put(Values.sdv, stats.getStandardDeviation());
        mapValuesToDouble.put(Values.geometricMean, stats.getGeometricMean());
        mapValuesToDouble.put(Values.skewness, stats.getSkewness());
        try {
            mapValuesToDouble.put(Values.q1, stats.getPercentile(25));
            mapValuesToDouble.put(Values.median, stats.getPercentile(50));
            mapValuesToDouble.put(Values.q1, stats.getPercentile(75));
        } catch (final MathIllegalStateException ex) {
            mapValuesToDouble.put(Values.q1, 0.0);
            mapValuesToDouble.put(Values.median, 0.0);
            mapValuesToDouble.put(Values.q3, 0.0);
        }
    }


}
