package com.dsecap.predictor.service;

import com.dsecap.predictor.model.College;
import com.dsecap.predictor.model.CollegeRecommendation;
import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.repository.CollegeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for predicting suitable colleges for DSE admission.
 * Implements filtering, ranking, and confidence scoring logic with performance optimization.
 */
@Service
@Slf4j
public class PredictionService {
    
    private final CollegeRepository collegeRepository;
    private final PerformanceMonitor performanceMonitor;
    private final CacheService cacheService;
    private static final double PERCENTAGE_RANGE = 10.0;
    private static final int MAX_RECOMMENDATIONS = 15;
    
    public PredictionService(CollegeRepository collegeRepository, 
                           PerformanceMonitor performanceMonitor,
                           CacheService cacheService) {
        this.collegeRepository = collegeRepository;
        this.performanceMonitor = performanceMonitor;
        this.cacheService = cacheService;
    }
    
    /**
     * Predicts suitable colleges for a student based on their profile.
     * 
     * @param student Student profile with academic and personal details
     * @return List of ranked college recommendations
     * @throws PredictionException if no matches are found or prediction fails
     * @throws IllegalArgumentException if student is null
     */
    public List<CollegeRecommendation> predictColleges(Student student) throws PredictionException {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }
        
        // Start performance monitoring for the entire prediction process
        performanceMonitor.startOperation("COLLEGE_PREDICTION");
        
        try {
            // Step 1: Filter colleges by branch and category (with caching)
            List<College> filteredColleges = filterByBranchAndCategoryWithCache(student);
            
            if (filteredColleges.isEmpty()) {
                throw new PredictionException(
                    String.format("No colleges found for branch '%s' and category '%s'. " +
                                "Please verify the branch name or try a different category.",
                                student.getBranch(), student.getCategory()));
            }
            
            // Step 2: Apply ±10% rule for cutoff percentage (with caching)
            List<College> eligibleColleges = applyPercentageRuleWithCache(student.getPercentage(), 
                    student.getBranch(), student.getCategory(), filteredColleges);
            
            if (eligibleColleges.isEmpty()) {
                throw new PredictionException(
                    String.format("No colleges found within ±%.1f%% range of your percentage (%.2f%%). " +
                                "Found %d colleges for your branch/category but none within the percentage range.",
                                PERCENTAGE_RANGE, student.getPercentage(), filteredColleges.size()));
            }
            
            // Step 3: Rank and score colleges
            performanceMonitor.startOperation("COLLEGE_RANKING");
            List<CollegeRecommendation> recommendations = rankAndScore(student, eligibleColleges);
            performanceMonitor.endOperation("COLLEGE_RANKING");
            
            if (recommendations.isEmpty()) {
                throw new PredictionException("Failed to generate recommendations due to ranking issues.");
            }
            
            // Step 4: Limit to top recommendations
            List<CollegeRecommendation> finalRecommendations = recommendations.stream()
                    .limit(MAX_RECOMMENDATIONS)
                    .collect(Collectors.toList());
            
            // End performance monitoring
            long totalTime = performanceMonitor.endOperation("COLLEGE_PREDICTION");
            log.info("College prediction completed in {} ms for student with {}% in {} branch", 
                    totalTime, student.getPercentage(), student.getBranch());
            
            return finalRecommendations;
                    
        } catch (PredictionException e) {
            performanceMonitor.endOperation("COLLEGE_PREDICTION");
            throw e;
        } catch (Exception e) {
            performanceMonitor.endOperation("COLLEGE_PREDICTION");
            throw new PredictionException("Unexpected error during college prediction: " + e.getMessage(), e);
        }
    }
    
    /**
     * Filters colleges by matching branch and category with caching support.
     * 
     * @param student Student profile
     * @return List of colleges matching branch and category
     * @throws PredictionException if filtering fails
     */
    private List<College> filterByBranchAndCategoryWithCache(Student student) throws PredictionException {
        try {
            if (student.getBranch() == null || student.getBranch().trim().isEmpty()) {
                throw new PredictionException("Student branch cannot be null or empty");
            }
            
            if (student.getCategory() == null || student.getCategory().trim().isEmpty()) {
                throw new PredictionException("Student category cannot be null or empty");
            }
            
            String branch = student.getBranch().trim();
            String category = student.getCategory().trim();
            
            // Check cache first
            List<College> cachedResult = cacheService.getCachedCollegeFilter(branch, category);
            if (cachedResult != null) {
                return cachedResult;
            }
            
            // Cache miss - perform filtering and cache result
            performanceMonitor.startOperation("COLLEGE_FILTERING");
            List<College> filteredColleges = collegeRepository.findByBranchAndCategory(branch, category);
            performanceMonitor.endOperation("COLLEGE_FILTERING");
            
            // Cache the result for future use
            cacheService.cacheCollegeFilter(branch, category, filteredColleges);
            
            return filteredColleges;
        } catch (Exception e) {
            if (e instanceof PredictionException) {
                throw e;
            }
            throw new PredictionException("Error filtering colleges by branch and category: " + e.getMessage(), e);
        }
    }
    
    /**
     * Filters colleges by matching branch and category (legacy method for backward compatibility).
     * 
     * @param student Student profile
     * @return List of colleges matching branch and category
     * @throws PredictionException if filtering fails
     */
    private List<College> filterByBranchAndCategory(Student student) throws PredictionException {
        return filterByBranchAndCategoryWithCache(student);
    }
    
    /**
     * Applies ±10% rule to filter colleges within percentage range with caching support.
     * 
     * @param studentPercentage Student's academic percentage
     * @param branch Student's branch
     * @param category Student's category
     * @param colleges List of colleges to filter
     * @return List of colleges within the percentage range
     * @throws PredictionException if percentage filtering fails
     */
    private List<College> applyPercentageRuleWithCache(double studentPercentage, String branch, 
                                                      String category, List<College> colleges) throws PredictionException {
        try {
            if (studentPercentage < 0 || studentPercentage > 100) {
                throw new PredictionException("Invalid student percentage: " + studentPercentage + ". Must be between 0 and 100.");
            }
            
            if (colleges == null) {
                throw new PredictionException("College list cannot be null for percentage filtering");
            }
            
            // Check cache first
            List<College> cachedResult = cacheService.getCachedPercentageFilter(studentPercentage, branch, category);
            if (cachedResult != null) {
                return cachedResult;
            }
            
            // Cache miss - perform filtering
            performanceMonitor.startOperation("PERCENTAGE_FILTERING");
            
            double lowerBound = Math.max(0, studentPercentage - PERCENTAGE_RANGE);
            double upperBound = Math.min(100, studentPercentage + PERCENTAGE_RANGE);
            
            List<College> filteredColleges = colleges.stream()
                    .filter(college -> {
                        if (college == null || college.getCutoffPercent() == null) {
                            return false; // Skip invalid college records
                        }
                        double cutoff = college.getCutoffPercent();
                        return cutoff >= lowerBound && cutoff <= upperBound;
                    })
                    .collect(Collectors.toList());
            
            performanceMonitor.endOperation("PERCENTAGE_FILTERING");
            
            // Cache the result for future use
            cacheService.cachePercentageFilter(studentPercentage, branch, category, filteredColleges);
            
            return filteredColleges;
        } catch (Exception e) {
            if (e instanceof PredictionException) {
                throw e;
            }
            throw new PredictionException("Error applying percentage rule: " + e.getMessage(), e);
        }
    }
    
    /**
     * Applies ±10% rule to filter colleges within percentage range (legacy method for backward compatibility).
     * 
     * @param studentPercentage Student's academic percentage
     * @param colleges List of colleges to filter
     * @return List of colleges within the percentage range
     * @throws PredictionException if percentage filtering fails
     */
    private List<College> applyPercentageRule(double studentPercentage, List<College> colleges) throws PredictionException {
        // For legacy compatibility, we'll use a generic cache key
        return applyPercentageRuleWithCache(studentPercentage, "unknown", "unknown", colleges);
    }
    
    /**
     * Ranks colleges and assigns confidence levels and match scores.
     * 
     * @param student Student profile
     * @param colleges List of eligible colleges
     * @return List of ranked college recommendations
     * @throws PredictionException if ranking fails
     */
    private List<CollegeRecommendation> rankAndScore(Student student, List<College> colleges) throws PredictionException {
        try {
            if (student == null) {
                throw new PredictionException("Student cannot be null for ranking");
            }
            
            if (colleges == null || colleges.isEmpty()) {
                throw new PredictionException("No colleges provided for ranking");
            }
            
            List<CollegeRecommendation> recommendations = new ArrayList<>();
            
            for (College college : colleges) {
                if (college == null) {
                    continue; // Skip null colleges
                }
                
                try {
                    CollegeRecommendation recommendation = CollegeRecommendation.builder()
                            .college(college)
                            .confidence(calculateConfidenceLevel(student.getPercentage(), college.getCutoffPercent()))
                            .matchScore(calculateMatchScore(student.getPercentage(), college.getCutoffPercent()))
                            .build();
                    
                    recommendations.add(recommendation);
                } catch (Exception e) {
                    // Log and skip problematic college records
                    System.err.println("Warning: Skipping college due to ranking error: " + 
                                     (college.getName() != null ? college.getName() : "Unknown") + 
                                     " - " + e.getMessage());
                }
            }
            
            if (recommendations.isEmpty()) {
                throw new PredictionException("No valid recommendations could be generated from the provided colleges");
            }
            
            // Sort by confidence level priority and then by match score
            return recommendations.stream()
                    .sorted(getRecommendationComparator())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            if (e instanceof PredictionException) {
                throw e;
            }
            throw new PredictionException("Error during college ranking and scoring: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculates confidence level based on student percentage vs cutoff.
     * 
     * @param studentPercentage Student's academic percentage
     * @param cutoffPercent College's cutoff percentage
     * @return Confidence level for the match
     */
    private CollegeRecommendation.ConfidenceLevel calculateConfidenceLevel(
            double studentPercentage, double cutoffPercent) {
        
        if (studentPercentage >= cutoffPercent) {
            return CollegeRecommendation.ConfidenceLevel.STRONG;
        } else if (studentPercentage >= cutoffPercent - 5.0) {
            return CollegeRecommendation.ConfidenceLevel.SAFE;
        } else {
            return CollegeRecommendation.ConfidenceLevel.STRETCH;
        }
    }
    
    /**
     * Calculates match score based on percentage difference.
     * Higher score indicates better match.
     * 
     * @param studentPercentage Student's academic percentage
     * @param cutoffPercent College's cutoff percentage
     * @return Match score between 0 and 100
     */
    private double calculateMatchScore(double studentPercentage, double cutoffPercent) {
        double percentageDifference = Math.abs(studentPercentage - cutoffPercent);
        
        // Convert difference to score (lower difference = higher score)
        // Maximum difference in our range is 10%, so we normalize to 0-100 scale
        double normalizedDifference = Math.min(percentageDifference / PERCENTAGE_RANGE, 1.0);
        double baseScore = (1.0 - normalizedDifference) * 100.0;
        
        // Bonus for strong matches (student percentage >= cutoff)
        if (studentPercentage >= cutoffPercent) {
            baseScore = Math.min(baseScore + 10.0, 100.0);
        }
        
        return Math.round(baseScore * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    /**
     * Creates comparator for sorting recommendations.
     * Prioritizes: Strong > Safe > Stretch, then by match score descending.
     * 
     * @return Comparator for CollegeRecommendation
     */
    private Comparator<CollegeRecommendation> getRecommendationComparator() {
        return Comparator
                .comparing((CollegeRecommendation r) -> getConfidencePriority(r.getConfidence()))
                .thenComparing(CollegeRecommendation::getMatchScore, Comparator.reverseOrder());
    }
    
    /**
     * Gets priority value for confidence level (lower value = higher priority).
     * 
     * @param confidence Confidence level
     * @return Priority value
     */
    private int getConfidencePriority(CollegeRecommendation.ConfidenceLevel confidence) {
        switch (confidence) {
            case STRONG:
                return 1;
            case SAFE:
                return 2;
            case STRETCH:
                return 3;
            default:
                return 4;
        }
    }
}