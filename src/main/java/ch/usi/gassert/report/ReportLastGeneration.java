package ch.usi.gassert.report;

import ch.usi.gassert.Stats;
import ch.usi.gassert.Time;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.interpreter.CriteriaCompareIndividuals;
import ch.usi.gassert.util.TimeUtils;

import java.util.List;

public class ReportLastGeneration {


    public static String getInfoLastGeneration(final List<Report> reports) {
        return getInfoLastGeneration(reports.get(reports.size() - 1));
    }

    private static String getInfoLastGeneration(final Report report) {
        final StringBuilder sb = new StringBuilder();
        sb.append("COUNTERS: \n");
        for (final Stats.KeysCounter key : Stats.KeysCounter.values()) {
            sb.append(key + ": " + report.key2counterStats.get(key) + "\n");
        }
        sb.append("TIME: \n");
        for (final Time.KeysCounter key : Time.KeysCounter.values()) {
            sb.append(key + ": " + TimeUtils.getPrettyPrintTime(report.key2counterTime.get(key).get().longValue()) + "\n");
        }
        sb.append("BEST ASSERTIONS\n");
        for (final Integer complexity : report.complexity2Criteria2BestSolutions.keySet()) {
            sb.append("complexity: " + complexity + "\n");
            for (final CriteriaCompareIndividuals criteria : CriteriaCompareIndividuals.values()) {
                sb.append("========================\n");

                sb.append("criteria: " + criteria + "\n");
                int count = 0;
                for (final Individual individual :
                        report.complexity2Criteria2BestSolutions.get(complexity).get(criteria)) {
                    count++;
                    sb.append("TOP: " + count + "\n");

                    sb.append(individual.toString() + "\n");
                    break;
                }
            }
        }
        sb.append("BEST NO MATTER THE COMPLEXITY\n");
        for (final CriteriaCompareIndividuals criteria : CriteriaCompareIndividuals.values()) {
            sb.append("========================\n");

            sb.append("criteria: " + criteria + "\n");
            int count = 0;
            for (final Individual individual :
                    report.criteria2BestSolutions.get(criteria)) {
                count++;
                sb.append("TOP: " + count + "\n");
                sb.append(individual.toString() + "\n");
                break;
            }
        }
        return sb.toString();

    }
}