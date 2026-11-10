package com.dsecap.predictor.performance;

import com.dsecap.predictor.model.College;
import com.dsecap.predictor.model.CollegeRecommendation;
import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.repository.CollegeRepository;
import com.dsecap.predictor.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple performance tests without Spring Boot context.
 * Tests core performance metrics for CSV loading, filtering, and prediction operations.
 */
public class SimplePerformanceTest {
    
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
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        performanceMonitor = new PerformanceMonitor();
        cacheService = new CacheService();
        collegeRepository = new CollegeRepository(validator);
        csvDataLoader = new CsvDataLoader(performanceMonitor);
        predictionService = new PredictionService(collegeRepository, performanceMonitor, cacheService);
        datasetGenerator = new LargeDatasetGenerator();
    }
    
    @Test
    void testSmallDatasetPerformance() throws Exception {
        System.out.println("=== Small Dataset Performance Test ===");
        
        // Generate and test with 1,000 records
        File testFile = generateTestDataset("small-dataset.csv", 1000);
        
        performanceMonitor.startOperation("SMALL_DATASET_TEST");
        
        // Load data and measure performance
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Test prediction performance
        Student testStudent = createTestStudent();
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        long totalTime = performanceMonitor.endOperation("SMALL_DATASET_TEST");
        
        // Assertions
        assertNotNull(recommendations);
        assertTrue(totalTime < 5000, "Small dataset should process in under 5 seconds, took: " + totalTime + "ms");
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), "Memory usage should be acceptable");
        
        System.out.println("✓ Small dataset test completed in " + totalTime + "ms with " + recommendations.size() + " recommendations");
        System.out.println("✓ Loaded " + colleges.size() + " college records");
        System.out.println();
    }
    
    @Test
    void testMediumDatasetPerformance() throws Exception {
        System.out.println("=== Medium Dataset Performance Test ===");
        
        // Generate and test with 10,000 records
        File testFile = generateTestDataset("medium-dataset.csv", 10000);
        
        performanceMonitor.startOperation("MEDIUM_DATASET_TEST");
        
        // Load data and measure performance
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Test prediction performance
        Student testStudent = createTestStudent();
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        long totalTime = performanceMonitor.endOperation("MEDIUM_DATASET_TEST");
        
        // Assertions
        assertNotNull(recommendations);
        assertTrue(totalTime < 15000, "Medium dataset should process in under 15 seconds, took: " + totalTime + "ms");
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), "Memory usage should be acceptable");
        
        System.out.println("✓ Medium dataset test completed in " + totalTime + "ms with " + recommendations.size() + " recommendations");
        System.out.println("✓ Loaded " + colleges.size() + " college records");
        System.out.println();
    }
    
    @Test
    void testLargeDatasetPerformance() throws Exception {
        System.out.println("=== Large Dataset Performance Test ===");
        
        // Generate and test with 25,000 records (reduced for faster testing)
        File testFile = generateTestDataset("large-dataset.csv", 25000);
        
        performanceMonitor.startOperation("LARGE_DATASET_TEST");
        
        // Load data and measure performance
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Test prediction performance
        Student testStudent = createTestStudent();
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        long totalTime = performanceMonitor.endOperation("LARGE_DATASET_TEST");
        
        // Assertions
        assertNotNull(recommendations);
        assertTrue(totalTime < 30000, "Large dataset should process in under 30 seconds, took: " + totalTime + "ms");
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), "Memory usage should be acceptable");
        
        System.out.println("✓ Large dataset test completed in " + totalTime + "ms with " + recommendations.size() + " recommendations");
        System.out.println("✓ Loaded " + colleges.size() + " college records");
        System.out.println();
    }
    
    @Test
    void testCacheEffectiveness() throws Exception {
        System.out.println("=== Cache Effectiveness Test ===");
        
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
        assertTrue(secondTime <= firstTime, 
                "Second prediction should be faster or equal due to caching. First: " + firstTime + "ms, Second: " + secondTime + "ms");
        
        // Check cache statistics
        CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
        assertTrue(stats.getTotalCacheSize() > 0, "Cache should contain entries after predictions");
        
        double speedupPercent = ((double)(firstTime - secondTime) / firstTime) * 100;
        System.out.println("✓ Cache effectiveness test completed");
        System.out.println("✓ First prediction: " + firstTime + "ms, Second: " + secondTime + "ms");
        System.out.println("✓ Cache entries: " + stats.getTotalCacheSize());
        System.out.println("✓ Speed improvement: " + String.format("%.1f", speedupPercent) + "%");
        System.out.println();
    }
    
    @Test
    void testMemoryUsageWithLargeDataset() throws Exception {
        System.out.println("=== Memory Usage Test ===");
        
        // Generate large dataset for memory testing
        File testFile = generateTestDataset("memory-test-dataset.csv", 15000);
        
        // Monitor memory before loading
        performanceMonitor.logMemoryUsage("Before loading dataset");
        
        // Load data
        performanceMonitor.startOperation("MEMORY_TEST_LOADING");
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        performanceMonitor.endOperation("MEMORY_TEST_LOADING");
        
        // Monitor memory after loading
        performanceMonitor.logMemoryUsage("After loading dataset");
        
        // Perform multiple predictions to test memory stability
        Student testStudent = createTestStudent();
        for (int i = 0; i < 5; i++) {
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
            assertNotNull(recommendations);
        }
        
        // Monitor memory after predictions
        performanceMonitor.logMemoryUsage("After multiple predictions");
        
        // Memory should still be acceptable
        assertTrue(performanceMonitor.isMemoryUsageAcceptable(), 
                "Memory usage should remain acceptable after large dataset operations");
        
        System.out.println("✓ Memory usage test completed successfully");
        System.out.println("✓ Processed " + colleges.size() + " records with acceptable memory usage");
        System.out.println();
    }
    
    @Test
    void testMultipleStudentProfiles() throws Exception {
        System.out.println("=== Multiple Student Profiles Test ===");
        
        // Generate test dataset
        File testFile = generateTestDataset("multi-student-dataset.csv", 8000);
        
        // Load data
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Create multiple test students with different profiles
        Student[] students = {
                createTestStudent("Computer Engineering", "OPEN", 85.0),
                createTestStudent("Mechanical Engineering", "OBC", 78.0),
                createTestStudent("Civil Engineering", "SC", 72.0),
                createTestStudent("Electronics Engineering", "ST", 70.0),
                createTestStudent("Information Technology", "EWS", 82.0)
        };
        
        performanceMonitor.startOperation("MULTIPLE_STUDENTS_TEST");
        
        int totalRecommendations = 0;
        for (int i = 0; i < students.length; i++) {
            Student student = students[i];
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
            assertNotNull(recommendations);
            totalRecommendations += recommendations.size();
            
            System.out.println("✓ Student " + (i + 1) + " (" + student.getBranch() + ", " + 
                             student.getCategory() + ", " + student.getPercentage() + "%): " + 
                             recommendations.size() + " recommendations");
        }
        
        long totalTime = performanceMonitor.endOperation("MULTIPLE_STUDENTS_TEST");
        
        // Check that cache is being utilized effectively
        CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
        assertTrue(stats.getTotalCacheSize() > 0, "Cache should contain entries from multiple predictions");
        
        System.out.println("✓ Multiple students test completed in " + totalTime + "ms");
        System.out.println("✓ Total recommendations generated: " + totalRecommendations);
        System.out.println("✓ Cache utilization: " + String.format("%.1f", stats.getCacheUtilization()) + "%");
        System.out.println();
    }
    
    @Test
    void testPerformanceSummary() throws Exception {
        System.out.println("=== Performance Summary Test ===");
        
        // Run a comprehensive test and display performance summary
        File testFile = generateTestDataset("summary-test-dataset.csv", 5000);
        
        // Load data
        List<College> colleges = csvDataLoader.loadFromCsv(testFile.getAbsolutePath());
        collegeRepository.loadColleges(colleges);
        
        // Run multiple operations
        Student testStudent = createTestStudent();
        
        for (int i = 0; i < 3; i++) {
            performanceMonitor.startOperation("SUMMARY_PREDICTION_" + (i + 1));
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
            performanceMonitor.endOperation("SUMMARY_PREDICTION_" + (i + 1));
            assertNotNull(recommendations);
        }
        
        // Display comprehensive performance summary
        performanceMonitor.logPerformanceSummary();
        
        // Display cache statistics
        CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
        System.out.println("Final Cache Statistics:");
        System.out.println("- College Filter Cache: " + stats.getCollegeFilterCacheSize() + " entries");
        System.out.println("- Percentage Filter Cache: " + stats.getPercentageFilterCacheSize() + " entries");
        System.out.println("- Total Cache Utilization: " + String.format("%.1f", stats.getCacheUtilization()) + "%");
        
        System.out.println("✓ Performance summary test completed");
        System.out.println();
    }
    
    /**
     * Generates a test dataset file.
     */
    private File generateTestDataset(String filename, int recordCount) throws IOException {
        File testFile = tempDir.resolve(filename).toFile();
        System.out.println("Generating " + recordCount + " records for " + filename + "...");
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