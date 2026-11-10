package com.dsecap.predictor.service;

import com.dsecap.predictor.model.College;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for caching filtered college results to improve performance on repeated operations.
 * Uses in-memory caching with configurable size limits and automatic cleanup.
 */
@Service
@Slf4j
public class CacheService {
    
    private final ConcurrentMap<String, CacheEntry<List<College>>> collegeFilterCache;
    private final ConcurrentMap<String, CacheEntry<List<College>>> percentageFilterCache;
    
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MILLIS = 300_000; // 5 minutes
    
    public CacheService() {
        this.collegeFilterCache = new ConcurrentHashMap<>();
        this.percentageFilterCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets cached college filter results.
     * 
     * @param branch Engineering branch
     * @param category Student category
     * @return Cached list of colleges or null if not found/expired
     */
    public List<College> getCachedCollegeFilter(String branch, String category) {
        String key = createCollegeFilterKey(branch, category);
        CacheEntry<List<College>> entry = collegeFilterCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit for college filter: branch={}, category={}", branch, category);
            return entry.getValue();
        }
        
        if (entry != null) {
            collegeFilterCache.remove(key);
            log.debug("Cache entry expired and removed: {}", key);
        }
        
        return null;
    }
    
    /**
     * Caches college filter results.
     * 
     * @param branch Engineering branch
     * @param category Student category
     * @param colleges List of filtered colleges
     */
    public void cacheCollegeFilter(String branch, String category, List<College> colleges) {
        if (colleges == null) {
            return;
        }
        
        String key = createCollegeFilterKey(branch, category);
        
        // Check cache size and clean up if necessary
        if (collegeFilterCache.size() >= MAX_CACHE_SIZE) {
            cleanupExpiredEntries(collegeFilterCache);
            
            // If still at capacity, remove oldest entries
            if (collegeFilterCache.size() >= MAX_CACHE_SIZE) {
                removeOldestEntries(collegeFilterCache, MAX_CACHE_SIZE / 4);
            }
        }
        
        collegeFilterCache.put(key, new CacheEntry<>(colleges));
        log.debug("Cached college filter results: branch={}, category={}, count={}", 
                branch, category, colleges.size());
    }
    
    /**
     * Gets cached percentage filter results.
     * 
     * @param studentPercentage Student's percentage
     * @param branch Engineering branch
     * @param category Student category
     * @return Cached list of colleges or null if not found/expired
     */
    public List<College> getCachedPercentageFilter(double studentPercentage, String branch, String category) {
        String key = createPercentageFilterKey(studentPercentage, branch, category);
        CacheEntry<List<College>> entry = percentageFilterCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit for percentage filter: percentage={}, branch={}, category={}", 
                    studentPercentage, branch, category);
            return entry.getValue();
        }
        
        if (entry != null) {
            percentageFilterCache.remove(key);
            log.debug("Cache entry expired and removed: {}", key);
        }
        
        return null;
    }
    
    /**
     * Caches percentage filter results.
     * 
     * @param studentPercentage Student's percentage
     * @param branch Engineering branch
     * @param category Student category
     * @param colleges List of filtered colleges
     */
    public void cachePercentageFilter(double studentPercentage, String branch, String category, List<College> colleges) {
        if (colleges == null) {
            return;
        }
        
        String key = createPercentageFilterKey(studentPercentage, branch, category);
        
        // Check cache size and clean up if necessary
        if (percentageFilterCache.size() >= MAX_CACHE_SIZE) {
            cleanupExpiredEntries(percentageFilterCache);
            
            // If still at capacity, remove oldest entries
            if (percentageFilterCache.size() >= MAX_CACHE_SIZE) {
                removeOldestEntries(percentageFilterCache, MAX_CACHE_SIZE / 4);
            }
        }
        
        percentageFilterCache.put(key, new CacheEntry<>(colleges));
        log.debug("Cached percentage filter results: percentage={}, branch={}, category={}, count={}", 
                studentPercentage, branch, category, colleges.size());
    }
    
    /**
     * Clears all cached data.
     */
    public void clearCache() {
        int collegeFilterSize = collegeFilterCache.size();
        int percentageFilterSize = percentageFilterCache.size();
        
        collegeFilterCache.clear();
        percentageFilterCache.clear();
        
        log.info("Cache cleared: {} college filter entries, {} percentage filter entries", 
                collegeFilterSize, percentageFilterSize);
    }
    
    /**
     * Gets cache statistics for monitoring.
     * 
     * @return Cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
                collegeFilterCache.size(),
                percentageFilterCache.size(),
                MAX_CACHE_SIZE
        );
    }
    
    /**
     * Creates a cache key for college filtering.
     */
    private String createCollegeFilterKey(String branch, String category) {
        return String.format("college:%s:%s", 
                branch != null ? branch.toLowerCase().trim() : "null",
                category != null ? category.toLowerCase().trim() : "null");
    }
    
    /**
     * Creates a cache key for percentage filtering.
     */
    private String createPercentageFilterKey(double studentPercentage, String branch, String category) {
        // Round percentage to 1 decimal place for better cache hit rate
        double roundedPercentage = Math.round(studentPercentage * 10.0) / 10.0;
        return String.format("percentage:%.1f:%s:%s", 
                roundedPercentage,
                branch != null ? branch.toLowerCase().trim() : "null",
                category != null ? category.toLowerCase().trim() : "null");
    }
    
    /**
     * Removes expired entries from the cache.
     */
    private <T> void cleanupExpiredEntries(ConcurrentMap<String, CacheEntry<T>> cache) {
        int removedCount = 0;
        for (String key : cache.keySet()) {
            CacheEntry<T> entry = cache.get(key);
            if (entry != null && entry.isExpired()) {
                cache.remove(key);
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.debug("Cleaned up {} expired cache entries", removedCount);
        }
    }
    
    /**
     * Removes oldest entries from the cache.
     */
    private <T> void removeOldestEntries(ConcurrentMap<String, CacheEntry<T>> cache, int countToRemove) {
        cache.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().getCreationTime(), e2.getValue().getCreationTime()))
                .limit(countToRemove)
                .forEach(entry -> cache.remove(entry.getKey()));
        
        log.debug("Removed {} oldest cache entries to free up space", countToRemove);
    }
    
    /**
     * Cache entry wrapper with expiration support.
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long creationTime;
        
        public CacheEntry(T value) {
            this.value = value;
            this.creationTime = System.currentTimeMillis();
        }
        
        public T getValue() {
            return value;
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime > CACHE_TTL_MILLIS;
        }
    }
    
    /**
     * Cache statistics for monitoring.
     */
    public static class CacheStatistics {
        private final int collegeFilterCacheSize;
        private final int percentageFilterCacheSize;
        private final int maxCacheSize;
        
        public CacheStatistics(int collegeFilterCacheSize, int percentageFilterCacheSize, int maxCacheSize) {
            this.collegeFilterCacheSize = collegeFilterCacheSize;
            this.percentageFilterCacheSize = percentageFilterCacheSize;
            this.maxCacheSize = maxCacheSize;
        }
        
        public int getCollegeFilterCacheSize() {
            return collegeFilterCacheSize;
        }
        
        public int getPercentageFilterCacheSize() {
            return percentageFilterCacheSize;
        }
        
        public int getTotalCacheSize() {
            return collegeFilterCacheSize + percentageFilterCacheSize;
        }
        
        public int getMaxCacheSize() {
            return maxCacheSize;
        }
        
        public double getCacheUtilization() {
            return (double) getTotalCacheSize() / (maxCacheSize * 2) * 100;
        }
    }
}