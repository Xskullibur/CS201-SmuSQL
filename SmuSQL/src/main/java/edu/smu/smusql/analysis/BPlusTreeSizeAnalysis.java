package edu.smu.smusql.analysis;

import edu.smu.smusql.bplustreeA.BPlusTree;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * This analysis was developed with the assistance of Claude (Anthropic).
 *
 * 1. Initial Implementation: - An initial `analyzeOrders` method was developed to test different B+
 * tree orders
 *
 * 2. Visualization Enhancements (with Claude):
 *
 * - Prompted Claude to add visualization capabilities
 * using JFreeChart to visualize the statistics
 *
 * - This in my implementation of `analyzeOrders` which
 * analyzes the performance of different orders, instead of only outputting a string help me plot it
 * in a graph using Java
 */
public class BPlusTreeSizeAnalysis {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int TEST_ITERATIONS = 5;

    public static void main(String[] args) {

        int[] dataSizes = {1000, 10000, 100000, 1000000};
        int[] orders = {4, 8, 16, 32, 64, 128};

        Map<Integer, Map<Integer, PerformanceResult>> results = analyzeSizeImpact(dataSizes,
            orders);

        for (Map.Entry<Integer, Map<Integer, PerformanceResult>> sizeEntry : results.entrySet()) {
            System.out.printf("\nData Size: %d%n", sizeEntry.getKey());
            for (Map.Entry<Integer, PerformanceResult> orderEntry : sizeEntry.getValue()
                .entrySet()) {
                System.out.printf("\nOrder: %d%n", orderEntry.getKey());
                PerformanceResult result = orderEntry.getValue();
                System.out.printf("Insertion Time: %.2f ± %.2f ms%n", result.avgInsertionTime,
                    result.stdDevInsertion);
                System.out.printf("Search Time: %.2f ± %.2f ms%n", result.avgSearchTime,
                    result.stdDevSearch);
                System.out.printf("Range Query Time: %.2f ± %.2f ms%n", result.avgRangeQueryTime,
                    result.stdDevRangeQuery);
            }
        }

        SwingUtilities.invokeLater(() -> createAndShowCharts(results));
    }

    public static Map<Integer, Map<Integer, PerformanceResult>> analyzeSizeImpact(int[] treeSizes,
        int[] orders) {

        Map<Integer, Map<Integer, PerformanceResult>> results = new HashMap<>();

        for (int size : treeSizes) {
            System.out.println("\nTesting size: " + size);
            results.put(size, analyzeOrders(size, orders));
        }

        return results;
    }

    private static void createAndShowCharts(Map<Integer, Map<Integer, PerformanceResult>> results) {
        JFrame frame = new JFrame("B+ Tree Size Analysis");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(2, 2));

        // Create datasets
        XYSeriesCollection insertionDataset = new XYSeriesCollection();
        XYSeriesCollection searchDataset = new XYSeriesCollection();
        XYSeriesCollection rangeQueryDataset = new XYSeriesCollection();

        // Create series for each order
        for (int order : results.values().iterator().next().keySet()) {
            XYSeries insertionSeries = new XYSeries("Order " + order + " (Insertion)");
            XYSeries searchSeries = new XYSeries("Order " + order + " (Search)");
            XYSeries rangeQuerySeries = new XYSeries("Order " + order + " (Range)");

            for (Map.Entry<Integer, Map<Integer, PerformanceResult>> entry : results.entrySet()) {
                int size = entry.getKey();
                PerformanceResult metrics = entry.getValue().get(order);

                insertionSeries.add(size, metrics.avgInsertionTime);
                searchSeries.add(size, metrics.avgSearchTime);
                rangeQuerySeries.add(size, metrics.avgRangeQueryTime);
            }

            insertionDataset.addSeries(insertionSeries);
            searchDataset.addSeries(searchSeries);
            rangeQueryDataset.addSeries(rangeQuerySeries);
        }

        // Create and save insertion time chart
        JFreeChart insertionChart = ChartFactory.createXYLineChart("Insertion Time vs Data Size",
            "Data Size", "Time (ms)", insertionDataset, PlotOrientation.VERTICAL, true, true,
            false);
        customizeChart(insertionChart);
        frame.add(new ChartPanel(insertionChart));
        saveChart(insertionChart, "insertion_time.png", 800, 600);

        // Create and save search time chart
        JFreeChart searchChart = ChartFactory.createXYLineChart("Search Time vs Data Size",
            "Data Size", "Time (ms)", searchDataset, PlotOrientation.VERTICAL, true, true, false);
        customizeChart(searchChart);
        frame.add(new ChartPanel(searchChart));
        saveChart(searchChart, "search_time.png", 800, 600);

        // Create and save range query time chart
        JFreeChart rangeChart = ChartFactory.createXYLineChart("Range Query Time vs Data Size",
            "Data Size", "Time (ms)", rangeQueryDataset, PlotOrientation.VERTICAL, true, true,
            false);
        customizeChart(rangeChart);
        frame.add(new ChartPanel(rangeChart));
        saveChart(rangeChart, "range_query_time.png", 800, 600);

        // Add summary panel
        frame.add(createSummaryPanel(results));

        // Create and save a combined performance summary
        try {
            createAndSaveSummaryTable(results, "performance_summary.png");
        } catch (IOException e) {
            System.err.println("Error saving summary table: " + e.getMessage());
        }

        frame.pack();
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static Map<Integer, PerformanceResult> analyzeOrders(int size, int[] orders) {

        Map<Integer, PerformanceResult> orderResults = new HashMap<>();
        TestData testData = new TestData(size);

        System.out.println("Warming up...");
        performWarmup(orders[0], testData);

        for (int order : orders) {
            System.out.println("Testing order: " + order);
            List<Long> insertionTimes = new ArrayList<>();
            List<Long> searchTimes = new ArrayList<>();
            List<Long> rangeQueryTimes = new ArrayList<>();

            for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
                BPlusTree<Integer, String> tree = new BPlusTree<>(order);

                long startTime = System.nanoTime();
                for (int key : testData.insertKeys) {
                    tree.insert(key, "value-" + key);
                }
                insertionTimes.add((System.nanoTime() - startTime) / 1_000_000);

                startTime = System.nanoTime();
                for (int key : testData.searchKeys) {
                    tree.search(key);
                }
                searchTimes.add((System.nanoTime() - startTime) / 1_000_000);

                // Measure range queries
                startTime = System.nanoTime();
                for (RangeQuery query : testData.rangeQueries) {
                    tree.rangeSearch(query.start, query.end);
                }
                rangeQueryTimes.add((System.nanoTime() - startTime) / 1_000_000);
            }

            PerformanceResult result = new PerformanceResult();
            result.avgInsertionTime = calculateMean(insertionTimes);
            result.avgSearchTime = calculateMean(searchTimes);
            result.avgRangeQueryTime = calculateMean(rangeQueryTimes);
            result.stdDevInsertion = calculateStdDev(insertionTimes, result.avgInsertionTime);
            result.stdDevSearch = calculateStdDev(searchTimes, result.avgSearchTime);
            result.stdDevRangeQuery = calculateStdDev(rangeQueryTimes, result.avgRangeQueryTime);

            orderResults.put(order, result);
        }

        return orderResults;
    }

    private static void customizeChart(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setDomainGridlinePaint(Color.GRAY);
    }

    private static void saveChart(JFreeChart chart, String filename, int width, int height) {
        try {
            File outputFile = new File(filename);
            ChartUtils.saveChartAsPNG(outputFile, chart, width, height);
            System.out.println("Saved chart to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving chart: " + e.getMessage());
        }
    }

    private static JPanel createSummaryPanel(

        Map<Integer, Map<Integer, PerformanceResult>> results) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Performance Summary"));

        StringBuilder summary = new StringBuilder("<html><body>");
        summary.append("<h3>Performance Summary:</h3>");
        summary.append("<table border='1'>");
        summary.append(
            "<tr><th>Data Size</th><th>Best Order</th><th>Insertion Time</th><th>Search Time</th></tr>");

        for (Map.Entry<Integer, Map<Integer, PerformanceResult>> sizeEntry : results.entrySet()) {
            int size = sizeEntry.getKey();
            Map<Integer, PerformanceResult> orderResults = sizeEntry.getValue();

            // Find best order based on combined performance
            int bestOrder = findBestOrder(orderResults);
            PerformanceResult bestResult = orderResults.get(bestOrder);

            summary.append(
                String.format("<tr><td>%d</td><td>%d</td><td>%.2f ms</td><td>%.2f ms</td></tr>",
                    size, bestOrder, bestResult.avgInsertionTime, bestResult.avgSearchTime));
        }

        summary.append("</table></body></html>");

        JLabel label = new JLabel(summary.toString());
        panel.add(new JScrollPane(label), BorderLayout.CENTER);
        return panel;
    }

    private static void createAndSaveSummaryTable(
        Map<Integer, Map<Integer, PerformanceResult>> results, String filename) throws IOException {

        // Create a buffered image to draw the table
        int width = 800;
        int height = 200 + (results.size() * 25);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Set up the graphics context
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 14));

        // Draw table headers
        int y = 30;
        g2.drawString("Data Size", 50, y);
        g2.drawString("Best Order", 200, y);
        g2.drawString("Avg Insertion Time (ms)", 350, y);
        g2.drawString("Avg Search Time (ms)", 550, y);

        // Draw horizontal line
        g2.drawLine(40, y + 10, width - 40, y + 10);

        // Draw data rows
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        y += 40;

        for (Map.Entry<Integer, Map<Integer, PerformanceResult>> sizeEntry : results.entrySet()) {
            int size = sizeEntry.getKey();
            Map<Integer, PerformanceResult> orderResults = sizeEntry.getValue();

            // Find best order based on combined performance
            int bestOrder = findBestOrder(orderResults);
            PerformanceResult bestResult = orderResults.get(bestOrder);

            g2.drawString(String.format("%,d", size), 50, y);
            g2.drawString(String.format("%d", bestOrder), 200, y);
            g2.drawString(String.format("%.2f", bestResult.avgInsertionTime), 350, y);
            g2.drawString(String.format("%.2f", bestResult.avgSearchTime), 550, y);

            y += 25;
        }

        g2.dispose();

        // Save the image
        File outputFile = new File(filename);
        ImageIO.write(image, "PNG", outputFile);
        System.out.println("Saved summary table to: " + outputFile.getAbsolutePath());
    }

    private static void performWarmup(int order, TestData testData) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            System.out.printf("  Warmup iteration %d/%d%n", i + 1, WARMUP_ITERATIONS);
            BPlusTree<Integer, String> tree = new BPlusTree<>(order);

            for (Integer key : testData.insertKeys) {
                tree.insert(key, "value-" + key);
            }
            for (Integer key : testData.searchKeys) {
                tree.search(key);
            }
            for (RangeQuery query : testData.rangeQueries) {
                tree.rangeSearch(query.start, query.end);
            }
        }
        System.out.println("Warmup completed");
    }

    private static double calculateMean(List<Long> numbers) {
        return numbers.stream().mapToDouble(n -> n).average().orElse(0.0);
    }

    private static double calculateStdDev(List<Long> numbers, double mean) {
        double variance = numbers.stream().mapToDouble(n -> {
            double diff = n - mean;
            return diff * diff;
        }).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private static int findBestOrder(Map<Integer, PerformanceResult> orderResults) {
        return orderResults.entrySet().stream().min((e1, e2) -> {

            // Consider both insertion and search time with equal weight
            double score1 = e1.getValue().avgInsertionTime + e1.getValue().avgSearchTime;
            double score2 = e2.getValue().avgInsertionTime + e2.getValue().avgSearchTime;
            return Double.compare(score1, score2);
        }).map(Map.Entry::getKey).orElse(0);
    }

    private static ChartPanel createChart(String title, String xLabel, String yLabel,
        XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, dataset,
            PlotOrientation.VERTICAL, true, true, false);

        customizeChart(chart);
        return new ChartPanel(chart);
    }

    static class PerformanceResult {

        double avgInsertionTime;
        double avgSearchTime;
        double avgRangeQueryTime;
        double stdDevInsertion;
        double stdDevSearch;
        double stdDevRangeQuery;
    }

    static class TestData {

        List<Integer> insertKeys;
        List<Integer> searchKeys;
        List<RangeQuery> rangeQueries;

        TestData(int size) {
            // Generate sequential keys
            insertKeys = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                insertKeys.add(i);
            }
            Collections.shuffle(insertKeys);

            // Create search keys (50% of insert keys)
            searchKeys = new ArrayList<>(insertKeys.subList(0, size / 2));
            Collections.shuffle(searchKeys);

            // Create range queries
            rangeQueries = new ArrayList<>();
            int numRangeQueries = Math.min(1000, size / 10);
            int rangeSize = size / 100; // 1% of data size
            for (int i = 0; i < numRangeQueries; i++) {
                int start = (i * rangeSize) % size;
                int end = Math.min(start + rangeSize, size - 1);
                rangeQueries.add(new RangeQuery(start, end));
            }
        }
    }

    static class RangeQuery {

        final int start;
        final int end;

        RangeQuery(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}