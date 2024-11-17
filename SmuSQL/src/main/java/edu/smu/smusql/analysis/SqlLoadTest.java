package edu.smu.smusql.analysis;

import edu.smu.smusql.IEngine;
import edu.smu.smusql.analysis.utils.AbstractBenchmark;
import edu.smu.smusql.bplustreeA.bplustreeHashmap.BPlusTreeEngine;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

enum LoadType {
    LIGHT(100),
    MEDIUM(1000),
    HEAVY(10000),
    BURST(100000);

    private final int operationsPerSecond;

    LoadType(int operationsPerSecond) {
        this.operationsPerSecond = operationsPerSecond;
    }

    public int getOperationsPerSecond() {
        return operationsPerSecond;
    }
}

enum LoadPattern {
    CONSTANT,           // Maintain steady load
    INCREMENTAL,        // Gradually increase load
    RANDOM,             // Random fluctuations in load
    SPIKE,              // Sudden spikes in load
    WAVE               // Sinusoidal pattern
}

public class SqlLoadTest extends AbstractBenchmark {

    private static final String RESULTS_DIR = "src/main/java/edu/smu/smusql/analysis/load_test_results";
    private static final int REPORTING_INTERVAL = 100;
    private final LoadMetricsCollector metricsCollector;
    private final LoadType loadType;
    private final LoadPattern pattern;

    public SqlLoadTest(IEngine engine, LoadType loadType, LoadPattern pattern,
        long seed, String fileName) {
        super(engine, seed, RESULTS_DIR, fileName);
        this.loadType = loadType;
        this.pattern = pattern;
        this.metricsCollector = new LoadMetricsCollector();
    }

    public static void main(String[] args) {
        long seed = 12345L;
        int testDurationSeconds = 300; // 5 minutes per test

        // Test different load patterns with different loads
        LoadType[] loadTypes = LoadType.values();
        LoadPattern[] patterns = LoadPattern.values();

        for (LoadType loadType : loadTypes) {
            for (LoadPattern pattern : patterns) {
                String fileName = String.format("load_test_%s_%s.csv",
                    loadType.name().toLowerCase(),
                    pattern.name().toLowerCase());

                System.out.printf("Starting load test: %s load with %s pattern%n",
                    loadType, pattern);

                IEngine engine = new BPlusTreeEngine();
//                IEngine engine = new BPlusTreeArrayEngine();
//                IEngine engine = new HashMapEngine();
//                IEngine engine = new SkipHashEngine();
//                IEngine engine = new SkipLinkedListIndexedEngine();
                SqlLoadTest loadTest = new SqlLoadTest(engine, loadType, pattern,
                    seed, fileName);

                loadTest.runLoadTest(testDurationSeconds);

                // Cleanup
                engine.clearDatabase();
                System.gc();

                try {
                    Thread.sleep(5000); // Cool-down period between tests
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void runLoadTest(int durationSeconds) {
        setupDatabase();
        System.out.println(
            "Starting load test with " + loadType + " load and " + pattern + " pattern");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);
        long nextReportTime = startTime + (REPORTING_INTERVAL * 1000L);

        int totalOperations = 0;
        double currentLoad = loadType.getOperationsPerSecond();

        while (System.currentTimeMillis() < endTime) {
            long cycleStartTime = System.currentTimeMillis();

            // Adjust load based on pattern
            currentLoad = calculateCurrentLoad(cycleStartTime - startTime, durationSeconds);

            // Calculate operations for this second
            int operationsThisSecond = (int) currentLoad;

            // Execute operations
            for (int i = 0; i < operationsThisSecond && System.currentTimeMillis() < endTime; i++) {
                executeOperation();
                totalOperations++;

                // Report metrics at intervals
                if (System.currentTimeMillis() >= nextReportTime) {
                    writeMetricsToCSV(totalOperations, currentLoad);
                    nextReportTime += REPORTING_INTERVAL * 1000L;
                    System.out.println("Processed " + totalOperations + " operations...");
                }
            }

            // Sleep if needed to maintain desired load
            long sleepTime = 1000 - (System.currentTimeMillis() - cycleStartTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        writeMetricsToCSV(totalOperations, currentLoad);
        System.out.println("Load test completed. Results written to: " + outputFile);
    }

    // Database operations (similar to your existing benchmark)
    private void setupDatabase() {
        engine.executeSQL("CREATE TABLE users (id, name, age, city)");
        engine.executeSQL("CREATE TABLE products (id, name, price, category)");
        engine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");
        idManager.addInitialIds(100);
        prepopulateTables();
    }

    private double calculateCurrentLoad(long elapsedMillis, int durationSeconds) {
        double baseLoad = loadType.getOperationsPerSecond();
        double elapsedSeconds = elapsedMillis / 1000.0;
        double totalSeconds = durationSeconds;

        switch (pattern) {
            case CONSTANT:
                return baseLoad;

            case INCREMENTAL:
                return baseLoad * (1 + (elapsedSeconds / totalSeconds));

            case RANDOM:
                double randomFactor = 0.5 + random.nextDouble();
                return baseLoad * randomFactor;

            case SPIKE:
                // Create spikes every 10 seconds
                return (elapsedSeconds % 10 == 0) ? baseLoad * 2 : baseLoad;

            case WAVE:
                // Sinusoidal pattern
                double frequency = 2 * Math.PI / totalSeconds;
                return baseLoad * (1 + 0.5 * Math.sin(frequency * elapsedSeconds));

            default:
                return baseLoad;
        }
    }

    private void executeOperation() {
        long startTime = System.nanoTime();
        boolean success = true;

        try {
            // Choose operation type with weighted distribution
            int operationType = getWeightedOperationType();

            switch (operationType) {
                case 0: // INSERT (20%)
                    executeInsert();
                    break;
                case 1: // SELECT (40%)
                    if (random.nextBoolean()) {
                        executeRangeSelect();
                    } else {
                        executeEqualsSelect();
                    }
                    break;
                case 2: // UPDATE (30%)
                    if (random.nextBoolean()) {
                        executeRangeUpdate();
                    } else {
                        executeEqualsUpdate();
                    }
                    break;
                case 3: // DELETE (10%)
                    executeDelete();
                    break;
            }
        } catch (Exception e) {
            success = false;
        }

        long endTime = System.nanoTime();
        metricsCollector.recordOperation(endTime - startTime, success);
    }

    private void writeMetricsToCSV(int totalOperations, double currentLoad) {
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        LoadMetrics metrics = metricsCollector.getMetrics();

        try (FileWriter fw = new FileWriter(outputFile, true)) {
            fw.write(String.format("%s,%s,%s,%.2f,%s,%.2f,%.2f,%.2f,%d,%.2f,%.2f\n",
                timestamp,
                loadType,
                pattern,
                currentLoad,
                metrics.getOperationType(),
                metrics.getAverageLatency(),
                metrics.getThroughput(),
                metrics.getErrorRate(),
                heapMemory.getUsed(),
                metrics.getGcPauseTime(),
                metrics.getP95ResponseTime()
            ));
        } catch (IOException e) {
            System.err.println("Failed to write metrics to CSV: " + e.getMessage());
        }
    }

    private int getWeightedOperationType() {
        int rand = random.nextInt(100);
        if (rand < 20) {
            return 0;      // INSERT - 20%
        }
        if (rand < 60) {
            return 1;      // SELECT - 40%
        }
        if (rand < 90) {
            return 2;      // UPDATE - 30%
        }
        return 3;          // DELETE - 10%
    }

    @Override
    protected String getCSVHeader() {
        return "Timestamp,LoadType,LoadPattern,CurrentLoad,QueryType,AverageLatency," +
            "ThroughputQPS,ErrorRate,HeapMemoryUsed,GCPauseTime,ResponseTimeP95\n";
    }
}

class LoadMetrics {

    private final String operationType;
    private final double averageLatency;
    private final double throughput;
    private final double errorRate;
    private final double gcPauseTime;
    private final double p95ResponseTime;

    public LoadMetrics(String operationType, double averageLatency, double throughput,
        double errorRate, double gcPauseTime, double p95ResponseTime) {
        this.operationType = operationType;
        this.averageLatency = averageLatency;
        this.throughput = throughput;
        this.errorRate = errorRate;
        this.gcPauseTime = gcPauseTime;
        this.p95ResponseTime = p95ResponseTime;
    }

    // Getters
    public String getOperationType() {
        return operationType;
    }

    public double getAverageLatency() {
        return averageLatency;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public double getGcPauseTime() {
        return gcPauseTime;
    }

    public double getP95ResponseTime() {
        return p95ResponseTime;
    }
}

class LoadMetricsCollector {

    private static final int WINDOW_SIZE_MS = 1000;
    private final List<Long> responseTimes = new ArrayList<>();
    private final List<Long> windowedResponseTimes = new ArrayList<>();
    private int totalOperations = 0;
    private int failedOperations = 0;
    private long windowStart = System.currentTimeMillis();

    public synchronized void recordOperation(long responseTime, boolean success) {
        responseTimes.add(responseTime);
        windowedResponseTimes.add(responseTime);
        totalOperations++;
        if (!success) {
            failedOperations++;
        }

        // Clean up old windowed data
        long currentTime = System.currentTimeMillis();
        if (currentTime - windowStart > WINDOW_SIZE_MS) {
            windowedResponseTimes.clear();
            windowStart = currentTime;
        }
    }

    public LoadMetrics getMetrics() {
        if (responseTimes.isEmpty()) {
            return new LoadMetrics("NONE", 0, 0, 0, 0, 0);
        }

        double avgLatency = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0; // Convert to ms

        double throughput = windowedResponseTimes.size() * (1000.0 / WINDOW_SIZE_MS);
        double errorRate = totalOperations > 0 ?
            (failedOperations * 100.0) / totalOperations : 0;

        // Calculate P95 response time
        List<Long> sortedTimes = new ArrayList<>(responseTimes);
        Collections.sort(sortedTimes);
        int p95Index = (int) Math.ceil(0.95 * sortedTimes.size()) - 1;
        double p95 = sortedTimes.get(p95Index) / 1_000_000.0; // Convert to ms

        // Get GC info
        long gcPauseTime = getGCPauseTime();

        return new LoadMetrics(
            "MIXED",
            avgLatency,
            throughput,
            errorRate,
            gcPauseTime,
            p95
        );
    }

    private long getGCPauseTime() {
        // This is a simplified version. In practice, you might want to use JMX to get actual GC metrics
        long totalGcTime = ManagementFactory.getGarbageCollectorMXBeans()
            .stream()
            .mapToLong(gc -> gc.getCollectionTime())
            .sum();
        return totalGcTime;
    }
}