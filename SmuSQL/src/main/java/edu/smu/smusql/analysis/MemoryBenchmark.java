package edu.smu.smusql.analysis;

import edu.smu.smusql.IEngine;
import edu.smu.smusql.analysis.utils.UniqueIdManager;
import edu.smu.smusql.bplustreeA.bplustreeHashmap.BPlusTreeEngine;
import edu.smu.smusql.hashMap.HashMapEngine;
import edu.smu.smusql.skipHash.SkipHashEngine;
import edu.smu.smusql.skipLinkedListIndexed.SkipLinkedListIndexedEngine;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemoryBenchmark {

    private static final int[] DATASET_SIZES = {
        10_000,    // 10K
        100_000,   // 100K
        1_000_000  // 1M
    };
    private final List<MemoryMeasurement> measurements = new ArrayList<>();
    private final Random random = new Random(42);
    private final BPlusTreeEngine bPlusTreeEngine;
    private final BPlusTreeEngine bPlusTreeEngineCache;
    private final SkipLinkedListIndexedEngine skipListEngine;
    private final SkipHashEngine skipHashEngine;
    private final HashMapEngine hashMapEngine;
    private long startGCCount;
    private long startGCTime;
    private final UniqueIdManager idManager;

    public MemoryBenchmark() {
        this.bPlusTreeEngine = new BPlusTreeEngine(false);
        this.bPlusTreeEngineCache = new BPlusTreeEngine(true);
        this.skipListEngine = new SkipLinkedListIndexedEngine();
        this.skipHashEngine = new SkipHashEngine();
        this.hashMapEngine = new HashMapEngine();
        this.idManager = new UniqueIdManager();
    }

    public static void main(String[] args) throws Exception {
        MemoryBenchmark benchmark = new MemoryBenchmark();
        benchmark.runBenchmarks();
    }

    public void runBenchmarks() throws Exception {
        // Write headers to CSV files
        writeDetailedCSVHeader();
        writeSummaryCSVHeader();

        for (int size : DATASET_SIZES) {
            System.out.println("\n=== Testing Dataset Size: " + size + " ===");

            testImplementation(bPlusTreeEngine, size, "BPlusTree");
            testImplementation(bPlusTreeEngineCache, size, "BPlusTreeCache");
            testImplementation(skipListEngine, size, "SkipList");
            testImplementation(skipHashEngine, size, "SkipHash");
            testImplementation(hashMapEngine, size, "HashMap");

            // Force GC between implementations
            System.gc();
            Thread.sleep(2000);
        }

        // Write all measurements to CSV
        writeDetailedCSV();
        writeSummaryCSV();
    }

    private void writeDetailedCSVHeader() throws Exception {
        try (PrintWriter writer = new PrintWriter(
            new FileWriter("memory_measurements_detailed.csv"))) {
            writer.println("Implementation,DatasetSize,BatchNumber,Timestamp,TotalMemoryMB," +
                "MemoryPerRecord,RecordsProcessed,GCCount,GCTimeMs,HeapUsageMB,InsertionTimeMs");
        }
    }

    private void writeSummaryCSVHeader() throws Exception {
        try (PrintWriter writer = new PrintWriter(
            new FileWriter("memory_measurements_summary.csv"))) {
            writer.println("Implementation,DatasetSize,FinalMemoryMB,AverageMemoryPerRecord," +
                "TotalGCCount,TotalGCTimeMs,TotalInsertionTimeMs,AverageInsertionTimeMs");
        }
    }

    private void testImplementation(IEngine engine, int datasetSize, String implName)
        throws Exception {
        System.out.println("\nTesting " + implName + " with " + datasetSize + " records");

        // Clear previous data and warm up
        engine.clearDatabase();
        idManager.clearUsedIds();
        System.gc();
        Thread.sleep(1000);

        // Record initial GC state
        startGCCount = getGCCount();
        startGCTime = getGCTime();

        long baselineMemory = getUsedMemory();
        Instant implementationStart = Instant.now();

        // Create table
        engine.executeSQL("CREATE TABLE test_table (id, name, age, salary, department, hire_date)");

        // Insert data in batches
        int batchSize = Math.min(10000, datasetSize / 10);
        int numBatches = datasetSize / batchSize;

        for (int batch = 0; batch < numBatches; batch++) {
            Instant batchStart = Instant.now();

            // Insert batch data
            for (int i = 0; i < batchSize; i++) {
                int id = idManager.generateUniqueUserId(random);
                String sql = String.format(
                    "INSERT INTO test_table VALUES (%d, '%s', %d, %.2f, '%s', '%s')",
                    id,
                    generateString(20),
                    20 + random.nextInt(40),
                    30000 + random.nextDouble() * 70000,
                    generateString(10),
                    generateDate()
                );
                engine.executeSQL(sql);
            }

            // Record measurements
            long currentMemory = getUsedMemory();
            double memoryUsageMB = (currentMemory - baselineMemory) / (1024.0 * 1024.0);
            double memoryPerRecord = memoryUsageMB * 1024 * 1024 / ((batch + 1) * batchSize);
            long gcCount = getGCCount() - startGCCount;
            long gcTime = getGCTime() - startGCTime;
            double heapUsage = getHeapUsage();
            long batchTime = Duration.between(batchStart, Instant.now()).toMillis();

            // Store measurement
            measurements.add(new MemoryMeasurement(
                implName,
                datasetSize,
                batch + 1,
                memoryUsageMB,
                memoryPerRecord,
                (batch + 1) * batchSize,
                gcCount,
                gcTime,
                heapUsage,
                batchTime
            ));

            System.out.printf("Batch %d/%d - Memory: %.2f MB (%.2f bytes/record) - Time: %dms%n",
                batch + 1, numBatches, memoryUsageMB, memoryPerRecord, batchTime);
        }
    }

    private void writeDetailedCSV() throws Exception {
        try (PrintWriter writer = new PrintWriter(
            new FileWriter("memory_measurements_detailed.csv", true))) {
            for (MemoryMeasurement m : measurements) {
                writer.printf("%s,%d,%d,%d,%.2f,%.2f,%d,%d,%d,%.2f,%d%n",
                    m.implementation, m.datasetSize, m.batchNumber, m.timestamp,
                    m.totalMemoryMB, m.memoryPerRecord, m.recordsProcessed,
                    m.gcCount, m.gcTimeMs, m.heapUsageMB, m.insertionTimeMs);
            }
        }
    }

    private void writeSummaryCSV() throws Exception {
        try (PrintWriter writer = new PrintWriter(
            new FileWriter("memory_measurements_summary.csv", true))) {
            // Group measurements by implementation and dataset size
            for (int size : DATASET_SIZES) {
                for (String impl : new String[]{"BPlusTree", "SkipList", "SkipHash", "HashMap"}) {
                    List<MemoryMeasurement> filtered = new ArrayList<>();
                    for (MemoryMeasurement m : measurements) {
                        if (m.implementation.equals(impl) && m.datasetSize == size) {
                            filtered.add(m);
                        }
                    }

                    if (!filtered.isEmpty()) {
                        MemoryMeasurement last = filtered.get(filtered.size() - 1);
                        double avgMemoryPerRecord = filtered.stream()
                            .mapToDouble(m -> m.memoryPerRecord)
                            .average()
                            .orElse(0.0);
                        long totalInsertionTime = filtered.stream()
                            .mapToLong(m -> m.insertionTimeMs)
                            .sum();
                        double avgInsertionTime = totalInsertionTime / (double) filtered.size();

                        writer.printf("%s,%d,%.2f,%.2f,%d,%d,%d,%.2f%n",
                            impl, size, last.totalMemoryMB, avgMemoryPerRecord,
                            last.gcCount, last.gcTimeMs, totalInsertionTime, avgInsertionTime);
                    }
                }
            }
        }
    }

    private long getGCCount() {
        long count = 0;
        for (java.lang.management.GarbageCollectorMXBean gc :
            java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            count += gc.getCollectionCount();
        }
        return count;
    }

    private long getGCTime() {
        long time = 0;
        for (java.lang.management.GarbageCollectorMXBean gc :
            java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            time += gc.getCollectionTime();
        }
        return time;
    }

    // Utility methods for memory and GC statistics
    private long getUsedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private String generateString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private String generateDate() {
        return String.format("%d-%02d-%02d",
            2020 + random.nextInt(4),
            1 + random.nextInt(12),
            1 + random.nextInt(28));
    }

    private double getHeapUsage() {
        java.lang.management.MemoryMXBean memory =
            java.lang.management.ManagementFactory.getMemoryMXBean();
        return memory.getHeapMemoryUsage().getUsed() / (1024.0 * 1024.0);
    }

    // Data collection classes
    private static class MemoryMeasurement {

        final String implementation;
        final int datasetSize;
        final int batchNumber;
        final long timestamp;
        final double totalMemoryMB;
        final double memoryPerRecord;
        final int recordsProcessed;
        final long gcCount;
        final long gcTimeMs;
        final double heapUsageMB;
        final long insertionTimeMs;

        MemoryMeasurement(String implementation, int datasetSize, int batchNumber,
            double totalMemoryMB, double memoryPerRecord, int recordsProcessed,
            long gcCount, long gcTimeMs, double heapUsageMB, long insertionTimeMs) {
            this.implementation = implementation;
            this.datasetSize = datasetSize;
            this.batchNumber = batchNumber;
            this.timestamp = System.currentTimeMillis();
            this.totalMemoryMB = totalMemoryMB;
            this.memoryPerRecord = memoryPerRecord;
            this.recordsProcessed = recordsProcessed;
            this.gcCount = gcCount;
            this.gcTimeMs = gcTimeMs;
            this.heapUsageMB = heapUsageMB;
            this.insertionTimeMs = insertionTimeMs;
        }
    }
}