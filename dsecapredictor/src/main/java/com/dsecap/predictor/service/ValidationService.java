package com.dsecap.predictor.service;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Service for validating student input data.
 * Provides comprehensive validation for all student fields with detailed error messages.
 */
@Service
public class ValidationService {
    
    private static final Set<String> VALID_CATEGORIES = new HashSet<>(
        Arrays.asList("OPEN", "OBC", "SC", "ST", "NT", "EWS", "SEBC")
    );
    
    private static final Set<String> VALID_GENDERS = new HashSet<>(
        Arrays.asList("Male", "Female", "Other")
    );
    
    /**
     * Validates student percentage.
     * 
     * @param percentage the percentage to validate
     * @throws ValidationException if percentage is invalid
     */
    public void validatePercentage(Double percentage) throws ValidationException {
        if (percentage == null) {
            throw new ValidationException("Percentage is required and cannot be null");
        }
        
        if (percentage < 0.0) {
            throw new ValidationException("Percentage must be at least 0. You entered: " + percentage);
        }
        
        if (percentage > 100.0) {
            throw new ValidationException("Percentage must not exceed 100. You entered: " + percentage);
        }
    }
    
    /**
     * Validates student category.
     * 
     * @param category the category to validate
     * @throws ValidationException if category is invalid
     */
    public void validateCategory(String category) throws ValidationException {
        if (category == null || category.trim().isEmpty()) {
            throw new ValidationException("Category is required and cannot be empty");
        }
        
        String trimmedCategory = category.trim();
        if (!VALID_CATEGORIES.contains(trimmedCategory)) {
            throw new ValidationException(
                "Invalid category '" + trimmedCategory + "'. Valid categories are: " + 
                String.join(", ", VALID_CATEGORIES)
            );
        }
    }
    
    /**
     * Validates engineering branch.
     * 
     * @param branch the branch to validate
     * @throws ValidationException if branch is invalid
     */
    public void validateBranch(String branch) throws ValidationException {
        if (branch == null || branch.trim().isEmpty()) {
            throw new ValidationException("Engineering branch is required and cannot be empty");
        }
        
        String trimmedBranch = branch.trim();
        if (trimmedBranch.length() < 2) {
            throw new ValidationException("Engineering branch must be at least 2 characters long");
        }
        
        if (trimmedBranch.length() > 50) {
            throw new ValidationException("Engineering branch must not exceed 50 characters");
        }
        
        // Check for valid characters (letters, spaces, hyphens, and common abbreviations)
        if (!trimmedBranch.matches("^[a-zA-Z\\s\\-&()]+$")) {
            throw new ValidationException(
                "Engineering branch contains invalid characters. Only letters, spaces, hyphens, and parentheses are allowed"
            );
        }
    }
    
    /**
     * Validates student gender.
     * 
     * @param gender the gender to validate
     * @throws ValidationException if gender is invalid
     */
    public void validateGender(String gender) throws ValidationException {
        if (gender == null || gender.trim().isEmpty()) {
            throw new ValidationException("Gender is required and cannot be empty");
        }
        
        String trimmedGender = gender.trim();
        if (!VALID_GENDERS.contains(trimmedGender)) {
            throw new ValidationException(
                "Invalid gender '" + trimmedGender + "'. Valid options are: " + 
                String.join(", ", VALID_GENDERS)
            );
        }
    }
    
    /**
     * Validates student district.
     * 
     * @param district the district to validate
     * @throws ValidationException if district is invalid
     */
    public void validateDistrict(String district) throws ValidationException {
        if (district == null || district.trim().isEmpty()) {
            throw new ValidationException("District is required and cannot be empty");
        }
        
        String trimmedDistrict = district.trim();
        if (trimmedDistrict.length() < 2) {
            throw new ValidationException("District name must be at least 2 characters long");
        }
        
        if (trimmedDistrict.length() > 30) {
            throw new ValidationException("District name must not exceed 30 characters");
        }
        
        // Check for valid characters (letters, spaces, and hyphens)
        if (!trimmedDistrict.matches("^[a-zA-Z\\s\\-]+$")) {
            throw new ValidationException(
                "District name contains invalid characters. Only letters, spaces, and hyphens are allowed"
            );
        }
    }
    
    /**
     * Validates PWD status.
     * 
     * @param isPwd the PWD status to validate
     * @throws ValidationException if PWD status is invalid
     */
    public void validatePwdStatus(Boolean isPwd) throws ValidationException {
        if (isPwd == null) {
            throw new ValidationException("PWD status is required and cannot be null");
        }
    }
    
    /**
     * Validates all student fields at once.
     * 
     * @param percentage student percentage
     * @param branch engineering branch
     * @param category student category
     * @param gender student gender
     * @param district student district
     * @param isPwd PWD status
     * @throws ValidationException if any field is invalid
     */
    public void validateAllFields(Double percentage, String branch, String category, 
                                String gender, String district, Boolean isPwd) throws ValidationException {
        validatePercentage(percentage);
        validateBranch(branch);
        validateCategory(category);
        validateGender(gender);
        validateDistrict(district);
        validatePwdStatus(isPwd);
    }
    
    /**
     * Checks if a percentage value is valid without throwing an exception.
     * 
     * @param percentage the percentage to check
     * @return true if valid, false otherwise
     */
    public boolean isValidPercentage(Double percentage) {
        try {
            validatePercentage(percentage);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
    
    /**
     * Checks if a category is valid without throwing an exception.
     * 
     * @param category the category to check
     * @return true if valid, false otherwise
     */
    public boolean isValidCategory(String category) {
        try {
            validateCategory(category);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
    
    /**
     * Checks if a branch is valid without throwing an exception.
     * 
     * @param branch the branch to check
     * @return true if valid, false otherwise
     */
    public boolean isValidBranch(String branch) {
        try {
            validateBranch(branch);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
    
    /**
     * Gets all valid categories.
     * 
     * @return set of valid categories
     */
    public Set<String> getValidCategories() {
        return new HashSet<>(VALID_CATEGORIES);
    }
    
    /**
     * Gets all valid genders.
     * 
     * @return set of valid genders
     */
    public Set<String> getValidGenders() {
        return new HashSet<>(VALID_GENDERS);
    }
}