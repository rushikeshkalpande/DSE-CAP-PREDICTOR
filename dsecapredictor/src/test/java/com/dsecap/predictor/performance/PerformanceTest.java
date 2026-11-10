package com.dsecap.predictor.performance;

import com.dsecap.predictor.model.College;
import com.dsecap.predictor.model.CollegeRecommendation;
import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.repository.CollegeRepository;
import com.dsecap.predictor.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the DSE College Predictor application.
 * Tests memory usage, response times, and scalability with large datasets.
 */
@SpringBootTest
public class PerformanceTest {
    
    @Autowired
    private Validator validator;
    
    private CollegeRepository collegeRepository;
    private CsvDataLoader csvDataLoader;
    private PredictionService predictionService;
    private PerformanceMonitor performanceMonitor;
    private CacheService cacheService;
    private LargeDatasetGenerator datasetGenerator;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        performanceMonitor = new PerformanceMonitor();
        cacheService = new CacheService();
        collegeRepository = new CollegeRepository(validator);
        csvDataLoader = new CsvDataLoader(performanceMonitor);
        predictionService = new PredictionService(collegeRepository, performanceMonitor, cacheService);
        datasetGenerator = new LargeDatasetGenerator();
    }
    
    @Test
    void testSmallDatasetPerformance() throws Exception {
        // Generate and test with 1,000 records
        File testFile = generateTestDataset("small-dataset.csv", 1000);
        
        performanceMonitor.startOperation("SMALL_DATASET_TEST");
        
        // Load data and measure performance
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Test prediction performance directly (bypass console input)
        Student testStudent = createTestStudent();
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        long totalTime = performanceMonitor.endOperation("SMALL_DATASET_TEST");
        
        // Assertions
        assertNotNull(recommendations);
        assertTrue(totalTime < 5000, "Small dataset should process in under 5 seconds, took: " + totalTime + "ms");
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), "Memory usage should be acceptable");
        
        System.out.println("Small dataset test completed in " + totalTime + "ms with " + recommendations.size() + " recommendations");
    }
    
    @Test
    void testMediumDatasetPerformance() throws Exception {
        // Generate and test with 10,000 records
        File testFile = generateTestDataset("medium-dataset.csv", 10000);
        
        performanceMonitor.startOperation("MEDIUM_DATASET_TEST");
        
        // Load data and measure performance
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Test prediction performance directly (bypass console input)
        Student testStudent = createTestStudent();
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        long totalTime = performanceMonitor.endOperation("MEDIUM_DATASET_TEST");
        
        // Assertions
        assertNotNull(recommendations);
        assertTrue(totalTime < 10000, "Medium dataset should process in under 10 seconds, took: " + totalTime + "ms");
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), "Memory usage should be acceptable");
        
        System.out.println("Medium dataset test completed in " + totalTime + "ms with " + recommendations.size() + " recommendations");
    }
    
    @Test
    void testLargeDatasetPerformance() throws Exception {
        // Generate and test with 50,000 records
        File testFile = generateTestDataset("large-dataset.csv", 50000);
        
        performanceMonitor.startOperation("LARGE_DATASET_TEST");
        
        // Load data and measure performance
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Test prediction performance directly (bypass console input)
        Student testStudent = createTestStudent();
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        long totalTime = performanceMonitor.endOperation("LARGE_DATASET_TEST");
        
        // Assertions
        assertNotNull(recommendations);
        assertTrue(totalTime < 30000, "Large dataset should process in under 30 seconds, took: " + totalTime + "ms");
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), "Memory usage should be acceptable");
        
        System.out.println("Large dataset test completed in " + totalTime + "ms with " + recommendations.size() + " recommendations");
    }
    
    @Test
    void testCacheEffectiveness() throws Exception {
        // Generate test dataset
        File testFile = generateTestDataset("cache-test-dataset.csv", 5000);
        
        // Load data
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        Student testStudent = createTestStudent();
        
        // First prediction (cache miss)
        performanceMonitor.startOperation("FIRST_PREDICTION");
        List<CollegeRecommendation> recommendations1 = predictionService.predictColleges(testStudent);
        long firstTime = performanceMonitor.endOperation("FIRST_PREDICTION");
        
        // Second prediction (cache hit)
        performanceMonitor.startOperation("SECOND_PREDICTION");
        List<CollegeRecommendation> recommendations2 = predictionService.predictColleges(testStudent);
        long secondTime = performanceMonitor.endOperation("SECOND_PREDICTION");
        
        // Assertions
        assertNotNull(recommendations1);
        assertNotNull(recommendations2);
        assertEquals(recommendations1.size(), recommendations2.size());
        assertTrue(secondTime < firstTime, 
                "Second prediction should be faster due to caching. First: " + firstTime + "ms, Second: " + secondTime + "ms");
        
        // Check cache statistics
        CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
        assertTrue(stats.getTotalCacheSize() > 0, "Cache should contain entries after predictions");
        
        System.out.println("Cache effectiveness test - First: " + firstTime + "ms, Second: " + secondTime + "ms");
        System.out.println("Cache entries: " + stats.getTotalCacheSize());
    }
    
    @Test
    void testMemoryUsageWithLargeDataset() throws Exception {
        // Generate large dataset for memory testing
        File testFile = generateTestDataset("memory-test-dataset.csv", 25000);
        
        // Monitor memory before loading
        performanceMonitor.logMemoryUsage("Before loading large dataset");
        
        // Load data
        performanceMonitor.startOperation("MEMORY_TEST_LOADING");
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        performanceMonitor.endOperation("MEMORY_TEST_LOADING");
        
        // Monitor memory after loading
        performanceMonitor.logMemoryUsage("After loading large dataset");
        
        // Perform multiple predictions to test memory stability
        Student testStudent = createTestStudent();
        for (int i = 0; i < 10; i++) {
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
            assertNotNull(recommendations);
        }
        
        // Monitor memory after predictions
        performanceMonitor.logMemoryUsage("After multiple predictions");
        
        // Memory should still be acceptable
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), 
                "Memory usage should remain acceptable after large dataset operations");
    }
    
    @Test
    void testConcurrentPredictions() throws Exception {
        // Generate test dataset
        File testFile = generateTestDataset("concurrent-test-dataset.csv", 10000);
        
        // Load data
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Create multiple test students
        Student[] students = {
                createTestStudent("Computer Engineering", "OPEN", 85.0),
                createTestStudent("Mechanical Engineering", "OBC", 78.0),
                createTestStudent("Civil Engineering", "SC", 72.0),
                createTestStudent("Electronics Engineering", "ST", 70.0)
        };
        
        performanceMonitor.startOperation("CONCURRENT_PREDICTIONS");
        
        // Simulate concurrent predictions (sequential for simplicity in test)
        for (Student student : students) {
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
            assertNotNull(recommendations);
        }
        
        long totalTime = performanceMonitor.endOperation("CONCURRENT_PREDICTIONS");
        
        // Check that cache is being utilized effectively
        CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
        assertTrue(stats.getTotalCacheSize() > 0, "Cache should contain entries from multiple predictions");
        
        System.out.println("Concurrent predictions test completed in " + totalTime + "ms");
        System.out.println("Cache utilization: " + String.format("%.1f", stats.getCacheUtilization()) + "%");
    }
    
    /**
     * Generates a test dataset file.
     */
    private File generateTestDataset(String filename, int recordCount) throws IOException {
        File testFile = tempDir.resolve(filename).toFile();
        datasetGenerator.generateLargeDataset(testFile.getAbsolutePath(), recordCount);
        return testFile;
    }
    
    /**
     * Creates a test student with default values.
     */
    private Student createTestStudent() {
        return createTestStudent("Computer Engineering", "OPEN", 85.0);
    }
    
    /**
     * Creates a test student with specified parameters.
     */
    private Student createTestStudent(String branch, String category, double percentage) {
        return Student.builder()
                .percentage(percentage)
                .branch(branch)
                .category(category)
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();
    }
}