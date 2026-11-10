package com.dsecap.predictor.repository;

import com.dsecap.predictor.model.College;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollegeRepositoryTest {

    @Mock
    private Validator validator;

    private CollegeRepository collegeRepository;
    private College validCollege1;
    private College validCollege2;
    private College validCollege3;

    @BeforeEach
    void setUp() {
        collegeRepository = new CollegeRepository(validator);
        
        // Create test colleges
        validCollege1 = College.builder()
                .name("Government College of Engineering, Pune")
                .district("Pune")
                .choiceCode("GCP001")
                .branch("Computer Engineering")
                .year(2023)
                .round("CAP Round 1")
                .category("OPEN")
                .cutoffPercent(85.5)
                .build();

        validCollege2 = College.builder()
                .name("Walchand College of Engineering, Sangli")
                .district("Sangli")
                .choiceCode("WCE002")
                .branch("Computer Engineering")
                .year(2023)
                .round("CAP Round 1")
                .category("OBC")
                .cutoffPercent(78.2)
                .build();

        validCollege3 = College.builder()
                .name("College of Engineering, Pune")
                .district("Pune")
                .choiceCode("COEP003")
                .branch("Mechanical Engineering")
                .year(2023)
                .round("CAP Round 1")
                .category("OPEN")
                .cutoffPercent(82.0)
                .build();
    }

    @Test
    void testLoadColleges_ValidData_Success() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        List<College> colleges = Arrays.asList(validCollege1, validCollege2);

        // Act
        collegeRepository.loadColleges(colleges);

        // Assert
        assertEquals(2, collegeRepository.getCollegeCount());
        List<College> allColleges = collegeRepository.findAll();
        assertEquals(2, allColleges.size());
        assertTrue(allColleges.contains(validCollege1));
        assertTrue(allColleges.contains(validCollege2));
    }

    @Test
    void testLoadColleges_NullList_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> collegeRepository.loadColleges(null)
        );
        assertEquals("College list cannot be null", exception.getMessage());
    }

    @Test
    void testLoadColleges_InvalidCollege_ThrowsException() {
        // Arrange
        Set<ConstraintViolation<College>> violations = new HashSet<>();
        ConstraintViolation<College> violation = createMockViolation("College name is required");
        violations.add(violation);
        when(validator.validate(any(College.class))).thenReturn(violations);

        List<College> colleges = Arrays.asList(validCollege1);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> collegeRepository.loadColleges(colleges)
        );
        assertTrue(exception.getMessage().contains("College validation failed"));
    }

    @Test
    void testLoadColleges_ReplacesExistingData() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        
        // Load initial data
        collegeRepository.loadColleges(Arrays.asList(validCollege1));
        assertEquals(1, collegeRepository.getCollegeCount());

        // Act - Load new data
        collegeRepository.loadColleges(Arrays.asList(validCollege2, validCollege3));

        // Assert
        assertEquals(2, collegeRepository.getCollegeCount());
        List<College> allColleges = collegeRepository.findAll();
        assertFalse(allColleges.contains(validCollege1));
        assertTrue(allColleges.contains(validCollege2));
        assertTrue(allColleges.contains(validCollege3));
    }

    @Test
    void testFindAll_ReturnsDefensiveCopy() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1, validCollege2));

        // Act
        List<College> colleges1 = collegeRepository.findAll();
        List<College> colleges2 = collegeRepository.findAll();

        // Assert
        assertNotSame(colleges1, colleges2); // Different instances
        assertEquals(colleges1, colleges2); // Same content
        
        // Modifying returned list should not affect repository
        colleges1.clear();
        assertEquals(2, collegeRepository.getCollegeCount());
    }

    @Test
    void testFindByBranchAndCategory_ValidFilters_ReturnsMatchingColleges() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1, validCollege2, validCollege3));

        // Act
        List<College> computerOpenColleges = collegeRepository.findByBranchAndCategory("Computer Engineering", "OPEN");
        List<College> computerObcColleges = collegeRepository.findByBranchAndCategory("Computer Engineering", "OBC");
        List<College> mechanicalOpenColleges = collegeRepository.findByBranchAndCategory("Mechanical Engineering", "OPEN");

        // Assert
        assertEquals(1, computerOpenColleges.size());
        assertEquals(validCollege1, computerOpenColleges.get(0));

        assertEquals(1, computerObcColleges.size());
        assertEquals(validCollege2, computerObcColleges.get(0));

        assertEquals(1, mechanicalOpenColleges.size());
        assertEquals(validCollege3, mechanicalOpenColleges.get(0));
    }

    @Test
    void testFindByBranchAndCategory_CaseInsensitive() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1));

        // Act
        List<College> results = collegeRepository.findByBranchAndCategory("computer engineering", "open");

        // Assert
        assertEquals(1, results.size());
        assertEquals(validCollege1, results.get(0));
    }

    @Test
    void testFindByBranchAndCategory_NoMatches_ReturnsEmptyList() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1, validCollege2));

        // Act
        List<College> results = collegeRepository.findByBranchAndCategory("Civil Engineering", "OPEN");

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByBranchAndCategory_NullParameters_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> collegeRepository.findByBranchAndCategory(null, "OPEN"));
        
        assertThrows(IllegalArgumentException.class,
                () -> collegeRepository.findByBranchAndCategory("Computer Engineering", null));
        
        assertThrows(IllegalArgumentException.class,
                () -> collegeRepository.findByBranchAndCategory(null, null));
    }

    @Test
    void testFindByBranch_ValidBranch_ReturnsMatchingColleges() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1, validCollege2, validCollege3));

        // Act
        List<College> computerColleges = collegeRepository.findByBranch("Computer Engineering");

        // Assert
        assertEquals(2, computerColleges.size());
        assertTrue(computerColleges.contains(validCollege1));
        assertTrue(computerColleges.contains(validCollege2));
    }

    @Test
    void testFindByBranch_NullBranch_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> collegeRepository.findByBranch(null));
    }

    @Test
    void testFindByCategory_ValidCategory_ReturnsMatchingColleges() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1, validCollege2, validCollege3));

        // Act
        List<College> openColleges = collegeRepository.findByCategory("OPEN");

        // Assert
        assertEquals(2, openColleges.size());
        assertTrue(openColleges.contains(validCollege1));
        assertTrue(openColleges.contains(validCollege3));
    }

    @Test
    void testFindByCategory_NullCategory_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> collegeRepository.findByCategory(null));
    }

    @Test
    void testGetCollegeCount_EmptyRepository_ReturnsZero() {
        // Act & Assert
        assertEquals(0, collegeRepository.getCollegeCount());
    }

    @Test
    void testGetCollegeCount_WithColleges_ReturnsCorrectCount() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1, validCollege2, validCollege3));

        // Act & Assert
        assertEquals(3, collegeRepository.getCollegeCount());
    }

    @Test
    void testClear_RemovesAllColleges() {
        // Arrange
        when(validator.validate(any(College.class))).thenReturn(Collections.emptySet());
        collegeRepository.loadColleges(Arrays.asList(validCollege1, validCollege2));
        assertEquals(2, collegeRepository.getCollegeCount());

        // Act
        collegeRepository.clear();

        // Assert
        assertEquals(0, collegeRepository.getCollegeCount());
        assertTrue(collegeRepository.findAll().isEmpty());
    }

    @Test
    void testValidateCollege_NullCollege_ThrowsException() {
        // This tests the private method indirectly through loadColleges
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> collegeRepository.loadColleges(Arrays.asList((College) null)));
    }

    // Helper method to create mock constraint violation
    @SuppressWarnings("unchecked")
    private ConstraintViolation<College> createMockViolation(String message) {
        ConstraintViolation<College> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }
}