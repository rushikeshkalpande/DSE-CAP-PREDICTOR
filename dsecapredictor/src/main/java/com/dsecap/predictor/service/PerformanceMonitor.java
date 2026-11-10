package com.dsecap.predictor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service for monitoring application performance including memory usage and execution times.
 * Provides detailed metrics for optimization and troubleshooting.
 */
@Component
@Slf4j
public class PerformanceMonitor {
    
    private final MemoryMXBean memoryBean;
    private final Map<String, Long> operationStartTimes;
    private final Map<String, PerformanceMetrics> operationMetrics;
    
    public PerformanceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.operationStartTimes = new ConcurrentHashMap<>();
        this.operationMetrics = new ConcurrentHashMap<>();
    }
    
    /**
     * Starts timing an operation.
     * 
     * @param operationName Name of the operation to time
     */
    public void startOperation(String operationName) {
        operationStartTimes.put(operationName, System.nanoTime());
        logMemoryUsage("Before " + operationName);
    }
    
    /**
     * Ends timing an operation and logs the results.
     * 
     * @param operationName Name of the operation that completed
     * @return Duration in milliseconds
     */
    public long endOperation(String operationName) {
        Long startTime = operationStartTimes.remove(operationName);
        if (startTime == null) {
            log.warn("No start time found for operation: {}", operationName);
            return 0;
        }
        
        long durationNanos = System.nanoTime() - startTime;
        long durationMillis = durationNanos / 1_000_000;
        
        // Update metrics
        PerformanceMetrics metrics = operationMetrics.computeIfAbsent(
            operationName, 
            k -> new PerformanceMetrics()
        );
        metrics.addMeasurement(durationMillis);
        
        // Log performance results
        logMemoryUsage("After " + operationName);
        log.info("Operation '{}' completed in {} ms (avg: {} ms, count: {})", 
                operationName, durationMillis, metrics.getAverageTime(), metrics.getCount());
        
        return durationMillis;
    }
    
    /**
     * Logs current memory usage with detailed breakdown.
     * 
     * @param context Context description for the memory measurement
     */
    public void logMemoryUsage(String context) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        long heapUsedMB = heapUsage.getUsed() / (1024 * 1024);
        long heapMaxMB = heapUsage.getMax() / (1024 * 1024);
        long heapCommittedMB = heapUsage.getCommitted() / (1024 * 1024);
        
        double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        
        log.info("Memory Usage [{}]: Heap: {} MB / {} MB ({}% used, {} MB committed), Non-Heap: {} MB", 
                context,
                heapUsedMB, 
                heapMaxMB, 
                String.format("%.1f", heapUsagePercent),
                heapCommittedMB,
                nonHeapUsage.getUsed() / (1024 * 1024));
        
        // Warn if memory usage is high
        if (heapUsagePercent > 80) {
            log.warn("High memory usage detected: {}% of heap memory in use", 
                    String.format("%.1f", heapUsagePercent));
        }
    }
    
    /**
     * Gets performance metrics for a specific operation.
     * 
     * @param operationName Name of the operation
     * @return Performance metrics or null if operation not found
     */
    public PerformanceMetrics getMetrics(String operationName) {
        return operationMetrics.get(operationName);
    }
    
    /**
     * Logs a summary of all performance metrics.
     */
    public void logPerformanceSummary() {
        log.info("=== Performance Summary ===");
        
        if (operationMetrics.isEmpty()) {
            log.info("No performance metrics recorded");
            return;
        }
        
        operationMetrics.forEach((operation, metrics) -> {
            log.info("Operation '{}': {} executions, avg: {} ms, min: {} ms, max: {} ms", 
                    operation, 
                    metrics.getCount(),
                    metrics.getAverageTime(),
                    metrics.getMinTime(),
                    metrics.getMaxTime());
        });
        
        logMemoryUsage("Final");
    }
    
    /**
     * Clears all performance metrics.
     */
    public void clearMetrics() {
        operationStartTimes.clear();
        operationMetrics.clear();
    }
    
    /**
     * Checks if memory usage is within acceptable limits.
     * 
     * @return true if memory usage is acceptable, false if concerning
     */
    public boolean isMemoryUsageAcceptable() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        return heapUsagePercent < 85; // Consider 85% as the threshold
    }
    
    /**
     * Forces garbage collection and logs memory usage before and after.
     * Should be used sparingly and only for performance testing.
     */
    public void forceGarbageCollection() {
        logMemoryUsage("Before GC");
        System.gc();
        try {
            Thread.sleep(100); // Give GC time to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logMemoryUsage("After GC");
    }
    
    /**
     * Inner class to track performance metrics for operations.
     */
    public static class PerformanceMetrics {
        private long totalTime = 0;
        private long count = 0;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = Long.MIN_VALUE;
        
        public void addMeasurement(long timeMillis) {
            totalTime += timeMillis;
            count++;
            minTime = Math.min(minTime, timeMillis);
            maxTime = Math.max(maxTime, timeMillis);
        }
        
        public long getAverageTime() {
            return count > 0 ? totalTime / count : 0;
        }
        
        public long getCount() {
            return count;
        }
        
        public long getMinTime() {
            return minTime == Long.MAX_VALUE ? 0 : minTime;
        }
        
        public long getMaxTime() {
            return maxTime == Long.MIN_VALUE ? 0 : maxTime;
        }
        
        public long getTotalTime() {
            return totalTime;
        }
    }
}