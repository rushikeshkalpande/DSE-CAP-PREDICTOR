package com.dsecap.predictor.service;

import com.dsecap.predictor.model.College;
import com.dsecap.predictor.model.CollegeRecommendation;
import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.repository.CollegeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {
    
    @Mock
    private CollegeRepository collegeRepository;
    
    private PredictionService predictionService;
    private Student testStudent;
    private List<College> testColleges;
    
    @BeforeEach
    void setUp() {
        PerformanceMonitor performanceMonitor = new PerformanceMonitor();
        CacheService cacheService = new CacheService();
        predictionService = new PredictionService(collegeRepository, performanceMonitor, cacheService);
        
        testStudent = Student.builder()
                .percentage(75.0)
                .branch("Computer Engineering")
                .category("OPEN")
                .gender("Male")
                .district("Mumbai")
                .isPwd(false)
                .build();
        
        testColleges = Arrays.asList(
            // Strong match - student percentage >= cutoff
            College.builder()
                    .name("College A")
                    .district("Mumbai")
                    .choiceCode("CA001")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(70.0)
                    .build(),
            
            // Safe match - within 5% below cutoff
            College.builder()
                    .name("College B")
                    .district("Pune")
                    .choiceCode("CB002")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(78.0)
                    .build(),
            
            // Stretch match - more than 5% below cutoff but within 10%
            College.builder()
                    .name("College C")
                    .district("Nashik")
                    .choiceCode("CC003")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(82.0)
                    .build(),
            
            // Outside range - should be filtered out
            College.builder()
                    .name("College D")
                    .district("Nagpur")
                    .choiceCode("CD004")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(90.0)
                    .build()
        );
    }
    
    @Test
    void testPredictColleges_Success() throws PredictionException {
        // Arrange
        when(collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN"))
                .thenReturn(testColleges);
        
        // Act
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        // Assert
        assertNotNull(recommendations);
        assertEquals(3, recommendations.size()); // College D should be filtered out
        
        // Verify sorting: Strong > Safe > Stretch
        assertEquals(CollegeRecommendation.ConfidenceLevel.STRONG, 
                    recommendations.get(0).getConfidence());
        assertEquals("College A", recommendations.get(0).getCollege().getName());
        
        assertEquals(CollegeRecommendation.ConfidenceLevel.SAFE, 
                    recommendations.get(1).getConfidence());
        assertEquals("College B", recommendations.get(1).getCollege().getName());
        
        assertEquals(CollegeRecommendation.ConfidenceLevel.STRETCH, 
                    recommendations.get(2).getConfidence());
        assertEquals("College C", recommendations.get(2).getCollege().getName());
        
        // Verify repository was called correctly
        verify(collegeRepository).findByBranchAndCategory("Computer Engineering", "OPEN");
    }
    
    @Test
    void testPredictColleges_NullStudent() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            predictionService.predictColleges(null);
        });
        
        verifyNoInteractions(collegeRepository);
    }
    
    @Test
    void testPredictColleges_NoMatchingColleges() {
        // Arrange
        when(collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN"))
                .thenReturn(Collections.emptyList());
        
        // Act & Assert
        assertThrows(PredictionException.class, () -> {
            predictionService.predictColleges(testStudent);
        });
        
        verify(collegeRepository).findByBranchAndCategory("Computer Engineering", "OPEN");
    }
    
    @Test
    void testPredictColleges_NoCollegesInRange() {
        // Arrange
        List<College> outOfRangeColleges = Arrays.asList(
            College.builder()
                    .name("High Cutoff College")
                    .district("Mumbai")
                    .choiceCode("HC001")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(90.0) // Outside ±10% range
                    .build(),
            College.builder()
                    .name("Low Cutoff College")
                    .district("Pune")
                    .choiceCode("LC002")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(60.0) // Outside ±10% range
                    .build()
        );
        
        when(collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN"))
                .thenReturn(outOfRangeColleges);
        
        // Act & Assert
        assertThrows(PredictionException.class, () -> {
            predictionService.predictColleges(testStudent);
        });
    }
    
    @Test
    void testConfidenceLevelCalculation() throws PredictionException {
        // Arrange
        List<College> testCollegesForConfidence = Arrays.asList(
            College.builder()
                    .name("Strong Match")
                    .district("Mumbai")
                    .choiceCode("SM001")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(75.0) // Equal to student percentage
                    .build(),
            College.builder()
                    .name("Safe Match")
                    .district("Pune")
                    .choiceCode("SAM002")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(78.0) // 3% above student (within 5%)
                    .build(),
            College.builder()
                    .name("Stretch Match")
                    .district("Nashik")
                    .choiceCode("STM003")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(82.0) // 7% above student (more than 5%)
                    .build()
        );
        
        when(collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN"))
                .thenReturn(testCollegesForConfidence);
        
        // Act
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        // Assert
        assertEquals(3, recommendations.size());
        
        // Find recommendations by college name and verify confidence levels
        CollegeRecommendation strongMatch = recommendations.stream()
                .filter(r -> "Strong Match".equals(r.getCollege().getName()))
                .findFirst().orElse(null);
        assertNotNull(strongMatch);
        assertEquals(CollegeRecommendation.ConfidenceLevel.STRONG, strongMatch.getConfidence());
        
        CollegeRecommendation safeMatch = recommendations.stream()
                .filter(r -> "Safe Match".equals(r.getCollege().getName()))
                .findFirst().orElse(null);
        assertNotNull(safeMatch);
        assertEquals(CollegeRecommendation.ConfidenceLevel.SAFE, safeMatch.getConfidence());
        
        CollegeRecommendation stretchMatch = recommendations.stream()
                .filter(r -> "Stretch Match".equals(r.getCollege().getName()))
                .findFirst().orElse(null);
        assertNotNull(stretchMatch);
        assertEquals(CollegeRecommendation.ConfidenceLevel.STRETCH, stretchMatch.getConfidence());
    }
    
    @Test
    void testMatchScoreCalculation() throws PredictionException {
        // Arrange
        College perfectMatch = College.builder()
                .name("Perfect Match")
                .district("Mumbai")
                .choiceCode("PM001")
                .branch("Computer Engineering")
                .year(2023)
                .round("Round 1")
                .category("OPEN")
                .cutoffPercent(75.0) // Exact match with student percentage
                .build();
        
        when(collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN"))
                .thenReturn(Arrays.asList(perfectMatch));
        
        // Act
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        // Assert
        assertEquals(1, recommendations.size());
        CollegeRecommendation recommendation = recommendations.get(0);
        
        // Perfect match should have high score (100 base + 10 bonus for strong match)
        assertTrue(recommendation.getMatchScore() > 90.0);
        assertEquals(CollegeRecommendation.ConfidenceLevel.STRONG, recommendation.getConfidence());
    }
    
    @Test
    void testRankingOrder() throws PredictionException {
        // Arrange - Create colleges with different confidence levels and scores
        List<College> mixedColleges = Arrays.asList(
            College.builder()
                    .name("Stretch High Score")
                    .district("Mumbai")
                    .choiceCode("SHS001")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(84.0) // Stretch but closer
                    .build(),
            College.builder()
                    .name("Strong Low Score")
                    .district("Pune")
                    .choiceCode("SLS002")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(65.0) // Strong but farther
                    .build(),
            College.builder()
                    .name("Safe Medium Score")
                    .district("Nashik")
                    .choiceCode("SMS003")
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(77.0) // Safe
                    .build()
        );
        
        when(collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN"))
                .thenReturn(mixedColleges);
        
        // Act
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        // Assert - Should be ordered by confidence priority first
        assertEquals(3, recommendations.size());
        
        // First should be Strong (regardless of score)
        assertEquals(CollegeRecommendation.ConfidenceLevel.STRONG, 
                    recommendations.get(0).getConfidence());
        assertEquals("Strong Low Score", recommendations.get(0).getCollege().getName());
        
        // Second should be Safe
        assertEquals(CollegeRecommendation.ConfidenceLevel.SAFE, 
                    recommendations.get(1).getConfidence());
        assertEquals("Safe Medium Score", recommendations.get(1).getCollege().getName());
        
        // Third should be Stretch
        assertEquals(CollegeRecommendation.ConfidenceLevel.STRETCH, 
                    recommendations.get(2).getConfidence());
        assertEquals("Stretch High Score", recommendations.get(2).getCollege().getName());
    }
    
    @Test
    void testMaxRecommendationsLimit() throws PredictionException {
        // Arrange - Create more than 15 colleges
        List<College> manyColleges = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            manyColleges.add(College.builder()
                    .name("College " + i)
                    .district("District " + i)
                    .choiceCode("C" + String.format("%03d", i))
                    .branch("Computer Engineering")
                    .year(2023)
                    .round("Round 1")
                    .category("OPEN")
                    .cutoffPercent(70.0 + i) // All within range
                    .build());
        }
        
        when(collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN"))
                .thenReturn(manyColleges);
        
        // Act
        List<CollegeRecommendation> recommendations = predictionService.predictColleges(testStudent);
        
        // Assert - Should be limited to 15 recommendations
        assertEquals(15, recommendations.size());
    }
}