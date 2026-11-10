package com.dsecap.predictor.integration;

import com.dsecap.predictor.config.PredictorConfig;
import com.dsecap.predictor.model.CollegeRecommendation;
import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.repository.CollegeRepository;
import com.dsecap.predictor.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Simple integration tests for DSE College Predictor application.
 * Tests the complete workflow from data loading to recommendation generation.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "dse.predictor.csv-file-path=src/test/resources/test-data/integration-test-data.csv",
    "dse.predictor.max-recommendations=10",
    "dse.predictor.percentage-range=10.0"
})
class SimpleIntegrationTest {

    @Autowired
    private CsvDataLoader csvDataLoader;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ErrorHandler errorHandler;

    @Autowired
    private PredictorConfig config;

    @BeforeEach
    void setUp() {
        // Load test data before each test
        try {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
        } catch (Exception e) {
            fail("Failed to load test data: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should load Spring Boot context successfully")
    void contextLoads() {
        // Test passes if context loads without exceptions
        assertThat(csvDataLoader).isNotNull();
        assertThat(collegeRepository).isNotNull();
        assertThat(predictionService).isNotNull();
        assertThat(validationService).isNotNull();
        assertThat(errorHandler).isNotNull();
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Should load CSV data successfully")
    void shouldLoadCsvDataSuccessfully() {
        // Test CSV data loading
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            assertThat(colleges).isNotEmpty();
            
            // Verify data structure
            colleges.forEach(college -> {
                assertThat(college.getName()).isNotNull().isNotEmpty();
                assertThat(college.getBranch()).isNotNull().isNotEmpty();
                assertThat(college.getCategory()).isNotNull().isNotEmpty();
                assertThat(college.getCutoffPercent()).isGreaterThan(0);
            });
        });
    }

    @Test
    @DisplayName("Should generate predictions for valid student profile")
    void shouldGeneratePredictionsForValidStudent() {
        // Given: A valid student profile
        Student student = Student.builder()
            .percentage(85.0)
            .branch("Computer Engineering")
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();

        // When & Then: Should generate predictions without exceptions
        assertThatNoException().isThrownBy(() -> {
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
            
            // Verify recommendations
            assertThat(recommendations).isNotNull();
            if (!recommendations.isEmpty()) {
                assertThat(recommendations).hasSizeLessThanOrEqualTo(config.getMaxRecommendations());
                
                // Verify recommendation structure
                recommendations.forEach(rec -> {
                    assertThat(rec.getCollege()).isNotNull();
                    assertThat(rec.getConfidence()).isNotNull();
                    assertThat(rec.getMatchScore()).isGreaterThanOrEqualTo(0.0);
                    assertThat(rec.getCollege().getBranch()).isEqualTo("Computer Engineering");
                    assertThat(rec.getCollege().getCategory()).isEqualTo("OPEN");
                });
            }
        });
    }

    @Test
    @DisplayName("Should handle different student categories")
    void shouldHandleDifferentCategories() {
        String[] categories = {"OPEN", "OBC", "SC", "ST"};

        for (String category : categories) {
            Student student = Student.builder()
                .percentage(80.0)
                .branch("Computer Engineering")
                .category(category)
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();

            // Handle both successful predictions and no matches scenarios
            try {
                List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
                
                assertThat(recommendations).isNotNull();
                
                // If recommendations exist, verify they match the category
                if (!recommendations.isEmpty()) {
                    recommendations.forEach(rec -> 
                        assertThat(rec.getCollege().getCategory()).isEqualTo(category)
                    );
                }
            } catch (com.dsecap.predictor.service.PredictionException e) {
                // No matches found is acceptable for some combinations
                assertThat(e.getMessage()).contains("No colleges found");
            }
        }
    }

    @Test
    @DisplayName("Should validate input correctly")
    void shouldValidateInputCorrectly() {
        // Test validation service
        assertThat(validationService.isValidPercentage(85.0)).isTrue();
        assertThat(validationService.isValidPercentage(-10.0)).isFalse();
        assertThat(validationService.isValidPercentage(110.0)).isFalse();
        
        assertThat(validationService.isValidCategory("OPEN")).isTrue();
        assertThat(validationService.isValidCategory("INVALID")).isFalse();
        
        assertThat(validationService.isValidBranch("Computer Engineering")).isTrue();
        assertThat(validationService.isValidBranch("")).isFalse();
    }

    @Test
    @DisplayName("Should handle error scenarios gracefully")
    void shouldHandleErrorScenariosGracefully() {
        // Test error handler is available
        assertThat(errorHandler).isNotNull();
        
        // Test error handler methods are accessible
        assertThatNoException().isThrownBy(() -> {
            errorHandler.displayTroubleshootingGuide();
        });
    }

    @Test
    @DisplayName("Should complete operations within reasonable time")
    void shouldCompleteOperationsWithinReasonableTime() {
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
        
        // Should complete within 5 seconds
        assertThat(endTime - startTime).isLessThan(5000);
    }

    @Test
    @DisplayName("Should handle repository operations correctly")
    void shouldHandleRepositoryOperations() {
        // Test repository integration
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
            
            // Test repository queries
            var allColleges = collegeRepository.findAll();
            assertThat(allColleges).isNotEmpty();
            assertThat(allColleges.size()).isEqualTo(colleges.size());
        });
    }

    @Test
    @DisplayName("Should handle missing data scenarios")
    void shouldHandleMissingDataScenarios() {
        // Test with non-existent file path
        assertThatThrownBy(() -> csvDataLoader.loadFromCsv("non-existent-file.csv"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle edge case student profiles")
    void shouldHandleEdgeCaseProfiles() {
        // Test with very low percentage
        Student lowPercentageStudent = Student.builder()
            .percentage(60.0)
            .branch("Civil Engineering")
            .category("ST")
            .gender("Female")
            .district("Aurangabad")
            .isPwd(false)
            .build();

        // Handle both successful predictions and no matches scenarios
        try {
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(lowPercentageStudent);
            assertThat(recommendations).isNotNull();
        } catch (com.dsecap.predictor.service.PredictionException e) {
            // No matches found is acceptable for low percentage students
            assertThat(e.getMessage()).contains("No colleges found");
        }
    }
}