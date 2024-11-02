package edu.smu.smusql.bplustreeA;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BPlusTreePerformanceTest {

    private static final int SMALL_DATASET = 10_000;
    private static final int MEDIUM_DATASET = 100_000;
    private static final int LARGE_DATASET = 1_000_000;
    // Test data generators
    private static final String[] NAMES = {"'John'", "'Jane'", "'Bob'", "'Alice'", "'Charlie'",
        "'Diana'", "'Eve'", "'Frank'", "'Grace'", "'Henry'"};
    private static final String[] CITIES = {"'New York'", "'Los Angeles'", "'Chicago'", "'Houston'",
        "'Phoenix'", "'Philadelphia'", "'San Antonio'", "'San Diego'", "'Dallas'", "'San Jose'"};
    private static BPlusTreeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BPlusTreeEngine();
        engine.executeSQL("CREATE TABLE performance_test (id, name, age, salary, city)");
    }

    @AfterEach
    void tearDown() {
        engine.clearDatabase();
        engine.clearCacheMetrics();
    }

    @Test
    @DisplayName("Comprehensive Query Performance Test")
    void testQueryPerformance() {
        System.out.println("\n=== Inserting test data... ===");
        measureBulkInsert(SMALL_DATASET);

        int iterations = SMALL_DATASET;

        System.out.println("\n=== Query Performance Test Results ===");
        System.out.println("Dataset size: " + SMALL_DATASET + " rows");
        System.out.println("Iterations per category: " + iterations);
        System.out.println("\nResults (averaged over " + iterations + " iterations):\n");

        // ==== STRING QUERIES ====
        System.out.println("\n=== String Field Queries ===");

        // Single String Equality
        testQueryCategory("Single String =", iterations, () -> {
            String field = ThreadLocalRandom.current().nextBoolean() ? "name" : "city";
            String value =
                field.equals("name") ? NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)]
                    : CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
            return String.format("SELECT * FROM performance_test WHERE %s = %s", field, value);
        });

        // Single String Inequality
        testQueryCategory("Single String !=", iterations, () -> {
            String field = ThreadLocalRandom.current().nextBoolean() ? "name" : "city";
            String value =
                field.equals("name") ? NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)]
                    : CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
            return String.format("SELECT * FROM performance_test WHERE %s != %s", field, value);
        });

        // Double String AND
        testQueryCategory("Double String AND", iterations, () -> {
            String name = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
            String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
            return String.format("SELECT * FROM performance_test WHERE name = %s AND city = %s",
                name, city);
        });

        // Double String OR
        testQueryCategory("Double String OR", iterations, () -> {
            String name = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
            String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
            return String.format("SELECT * FROM performance_test WHERE name = %s OR city = %s",
                name, city);
        });

        // ==== NUMERIC QUERIES ====
        System.out.println("\n=== Numeric Field Queries ===");

        // Single Number Equality (Age)
        testQueryCategory("Single Number = (Age)", iterations, () -> {
            int age = ThreadLocalRandom.current().nextInt(20, 61);
            return String.format("SELECT * FROM performance_test WHERE age = %d", age);
        });

        // Single Number Equality (Salary)
        testQueryCategory("Single Number = (Salary)", iterations, () -> {
            double salary = ThreadLocalRandom.current().nextDouble(30000, 120000);
            return String.format("SELECT * FROM performance_test WHERE salary = %.2f", salary);
        });

        // Single Number Inequality
        testQueryCategory("Single Number !=", iterations, () -> {
            String field = ThreadLocalRandom.current().nextBoolean() ? "age" : "salary";
            String value =
                field.equals("age") ? String.valueOf(ThreadLocalRandom.current().nextInt(20, 61))
                    : String.format("%.2f", ThreadLocalRandom.current().nextDouble(30000, 120000));
            return String.format("SELECT * FROM performance_test WHERE %s != %s", field, value);
        });

        // Number Range Queries (>)
        testQueryCategory("Number Range >", iterations, () -> {
            String field = ThreadLocalRandom.current().nextBoolean() ? "age" : "salary";
            String value =
                field.equals("age") ? String.valueOf(ThreadLocalRandom.current().nextInt(20, 61))
                    : String.format("%.2f", ThreadLocalRandom.current().nextDouble(30000, 120000));
            return String.format("SELECT * FROM performance_test WHERE %s > %s", field, value);
        });

        // Number Range Queries (>=)
        testQueryCategory("Number Range >=", iterations, () -> {
            String field = ThreadLocalRandom.current().nextBoolean() ? "age" : "salary";
            String value =
                field.equals("age") ? String.valueOf(ThreadLocalRandom.current().nextInt(20, 61))
                    : String.format("%.2f", ThreadLocalRandom.current().nextDouble(30000, 120000));
            return String.format("SELECT * FROM performance_test WHERE %s >= %s", field, value);
        });

        // Number Range Queries (<)
        testQueryCategory("Number Range <", iterations, () -> {
            String field = ThreadLocalRandom.current().nextBoolean() ? "age" : "salary";
            String value =
                field.equals("age") ? String.valueOf(ThreadLocalRandom.current().nextInt(20, 61))
                    : String.format("%.2f", ThreadLocalRandom.current().nextDouble(30000, 120000));
            return String.format("SELECT * FROM performance_test WHERE %s < %s", field, value);
        });

        // Number Range Queries (<=)
        testQueryCategory("Number Range <=", iterations, () -> {
            String field = ThreadLocalRandom.current().nextBoolean() ? "age" : "salary";
            String value =
                field.equals("age") ? String.valueOf(ThreadLocalRandom.current().nextInt(20, 61))
                    : String.format("%.2f", ThreadLocalRandom.current().nextDouble(30000, 120000));
            return String.format("SELECT * FROM performance_test WHERE %s <= %s", field, value);
        });

        // Double Number AND
        testQueryCategory("Double Number AND", iterations, () -> {
            int age = ThreadLocalRandom.current().nextInt(20, 61);
            double salary = ThreadLocalRandom.current().nextDouble(30000, 120000);
            return String.format("SELECT * FROM performance_test WHERE age = %d AND salary = %.2f",
                age, salary);
        });

        // Double Number OR
        testQueryCategory("Double Number OR", iterations, () -> {
            int age = ThreadLocalRandom.current().nextInt(20, 61);
            double salary = ThreadLocalRandom.current().nextDouble(30000, 120000);
            return String.format("SELECT * FROM performance_test WHERE age = %d OR salary = %.2f",
                age, salary);
        });

        // ==== MIXED TYPE QUERIES ====
        System.out.println("\n=== Mixed Type Queries ===");

        // String and Number AND
        testQueryCategory("String-Number AND", iterations, () -> {
            String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
            int age = ThreadLocalRandom.current().nextInt(20, 61);
            return String.format("SELECT * FROM performance_test WHERE city = %s AND age = %d",
                city, age);
        });

        // String and Number OR
        testQueryCategory("String-Number OR", iterations, () -> {
            String name = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
            double salary = ThreadLocalRandom.current().nextDouble(30000, 120000);
            return String.format("SELECT * FROM performance_test WHERE name = %s OR salary = %.2f",
                name, salary);
        });

        // Complex Mixed Query (3 conditions)
        testQueryCategory("Complex Mixed Query", iterations, () -> {
            String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];
            int age = ThreadLocalRandom.current().nextInt(20, 61);
            double salary = ThreadLocalRandom.current().nextDouble(30000, 120000);
            return String.format(
                "SELECT * FROM performance_test WHERE city = %s AND age > %d AND salary <= %.2f",
                city, age, salary);
        });
    }

    private PerformanceMetrics measureBulkInsert(int size) {
        long startTime = System.nanoTime();

        for (int i = 1; i <= size; i++) {
            engine.executeSQL(generateRandomInsert(i));
        }

        long timeElapsed = System.nanoTime() - startTime;
        return new PerformanceMetrics(timeElapsed, size);
    }

    private void testQueryCategory(String category, int iterations,
        java.util.function.Supplier<String> queryGenerator) {

        System.out.println("\nTesting category: " + category);

        long totalTime = 0;
        int totalRows = 0;
        double totalHitRate = 0;
        long maxTime = Long.MIN_VALUE;
        long minTime = Long.MAX_VALUE;

        for (int i = 0; i < iterations; i++) {
            engine.clearCacheMetrics();
            String query = queryGenerator.get();

            long startTime = System.nanoTime();
            String result = engine.executeSQL(query);
            long timeElapsed = System.nanoTime() - startTime;

            maxTime = Math.max(maxTime, timeElapsed);
            minTime = Math.min(minTime, timeElapsed);

            int rowCount = result.split("\n").length - 1;
            double hitRate = engine.getCacheHitRate();

            totalTime += timeElapsed;
            totalRows += rowCount;
            totalHitRate += hitRate;

            if (i % 1000 == 0) {
                System.out.printf("Progress: %d/%d iterations\n", i, iterations);
            }
        }

        // Calculate and print statistics
        double avgTime = (double) totalTime / iterations;
        double avgRows = (double) totalRows / iterations;
        double avgHitRate = totalHitRate / iterations;

        System.out.println("\nResults:");
        System.out.printf("Average Time: %d ms\n", TimeUnit.NANOSECONDS.toMicros((long) avgTime));
        System.out.printf("Min Time: %d ms\n", TimeUnit.NANOSECONDS.toMicros(minTime));
        System.out.printf("Max Time: %d ms\n", TimeUnit.NANOSECONDS.toMicros(maxTime));
        System.out.printf("Average Rows: %.2f\n", avgRows);
        System.out.printf("Average Cache Hit Rate: %.2f%%\n", avgHitRate * 100);
    }

    private String generateRandomInsert(int id) {
        String name = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
        int age = ThreadLocalRandom.current().nextInt(20, 61);
        double salary = ThreadLocalRandom.current().nextDouble(30000, 120000);
        String city = CITIES[ThreadLocalRandom.current().nextInt(CITIES.length)];

        return String.format("INSERT INTO performance_test VALUES (%d, %s, %d, %.2f, %s)", id, name,
            age, salary, city);
    }

    @Test
    @DisplayName("Cache Impact Analysis")
    void testCacheImpact() {
        System.out.println("\n=== Cache Impact Analysis ===");

        // Initialize test data
        measureBulkInsert(SMALL_DATASET);

        // Create and initialize both engines
        BPlusTreeEngine cachedEngine = new BPlusTreeEngine(true);
        cachedEngine.executeSQL("CREATE TABLE performance_test (id, name, age, salary, city)");

        BPlusTreeEngine uncachedEngine = new BPlusTreeEngine(false);
        uncachedEngine.executeSQL("CREATE TABLE performance_test (id, name, age, salary, city)");

        // Initialize both engines with same data
        for (int i = 1; i <= MEDIUM_DATASET; i++) {
            String insert = generateRandomInsert(i);
            cachedEngine.executeSQL(insert);
            uncachedEngine.executeSQL(insert);
        }

        String query = "SELECT * FROM performance_test";
        int iterations = SMALL_DATASET;

        System.out.println("Testing full table scan with " + iterations + " iterations");
        System.out.println("Query: " + query);

        long cachedTime = 0;
        long uncachedTime = 0;

        for (int i = 0; i < iterations; i++) {
            // Test with caching
            long start = System.nanoTime();
            cachedEngine.executeSQL(query);
            cachedTime += System.nanoTime() - start;

            // Test without caching
            start = System.nanoTime();
            uncachedEngine.executeSQL(query);
            uncachedTime += System.nanoTime() - start;

            if (i % 1000 == 0) { // Print progress
                System.out.printf("Progress: %d/%d iterations\n", i, iterations);
            }
        }

        double avgCachedTime = TimeUnit.NANOSECONDS.toMicros(cachedTime / iterations);
        double avgUncachedTime = TimeUnit.NANOSECONDS.toMicros(uncachedTime / iterations);
        double improvement = ((avgUncachedTime - avgCachedTime) / avgUncachedTime) * 100;

        System.out.print("\nResults:\n");
        System.out.printf("Cached avg time: %.2f µs\n", avgCachedTime);
        System.out.printf("Uncached avg time: %.2f µs\n", avgUncachedTime);
        System.out.printf("Performance improvement: %.2f%%\n", improvement);
        System.out.printf("Cache hit rate: %.2f%%\n", cachedEngine.getCacheHitRate() * 100);
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

        PerformanceMetrics rangeMetrics = measureRangeQuery(1, SMALL_DATASET / 2);
        System.out.println("Range Query: " + rangeMetrics);

        PerformanceMetrics complexMetrics = measureComplexQuery();
        System.out.println("Complex Query: " + complexMetrics);

        PerformanceMetrics updateMetrics = measureRandomUpdates(SMALL_DATASET / 10);
        System.out.println("Random Updates: " + updateMetrics);

        PerformanceMetrics deleteMetrics = measureRandomDeletes(SMALL_DATASET / 10);
        System.out.println("Random Deletes: " + deleteMetrics);
    }

    private PerformanceMetrics measureRangeQuery(int start, int end) {
        long startTime = System.nanoTime();

        String result = engine.executeSQL(
            String.format("SELECT * FROM performance_test WHERE id >= %d AND id <= %d", start,
                end));

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
                String.format("UPDATE performance_test SET salary = %.2f WHERE id = %d", newSalary,
                    randomId));

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

        PerformanceMetrics rangeMetrics = measureRangeQuery(1, MEDIUM_DATASET / 2);
        System.out.println("Range Query: " + rangeMetrics);

        PerformanceMetrics complexMetrics = measureComplexQuery();
        System.out.println("Complex Query: " + complexMetrics);

        PerformanceMetrics updateMetrics = measureRandomUpdates(MEDIUM_DATASET / 10);
        System.out.println("Random Updates: " + updateMetrics);

        PerformanceMetrics deleteMetrics = measureRandomDeletes(MEDIUM_DATASET / 10);
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

        PerformanceMetrics rangeMetrics = measureRangeQuery(1, LARGE_DATASET / 2);
        System.out.println("Range Query: " + rangeMetrics);

        PerformanceMetrics complexMetrics = measureComplexQuery();
        System.out.println("Complex Query: " + complexMetrics);

        PerformanceMetrics updateMetrics = measureRandomUpdates(LARGE_DATASET / 10);
        System.out.println("Random Updates: " + updateMetrics);

        PerformanceMetrics deleteMetrics = measureRandomDeletes(LARGE_DATASET / 10);
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

    private static class QueryTestCase {

        final String name;
        final String query;
        final String category;

        QueryTestCase(String name, String query, String category) {
            this.name = name;
            this.query = query;
            this.category = category;
        }
    }

    private static class PerformanceMetrics {

        final String queryName;
        final String category;
        final long executionTimeNanos;
        final int rowsAffected;
        final double hitRate;

        PerformanceMetrics(long timeNanos, int rows) {
            this.executionTimeNanos = timeNanos;
            this.rowsAffected = rows;
            this.queryName = null;
            this.category = null;
            this.hitRate = 0;
        }

        PerformanceMetrics(String queryName, String category, long timeNanos, int rows,
            double hitRate) {
            this.queryName = queryName;
            this.category = category;
            this.executionTimeNanos = timeNanos;
            this.rowsAffected = rows;
            this.hitRate = hitRate;
        }

        @Override
        public String toString() {
            return String.format(
                "%s (%s) - Time: %.2f µs, Rows: %d, Avg time per row: %.2f µs, Cache hit rate: %.2f%%",
                queryName, category, getMicros(), rowsAffected,
                rowsAffected > 0 ? getMicros() / rowsAffected : 0, hitRate * 100);
        }

        double getMicros() {
            return TimeUnit.NANOSECONDS.toMicros(executionTimeNanos);
        }
    }
}