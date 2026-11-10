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

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Final comprehensive integration tests for DSE College Predictor application.
 * Tests complete prediction workflow, Spring Boot context, and error scenarios.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "dse.predictor.csv-file-path=src/test/resources/test-data/integration-test-data.csv",
    "dse.predictor.max-recommendations=15",
    "dse.predictor.percentage-range=10.0"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinalIntegrationTest {

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

    private static boolean dataLoaded = false;

    @BeforeEach
    void setUp() {
        if (!dataLoaded) {
            try {
                var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
                collegeRepository.loadColleges(colleges);
                dataLoaded = true;
            } catch (Exception e) {
                fail("Failed to load test data: " + e.getMessage());
            }
        }
    }

    // ========== Spring Boot Context and Dependency Injection Tests ==========

    @Test
    @Order(1)
    @DisplayName("Should load Spring Boot context successfully with all dependencies")
    void shouldLoadSpringBootContextSuccessfully() {
        // Verify all required beans are properly injected
        assertThat(csvDataLoader).isNotNull();
        assertThat(collegeRepository).isNotNull();
        assertThat(predictionService).isNotNull();
        assertThat(validationService).isNotNull();
        assertThat(consoleInterface).isNotNull();
        assertThat(errorHandler).isNotNull();
        assertThat(config).isNotNull();
        
        // Verify configuration properties are loaded correctly
        assertThat(config.getMaxRecommendations()).isEqualTo(15);
        assertThat(config.getPercentageRange()).isEqualTo(10.0);
        assertThat(config.getCsvFilePath()).contains("integration-test-data.csv");
    }

    // ========== End-to-End Prediction Workflow Tests ==========

    @Test
    @Order(2)
    @DisplayName("Should complete end-to-end prediction workflow for high-performing OPEN category student")
    void shouldCompleteEndToEndWorkflowForHighPerformingOpenStudent() {
        // Given: High-performing OPEN category student
        Student student = Student.builder()
            .percentage(95.0)
            .branch("Computer Engineering")
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();

        // When: Complete prediction workflow
        assertThatNoException().isThrownBy(() -> {
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);

            // Then: Should get quality recommendations
            assertThat(recommendations).isNotNull();
            if (!recommendations.isEmpty()) {
                assertThat(recommendations).hasSizeLessThanOrEqualTo(config.getMaxRecommendations());
                
                // Verify all recommendations match student criteria
                recommendations.forEach(rec -> {
                    assertThat(rec.getCollege().getBranch()).isEqualTo("Computer Engineering");
                    assertThat(rec.getCollege().getCategory()).isEqualTo("OPEN");
                    assertThat(rec.getConfidence()).isNotNull();
                    assertThat(rec.getMatchScore()).isGreaterThanOrEqualTo(0.0);
                });
            }
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should handle various student profiles with different categories")
    void shouldHandleVariousStudentProfiles() {
        String[] categories = {"OPEN", "OBC", "SC", "ST"};
        String[] branches = {"Computer Engineering", "Mechanical Engineering", "Electronics Engineering"};

        for (String category : categories) {
            for (String branch : branches) {
                Student student = Student.builder()
                    .percentage(80.0)
                    .branch(branch)
                    .category(category)
                    .gender("Male")
                    .district("Pune")
                    .isPwd(false)
                    .build();

                // Handle both successful predictions and no matches scenarios
                try {
                    List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
                    assertThat(recommendations).isNotNull();
                    
                    // If recommendations exist, verify they match the criteria
                    if (!recommendations.isEmpty()) {
                        recommendations.forEach(rec -> {
                            assertThat(rec.getCollege().getBranch()).isEqualTo(branch);
                            assertThat(rec.getCollege().getCategory()).isEqualTo(category);
                        });
                    }
                } catch (com.dsecap.predictor.service.PredictionException e) {
                    // No matches found is acceptable for some combinations
                    assertThat(e.getMessage()).contains("No colleges found");
                }
            }
        }
    }

    // ========== Error Scenario Tests ==========

    @Test
    @Order(4)
    @DisplayName("Should handle missing CSV file gracefully")
    void shouldHandleMissingCsvFileGracefully() {
        // Test with non-existent file path
        assertThatThrownBy(() -> csvDataLoader.loadFromCsv("non-existent-file.csv"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @Order(5)
    @DisplayName("Should handle invalid student data gracefully")
    void shouldHandleInvalidStudentDataGracefully() {
        // Test validation service with invalid data
        assertThat(validationService.isValidPercentage(-10.0)).isFalse();
        assertThat(validationService.isValidPercentage(110.0)).isFalse();
        assertThat(validationService.isValidCategory("INVALID_CATEGORY")).isFalse();
        assertThat(validationService.isValidBranch("")).isFalse();
        assertThat(validationService.isValidBranch(null)).isFalse();
    }

    @Test
    @Order(6)
    @DisplayName("Should handle no matches found scenario gracefully")
    void shouldHandleNoMatchesFoundScenario() {
        // Create a student profile that likely won't have matches
        Student student = Student.builder()
            .percentage(45.0) // Very low percentage
            .branch("Aerospace Engineering") // Non-existent branch in test data
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();

        // Should throw PredictionException when no matches are found
        assertThatThrownBy(() -> predictionService.predictColleges(student))
            .isInstanceOf(com.dsecap.predictor.service.PredictionException.class)
            .hasMessageContaining("No colleges found");
    }  
  // ========== Performance and Data Integrity Tests ==========

    @Test
    @Order(7)
    @DisplayName("Should complete predictions within reasonable time limits")
    void shouldCompletePredictionsWithinReasonableTime() {
        Student student = Student.builder()
            .percentage(85.0)
            .branch("Computer Engineering")
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();

        long startTime = System.currentTimeMillis();
        
        assertThatNoException().isThrownBy(() -> {
            predictionService.predictColleges(student);
        });
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Should complete within 5 seconds for integration test
        assertThat(executionTime).isLessThan(5000);
        
        System.out.println("Prediction completed in " + executionTime + "ms");
    }

    @Test
    @Order(8)
    @DisplayName("Should maintain data integrity throughout prediction workflow")
    void shouldMaintainDataIntegrityThroughoutWorkflow() {
        int initialCount = collegeRepository.findAll().size();
        
        // Perform multiple predictions
        for (int i = 0; i < 5; i++) {
            Student student = Student.builder()
                .percentage(80.0 + i)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();
            
            // Handle both successful predictions and no matches scenarios
            try {
                predictionService.predictColleges(student);
            } catch (com.dsecap.predictor.service.PredictionException e) {
                // No matches found is acceptable for some combinations
                assertThat(e.getMessage()).contains("No colleges found");
            }
        }
        
        // Verify data integrity is maintained
        int finalCount = collegeRepository.findAll().size();
        assertThat(finalCount).isEqualTo(initialCount);
        
        // Verify data structure is intact
        var colleges = collegeRepository.findAll();
        colleges.forEach(college -> {
            assertThat(college.getName()).isNotNull().isNotEmpty();
            assertThat(college.getBranch()).isNotNull().isNotEmpty();
            assertThat(college.getCategory()).isNotNull().isNotEmpty();
            assertThat(college.getCutoffPercent()).isGreaterThan(0);
        });
    }

    // ========== Repository and Data Loading Integration Tests ==========

    @Test
    @Order(9)
    @DisplayName("Should integrate repository operations correctly with CSV data loading")
    void shouldIntegrateRepositoryOperationsCorrectly() {
        assertThatNoException().isThrownBy(() -> {
            // Test repository queries
            var allColleges = collegeRepository.findAll();
            assertThat(allColleges).isNotEmpty();
            
            // Test filtering operations
            var computerEngColleges = collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN");
            assertThat(computerEngColleges).isNotNull();
            
            // Verify filtered results
            computerEngColleges.forEach(college -> {
                assertThat(college.getBranch()).isEqualTo("Computer Engineering");
                assertThat(college.getCategory()).isEqualTo("OPEN");
            });
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should validate CSV data structure and content integrity")
    void shouldValidateCsvDataStructureAndContentIntegrity() {
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            
            assertThat(colleges).isNotEmpty();
            
            // Validate each college record
            colleges.forEach(college -> {
                // Required fields should not be null or empty
                assertThat(college.getName()).isNotNull().isNotEmpty();
                assertThat(college.getDistrict()).isNotNull().isNotEmpty();
                assertThat(college.getChoiceCode()).isNotNull().isNotEmpty();
                assertThat(college.getBranch()).isNotNull().isNotEmpty();
                assertThat(college.getCategory()).isNotNull().isNotEmpty();
                assertThat(college.getRound()).isNotNull().isNotEmpty();
                
                // Numeric fields should be valid
                assertThat(college.getYear()).isGreaterThan(2000).isLessThan(2030);
                assertThat(college.getCutoffPercent()).isGreaterThan(0).isLessThanOrEqualTo(100);
                
                // Category should be valid
                assertThat(college.getCategory()).isIn("OPEN", "OBC", "SC", "ST", "NT", "EWS", "SEBC");
            });
        });
    }

    // ========== Error Handler Integration Tests ==========

    @Test
    @Order(11)
    @DisplayName("Should integrate error handling throughout the application workflow")
    void shouldIntegrateErrorHandlingThroughoutWorkflow() {
        // Test error handler is properly integrated
        assertThat(errorHandler).isNotNull();
        
        // Test error handler methods are accessible and functional
        assertThatNoException().isThrownBy(() -> {
            errorHandler.displayTroubleshootingGuide();
        });
    }

    // ========== Comprehensive Workflow Validation Tests ==========

    @Test
    @Order(12)
    @DisplayName("Should maintain consistent results across multiple identical requests")
    void shouldMaintainConsistentResultsAcrossMultipleRequests() {
        Student student = Student.builder()
            .percentage(85.0)
            .branch("Computer Engineering")
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();

        // Execute same prediction multiple times
        assertThatNoException().isThrownBy(() -> {
            List<CollegeRecommendation> firstResult = predictionService.predictColleges(student);
            List<CollegeRecommendation> secondResult = predictionService.predictColleges(student);
            List<CollegeRecommendation> thirdResult = predictionService.predictColleges(student);

            // Results should be consistent
            assertThat(firstResult.size()).isEqualTo(secondResult.size());
            assertThat(secondResult.size()).isEqualTo(thirdResult.size());
            
            // If results exist, verify consistency of recommendations
            if (!firstResult.isEmpty()) {
                for (int i = 0; i < firstResult.size(); i++) {
                    assertThat(firstResult.get(i).getCollege().getName())
                        .isEqualTo(secondResult.get(i).getCollege().getName());
                    assertThat(firstResult.get(i).getConfidence())
                        .isEqualTo(secondResult.get(i).getConfidence());
                }
            }
        });
    }
}