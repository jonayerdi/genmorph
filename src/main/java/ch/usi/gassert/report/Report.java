package ch.usi.gassert.report;

import ch.usi.gassert.Config;
import ch.usi.gassert.Stats;
import ch.usi.gassert.Time;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.evolutionary.Population;
import ch.usi.gassert.interpreter.AssertionManager;
import ch.usi.gassert.interpreter.CriteriaCompareIndividuals;
import ch.usi.gassert.util.AtomicBigInteger;
import ch.usi.gassert.util.MyGson;
import ch.usi.gassert.util.Statistics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.*;
import java.util.*;

public class Report {
    protected int generation;
    protected long currentTime;
    protected Map<Time.KeysCounter, AtomicBigInteger> key2counterTime;
    protected Map<Stats.KeysCounter, AtomicBigInteger> key2counterStats;
    protected Map<Population.TypeStatistics, Statistics> populationFPstatistics;
    protected Map<Population.TypeStatistics, Statistics> populationFNstatistics;
    protected Map<Integer, Map<CriteriaCompareIndividuals, java.util.List<Individual>>> complexity2Criteria2BestSolutions;
    protected Map<CriteriaCompareIndividuals, List<Individual>> criteria2BestSolutions;

    public Report() {
        init();
    }

    public void init() {
        generation = 0;
        currentTime = 0;
        key2counterTime = null;
        key2counterStats = null;
        populationFPstatistics = null;
        populationFNstatistics = null;
        complexity2Criteria2BestSolutions = null;
        criteria2BestSolutions = null;
    }

    public void setIndex0(final int gen, final Population population) {
        generation = gen;
        currentTime = System.currentTimeMillis();
        key2counterTime = Time.getInstance().getKey2counterTime();
        key2counterStats = Stats.getInstance().getKey2counterStats();
        populationFPstatistics = population.getStatisticsPopulation();
        //complexity2Criteria2BestSolutions = AssertionManager.getComplexity2Criteria2BestSolutions();
        //criteria2BestSolutions = AssertionManager.getCriteria2BestSolutions();
        storeInfo(this, "statsFP" + gen + "-" + System.currentTimeMillis() + ".json");
    }

    public void setIndex1(final int gen, final Population population) {
        storeInfo(population.getStatisticsPopulation(), "statsFN" + gen + "-" + System.currentTimeMillis() + ".json");
    }

    static Gson gson = new GsonBuilder().disableHtmlEscaping().serializeSpecialFloatingPointValues().create();

    public static void storeInfo(final Object o, final String fileName) {
        final String json = gson.toJson(o);
        final File tmp = new File(new File("").getAbsolutePath() + File.separator + "logs");
        if (!tmp.exists()) {
            throw new RuntimeException("foder does not exists" + tmp.getAbsolutePath() + " I am here" + new File("").getAbsolutePath());
        }
        try {
            final FileWriter writer = new FileWriter(tmp.getAbsolutePath() + File.separator + fileName);
            writer.write(json);
            writer.close();
            System.out.println("wrote file: " + tmp.getAbsolutePath() + File.separator + fileName);

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Report> loadInfo(final String folder) {
        final List<Report> reports = new LinkedList<>();
        try {
            for (final File file : new File(folder).listFiles()) {
                if (file.getName().contains("FP")) {
                    final JsonReader reader = new JsonReader(new FileReader(file));
                    final Report report = gson.fromJson(reader, Report.class);
                    for (final File file2 : new File(folder).listFiles()) {
                        if (!file2.getName().contains("FP") && file2.getName().contains("-") && file2.getName().split("-")[0].replace("FN", "FP").equals(file.getName().split("-")[0])) {
                            final JsonReader reader2 = new JsonReader(new FileReader(file2));
                            final Map<Population.TypeStatistics, Statistics> data2 = gson.fromJson(reader2, Map.class);
                            report.populationFNstatistics = new HashMap<>();
                            report.populationFNstatistics.putAll(data2);
                            reports.add(report);
                        }
                    }
                }
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
        reports.sort(comparatorReport);
        return reports;
    }

    static Comparator<Report> comparatorReport = new Comparator<Report>() {
        @Override
        public int compare(final Report s1, final Report s2) {
            return s1.generation - s2.generation;
        }
    };

    public static void main(final String[] args) {
        System.out.println("THETA");
        System.out.println(ReportLastGeneration.getInfoLastGeneration(loadInfo("theta-logs")));
        //System.out.println("MINIMIZATION");
        //System.out.println(ReportLastGeneration.getInfoLastGeneration(loadInfo("minimization-logs")));
        System.out.println("MINIMIZATION NEW");
        System.out.println(ReportLastGeneration.getInfoLastGeneration(loadInfo("logs-minimization-new")));

        PDFReports.createReport(loadInfo("theta-logs"));
        PDFReports.createReport(loadInfo("minimization-logs"));

        //PDFReports.printFNplusFP(loadInfo("theta-logs"), loadInfo("logs-minimization-new"));

    }

}

