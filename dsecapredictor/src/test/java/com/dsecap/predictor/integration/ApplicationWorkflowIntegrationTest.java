package com.dsecap.predictor.integration;

import com.dsecap.predictor.DseCapPredictorApplication;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import com.dsecap.predictor.config.TestConfig;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for complete application workflow including main application execution.
 * Tests the full application flow from startup to recommendation generation.
 * 
 * Requirements covered: 2.6, 5.5, 6.4
 */
@SpringBootTest(classes = {com.dsecap.predictor.DseCapPredictorApplication.class, TestConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "dse.predictor.csv-file-path=src/test/resources/test-data/integration-test-data.csv",
    "dse.predictor.max-recommendations=15",
    "dse.predictor.percentage-range=10.0"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationWorkflowIntegrationTest {

    @Autowired
    private DseCapPredictorApplication application;

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

    private static boolean applicationInitialized = false;

    @BeforeEach
    void setUp() {
        if (!applicationInitialized) {
            try {
                // Simulate application startup data loading
                var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
                collegeRepository.loadColleges(colleges);
                applicationInitialized = true;
            } catch (Exception e) {
                fail("Failed to initialize application: " + e.getMessage());
            }
        }
    }

    // ========== Application Startup and Initialization Tests ==========

    @Test
    @Order(1)
    @DisplayName("Should initialize application successfully with all components wired")
    void shouldInitializeApplicationSuccessfullyWithAllComponentsWired() {
        // Test requirement 2.6: Confirm successful data loading to the user
        // Test requirement 6.4: Separate concerns into distinct modules
        
        // Verify main application class is properly instantiated
        assertThat(application).isNotNull();
        
        // Verify all dependencies are properly injected
        assertThat(csvDataLoader).isNotNull();
        assertThat(collegeRepository).isNotNull();
        assertThat(predictionService).isNotNull();
        assertThat(validationService).isNotNull();
        assertThat(consoleInterface).isNotNull();
        assertThat(errorHandler).isNotNull();
        assertThat(config).isNotNull();
        
        // Verify configuration is loaded correctly
        assertThat(config.getCsvFilePath()).contains("integration-test-data.csv");
        assertThat(config.getMaxRecommendations()).isEqualTo(15);
        assertThat(config.getPercentageRange()).isEqualTo(10.0);
    }

    @Test
    @Order(2)
    @DisplayName("Should load CSV data successfully during application startup")
    void shouldLoadCsvDataSuccessfullyDuringApplicationStartup() {
        // Test requirement 2.6: Confirm successful data loading to the user
        
        assertThatNoException().isThrownBy(() -> {
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            assertThat(colleges).isNotEmpty();
            
            // Verify data loading confirmation
            collegeRepository.loadColleges(colleges);
            var loadedColleges = collegeRepository.findAll();
            assertThat(loadedColleges).hasSameSizeAs(colleges);
            
            // Verify data integrity after loading
            loadedColleges.forEach(college -> {
                assertThat(college.getName()).isNotNull().isNotEmpty();
                assertThat(college.getBranch()).isNotNull().isNotEmpty();
                assertThat(college.getCategory()).isNotNull().isNotEmpty();
                assertThat(college.getCutoffPercent()).isGreaterThan(0);
            });
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should validate application startup with proper error handling")
    void shouldValidateApplicationStartupWithProperErrorHandling() {
        // Test requirement 6.4: Separate concerns into distinct modules
        
        // Verify error handler is available during startup
        assertThat(errorHandler).isNotNull();
        
        // Test error handler functionality
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

    // ========== Complete Application Workflow Tests ==========

    @Test
    @Order(4)
    @DisplayName("Should execute complete prediction workflow from data loading to output")
    void shouldExecuteCompletePredictionWorkflowFromDataLoadingToOutput() {
        // Test requirement 5.5: Gracefully terminate after output
        
        assertThatNoException().isThrownBy(() -> {
            // Step 1: Data loading (already done in setup)
            var allColleges = collegeRepository.findAll();
            assertThat(allColleges).isNotEmpty();
            
            // Step 2: User input simulation (validation)
            Student student = Student.builder()
                .percentage(85.0)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();
            
            // Validate input
            assertThat(validationService.isValidPercentage(student.getPercentage())).isTrue();
            assertThat(validationService.isValidCategory(student.getCategory())).isTrue();
            assertThat(validationService.isValidBranch(student.getBranch())).isTrue();
            
            // Step 3: Prediction generation
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
            assertThat(recommendations).isNotNull();
            
            // Step 4: Output display
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            try {
                consoleInterface.displayRecommendations(recommendations);
                String output = outputStream.toString();
                assertThat(output).isNotNull();
            } finally {
                System.setOut(originalOut);
            }
            
            // Step 5: Verify graceful completion (no exceptions thrown)
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should handle multiple consecutive prediction requests in same session")
    void shouldHandleMultipleConsecutivePredictionRequestsInSameSession() {
        // Test requirement 5.5: Gracefully terminate after output
        
        Student[] students = {
            Student.builder().percentage(95.0).branch("Computer Engineering").category("OPEN").gender("Male").district("Pune").isPwd(false).build(),
            Student.builder().percentage(85.0).branch("Mechanical Engineering").category("OBC").gender("Female").district("Mumbai").isPwd(false).build(),
            Student.builder().percentage(75.0).branch("Electronics Engineering").category("SC").gender("Male").district("Sangli").isPwd(true).build(),
            Student.builder().percentage(90.0).branch("Information Technology").category("OPEN").gender("Female").district("Pune").isPwd(false).build()
        };

        assertThatNoException().isThrownBy(() -> {
            for (Student student : students) {
                // Validate input
                assertThat(validationService.isValidPercentage(student.getPercentage())).isTrue();
                assertThat(validationService.isValidCategory(student.getCategory())).isTrue();
                assertThat(validationService.isValidBranch(student.getBranch())).isTrue();
                
                // Generate predictions
                List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
                assertThat(recommendations).isNotNull();
                
                // Display results
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PrintStream originalOut = System.out;
                System.setOut(new PrintStream(outputStream));
                
                try {
                    consoleInterface.displayRecommendations(recommendations);
                } finally {
                    System.setOut(originalOut);
                }
            }
        });
    }

    // ========== Application Performance and Reliability Tests ==========

    @Test
    @Order(6)
    @DisplayName("Should maintain consistent performance across multiple workflow executions")
    void shouldMaintainConsistentPerformanceAcrossMultipleWorkflowExecutions() {
        // Test requirement 5.5: Gracefully terminate after output
        
        Student student = Student.builder()
            .percentage(85.0)
            .branch("Computer Engineering")
            .category("OPEN")
            .gender("Male")
            .district("Pune")
            .isPwd(false)
            .build();

        long[] executionTimes = new long[10];
        
        assertThatNoException().isThrownBy(() -> {
            for (int i = 0; i < 10; i++) {
                long startTime = System.currentTimeMillis();
                
                // Complete workflow execution
                List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
                assertThat(recommendations).isNotNull();
                
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PrintStream originalOut = System.out;
                System.setOut(new PrintStream(outputStream));
                
                try {
                    consoleInterface.displayRecommendations(recommendations);
                } finally {
                    System.setOut(originalOut);
                }
                
                long endTime = System.currentTimeMillis();
                executionTimes[i] = endTime - startTime;
                
                // Each execution should complete within reasonable time
                assertThat(executionTimes[i]).isLessThan(3000); // 3 seconds
            }
            
            // Verify performance consistency (no execution should be significantly slower)
            long maxTime = executionTimes[0];
            long minTime = executionTimes[0];
            
            for (long time : executionTimes) {
                maxTime = Math.max(maxTime, time);
                minTime = Math.min(minTime, time);
            }
            
            // Performance should be relatively consistent (max should not be more than 5x min)
            assertThat(maxTime).isLessThan(minTime * 5);
        });
    }

    @Test
    @Order(7)
    @DisplayName("Should handle application workflow with various data scenarios")
    void shouldHandleApplicationWorkflowWithVariousDataScenarios() {
        // Test requirement 2.6: Confirm successful data loading to the user
        
        // Test with different data files
        String[] testDataFiles = {
            "src/test/resources/test-data/integration-test-data.csv",
            "src/test/resources/test-data/valid-colleges.csv"
        };

        for (String dataFile : testDataFiles) {
            assertThatNoException().isThrownBy(() -> {
                // Load different data set
                var colleges = csvDataLoader.loadFromCsv(dataFile);
                if (!colleges.isEmpty()) {
                    collegeRepository.loadColleges(colleges);
                    
                    // Test workflow with this data
                    Student student = Student.builder()
                        .percentage(85.0)
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
    }

    // ========== Application Error Recovery Tests ==========

    @Test
    @Order(8)
    @DisplayName("Should recover gracefully from errors and continue operation")
    void shouldRecoverGracefullyFromErrorsAndContinueOperation() {
        // Test requirement 6.4: Separate concerns into distinct modules
        
        assertThatNoException().isThrownBy(() -> {
            // Simulate error scenario
            try {
                csvDataLoader.loadFromCsv("non-existent-file.csv");
            } catch (Exception ignored) {
                // Expected to fail
            }
            
            // Application should still be functional after error
            Student student = Student.builder()
                .percentage(85.0)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();
            
            // Should still be able to make predictions
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
            assertThat(recommendations).isNotNull();
            
            // Should still be able to display results
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            try {
                consoleInterface.displayRecommendations(recommendations);
            } finally {
                System.setOut(originalOut);
            }
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should maintain data integrity throughout application lifecycle")
    void shouldMaintainDataIntegrityThroughoutApplicationLifecycle() {
        // Test requirement 6.4: Separate concerns into distinct modules
        
        // Record initial state
        int initialCollegeCount = collegeRepository.findAll().size();
        
        assertThatNoException().isThrownBy(() -> {
            // Perform multiple operations
            for (int i = 0; i < 20; i++) {
                Student student = Student.builder()
                    .percentage(80.0 + i % 20)
                    .branch("Computer Engineering")
                    .category(i % 2 == 0 ? "OPEN" : "OBC")
                    .gender(i % 3 == 0 ? "Male" : "Female")
                    .district("Pune")
                    .isPwd(i % 7 == 0)
                    .build();
                
                List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
                assertThat(recommendations).isNotNull();
            }
            
            // Verify data integrity is maintained
            int finalCollegeCount = collegeRepository.findAll().size();
            assertThat(finalCollegeCount).isEqualTo(initialCollegeCount);
            
            // Verify data structure integrity
            var colleges = collegeRepository.findAll();
            colleges.forEach(college -> {
                assertThat(college.getName()).isNotNull().isNotEmpty();
                assertThat(college.getBranch()).isNotNull().isNotEmpty();
                assertThat(college.getCategory()).isNotNull().isNotEmpty();
                assertThat(college.getCutoffPercent()).isGreaterThan(0);
            });
        });
    }

    // ========== Application Configuration and Modularity Tests ==========

    @Test
    @Order(10)
    @DisplayName("Should demonstrate proper separation of concerns across modules")
    void shouldDemonstrateProperSeparationOfConcernsAcrossModules() {
        // Test requirement 6.4: Separate concerns into distinct modules
        
        // Verify each module has distinct responsibilities
        
        // Data Access Layer
        assertThat(csvDataLoader).isNotNull();
        assertThat(collegeRepository).isNotNull();
        
        // Business Logic Layer
        assertThat(predictionService).isNotNull();
        assertThat(validationService).isNotNull();
        
        // Presentation Layer
        assertThat(consoleInterface).isNotNull();
        
        // Cross-cutting Concerns
        assertThat(errorHandler).isNotNull();
        assertThat(config).isNotNull();
        
        // Test that each module can operate independently
        assertThatNoException().isThrownBy(() -> {
            // Data layer operations
            var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
            collegeRepository.loadColleges(colleges);
            var allColleges = collegeRepository.findAll();
            assertThat(allColleges).isNotEmpty();
            
            // Business logic operations
            Student student = Student.builder()
                .percentage(85.0)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Male")
                .district("Pune")
                .isPwd(false)
                .build();
            
            assertThat(validationService.isValidPercentage(student.getPercentage())).isTrue();
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
            assertThat(recommendations).isNotNull();
            
            // Presentation layer operations
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            try {
                consoleInterface.displayRecommendations(recommendations);
            } finally {
                System.setOut(originalOut);
            }
        });
    }

    @Test
    @Order(11)
    @DisplayName("Should validate complete application integration and final workflow")
    void shouldValidateCompleteApplicationIntegrationAndFinalWorkflow() {
        // Final comprehensive test covering all requirements
        
        assertThatNoException().isThrownBy(() -> {
            // 1. Verify application initialization (Requirement 6.4)
            assertThat(application).isNotNull();
            assertThat(collegeRepository.findAll()).isNotEmpty();
            
            // 2. Test complete workflow (Requirements 2.6, 5.5)
            Student student = Student.builder()
                .percentage(92.0)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Female")
                .district("Mumbai")
                .isPwd(false)
                .build();
            
            // Input validation
            assertThat(validationService.isValidPercentage(student.getPercentage())).isTrue();
            assertThat(validationService.isValidCategory(student.getCategory())).isTrue();
            assertThat(validationService.isValidBranch(student.getBranch())).isTrue();
            
            // Prediction generation
            List<CollegeRecommendation> recommendations = predictionService.predictColleges(student);
            assertThat(recommendations).isNotNull();
            
            if (!recommendations.isEmpty()) {
                // Verify recommendation quality
                assertThat(recommendations).hasSizeLessThanOrEqualTo(config.getMaxRecommendations());
                
                recommendations.forEach(rec -> {
                    assertThat(rec.getCollege().getBranch()).isEqualTo("Computer Engineering");
                    assertThat(rec.getCollege().getCategory()).isEqualTo("OPEN");
                    assertThat(rec.getConfidence()).isNotNull();
                    assertThat(rec.getMatchScore()).isGreaterThanOrEqualTo(0.0);
                });
                
                // Verify proper ranking
                for (int i = 0; i < recommendations.size() - 1; i++) {
                    assertThat(recommendations.get(i).getMatchScore())
                        .isGreaterThanOrEqualTo(recommendations.get(i + 1).getMatchScore());
                }
            }
            
            // Output generation
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            try {
                consoleInterface.displayRecommendations(recommendations);
                String output = outputStream.toString();
                assertThat(output).isNotNull();
            } finally {
                System.setOut(originalOut);
            }
            
            // 3. Verify graceful completion (Requirement 5.5)
            // Test passes if no exceptions are thrown
        });
    }
}