package com.dsecap.predictor.model;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Represents a diploma engineering student seeking DSE admission.
 * Contains all necessary information for college prediction.
 */
@Data
@Builder
public class Student {
    
    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.0", message = "Percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Percentage must not exceed 100")
    private Double percentage;
    
    @NotBlank(message = "Engineering branch is required")
    private String branch;
    
    @NotBlank(message = "Category is required")
    @Pattern(regexp = "OPEN|OBC|SC|ST|NT|EWS|SEBC", 
             message = "Category must be one of: OPEN, OBC, SC, ST, NT, EWS, SEBC")
    private String category;
    
    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "Male|Female|Other", 
             message = "Gender must be one of: Male, Female, Other")
    private String gender;
    
    @NotBlank(message = "District is required")
    private String district;
    
    @NotNull(message = "PWD status is required")
    private Boolean isPwd;
}