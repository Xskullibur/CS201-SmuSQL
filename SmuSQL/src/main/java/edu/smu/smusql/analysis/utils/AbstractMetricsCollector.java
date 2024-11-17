package edu.smu.smusql.analysis.utils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractMetricsCollector {
    protected final List<Long> responseTimes = new ArrayList<>();
    protected int totalOperations = 0;
    protected int failedOperations = 0;

    public synchronized void recordOperation(long responseTime, boolean success) {
        responseTimes.add(responseTime);
        totalOperations++;
        if (!success) {
            failedOperations++;
        }
    }

    protected double calculateP95ResponseTime() {
        if (responseTimes.isEmpty()) {
            return 0.0;
        }
        List<Long> sortedTimes = new ArrayList<>(responseTimes);
        Collections.sort(sortedTimes);
        int p95Index = (int) Math.ceil(0.95 * sortedTimes.size()) - 1;
        return sortedTimes.get(p95Index) / 1_000_000.0; // Convert to ms
    }

    protected double calculateErrorRate() {
        return totalOperations > 0 ? (failedOperations * 100.0) / totalOperations : 0;
    }

    protected double calculateAverageLatency() {
        return responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0; // Convert to ms
    }

    protected long getGCPauseTime() {
        return ManagementFactory.getGarbageCollectorMXBeans()
            .stream()
            .mapToLong(gc -> gc.getCollectionTime())
            .sum();
    }
}