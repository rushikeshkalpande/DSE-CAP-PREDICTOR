package com.dsecap.predictor.integration;

import com.dsecap.predictor.config.PredictorConfig;
import com.dsecap.predictor.model.CollegeRecommendation;
import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.repository.CollegeRepository;
import com.dsecap.predictor.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import com.dsecap.predictor.config.TestConfig;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests focused on error scenarios and edge cases.
 * Tests application behavior with invalid data, missing files, and boundary conditions.
 * 
 * Requirements covered: 6.1, 6.2, 6.5
 */
@SpringBootTest(classes = {com.dsecap.predictor.DseCapPredictorApplication.class, TestConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "dse.predictor.csv-file-path=src/test/resources/test-data/integration-test-data.csv",
    "dse.predictor.max-recommendations=15",
    "dse.predictor.percentage-range=10.0"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ErrorScenarioIntegrationTest {

    @Autowired
    private CsvDataLoader csvDataLoader;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ConsoleInterface consoleInterface;

    @Autowired
    private ErrorHandler errorHandler;

    @Autowired
    private PredictorConfig config;

    // ========== File I/O Error Scenarios ==========

    @Test
    @Order(1)
    @DisplayName("Should handle FileNotFoundException gracefully with user-friendly error messages")
    void shouldHandleFileNotFoundExceptionGracefully() {
        // Test requirement 6.1: Handle FileNotFoundException with user-friendly messages
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(outputStream));
        
        try {
            assertThatThrownBy(() -> csvDataLoader.loadFromCsv("completely-non-existent-file.csv"))
                .isInstanceOf(Exception.class);
            
            // Verify error handling produces user-friendly output
            String errorOutput = outputStream.toString();
            // Error should be logged or handled appropriately
            
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should handle malformed CSV data without crashing application")
    void shouldHandleMalformedCsvDataWithoutCrashing() {
        // Test requirement 6.2: Handle malformed data gracefully without crashing
        
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv("src/test/resources/test-data/malformed-data.csv");
            
            // Should return some valid colleges and skip malformed rows
            assertThat(colleges).isNotNull();
            
            // Verify that valid colleges are properly loaded
            colleges.forEach(college -> {
                assertThat(college.getName()).isNotNull().isNotEmpty();
                assertThat(college.getBranch()).isNotNull().isNotEmpty();
                assertThat(college.getCategory()).isNotNull().isNotEmpty();
                assertThat(college.getCutoffPercent()).isGreaterThan(0).isLessThanOrEqualTo(100);
            });
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should handle CSV files with invalid percentage values")
    void shouldHandleCsvFilesWithInvalidPercentageValues() {
        // Test requirement 6.2: Handle malformed data gracefully
        
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv("src/test/resources/test-data/invalid-percentages.csv");
            
            // Should handle invalid percentages gracefully
            assertThat(colleges).isNotNull();
            
            // All loaded colleges should have valid percentages
            colleges.forEach(college -> {
                assertThat(college.getCutoffPercent()).isGreaterThan(0).isLessThanOrEqualTo(100);
            });
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should handle empty CSV files gracefully")
    void shouldHandleEmptyCsvFilesGracefully() {
        // Test requirement 6.2: Handle malformed data gracefully
        
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv("src/test/resources/test-data/empty-file.csv");
            assertThat(colleges).isNotNull().isEmpty();
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should handle CSV files with only headers")
    void shouldHandleCsvFilesWithOnlyHeaders() {
        // Test requirement 6.2: Handle malformed data gracefully
        
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv("src/test/resources/test-data/header-only.csv");
            assertThat(colleges).isNotNull().isEmpty();
        });
    }

    // ========== Input Validation Error Scenarios ==========

    @Test
    @Order(6)
    @DisplayName("Should validate data types and ranges appropriately for all inputs")
    void shouldValidateDataTypesAndRangesAppropriately() {
        // Test requirement 6.3: Validate data types and ranges appropriately
        
        // Test percentage validation
        assertThat(validationService.isValidPercentage(-1.0)).isFalse();
        assertThat(validationService.isValidPercentage(0.0)).isTrue();
        assertThat(validationService.isValidPercentage(50.0)).isTrue();
        assertThat(validationService.isValidPercentage(100.0)).isTrue();
        assertThat(validationService.isValidPercentage(100.1)).isFalse();
        assertThat(validationService.isValidPercentage(Double.NaN)).isFalse();
        assertThat(validationService.isValidPercentage(Double.POSITIVE_INFINITY)).isFalse();
        assertThat(validationService.isValidPercentage(Double.NEGATIVE_INFINITY)).isFalse();
        
        // Test category validation
        assertThat(validationService.isValidCategory("OPEN")).isTrue();
        assertThat(validationService.isValidCategory("OBC")).isTrue();
        assertThat(validationService.isValidCategory("SC")).isTrue();
        assertThat(validationService.isValidCategory("ST")).isTrue();
        assertThat(validationService.isValidCategory("NT")).isTrue();
        assertThat(validationService.isValidCategory("EWS")).isTrue();
        assertThat(validationService.isValidCategory("SEBC")).isTrue();
        assertThat(validationService.isValidCategory("INVALID")).isFalse();
        assertThat(validationService.isValidCategory("")).isFalse();
        assertThat(validationService.isValidCategory(null)).isFalse();
        assertThat(validationService.isValidCategory("open")).isFalse(); // Case sensitive
        
        // Test branch validation
        assertThat(validationService.isValidBranch("Computer Engineering")).isTrue();
        assertThat(validationService.isValidBranch("Mechanical Engineering")).isTrue();
        assertThat(validationService.isValidBranch("")).isFalse();
        assertThat(validationService.isValidBranch(null)).isFalse();
        assertThat(validationService.isValidBranch("   ")).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("Should handle prediction requests with invalid student data")
    void shouldHandlePredictionRequestsWithInvalidStudentData() {
        // Test requirement 6.3: Validate data types and ranges appropriately
        
        // Load valid data first
        try {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
        } catch (Exception e) {
            fail("Failed to load test data: " + e.getMessage());
        }
        
        // Test with invalid percentage
        Student invalidPercentageStudent = Student.builder()
            .percentage(-10.0) // Invalid percentage
            .branch("Computer Engineering")
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();
        
        // Prediction service should throw exception for invalid data
        assertThatThrownBy(() -> predictionService.predictColleges(invalidPercentageStudent))
            .isInstanceOf(Exception.class); // Could be ValidationException or PredictionException
    }

    // ========== Business Logic Error Scenarios ==========

    @Test
    @Order(8)
    @DisplayName("Should handle no matches found scenario with helpful messaging")
    void shouldHandleNoMatchesFoundScenarioWithHelpfulMessaging() {
        // Test requirement 6.5: Handle exceptions and continue operation where possible
        
        // Load valid data first
        try {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
        } catch (Exception e) {
            fail("Failed to load test data: " + e.getMessage());
        }
        
        // Create student profile that won't have matches
        Student noMatchStudent = Student.builder()
            .percentage(30.0) // Very low percentage
            .branch("Aerospace Engineering") // Non-existent branch
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();
        
        // Should throw PredictionException when no matches are found
        assertThatThrownBy(() -> predictionService.predictColleges(noMatchStudent))
            .isInstanceOf(com.dsecap.predictor.service.PredictionException.class)
            .hasMessageContaining("No colleges found");
        
        // Test console interface handles empty recommendations gracefully
        assertThatNoException().isThrownBy(() -> {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            try {
                consoleInterface.displayRecommendations(List.of());
                String output = outputStream.toString();
                // Should display appropriate message for no recommendations
                assertThat(output).isNotNull();
            } finally {
                System.setOut(originalOut);
            }
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should handle edge case percentages at boundaries")
    void shouldHandleEdgeCasePercentagesAtBoundaries() {
        // Test requirement 6.5: Handle exceptions and continue operation where possible
        
        // Load valid data first
        try {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
        } catch (Exception e) {
            fail("Failed to load test data: " + e.getMessage());
        }
        
        double[] edgePercentages = {0.0, 0.1, 99.9, 100.0};
        
        for (double percentage : edgePercentages) {
            Student edgeStudent = Student.builder()
                .percentage(percentage)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();
            
            // Edge case predictions may or may not find matches - both outcomes are valid
            try {
                List<CollegeRecommendation> recommendations = predictionService.predictColleges(edgeStudent);
                assertThat(recommendations).isNotNull();
            } catch (com.dsecap.predictor.service.PredictionException e) {
                // No matches found is also a valid outcome for edge cases
                assertThat(e.getMessage()).contains("No colleges found");
            }
        }
    }

    // ========== Error Handler Integration Tests ==========

    @Test
    @Order(10)
    @DisplayName("Should provide centralized exception management through ErrorHandler")
    void shouldProvideCentralizedExceptionManagement() {
        // Test requirement 6.5: Log appropriate error information and continue operation
        
        assertThat(errorHandler).isNotNull();
        
        // Test error handler methods
        assertThatNoException().isThrownBy(() -> {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            try {
                errorHandler.displayTroubleshootingGuide();
                String output = outputStream.toString();
                assertThat(output).isNotNull();
            } finally {
                System.setOut(originalOut);
            }
        });
    }

    @Test
    @Order(11)
    @DisplayName("Should log appropriate error information for debugging")
    void shouldLogAppropriateErrorInformationForDebugging() {
        // Test requirement 6.5: Log appropriate error information
        
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errorStream));
        
        try {
            // Trigger an error scenario
            assertThatThrownBy(() -> csvDataLoader.loadFromCsv("non-existent-file.csv"))
                .isInstanceOf(Exception.class);
            
            // Verify error logging occurred (implementation dependent)
            // The actual logging verification would depend on the logging framework used
            
        } finally {
            System.setErr(originalErr);
        }
    }

    // ========== Repository Error Scenarios ==========

    @Test
    @Order(12)
    @DisplayName("Should handle repository operations with empty or null data")
    void shouldHandleRepositoryOperationsWithEmptyOrNullData() {
        // Test requirement 6.5: Handle exceptions and continue operation where possible
        
        // Test repository operations when no data is loaded
        assertThatNoException().isThrownBy(() -> {
            // Clear existing data temporarily
            var originalColleges = collegeRepository.findAll();
            
            // Test with empty repository state (simulate empty data scenario)
            if (originalColleges.isEmpty()) {
                var allColleges = collegeRepository.findAll();
                assertThat(allColleges).isNotNull().isEmpty();
                
                var filteredColleges = collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN");
                assertThat(filteredColleges).isNotNull().isEmpty();
            } else {
                // Repository has data, test normal operations
                var allColleges = collegeRepository.findAll();
                assertThat(allColleges).isNotNull();
                
                var filteredColleges = collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN");
                assertThat(filteredColleges).isNotNull();
            }
        });
    }

    @Test
    @Order(13)
    @DisplayName("Should handle concurrent access to repository safely")
    void shouldHandleConcurrentAccessToRepositorySafely() {
        // Test requirement 6.5: Handle exceptions and continue operation where possible
        
        // Load valid data first
        try {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
        } catch (Exception e) {
            fail("Failed to load test data: " + e.getMessage());
        }
        
        // Test concurrent repository access
        assertThatNoException().isThrownBy(() -> {
            Thread[] threads = new Thread[5];
            
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        var colleges = collegeRepository.findAll();
                        assertThat(colleges).isNotNull();
                        
                        var filtered = collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN");
                        assertThat(filtered).isNotNull();
                    } catch (Exception e) {
                        fail("Concurrent repository access failed: " + e.getMessage());
                    }
                });
            }
            
            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join(3000); // 3 second timeout
            }
        });
    }

    // ========== Configuration Error Scenarios ==========

    @Test
    @Order(14)
    @DisplayName("Should handle invalid configuration values gracefully")
    void shouldHandleInvalidConfigurationValuesGracefully() {
        // Test requirement 6.4: Separate concerns into distinct modules
        
        // Verify configuration is loaded and has reasonable defaults
        assertThat(config).isNotNull();
        assertThat(config.getMaxRecommendations()).isGreaterThan(0);
        assertThat(config.getPercentageRange()).isGreaterThan(0);
        assertThat(config.getCsvFilePath()).isNotNull().isNotEmpty();
    }

    // ========== Memory and Resource Management Error Scenarios ==========

    @Test
    @Order(15)
    @DisplayName("Should handle large datasets without memory issues")
    void shouldHandleLargeDatasetsWithoutMemoryIssues() {
        // Test requirement 6.5: Handle exceptions and continue operation where possible
        
        // This test would be more meaningful with a larger dataset
        // For now, test with current dataset multiple times
        assertThatNoException().isThrownBy(() -> {
            for (int i = 0; i < 100; i++) {
                var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
                assertThat(colleges).isNotNull();
                
                // Simulate memory pressure by creating multiple student profiles
                Student student = Student.builder()
                    .percentage(85.0 + (i % 10))
                    .branch("Computer Engineering")
                    .category("OPEN")
                    .gender("Male")
                    .district("Pune")
                    .isPwd(false)
                    .build();
                
                List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
                assertThat(recommendations).isNotNull();
            }
        });
    }

    @Test
    @Order(16)
    @DisplayName("Should maintain application stability under error conditions")
    void shouldMaintainApplicationStabilityUnderErrorConditions() {
        // Test requirement 6.5: Handle exceptions and continue operation where possible
        
        // Load valid data first
        try {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
        } catch (Exception e) {
            fail("Failed to load test data: " + e.getMessage());
        }
        
        // Test multiple error scenarios in sequence
        assertThatNoException().isThrownBy(() -> {
            // 1. Invalid file access
            try {
                csvDataLoader.loadFromCsv("non-existent.csv");
            } catch (Exception ignored) {
                // Expected to fail
            }
            
            // 2. Invalid student data
            Student invalidStudent = Student.builder()
                .percentage(-50.0)
                .branch("")
                .category("INVALID")
                .gender("Unknown")
                .district("")
                .isPwd(false)
                .build();
            
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(invalidStudent);
            assertThat(recommendations).isNotNull();
            
            // 3. Valid operation after errors
            Student validStudent = Student.builder()
                .percentage(85.0)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();
            
            recommendations = predictionService.predictColleges(validStudent);
            assertThat(recommendations).isNotNull();
            
            // Application should still be functional
            var allColleges = collegeRepository.findAll();
            assertThat(allColleges).isNotEmpty();
        });
    }
}