package ch.usi.gassert.report;

import java.io.*;

import ch.usi.gassert.Stats;
import ch.usi.gassert.evolutionary.Individual;
import ch.usi.gassert.evolutionary.Population;
import ch.usi.gassert.util.Statistics;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PDFReports {


    public static void createReport(final List<Report> reports) {
        try {
            createGraph(createDataSet(reports));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }


    private static DefaultCategoryDataset createDataSet(final List<Report> reports) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (final Report report : reports) {
            final double value = report.populationFPstatistics.get(Population.TypeStatistics.complexity).mapValuesToDouble.get(Statistics.Values.mean);
            dataset.addValue(value, "complexity", String.valueOf(report.generation));
        }
        System.out.println(reports.get(reports.size() - 1).criteria2BestSolutions.toString());
        System.out.println(reports.get(reports.size() - 1).complexity2Criteria2BestSolutions.toString());

        return dataset;

    }

    private static void createGraph(final DefaultCategoryDataset lineChartDataset) throws FileNotFoundException, DocumentException {

        final JFreeChart lineChartObject;
        lineChartObject = ChartFactory.createLineChart(
                "Average Complexity Vs Generation", "Generation",
                "Average Complexity",
                lineChartDataset, PlotOrientation.VERTICAL,
                true, true, false);

        writeGraphInPDf(lineChartObject);
    }


    private static void writeGraphInPDf(final JFreeChart chart) throws FileNotFoundException, DocumentException {
        final int width = 640; /* Width of our chart */
        final int height = 480; /* Height of our chart */
        final Document document = new Document(new Rectangle(width, height)); /* Create a New Document Object */
        /* Create PDF Writer Object that will write the chart information for us */
        final PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("report.pdf"));
        /* Open the Document Object for adding contents */
        document.open();
        /* Get Direct Content of the PDF document for writing */
        final PdfContentByte Add_Chart_Content = writer.getDirectContent();
        /* Create a template using the PdfContent Byte object */
        final PdfTemplate template_Chart_Holder = Add_Chart_Content.createTemplate(width, height);
        /* Create a 2D graphics object to write on the template */
        final Graphics2D Graphics_Chart = template_Chart_Holder.createGraphics(width, height, new DefaultFontMapper());
        /* Create a Rectangle object */
        final Rectangle2D Chart_Region = new Rectangle2D.Double(0, 0, 540, 380);
        /* Invoke the draw method passing the Graphics and Rectangle 2D object to draw the chart */
        chart.draw(Graphics_Chart, Chart_Region);
        Graphics_Chart.dispose();
        /* Add template to PdfContentByte and then to the PDF document */
        Add_Chart_Content.addTemplate(template_Chart_Holder, 0, 0);
        /* Close the Document, writer will create a beautiful 2D chart inside the PDF document */
        document.close();
    }


    public static void printFNplusFP(final List<Report> theta, final List<Report> minimization) {

        final XYSeriesCollection dataset = new XYSeriesCollection();
        final XYSeries series1 = new XYSeries("theta");
        final XYSeries series2 = new XYSeries("minimization");

        for (final Report report : theta) {
            final double value = report.populationFPstatistics.get(Population.TypeStatistics.complexity).mapValuesToDouble.get(Statistics.Values.mean);
            series1.add(report.generation, value);
        }

        for (final Report report : minimization) {
            final double value = report.populationFPstatistics.get(Population.TypeStatistics.complexity).mapValuesToDouble.get(Statistics.Values.mean);
            series2.add(report.generation, value);
        }
        dataset.addSeries(series1);
        dataset.addSeries(series2);
        final JFreeChart lineChartObject;

        lineChartObject = ChartFactory.createXYLineChart(
                "Average Complexity Vs Generation", "Generation",
                "Average Complexity",
                dataset, PlotOrientation.VERTICAL,
                true, true, false);

        try {
            writeGraphInPDf(lineChartObject);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final DocumentException e) {
            e.printStackTrace();
        }
    }
}
