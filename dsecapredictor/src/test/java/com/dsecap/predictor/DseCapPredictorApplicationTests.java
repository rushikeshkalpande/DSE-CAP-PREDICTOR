package com.dsecap.predictor;

import com.dsecap.predictor.config.PredictorConfig;
import com.dsecap.predictor.config.TestConfig;
import com.dsecap.predictor.repository.CollegeRepository;
import com.dsecap.predictor.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic Spring Boot application tests to verify context loading and dependency injection.
 */
@SpringBootTest(classes = {DseCapPredictorApplication.class, TestConfig.class})
@ActiveProfiles("test")
class DseCapPredictorApplicationTests {

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

    @Test
    @DisplayName("Should load Spring Boot application context successfully")
    void contextLoads() {
        // Test passes if context loads without exceptions
        assertThat(application).isNotNull();
    }

    @Test
    @DisplayName("Should inject all required Spring beans")
    void shouldInjectAllRequiredBeans() {
        // Verify all required beans are properly injected
        assertThat(application).isNotNull();
        assertThat(csvDataLoader).isNotNull();
        assertThat(collegeRepository).isNotNull();
        assertThat(predictionService).isNotNull();
        assertThat(validationService).isNotNull();
        assertThat(consoleInterface).isNotNull();
        assertThat(errorHandler).isNotNull();
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Should load configuration properties correctly")
    void shouldLoadConfigurationProperties() {
        // Verify configuration is loaded and accessible
        assertThat(config).isNotNull();
        assertThat(config.getMaxRecommendations()).isGreaterThan(0);
        assertThat(config.getPercentageRange()).isGreaterThan(0);
        assertThat(config.getCsvFilePath()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should have proper component annotations and Spring configuration")
    void shouldHaveProperSpringConfiguration() {
        // Verify that all services are properly configured as Spring components
        assertThat(csvDataLoader.getClass().isAnnotationPresent(org.springframework.stereotype.Component.class) ||
                  csvDataLoader.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class)).isTrue();
        
        assertThat(collegeRepository.getClass().isAnnotationPresent(org.springframework.stereotype.Repository.class) ||
                  collegeRepository.getClass().isAnnotationPresent(org.springframework.stereotype.Component.class)).isTrue();
        
        assertThat(predictionService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class) ||
                  predictionService.getClass().isAnnotationPresent(org.springframework.stereotype.Component.class)).isTrue();
        
        assertThat(validationService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class) ||
                  validationService.getClass().isAnnotationPresent(org.springframework.stereotype.Component.class)).isTrue();
    }
}
