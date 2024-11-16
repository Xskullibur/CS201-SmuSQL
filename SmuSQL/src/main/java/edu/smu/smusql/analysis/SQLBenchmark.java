package edu.smu.smusql.analysis;

import edu.smu.smusql.IEngine;
import edu.smu.smusql.analysis.utils.AbstractBenchmark;
import edu.smu.smusql.analysis.utils.UniqueIdManager;
import edu.smu.smusql.bplustreeA.bplustreeArray.BPlusTreeArrayEngine;
import edu.smu.smusql.bplustreeA.bplustreeArray.BPlusTree_MultiRange_ArrayEngine;
import edu.smu.smusql.bplustreeA.bplustreeHashmap.BPlusTreeEngine;
import edu.smu.smusql.hashMap.HashMapEngine;
import edu.smu.smusql.skipHash.SkipHashEngine;
import edu.smu.smusql.skipLinkedListIndexed.SkipLinkedListIndexedEngine;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum EngineType {
    BPLUSHASHMAP, BPLUSARRAY, BPLUSMULTIRANGEARRAY, HASHMAP, 
    SKIPHASH, SKIPINDEXED
}

enum QueryType {
    INSERT, SIMPLE_SELECT, UPDATE, DELETE, RANGE_SELECT, EQUALS_SELECT, RANGE_UPDATE, EQUALS_UPDATE
}

// Factory for creating different SQL engine implementations
class SQLEngineFactory {

    public static IEngine createEngine(EngineType type, EngineConfig config) {
        switch (type) {
            case BPLUSHASHMAP:
                return new BPlusTreeEngine((boolean) config.getParameter("useCache"));
            case BPLUSARRAY:
                return new BPlusTreeArrayEngine((boolean) config.getParameter("useCache"));
            case BPLUSMULTIRANGEARRAY:
                return new BPlusTree_MultiRange_ArrayEngine(
                    (boolean) config.getParameter("useCache"));
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

public class SQLBenchmark extends AbstractBenchmark {

    private static final String RESULTS_DIR = "src/main/java/edu/smu/smusql/analysis/visualization/results";
    private static final int REPORTING_INTERVAL = 10000;
    private final UniqueIdManager idManager = new UniqueIdManager();
    private MetricsCollector metricsCollector;

    public SQLBenchmark(EngineType engineType, EngineConfig config, long seed, String fileName) {
        super(SQLEngineFactory.createEngine(engineType, config), seed,
            RESULTS_DIR, fileName);
        this.metricsCollector = new MetricsCollector();
    }

    public static void main(String[] args) {
        long seed = 12345L;
        int numberOfQueries = 100000;

        // Define engines and their configurations
        Map<EngineType, List<EngineConfig>> engineConfigs = new HashMap<>();

        // BPlusTree configurations
        List<EngineConfig> bplusConfigs = Arrays.asList(
            new EngineConfig.Builder().addParameter("useCache", true).build(),
            new EngineConfig.Builder().addParameter("useCache", false).build()
        );

        engineConfigs.put(EngineType.BPLUSHASHMAP, bplusConfigs);
        engineConfigs.put(EngineType.BPLUSARRAY, bplusConfigs);
        engineConfigs.put(EngineType.SKIPHASH,
            Collections.singletonList(new EngineConfig.Builder().build()));
        // engineConfigs.put(EngineType.HASHMAP,
        //     Collections.singletonList(new EngineConfig.Builder().build()));
        // engineConfigs.put(EngineType.SKIPINDEXED,
        //     Collections.singletonList(new EngineConfig.Builder().build()));

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
            case BPLUSHASHMAP:
                return config.getParameter("useCache") != null && (boolean) config.getParameter(
                    "useCache") ? "withcache" : "nocache";
            case BPLUSARRAY:
                return config.getParameter("useCache") != null && (boolean) config.getParameter(
                    "useCache") ? "withcache" : "nocache";
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
            int queryType = random.nextInt(8);
            long startTime = System.nanoTime();
            boolean success = true;

            try {
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
                success = false;
                System.err.println("Query failed" + queryType + ": " + e.getMessage());
            }

            long endTime = System.nanoTime();
            QueryType type = QueryType.values()[queryType];
            metricsCollector.recordQuery(type, endTime - startTime, success);

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
        System.out.printf("%nCompleted %s-%s benchmark:%n", engineType, configStr);
        System.out.println("----------------------------------------");
    }

    private void writeMetricsToCSV(int queryCount) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try (FileWriter fw = new FileWriter(outputFile, true)) {
            for (QueryType type : QueryType.values()) {
                QueryMetrics metrics = metricsCollector.getMetrics(type);
                if (metrics.getTotalQueries() > 0) {
                    fw.write(String.format("%s,%s,%d,%s,%.2f,%.2f,%.2f\n",
                        timestamp,
                        engine.getClass().getSimpleName(),
                        metrics.getTotalQueries(),         // <-- Use individual query type count
                        type.name(),
                        metrics.getAverageExecutionTime(),
                        metrics.getTotalExecutionTime(),
                        metrics.getSuccessRate()));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to write metrics to CSV: " + e.getMessage());
        }
    }

    @Override
    protected String getCSVHeader() {
        return "Timestamp,EngineType,QueryCount,QueryType,AverageExecutionTime,TotalExecutionTime,SuccessRate\n";
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

}

class QueryMetrics {
    private long totalExecutionTime;
    private int totalQueries;
    private int successfulQueries;

    public void recordQuery(long executionTime, boolean success) {
        totalExecutionTime += executionTime;
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
}

class MetricsCollector {

    private final Map<QueryType, QueryMetrics> metricsMap;

    public MetricsCollector() {
        metricsMap = new EnumMap<>(QueryType.class);
        for (QueryType type : QueryType.values()) {
            metricsMap.put(type, new QueryMetrics());
        }
    }

    public void recordQuery(QueryType type, long executionTime, boolean success) {
        metricsMap.get(type).recordQuery(executionTime, success);
    }

    public QueryMetrics getMetrics(QueryType type) {
        return metricsMap.get(type);
    }
}