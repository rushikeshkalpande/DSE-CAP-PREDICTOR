package com.dsecap.predictor.model;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a college recommendation with confidence level and match score.
 * Used to rank and present colleges to students.
 */
@Data
@Builder
public class CollegeRecommendation {
    
    @NotNull(message = "College is required")
    @Valid
    private College college;
    
    @NotNull(message = "Confidence level is required")
    private ConfidenceLevel confidence;
    
    @NotNull(message = "Match score is required")
    @DecimalMin(value = "0.0", message = "Match score must be at least 0")
    @DecimalMax(value = "100.0", message = "Match score must not exceed 100")
    private Double matchScore;
    
    /**
     * Confidence levels for college recommendations based on student's chances.
     */
    public enum ConfidenceLevel {
        /**
         * Strong match - student percentage is equal to or above cutoff
         */
        STRONG,
        
        /**
         * Safe match - student percentage is slightly below cutoff but within range
         */
        SAFE,
        
        /**
         * Stretch match - student percentage is below cutoff, requires improvement
         */
        STRETCH
    }
}