package com.dsecap.predictor.service;

import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.model.College;
import com.dsecap.predictor.model.CollegeRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsoleInterface component.
 * Tests user input collection, validation integration, and output formatting.
 */
@ExtendWith(MockitoExtension.class)
class ConsoleInterfaceTest {
    
    @Mock
    private ValidationService validationService;
    
    private ConsoleInterface consoleInterface;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @BeforeEach
    void setUp() {
        // Capture console output for testing
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(System.in);
    }
    
    @Test
    void testCollectStudentInput_ValidInput_ReturnsStudent() throws IOException, ValidationException {
        // Arrange
        String input = "85.5\nComputer Engineering\nOPEN\nMale\nPune\nno\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        // Setup mock validation service for this test
        when(validationService.getValidCategories()).thenReturn(
            new HashSet<>(Arrays.asList("OPEN", "OBC", "SC", "ST", "NT", "EWS", "SEBC"))
        );
        when(validationService.getValidGenders()).thenReturn(
            new HashSet<>(Arrays.asList("Male", "Female", "Other"))
        );
        
        consoleInterface = new ConsoleInterface(validationService);
        
        // Setup validation mocks to pass
        doNothing().when(validationService).validatePercentage(85.5);
        doNothing().when(validationService).validateBranch("Computer Engineering");
        doNothing().when(validationService).validateCategory("OPEN");
        doNothing().when(validationService).validateGender("Male");
        doNothing().when(validationService).validateDistrict("Pune");
        doNothing().when(validationService).validatePwdStatus(false);
        
        // Act
        Student result = consoleInterface.collectStudentInput();
        
        // Assert
        assertNotNull(result);
        assertEquals(85.5, result.getPercentage());
        assertEquals("Computer Engineering", result.getBranch());
        assertEquals("OPEN", result.getCategory());
        assertEquals("Male", result.getGender());
        assertEquals("Pune", result.getDistrict());
        assertEquals(false, result.getIsPwd());
        
        // Verify validation methods were called
        verify(validationService).validatePercentage(85.5);
        verify(validationService).validateBranch("Computer Engineering");
        verify(validationService).validateCategory("OPEN");
        verify(validationService).validateGender("Male");
        verify(validationService).validateDistrict("Pune");
        verify(validationService).validatePwdStatus(false);
    }
    
    @Test
    void testCollectStudentInput_InvalidPercentage_RetriesUntilValid() throws IOException, ValidationException {
        // Arrange
        String input = "150\n85.5\nComputer Engineering\nOPEN\nMale\nPune\nyes\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        // Setup mock validation service for this test
        when(validationService.getValidCategories()).thenReturn(
            new HashSet<>(Arrays.asList("OPEN", "OBC", "SC", "ST", "NT", "EWS", "SEBC"))
        );
        when(validationService.getValidGenders()).thenReturn(
            new HashSet<>(Arrays.asList("Male", "Female", "Other"))
        );
        
        consoleInterface = new ConsoleInterface(validationService);
        
        // Setup validation mocks
        doThrow(new ValidationException("Percentage must not exceed 100"))
            .when(validationService).validatePercentage(150.0);
        doNothing().when(validationService).validatePercentage(85.5);
        doNothing().when(validationService).validateBranch(anyString());
        doNothing().when(validationService).validateCategory(anyString());
        doNothing().when(validationService).validateGender(anyString());
        doNothing().when(validationService).validateDistrict(anyString());
        doNothing().when(validationService).validatePwdStatus(any(Boolean.class));
        
        // Act
        Student result = consoleInterface.collectStudentInput();
        
        // Assert
        assertNotNull(result);
        assertEquals(85.5, result.getPercentage());
        assertEquals(true, result.getIsPwd());
        
        // Verify validation was called twice for percentage (once failed, once succeeded)
        verify(validationService).validatePercentage(150.0);
        verify(validationService).validatePercentage(85.5);
        
        // Verify error message was displayed
        String output = outputStream.toString();
        assertTrue(output.contains("Error: Percentage must not exceed 100"));
    }
    
    @Test
    void testDisplayRecommendations_WithValidRecommendations_DisplaysFormattedOutput() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        College college1 = College.builder()
            .name("Government College of Engineering, Pune")
            .branch("Computer Engineering")
            .district("Pune")
            .cutoffPercent(85.5)
            .year(2023)
            .round("Round 1")
            .choiceCode("GCP001")
            .build();
            
        College college2 = College.builder()
            .name("Walchand College of Engineering, Sangli")
            .branch("Computer Engineering")
            .district("Sangli")
            .cutoffPercent(82.0)
            .year(2023)
            .round("Round 2")
            .choiceCode("WCE002")
            .build();
        
        CollegeRecommendation rec1 = CollegeRecommendation.builder()
            .college(college1)
            .confidence(CollegeRecommendation.ConfidenceLevel.STRONG)
            .matchScore(95.0)
            .build();
            
        CollegeRecommendation rec2 = CollegeRecommendation.builder()
            .college(college2)
            .confidence(CollegeRecommendation.ConfidenceLevel.SAFE)
            .matchScore(88.0)
            .build();
        
        List<CollegeRecommendation> recommendations = Arrays.asList(rec1, rec2);
        
        // Act
        consoleInterface.displayRecommendations(recommendations);
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("COLLEGE RECOMMENDATIONS"));
        assertTrue(output.contains("Found 2 college recommendation(s)"));
        assertTrue(output.contains("Government College of Engineering, Pune"));
        assertTrue(output.contains("Walchand College of Engineering, Sangli"));
        assertTrue(output.contains("Computer Engineering"));
        assertTrue(output.contains("Pune"));
        assertTrue(output.contains("Sangli"));
        assertTrue(output.contains("85.50%"));
        assertTrue(output.contains("82.00%"));
        assertTrue(output.contains("Strong Match"));
        assertTrue(output.contains("Safe Match"));
        assertTrue(output.contains("LEGEND:"));
        assertTrue(output.contains("🟢 Strong Match"));
        assertTrue(output.contains("🟡 Safe Match"));
    }
    
    @Test
    void testDisplayRecommendations_EmptyList_DisplaysNoRecommendationsMessage() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        List<CollegeRecommendation> emptyRecommendations = Collections.emptyList();
        
        // Act
        consoleInterface.displayRecommendations(emptyRecommendations);
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("NO RECOMMENDATIONS FOUND"));
        assertTrue(output.contains("no close matches were found"));
        assertTrue(output.contains("Suggestions:"));
        assertTrue(output.contains("Try broadening your search criteria"));
        assertTrue(output.contains("Consider alternate engineering branches"));
    }
    
    @Test
    void testDisplayRecommendations_NullList_DisplaysNoRecommendationsMessage() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        // Act
        consoleInterface.displayRecommendations(null);
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("NO RECOMMENDATIONS FOUND"));
    }
    
    @Test
    void testDisplayErrorMessage_ShowsFormattedError() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        // Act
        consoleInterface.displayErrorMessage("Invalid input provided");
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("❌ Error: Invalid input provided"));
        assertTrue(output.contains("Please try again"));
    }
    
    @Test
    void testDisplayCriticalError_WithSuggestion_ShowsFormattedError() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        // Act
        consoleInterface.displayCriticalError("Database connection failed", "Check your network connection");
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("CRITICAL ERROR"));
        assertTrue(output.contains("❌ Database connection failed"));
        assertTrue(output.contains("💡 Suggestion: Check your network connection"));
    }
    
    @Test
    void testDisplayWelcomeMessage_ShowsWelcomeText() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        // Act
        consoleInterface.displayWelcomeMessage();
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("WELCOME TO DSE COLLEGE PREDICTOR"));
        assertTrue(output.contains("diploma engineering students"));
        assertTrue(output.contains("Direct Second Year (DSE) admission"));
    }
    
    @Test
    void testDisplayCompletionMessage_ShowsCompletionText() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        // Act
        consoleInterface.displayCompletionMessage();
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Thank you for using DSE College Predictor"));
        assertTrue(output.contains("Best of luck"));
        assertTrue(output.contains("🎓"));
    }
    
    @Test
    void testDisplayLoadingMessage_ShowsLoadingText() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        // Act
        consoleInterface.displayLoadingMessage("Loading college data");
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("⏳ Loading college data..."));
    }
    
    @Test
    void testDisplaySuccessMessage_ShowsSuccessText() {
        // Arrange
        consoleInterface = new ConsoleInterface(validationService);
        
        // Act
        consoleInterface.displaySuccessMessage("Data loaded successfully");
        
        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("✅ Data loaded successfully"));
    }
}