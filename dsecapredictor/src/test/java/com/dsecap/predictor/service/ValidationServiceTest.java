package com.dsecap.predictor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ValidationService.
 * Tests all validation methods with valid inputs, invalid inputs, and edge cases.
 */
class ValidationServiceTest {
    
    private ValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }
    
    @Nested
    @DisplayName("Percentage Validation Tests")
    class PercentageValidationTests {
        
        @Test
        @DisplayName("Should accept valid percentage values")
        void shouldAcceptValidPercentages() {
            assertDoesNotThrow(() -> validationService.validatePercentage(0.0));
            assertDoesNotThrow(() -> validationService.validatePercentage(50.0));
            assertDoesNotThrow(() -> validationService.validatePercentage(100.0));
            assertDoesNotThrow(() -> validationService.validatePercentage(75.5));
            assertDoesNotThrow(() -> validationService.validatePercentage(0.1));
            assertDoesNotThrow(() -> validationService.validatePercentage(99.9));
        }
        
        @Test
        @DisplayName("Should reject null percentage")
        void shouldRejectNullPercentage() {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validatePercentage(null));
            assertEquals("Percentage is required and cannot be null", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should reject negative percentage")
        void shouldRejectNegativePercentage() {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validatePercentage(-1.0));
            assertTrue(exception.getMessage().contains("Percentage must be at least 0"));
            assertTrue(exception.getMessage().contains("-1.0"));
        }
        
        @Test
        @DisplayName("Should reject percentage over 100")
        void shouldRejectPercentageOver100() {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validatePercentage(101.0));
            assertTrue(exception.getMessage().contains("Percentage must not exceed 100"));
            assertTrue(exception.getMessage().contains("101.0"));
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {-0.1, -10.0, -100.0, 100.1, 150.0, 200.0})
        @DisplayName("Should reject invalid percentage values")
        void shouldRejectInvalidPercentages(double percentage) {
            assertThrows(ValidationException.class, 
                () -> validationService.validatePercentage(percentage));
        }
        
        @Test
        @DisplayName("isValidPercentage should return correct boolean values")
        void isValidPercentageShouldReturnCorrectValues() {
            assertTrue(validationService.isValidPercentage(50.0));
            assertTrue(validationService.isValidPercentage(0.0));
            assertTrue(validationService.isValidPercentage(100.0));
            
            assertFalse(validationService.isValidPercentage(null));
            assertFalse(validationService.isValidPercentage(-1.0));
            assertFalse(validationService.isValidPercentage(101.0));
        }
    }
    
    @Nested
    @DisplayName("Category Validation Tests")
    class CategoryValidationTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"OPEN", "OBC", "SC", "ST", "NT", "EWS", "SEBC"})
        @DisplayName("Should accept valid categories")
        void shouldAcceptValidCategories(String category) {
            assertDoesNotThrow(() -> validationService.validateCategory(category));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should reject null, empty, or whitespace-only categories")
        void shouldRejectNullOrEmptyCategories(String category) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateCategory(category));
            assertEquals("Category is required and cannot be empty", exception.getMessage());
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "open", "obc", "General", "Other", "ABC", "123"})
        @DisplayName("Should reject invalid categories")
        void shouldRejectInvalidCategories(String category) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateCategory(category));
            assertTrue(exception.getMessage().contains("Invalid category"));
            assertTrue(exception.getMessage().contains("Valid categories are:"));
        }
        
        @Test
        @DisplayName("Should handle categories with leading/trailing spaces")
        void shouldHandleCategoriesWithSpaces() {
            assertDoesNotThrow(() -> validationService.validateCategory("  OPEN  "));
            assertDoesNotThrow(() -> validationService.validateCategory("\tOBC\n"));
        }
        
        @Test
        @DisplayName("isValidCategory should return correct boolean values")
        void isValidCategoryShouldReturnCorrectValues() {
            assertTrue(validationService.isValidCategory("OPEN"));
            assertTrue(validationService.isValidCategory("OBC"));
            
            assertFalse(validationService.isValidCategory(null));
            assertFalse(validationService.isValidCategory(""));
            assertFalse(validationService.isValidCategory("INVALID"));
        }
    }
    
    @Nested
    @DisplayName("Branch Validation Tests")
    class BranchValidationTests {
        
        @ParameterizedTest
        @ValueSource(strings = {
            "Computer Engineering", 
            "Mechanical Engineering", 
            "Civil Engineering",
            "Electronics & Telecommunication",
            "Information Technology",
            "Electrical Engineering",
            "Chemical Engineering",
            "Automobile Engineering"
        })
        @DisplayName("Should accept valid branch names")
        void shouldAcceptValidBranches(String branch) {
            assertDoesNotThrow(() -> validationService.validateBranch(branch));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should reject null, empty, or whitespace-only branches")
        void shouldRejectNullOrEmptyBranches(String branch) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateBranch(branch));
            assertEquals("Engineering branch is required and cannot be empty", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should reject branch names that are too short")
        void shouldRejectShortBranches() {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateBranch("A"));
            assertEquals("Engineering branch must be at least 2 characters long", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should reject branch names that are too long")
        void shouldRejectLongBranches() {
            String longBranch = "A".repeat(51);
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateBranch(longBranch));
            assertEquals("Engineering branch must not exceed 50 characters", exception.getMessage());
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"Computer123", "Mech@nical", "Civil#Engineering", "IT$", "Comp*ter"})
        @DisplayName("Should reject branches with invalid characters")
        void shouldRejectBranchesWithInvalidCharacters(String branch) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateBranch(branch));
            assertTrue(exception.getMessage().contains("contains invalid characters"));
        }
        
        @Test
        @DisplayName("Should handle branches with leading/trailing spaces")
        void shouldHandleBranchesWithSpaces() {
            assertDoesNotThrow(() -> validationService.validateBranch("  Computer Engineering  "));
        }
        
        @Test
        @DisplayName("isValidBranch should return correct boolean values")
        void isValidBranchShouldReturnCorrectValues() {
            assertTrue(validationService.isValidBranch("Computer Engineering"));
            assertTrue(validationService.isValidBranch("Mechanical"));
            
            assertFalse(validationService.isValidBranch(null));
            assertFalse(validationService.isValidBranch(""));
            assertFalse(validationService.isValidBranch("A"));
            assertFalse(validationService.isValidBranch("Computer123"));
        }
    }
    
    @Nested
    @DisplayName("Gender Validation Tests")
    class GenderValidationTests {
        
        @ParameterizedTest
        @ValueSource(strings = {"Male", "Female", "Other"})
        @DisplayName("Should accept valid genders")
        void shouldAcceptValidGenders(String gender) {
            assertDoesNotThrow(() -> validationService.validateGender(gender));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should reject null, empty, or whitespace-only genders")
        void shouldRejectNullOrEmptyGenders(String gender) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateGender(gender));
            assertEquals("Gender is required and cannot be empty", exception.getMessage());
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"male", "female", "other", "M", "F", "Unknown", "Prefer not to say"})
        @DisplayName("Should reject invalid genders")
        void shouldRejectInvalidGenders(String gender) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateGender(gender));
            assertTrue(exception.getMessage().contains("Invalid gender"));
            assertTrue(exception.getMessage().contains("Valid options are:"));
        }
        
        @Test
        @DisplayName("Should handle genders with leading/trailing spaces")
        void shouldHandleGendersWithSpaces() {
            assertDoesNotThrow(() -> validationService.validateGender("  Male  "));
        }
    }
    
    @Nested
    @DisplayName("District Validation Tests")
    class DistrictValidationTests {
        
        @ParameterizedTest
        @ValueSource(strings = {
            "Mumbai", 
            "Pune", 
            "Nashik", 
            "Aurangabad",
            "Kolhapur",
            "Solapur",
            "Ahmednagar",
            "Satara"
        })
        @DisplayName("Should accept valid district names")
        void shouldAcceptValidDistricts(String district) {
            assertDoesNotThrow(() -> validationService.validateDistrict(district));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should reject null, empty, or whitespace-only districts")
        void shouldRejectNullOrEmptyDistricts(String district) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateDistrict(district));
            assertEquals("District is required and cannot be empty", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should reject district names that are too short")
        void shouldRejectShortDistricts() {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateDistrict("A"));
            assertEquals("District name must be at least 2 characters long", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should reject district names that are too long")
        void shouldRejectLongDistricts() {
            String longDistrict = "A".repeat(31);
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateDistrict(longDistrict));
            assertEquals("District name must not exceed 30 characters", exception.getMessage());
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"Mumbai123", "Pune@", "Nash#ik", "Aurang$bad", "Kolha*pur"})
        @DisplayName("Should reject districts with invalid characters")
        void shouldRejectDistrictsWithInvalidCharacters(String district) {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validateDistrict(district));
            assertTrue(exception.getMessage().contains("contains invalid characters"));
        }
        
        @Test
        @DisplayName("Should accept districts with hyphens and spaces")
        void shouldAcceptDistrictsWithHyphensAndSpaces() {
            assertDoesNotThrow(() -> validationService.validateDistrict("Ahmednagar-City"));
            assertDoesNotThrow(() -> validationService.validateDistrict("Greater Mumbai"));
        }
    }
    
    @Nested
    @DisplayName("PWD Status Validation Tests")
    class PwdStatusValidationTests {
        
        @Test
        @DisplayName("Should accept valid PWD status values")
        void shouldAcceptValidPwdStatus() {
            assertDoesNotThrow(() -> validationService.validatePwdStatus(true));
            assertDoesNotThrow(() -> validationService.validatePwdStatus(false));
        }
        
        @Test
        @DisplayName("Should reject null PWD status")
        void shouldRejectNullPwdStatus() {
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> validationService.validatePwdStatus(null));
            assertEquals("PWD status is required and cannot be null", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("All Fields Validation Tests")
    class AllFieldsValidationTests {
        
        @Test
        @DisplayName("Should accept all valid fields")
        void shouldAcceptAllValidFields() {
            assertDoesNotThrow(() -> validationService.validateAllFields(
                75.5, 
                "Computer Engineering", 
                "OPEN", 
                "Male", 
                "Mumbai", 
                false
            ));
        }
        
        @Test
        @DisplayName("Should reject when any field is invalid")
        void shouldRejectWhenAnyFieldIsInvalid() {
            // Test with invalid percentage
            assertThrows(ValidationException.class, () -> validationService.validateAllFields(
                -1.0, "Computer Engineering", "OPEN", "Male", "Mumbai", false
            ));
            
            // Test with invalid category
            assertThrows(ValidationException.class, () -> validationService.validateAllFields(
                75.5, "Computer Engineering", "INVALID", "Male", "Mumbai", false
            ));
            
            // Test with null PWD status
            assertThrows(ValidationException.class, () -> validationService.validateAllFields(
                75.5, "Computer Engineering", "OPEN", "Male", "Mumbai", null
            ));
        }
    }
    
    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("Should return valid categories set")
        void shouldReturnValidCategories() {
            var categories = validationService.getValidCategories();
            assertEquals(7, categories.size());
            assertTrue(categories.contains("OPEN"));
            assertTrue(categories.contains("OBC"));
            assertTrue(categories.contains("SC"));
            assertTrue(categories.contains("ST"));
            assertTrue(categories.contains("NT"));
            assertTrue(categories.contains("EWS"));
            assertTrue(categories.contains("SEBC"));
        }
        
        @Test
        @DisplayName("Should return valid genders set")
        void shouldReturnValidGenders() {
            var genders = validationService.getValidGenders();
            assertEquals(3, genders.size());
            assertTrue(genders.contains("Male"));
            assertTrue(genders.contains("Female"));
            assertTrue(genders.contains("Other"));
        }
        
        @Test
        @DisplayName("Returned sets should be immutable copies")
        void returnedSetsShouldBeImmutableCopies() {
            var categories = validationService.getValidCategories();
            var originalSize = categories.size();
            
            // Modifying returned set should not affect the service
            categories.add("NEW_CATEGORY");
            
            // Getting categories again should return original set
            var categoriesAgain = validationService.getValidCategories();
            assertEquals(originalSize, categoriesAgain.size());
            assertFalse(categoriesAgain.contains("NEW_CATEGORY"));
        }
    }
}