package com.dsecap.predictor.model;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a college with historical cutoff data for DSE admission.
 * Contains all necessary information for matching and ranking.
 */
@Data
@Builder
public class College {
    
    @NotBlank(message = "College name is required")
    private String name;
    
    @NotBlank(message = "District is required")
    private String district;
    
    @NotBlank(message = "Choice code is required")
    private String choiceCode;
    
    @NotBlank(message = "Branch is required")
    private String branch;
    
    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be valid")
    private Integer year;
    
    @NotBlank(message = "Round is required")
    private String round;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotNull(message = "Cutoff percentage is required")
    @DecimalMin(value = "0.0", message = "Cutoff percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Cutoff percentage must not exceed 100")
    private Double cutoffPercent;
}