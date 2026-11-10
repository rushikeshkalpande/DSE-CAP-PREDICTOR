package com.dsecap.predictor.repository;

import com.dsecap.predictor.model.College;
import org.springframework.stereotype.Repository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository class for managing in-memory college data storage.
 * Provides methods for loading colleges and filtering by branch and category.
 */
@Repository
public class CollegeRepository {
    
    private final List<College> colleges;
    private final Validator validator;
    
    public CollegeRepository(Validator validator) {
        this.colleges = new ArrayList<>();
        this.validator = validator;
    }
    
    /**
     * Loads colleges into the repository with validation.
     * 
     * @param collegeList List of colleges to load
     * @throws IllegalArgumentException if any college data is invalid
     */
    public void loadColleges(List<College> collegeList) {
        if (collegeList == null) {
            throw new IllegalArgumentException("College list cannot be null");
        }
        
        // Validate each college before loading
        for (College college : collegeList) {
            validateCollege(college);
        }
        
        // Clear existing data and load new data
        colleges.clear();
        colleges.addAll(collegeList);
    }
    
    /**
     * Returns all colleges in the repository.
     * 
     * @return List of all colleges
     */
    public List<College> findAll() {
        return new ArrayList<>(colleges);
    }
    
    /**
     * Finds colleges by branch and category.
     * 
     * @param branch Engineering branch to filter by
     * @param category Student category to filter by
     * @return List of colleges matching the criteria
     */
    public List<College> findByBranchAndCategory(String branch, String category) {
        if (branch == null || category == null) {
            throw new IllegalArgumentException("Branch and category cannot be null");
        }
        
        return colleges.stream()
                .filter(college -> branch.equalsIgnoreCase(college.getBranch()))
                .filter(college -> category.equalsIgnoreCase(college.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * Finds colleges by branch only.
     * 
     * @param branch Engineering branch to filter by
     * @return List of colleges matching the branch
     */
    public List<College> findByBranch(String branch) {
        if (branch == null) {
            throw new IllegalArgumentException("Branch cannot be null");
        }
        
        return colleges.stream()
                .filter(college -> branch.equalsIgnoreCase(college.getBranch()))
                .collect(Collectors.toList());
    }
    
    /**
     * Finds colleges by category only.
     * 
     * @param category Student category to filter by
     * @return List of colleges matching the category
     */
    public List<College> findByCategory(String category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        
        return colleges.stream()
                .filter(college -> category.equalsIgnoreCase(college.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * Returns the total number of colleges in the repository.
     * 
     * @return Number of colleges
     */
    public int getCollegeCount() {
        return colleges.size();
    }
    
    /**
     * Clears all colleges from the repository.
     */
    public void clear() {
        colleges.clear();
    }
    
    /**
     * Validates college data using Bean Validation annotations.
     * 
     * @param college College to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCollege(College college) {
        if (college == null) {
            throw new IllegalArgumentException("College cannot be null");
        }
        
        Set<ConstraintViolation<College>> violations = validator.validate(college);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("College validation failed: " + errorMessage);
        }
    }
}