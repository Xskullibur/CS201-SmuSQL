package edu.smu.smusql.bplustreeA;

import org.junit.jupiter.api.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

class BPlusTreePerformanceTest {
    private static BPlusTreeEngine engine;
    private static final int SMALL_DATASET = 10_000;
    private static final int MEDIUM_DATASET = 100_000;
    private static final int LARGE_DATASET = 1_000_000;

    // Test data generators
    private static final String[] NAMES = {"'John'", "'Jane'", "'Bob'", "'Alice'", "'Charlie'", "'Diana'", "'Eve'", "'Frank'", "'Grace'", "'Henry'"};
    private static final String[] CITIES = {"'New York'", "'Los Angeles'", "'Chicago'", "'Houston'", "'Phoenix'", "'Philadelphia'", "'San Antonio'", "'San Diego'", "'Dallas'", "'San Jose'"};

    private static class PerformanceMetrics {
        final long executionTimeNanos;
        final int rowsAffected;

        PerformanceMetrics(long timeNanos, int rows) {
            this.executionTimeNanos = timeNanos;
            this.rowsAffected = rows;
        }

        double getMicros() {
            return TimeUnit.NANOSECONDS.toMicros(executionTimeNanos);
        }

        @Override
        public String toString() {
            return String.format(
                "Time: %.2f ms, Rows: %d, Avg time per row: %.2f ms",
                getMicros(),
                rowsAffected,
                rowsAffected > 0 ? getMicros() / rowsAffected : 0
            );
        }
    }

    @BeforeEach
    void setUp() {
        engine = new BPlusTreeEngine();
        engine.executeSQL("CREATE TABLE performance_test (id, name, age, salary, city)");
    }

    @AfterEach
    void tearDown() {
        engine.clearDatabase();
    }

    private String generateRandomInsert(int id) {
        String name = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
        int age = ThreadLocalRandom.current().nextInt(20, 61);
        double salary = ThreadLocalRandom.current().nextDouble(30000, 120000);
        String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];

        return String.format("INSERT INTO performance_test VALUES (%d, %s, %d, %.2f, %s)",
            id, name, age, salary, city);
    }

    private PerformanceMetrics measureBulkInsert(int size) {
        long startTime = System.nanoTime();

        for (int i = 1; i <= size; i++) {
            engine.executeSQL(generateRandomInsert(i));
        }

        long timeElapsed = System.nanoTime() - startTime;
        return new PerformanceMetrics(timeElapsed, size);
    }

    private PerformanceMetrics measureRangeQuery(int start, int end) {
        long startTime = System.nanoTime();

        String result = engine.executeSQL(
            String.format("SELECT * FROM performance_test WHERE id >= %d AND id <= %d", start, end));

        long timeElapsed = System.nanoTime() - startTime;
        int rowCount = result.split("\n").length - 1;

        return new PerformanceMetrics(timeElapsed, rowCount);
    }

    private PerformanceMetrics measureComplexQuery() {
        long startTime = System.nanoTime();

        String result = engine.executeSQL(
            "SELECT * FROM performance_test WHERE age > 30 AND salary >= 60000 OR city = 'New York'");

        long timeElapsed = System.nanoTime() - startTime;
        int rowCount = result.split("\n").length - 1;

        return new PerformanceMetrics(timeElapsed, rowCount);
    }

    private PerformanceMetrics measureRandomUpdates(int numberOfUpdates) {
        long startTime = System.nanoTime();
        int totalRowsAffected = 0;

        for (int i = 0; i < numberOfUpdates; i++) {
            int randomId = ThreadLocalRandom.current().nextInt(1, LARGE_DATASET + 1);
            double newSalary = ThreadLocalRandom.current().nextDouble(30000, 120000);
            String result = engine.executeSQL(
                String.format("UPDATE performance_test SET salary = %.2f WHERE id = %d",
                    newSalary, randomId));

            if (result.contains("successfully")) {
                totalRowsAffected += Integer.parseInt(result.split(" ")[0]);
            }
        }

        long timeElapsed = System.nanoTime() - startTime;
        return new PerformanceMetrics(timeElapsed, totalRowsAffected);
    }

    private PerformanceMetrics measureRandomDeletes(int numberOfDeletes) {
        long startTime = System.nanoTime();
        int totalRowsDeleted = 0;

        for (int i = 0; i < numberOfDeletes; i++) {
            int randomId = ThreadLocalRandom.current().nextInt(1, LARGE_DATASET + 1);
            String result = engine.executeSQL(
                String.format("DELETE FROM performance_test WHERE id = %d", randomId));

            if (result.contains("successfully")) {
                totalRowsDeleted += Integer.parseInt(result.split(" ")[0]);
            }
        }

        long timeElapsed = System.nanoTime() - startTime;
        return new PerformanceMetrics(timeElapsed, totalRowsDeleted);
    }

    @Test
    @DisplayName("Performance Test - Small Dataset")
    void testSmallDatasetPerformance() {
        System.out.println("\n=== Small Dataset Performance Test (10,000 rows) ===");

        // Warm-up phase
        for (int i = 1; i <= 1000; i++) {
            engine.executeSQL(generateRandomInsert(i));
        }
        engine.clearDatabase();
        engine.executeSQL("CREATE TABLE performance_test (id, name, age, salary, city)");

        // Actual measurements
        PerformanceMetrics insertMetrics = measureBulkInsert(SMALL_DATASET);
        System.out.println("Bulk Insert: " + insertMetrics);

        PerformanceMetrics rangeMetrics = measureRangeQuery(1, SMALL_DATASET/2);
        System.out.println("Range Query: " + rangeMetrics);

        PerformanceMetrics complexMetrics = measureComplexQuery();
        System.out.println("Complex Query: " + complexMetrics);

        PerformanceMetrics updateMetrics = measureRandomUpdates(SMALL_DATASET/10);
        System.out.println("Random Updates: " + updateMetrics);

        PerformanceMetrics deleteMetrics = measureRandomDeletes(SMALL_DATASET/10);
        System.out.println("Random Deletes: " + deleteMetrics);
    }

    @Test
    @DisplayName("Performance Test - Medium Dataset")
    void testMediumDatasetPerformance() {
        System.out.println("\n=== Medium Dataset Performance Test (100,000 rows) ===");

        // Warm-up phase
        for (int i = 1; i <= 1000; i++) {
            engine.executeSQL(generateRandomInsert(i));
        }
        engine.clearDatabase();
        engine.executeSQL("CREATE TABLE performance_test (id, name, age, salary, city)");

        PerformanceMetrics insertMetrics = measureBulkInsert(MEDIUM_DATASET);
        System.out.println("Bulk Insert: " + insertMetrics);

        PerformanceMetrics rangeMetrics = measureRangeQuery(1, MEDIUM_DATASET/2);
        System.out.println("Range Query: " + rangeMetrics);

        PerformanceMetrics complexMetrics = measureComplexQuery();
        System.out.println("Complex Query: " + complexMetrics);

        PerformanceMetrics updateMetrics = measureRandomUpdates(MEDIUM_DATASET/10);
        System.out.println("Random Updates: " + updateMetrics);

        PerformanceMetrics deleteMetrics = measureRandomDeletes(MEDIUM_DATASET/10);
        System.out.println("Random Deletes: " + deleteMetrics);
    }

    @Test
    @DisplayName("Performance Test - Large Dataset")
    void testLargeDatasetPerformance() {
        System.out.println("\n=== Large Dataset Performance Test (1,000,000 rows) ===");

        // Warm-up phase
        for (int i = 1; i <= 1000; i++) {
            engine.executeSQL(generateRandomInsert(i));
        }
        engine.clearDatabase();
        engine.executeSQL("CREATE TABLE performance_test (id, name, age, salary, city)");

        PerformanceMetrics insertMetrics = measureBulkInsert(LARGE_DATASET);
        System.out.println("Bulk Insert: " + insertMetrics);

        PerformanceMetrics rangeMetrics = measureRangeQuery(1, LARGE_DATASET/2);
        System.out.println("Range Query: " + rangeMetrics);

        PerformanceMetrics complexMetrics = measureComplexQuery();
        System.out.println("Complex Query: " + complexMetrics);

        PerformanceMetrics updateMetrics = measureRandomUpdates(LARGE_DATASET/10);
        System.out.println("Random Updates: " + updateMetrics);

        PerformanceMetrics deleteMetrics = measureRandomDeletes(LARGE_DATASET/10);
        System.out.println("Random Deletes: " + deleteMetrics);
    }

    @Test
    @DisplayName("Index Performance Test")
    void testIndexPerformance() {
        System.out.println("\n=== Index Performance Test ===");

        // First, insert test data
        measureBulkInsert(MEDIUM_DATASET);

        // Test queries on different indexed columns
        long startTime = System.nanoTime();
        engine.executeSQL("SELECT * FROM performance_test WHERE id = 5000");
        long idQueryTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        engine.executeSQL("SELECT * FROM performance_test WHERE age = 30");
        long ageQueryTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        engine.executeSQL("SELECT * FROM performance_test WHERE city = 'New York'");
        long cityQueryTime = System.nanoTime() - startTime;

        System.out.printf("Query by ID (Primary Key) time: %d ms%n",
            TimeUnit.NANOSECONDS.toMicros(idQueryTime));
        System.out.printf("Query by Age (Secondary Index) time: %d ms%n",
            TimeUnit.NANOSECONDS.toMicros(ageQueryTime));
        System.out.printf("Query by City (Secondary Index) time: %d ms%n",
            TimeUnit.NANOSECONDS.toMicros(cityQueryTime));
    }
}