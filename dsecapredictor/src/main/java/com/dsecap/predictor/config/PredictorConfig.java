package com.dsecap.predictor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for the DSE College Predictor application.
 * Allows customization of application behavior through properties files.
 */
@Data
@Component
@ConfigurationProperties(prefix = "dse.predictor")
public class PredictorConfig {
    
    /**
     * Path to the CSV file containing historical cutoff data
     */
    @NotBlank(message = "CSV file path is required")
    private String csvFilePath = "data/cutoff-data.csv";
    
    /**
     * Maximum number of college recommendations to display
     */
    @NotNull(message = "Max recommendations is required")
    @Min(value = 1, message = "Max recommendations must be at least 1")
    private Integer maxRecommendations = 15;
    
    /**
     * Percentage range for filtering colleges (±percentage)
     */
    @NotNull(message = "Percentage range is required")
    @DecimalMin(value = "0.0", message = "Percentage range must be positive")
    private Double percentageRange = 10.0;
    
    /**
     * Whether to enable detailed logging during prediction
     */
    private Boolean enableDetailedLogging = false;
    
    /**
     * Minimum year for considering historical data
     */
    @NotNull(message = "Minimum year is required")
    @Min(value = 2000, message = "Minimum year must be valid")
    private Integer minYear = 2019;
}