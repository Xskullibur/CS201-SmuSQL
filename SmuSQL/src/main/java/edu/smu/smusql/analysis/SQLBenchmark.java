package edu.smu.smusql.analysis;

import edu.smu.smusql.IEngine;
import edu.smu.smusql.bplustreeA.BPlusTreeEngine;
import edu.smu.smusql.hashMap.HashMapEngine;
import edu.smu.smusql.skipHash.SkipHashEngine;
import edu.smu.smusql.skipLinkedListIndexed.SkipLinkedListIndexedEngine;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

enum EngineType {
    BPLUS, HASHMAP, SKIPHASH, SKIPINDEXED
}

enum QueryType {
    INSERT, SIMPLE_SELECT, UPDATE, DELETE, RANGE_SELECT, EQUALS_SELECT, RANGE_UPDATE, EQUALS_UPDATE
}

// Factory for creating different SQL engine implementations
class SQLEngineFactory {

    public static IEngine createEngine(EngineType type, EngineConfig config) {
        switch (type) {
            case BPLUS:
                return new BPlusTreeEngine((boolean) config.getParameter("useCache"));
            case HASHMAP:
                return new HashMapEngine();
            case SKIPHASH:
                return new SkipHashEngine();
            case SKIPINDEXED:
                return new SkipLinkedListIndexedEngine();
            default:
                throw new IllegalArgumentException("Unsupported engine type: " + type);
        }
    }
}

class EngineConfig {

    private final Map<String, Object> additionalParams;

    private EngineConfig(Builder builder) {
        this.additionalParams = builder.additionalParams;
    }

    public Object getParameter(String key) {
        return additionalParams.get(key);
    }

    public static class Builder {

        private Map<String, Object> additionalParams = new HashMap<>();

        public Builder addParameter(String key, Object value) {
            this.additionalParams.put(key, value);
            return this;
        }

        public EngineConfig build() {
            return new EngineConfig(this);
        }
    }
}

public class SQLBenchmark {

    private static final String RESULTS_DIR = "src/main/java/edu/smu/smusql/analysis/visualization/results";
    private static final int REPORTING_INTERVAL = 10000;
    private final IEngine engine;
    private final Random random;
    private final String outputFile;
    private final MemoryMXBean memoryBean;
    private MetricsCollector metricsCollector;

    public SQLBenchmark(EngineType engineType, EngineConfig config, long seed, String fileName) {
        this.engine = SQLEngineFactory.createEngine(engineType, config);
        this.random = new Random(seed);
        this.outputFile = RESULTS_DIR + File.separator + fileName;
        this.metricsCollector = new MetricsCollector();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        initializeCSV();
    }

    private void initializeCSV() {

        File directory = new File(RESULTS_DIR);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create results directory: " + RESULTS_DIR);
            }
        }

        try (FileWriter fw = new FileWriter(outputFile)) {
            fw.write("Timestamp,EngineType,QueryCount,QueryType,AverageExecutionTime,"
                + "TotalExecutionTime,HeapMemoryUsed,HeapMemoryDelta,NonHeapMemoryUsed,SuccessRate\n");
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialisze CSV file", e);
        }
    }

    public static void main(String[] args) {
        long seed = 12345L;
        int numberOfQueries = 100000;

        // Define engines and their configurations
        Map<EngineType, List<EngineConfig>> engineConfigs = new HashMap<>();

        // BPlusTree configurations
        List<EngineConfig> bplusConfigs = Arrays.asList(
            new EngineConfig.Builder().addParameter("useCache", true).build(),
            new EngineConfig.Builder().addParameter("useCache", false).build());

        engineConfigs.put(EngineType.BPLUS, bplusConfigs);
        engineConfigs.put(EngineType.SKIPHASH,
            Collections.singletonList(new EngineConfig.Builder().build()));
        engineConfigs.put(EngineType.HASHMAP,
            Collections.singletonList(new EngineConfig.Builder().build()));
        engineConfigs.put(EngineType.SKIPINDEXED,
            Collections.singletonList(new EngineConfig.Builder().build()));

        // Run benchmarks for each engine and configuration
        for (Map.Entry<EngineType, List<EngineConfig>> entry : engineConfigs.entrySet()) {
            EngineType engineType = entry.getKey();
            List<EngineConfig> configs = entry.getValue();

            for (EngineConfig config : configs) {
                String configStr = getConfigString(engineType, config);
                String fileName = String.format("%s_%s_results.csv",
                    engineType.name().toLowerCase(), configStr);

                System.out.printf("Starting benchmark for %s with config: %s%n", engineType,
                    configStr);

                SQLBenchmark benchmark = new SQLBenchmark(engineType, config, seed, fileName);

                benchmark.runBenchmark(numberOfQueries);
                benchmark.cleanupAfterBenchmark(engineType, configStr);
            }
        }
    }

    private static String getConfigString(EngineType engineType, EngineConfig config) {
        switch (engineType) {
            case BPLUS:
                return config.getParameter("useCache") != null && (boolean) config.getParameter(
                    "useCache") ? "withcache" : "nocache";
            // Add cases for other engine types as needed
            default:
                return "default";
        }
    }

    public void runBenchmark(int numberOfQueries) {
        setupTables();
        System.out.println("Prepopulating tables...");
        prepopulateTables();

        // Warmup phase
        System.out.println("Warming up the JVM...");
        int warmupQueries = Math.min(numberOfQueries / 10, 1000);
        for (int i = 0; i < warmupQueries; i++) {
            try {
                int queryType = random.nextInt(8);
                switch (queryType) {
                    case 0:
                        executeInsert();
                        break;
                    case 1:
                        executeSimpleSelect();
                        break;
                    case 2:
                        executeUpdate();
                        break;
                    case 3:
                        executeDelete();
                        break;
                    case 4:
                        executeRangeSelect();
                        break;
                    case 5:
                        executeEqualsSelect();
                        break;
                    case 6:
                        executeRangeUpdate();
                        break;
                    case 7:
                        executeEqualsUpdate();
                        break;
                }
            } catch (Exception e) {
                // Swallow exceptions
            }
        }

        System.out.println("Starting benchmark with " + numberOfQueries + " queries on engine: "
            + engine.getClass().getSimpleName());

        // Reset metrics after warmup
        metricsCollector = new MetricsCollector();

        for (int i = 0; i < numberOfQueries; i++) {
            int queryType = random.nextInt(8); // Updated to handle 8 query types
            long startTime = System.nanoTime();
            boolean success = true;
            long memoryDelta = 0;

            try {
                switch (queryType) {
                    case 0:
                        memoryDelta = measureMemoryUsage(this::executeInsert);
                        break;
                    case 1:
                        memoryDelta = measureMemoryUsage(this::executeSimpleSelect);
                        break;
                    case 2:
                        memoryDelta = measureMemoryUsage(this::executeUpdate);
                        break;
                    case 3:
                        memoryDelta = measureMemoryUsage(this::executeDelete);
                        break;
                    case 4:
                        memoryDelta = measureMemoryUsage(this::executeRangeSelect);
                        break;
                    case 5:
                        memoryDelta = measureMemoryUsage(this::executeEqualsSelect);
                        break;
                    case 6:
                        memoryDelta = measureMemoryUsage(this::executeRangeUpdate);
                        break;
                    case 7:
                        memoryDelta = measureMemoryUsage(this::executeEqualsUpdate);
                        break;
                }
            } catch (Exception e) {
                success = false;
                System.err.println("Query failed" + queryType + ": " + e.getMessage());
            }

            long endTime = System.nanoTime();
            QueryType type = QueryType.values()[queryType];
            metricsCollector.recordQuery(type, endTime - startTime, memoryDelta, success);

            if ((i + 1) % REPORTING_INTERVAL == 0) {
                writeMetricsToCSV(i + 1);
                System.out.println("Processed " + (i + 1) + " queries...");
            }
        }

        writeMetricsToCSV(numberOfQueries);
        System.out.println("Benchmark completed. Results written to: " + outputFile);
    }

    private void cleanupAfterBenchmark(EngineType engineType, String configStr) {

        // Clear the engine's data
        engine.clearDatabase();

        // Perform garbage collection
        performGC();

        // Log memory stats after cleanup
        Runtime rt = Runtime.getRuntime();
        long usedMemory = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.printf("%nCompleted %s-%s benchmark:%n", engineType, configStr);
        System.out.printf("Final memory usage after cleanup: %dMB%n", usedMemory);
        System.out.println("----------------------------------------");
    }

    private void setupTables() {
        engine.executeSQL("CREATE TABLE users (id, name, age, city)");
        engine.executeSQL("CREATE TABLE products (id, name, price, category)");
        engine.executeSQL("CREATE TABLE orders (id, user_id, product_id, quantity)");
    }

    private void prepopulateTables() {
        // Prepopulate users with more varied data
        for (int i = 0; i < 100; i++) {
            int id = i + 1;
            String name = "User" + id;
            int age = 20 + random.nextInt(41); // Ages 20-60
            String city = getRandomCity();
            engine.executeSQL(
                String.format("INSERT INTO users VALUES (%d, '%s', %d, '%s')", id, name, age,
                    city));
        }

        // Prepopulate products with more varied data
        for (int i = 0; i < 100; i++) {
            int id = i + 1;
            String name = "Product" + id;
            double price = 50 + (random.nextDouble() * 950); // Prices $50-$1000
            String category = getRandomCategory();
            engine.executeSQL(
                String.format("INSERT INTO products VALUES (%d, '%s', %.2f, '%s')", id, name, price,
                    category));
        }

        // Prepopulate orders
        for (int i = 0; i < 100; i++) {
            int id = i + 1;
            int userId = random.nextInt(100) + 1;
            int productId = random.nextInt(100) + 1;
            int quantity = random.nextInt(10) + 1;
            engine.executeSQL(
                String.format("INSERT INTO orders VALUES (%d, %d, %d, %d)", id, userId, productId,
                    quantity));
        }
    }

    private void executeInsert() {
        int tableChoice = random.nextInt(3);
        switch (tableChoice) {
            case 0:
                insertUser();
                break;
            case 1:
                insertProduct();
                break;
            case 2:
                insertOrder();
                break;
        }
    }

    private void executeSimpleSelect() {
        String[] tables = {"users", "products", "orders"};
        String table = tables[random.nextInt(tables.length)];
        engine.executeSQL("SELECT * FROM " + table);
    }

    private void executeUpdate() {
        int tableChoice = random.nextInt(3);
        switch (tableChoice) {
            case 0:
                updateUser();
                break;
            case 1:
                updateProduct();
                break;
            case 2:
                updateOrder();
                break;
        }
    }

    private void executeDelete() {
        String[] tables = {"users", "products", "orders"};
        String table = tables[random.nextInt(tables.length)];
        int id = random.nextInt(10000) + 1;
        engine.executeSQL(String.format("DELETE FROM %s WHERE id = %d", table, id));
    }

    private void executeRangeSelect() {
        if (random.nextBoolean()) {
            // Age range query
            int minAge = random.nextInt(20) + 20;
            int maxAge = minAge + random.nextInt(30);
            engine.executeSQL(
                String.format("SELECT * FROM users WHERE age >= %d AND age <= %d", minAge, maxAge));
        } else {
            // Price range query
            double minPrice = 50 + (random.nextDouble() * 200);
            double maxPrice = minPrice + random.nextDouble() * 500;
            engine.executeSQL(
                String.format("SELECT * FROM products WHERE price >= %.2f AND price <= %.2f",
                    minPrice, maxPrice));
        }
    }

    // New equality-based SELECT method
    private void executeEqualsSelect() {
        if (random.nextBoolean()) {
            // City equality query
            String city = getRandomCity();
            engine.executeSQL(String.format("SELECT * FROM users WHERE city = '%s'", city));
        } else {
            // Category equality query
            String category = getRandomCategory();
            engine.executeSQL(
                String.format("SELECT * FROM products WHERE category = '%s'", category));
        }
    }

    // New range-based UPDATE method
    private void executeRangeUpdate() {
        if (random.nextBoolean()) {
            // Update ages within a range
            int minAge = random.nextInt(20) + 20;
            int maxAge = minAge + random.nextInt(30);
            int newAge = random.nextInt(60) + 20;
            engine.executeSQL(
                String.format("UPDATE users SET age = %d WHERE age >= %d AND age <= %d", newAge,
                    minAge, maxAge));
        } else {
            // Update prices within a range
            double minPrice = 50 + (random.nextDouble() * 200);
            double maxPrice = minPrice + random.nextDouble() * 500;
            double newPrice = 50 + (random.nextDouble() * 1000);
            engine.executeSQL(String.format(
                "UPDATE products SET price = %.2f WHERE price >= %.2f AND price <= %.2f", newPrice,
                minPrice, maxPrice));
        }
    }

    // New equality-based UPDATE method
    private void executeEqualsUpdate() {
        if (random.nextBoolean()) {
            // Update all users in a specific city
            String city = getRandomCity();
            int newAge = random.nextInt(60) + 20;
            engine.executeSQL(
                String.format("UPDATE users SET age = %d WHERE city = '%s'", newAge, city));
        } else {
            // Update all products in a specific category
            String category = getRandomCategory();
            double newPrice = 50 + (random.nextDouble() * 1000);
            engine.executeSQL(
                String.format("UPDATE products SET price = %.2f WHERE category = '%s'", newPrice,
                    category));
        }
    }

    private long measureMemoryUsage(Runnable operation) {
        // Record memory before operation
        long beforeMemory = memoryBean.getHeapMemoryUsage().getUsed();

        // Time the operation execution with warmup
        try {
            operation.run();
        } catch (Exception e) {
            System.err.println("Operation failed: " + e.getMessage());
        }

        // Record memory after operation
        long afterMemory = memoryBean.getHeapMemoryUsage().getUsed();
        return afterMemory - beforeMemory;
    }

    private void writeMetricsToCSV(int queryCount) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try (FileWriter fw = new FileWriter(outputFile, true)) {
            for (QueryType type : QueryType.values()) {
                QueryMetrics metrics = metricsCollector.getMetrics(type);
                if (metrics.getTotalQueries() > 0) {
                    fw.write(String.format("%s,%s,%d,%s,%.2f,%.2f,%d,%d,%d,%.2f\n", timestamp,
                        engine.getClass().getSimpleName(), queryCount, type.name(),
                        metrics.getAverageExecutionTime(), metrics.getTotalExecutionTime(),
                        heapMemory.getUsed(), metrics.getTotalMemoryDelta(),
                        nonHeapMemory.getUsed(), metrics.getSuccessRate()));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to write metrics to CSV: " + e.getMessage());
        }
    }

    private static void performGC() {
        System.out.println("Performing garbage collection...");
        System.gc();
        System.runFinalization();
        System.gc();

        // Small pause to let GC complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getRandomCity() {
        String[] cities = {"New York", "Los Angeles", "Chicago", "Boston", "Miami", "Seattle",
            "Austin", "Dallas", "Atlanta", "Denver"};
        return cities[random.nextInt(cities.length)];
    }

    private String getRandomCategory() {
        String[] categories = {"Electronics", "Appliances", "Clothing", "Furniture", "Toys",
            "Sports", "Books", "Beauty", "Garden"};
        return categories[random.nextInt(categories.length)];
    }

    private void insertUser() {
        int id = random.nextInt(10000) + 10000;
        String name = "User" + id;
        int age = random.nextInt(60) + 20;
        String city = getRandomCity();
        engine.executeSQL(
            String.format("INSERT INTO users VALUES (%d, '%s', %d, '%s')", id, name, age, city));
    }

    private void insertProduct() {
        int id = random.nextInt(1000) + 10000;
        String name = "Product" + id;
        double price = 50 + (random.nextDouble() * 1000);
        String category = getRandomCategory();
        engine.executeSQL(
            String.format("INSERT INTO products VALUES (%d, '%s', %.2f, '%s')", id, name, price,
                category));
    }

    private void insertOrder() {
        int id = random.nextInt(10000) + 1;
        int userId = random.nextInt(10000) + 1;
        int productId = random.nextInt(1000) + 1;
        int quantity = random.nextInt(10) + 1;
        engine.executeSQL(
            String.format("INSERT INTO orders VALUES (%d, %d, %d, %d)", id, userId, productId,
                quantity));
    }

    private void updateUser() {
        int id = random.nextInt(10000) + 1;
        int newAge = random.nextInt(60) + 20;
        engine.executeSQL(String.format("UPDATE users SET age = %d WHERE id = %d", newAge, id));
    }

    private void updateProduct() {
        int id = random.nextInt(1000) + 1;
        double newPrice = 50 + (random.nextDouble() * 1000);
        engine.executeSQL(
            String.format("UPDATE products SET price = %.2f WHERE id = %d", newPrice, id));
    }

    private void updateOrder() {
        int id = random.nextInt(10000) + 1;
        int newQuantity = random.nextInt(10) + 1;
        engine.executeSQL(
            String.format("UPDATE orders SET quantity = %d WHERE id = %d", newQuantity, id));
    }
}

class QueryMetrics {

    private long totalExecutionTime;
    private long totalMemoryDelta;
    private int totalQueries;
    private int successfulQueries;

    public void recordQuery(long executionTime, long memoryDelta, boolean success) {
        totalExecutionTime += executionTime;
        totalMemoryDelta += memoryDelta;
        totalQueries++;
        if (success) {
            successfulQueries++;
        }
    }

    public double getAverageExecutionTime() {
        return totalQueries > 0 ? (double) totalExecutionTime / totalQueries / 1_000_000
            : 0.0; // Convert to milliseconds
    }

    public double getTotalExecutionTime() {
        return (double) totalExecutionTime / 1_000_000; // Convert to milliseconds
    }

    public double getSuccessRate() {
        return totalQueries > 0 ? (double) successfulQueries / totalQueries * 100 : 0.0;
    }

    public int getTotalQueries() {
        return totalQueries;
    }

    public long getTotalMemoryDelta() {
        return totalMemoryDelta;
    }

    public double getAverageMemoryDelta() {
        return totalQueries > 0 ? (double) totalMemoryDelta / totalQueries : 0.0;
    }
}

class MetricsCollector {

    private final Map<QueryType, QueryMetrics> metricsMap;

    public MetricsCollector() {
        metricsMap = new EnumMap<>(QueryType.class);
        for (QueryType type : QueryType.values()) {
            metricsMap.put(type, new QueryMetrics());
        }
    }

    public void recordQuery(QueryType type, long executionTime, long memoryDelta, boolean success) {
        metricsMap.get(type).recordQuery(executionTime, memoryDelta, success);
    }

    public QueryMetrics getMetrics(QueryType type) {
        return metricsMap.get(type);
    }
}