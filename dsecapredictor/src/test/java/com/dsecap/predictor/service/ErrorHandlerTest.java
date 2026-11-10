package com.dsecap.predictor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ErrorHandler component.
 * Tests centralized exception management and user-friendly error messaging.
 */
@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {
    
    @Mock
    private ConsoleInterface consoleInterface;
    
    private ErrorHandler errorHandler;
    
    @BeforeEach
    void setUp() {
        errorHandler = new ErrorHandler(consoleInterface);
    }
    
    @Test
    void testHandleDataLoadingError_FileNotFound() {
        // Given
        DataLoadingException exception = new DataLoadingException("CSV file not found: data.csv");
        
        // When
        boolean result = errorHandler.handleDataLoadingError(exception);
        
        // Then
        assertFalse(result, "Data loading errors should not be recoverable");
        verify(consoleInterface).displayCriticalError(
            eq("CSV file not found: data.csv"),
            eq("Please ensure the CSV file exists at the specified path and you have read permissions.")
        );
    }
    
    @Test
    void testHandleDataLoadingError_NoValidData() {
        // Given
        DataLoadingException exception = new DataLoadingException("No valid college data found in CSV file");
        
        // When
        boolean result = errorHandler.handleDataLoadingError(exception);
        
        // Then
        assertFalse(result);
        verify(consoleInterface).displayCriticalError(
            eq("No valid college data found in CSV file"),
            eq("Please check that the CSV file contains valid college records with proper formatting.")
        );
    }
    
    @Test
    void testHandleDataLoadingError_ReadError() {
        // Given
        DataLoadingException exception = new DataLoadingException("Error reading CSV file: data.csv");
        
        // When
        boolean result = errorHandler.handleDataLoadingError(exception);
        
        // Then
        assertFalse(result);
        verify(consoleInterface).displayCriticalError(
            eq("Error reading CSV file: data.csv"),
            eq("Please verify the CSV file is not corrupted and is accessible.")
        );
    }
    
    @Test
    void testHandleDataLoadingError_GenericError() {
        // Given
        DataLoadingException exception = new DataLoadingException("Some other data loading error");
        
        // When
        boolean result = errorHandler.handleDataLoadingError(exception);
        
        // Then
        assertFalse(result);
        verify(consoleInterface).displayCriticalError(
            eq("Some other data loading error"),
            eq("Please check the CSV file format and ensure it matches the expected structure.")
        );
    }
    
    @Test
    void testHandleValidationError_PercentageError() {
        // Given
        ValidationException exception = new ValidationException("Percentage must be in valid range");
        
        // When
        boolean result = errorHandler.handleValidationError(exception);
        
        // Then
        assertTrue(result, "Validation errors should be recoverable");
        verify(consoleInterface).displayErrorMessage(
            eq("Percentage must be in valid range. Please enter a value between 0 and 100.")
        );
    }
    
    @Test
    void testHandleValidationError_CategoryError() {
        // Given
        ValidationException exception = new ValidationException("Invalid category provided");
        
        // When
        boolean result = errorHandler.handleValidationError(exception);
        
        // Then
        assertTrue(result);
        verify(consoleInterface).displayErrorMessage(
            eq("Invalid category provided. Please use one of the valid categories: OPEN, OBC, SC, ST, NT, EWS, SEBC.")
        );
    }
    
    @Test
    void testHandleValidationError_GenderError() {
        // Given
        ValidationException exception = new ValidationException("Invalid gender specified");
        
        // When
        boolean result = errorHandler.handleValidationError(exception);
        
        // Then
        assertTrue(result);
        verify(consoleInterface).displayErrorMessage(
            eq("Invalid gender specified. Please enter Male, Female, or Other.")
        );
    }
    
    @Test
    void testHandleValidationError_BranchError() {
        // Given
        ValidationException exception = new ValidationException("Branch name is invalid");
        
        // When
        boolean result = errorHandler.handleValidationError(exception);
        
        // Then
        assertTrue(result);
        verify(consoleInterface).displayErrorMessage(
            eq("Branch name is invalid. Please enter a valid engineering branch name.")
        );
    }
    
    @Test
    void testHandleValidationError_DistrictError() {
        // Given
        ValidationException exception = new ValidationException("District information is required");
        
        // When
        boolean result = errorHandler.handleValidationError(exception);
        
        // Then
        assertTrue(result);
        verify(consoleInterface).displayErrorMessage(
            eq("District information is required. Please enter your home district or region name.")
        );
    }
    
    @Test
    void testHandleValidationError_GenericError() {
        // Given
        ValidationException exception = new ValidationException("Some validation error");
        
        // When
        boolean result = errorHandler.handleValidationError(exception);
        
        // Then
        assertTrue(result);
        verify(consoleInterface).displayErrorMessage(eq("Some validation error"));
    }
    
    @Test
    void testHandlePredictionError_NoMatches() {
        // Given
        PredictionException exception = new PredictionException("No close matches found for student profile");
        
        // When
        boolean result = errorHandler.handlePredictionError(exception);
        
        // Then
        assertTrue(result, "No matches found should be recoverable");
        verify(consoleInterface).displayNoMatchesFoundMessage();
    }
    
    @Test
    void testHandlePredictionError_InsufficientData() {
        // Given
        PredictionException exception = new PredictionException("Insufficient data for prediction");
        
        // When
        boolean result = errorHandler.handlePredictionError(exception);
        
        // Then
        assertFalse(result);
        verify(consoleInterface).displayCriticalError(
            eq("Insufficient data for prediction"),
            eq("The system may need more historical data to make accurate predictions.")
        );
    }
    
    @Test
    void testHandlePredictionError_GenericError() {
        // Given
        PredictionException exception = new PredictionException("Some prediction error");
        
        // When
        boolean result = errorHandler.handlePredictionError(exception);
        
        // Then
        assertFalse(result);
        verify(consoleInterface).displayCriticalError(
            eq("Some prediction error"),
            eq("Please try again or contact support if the problem persists.")
        );
    }
    
    @Test
    void testHandleInputOutputError_EndOfStream() {
        // Given
        IOException exception = new IOException("End of input stream reached");
        
        // When
        boolean result = errorHandler.handleInputOutputError(exception);
        
        // Then
        assertFalse(result, "I/O errors should not be recoverable");
        verify(consoleInterface).displayCriticalError(
            eq("Input stream was unexpectedly closed."),
            eq("Please restart the application and try again.")
        );
    }
    
    @Test
    void testHandleInputOutputError_GenericIOError() {
        // Given
        IOException exception = new IOException("Some I/O error");
        
        // When
        boolean result = errorHandler.handleInputOutputError(exception);
        
        // Then
        assertFalse(result);
        verify(consoleInterface).displayCriticalError(
            eq("An error occurred while reading your input."),
            eq("Please ensure your console supports interactive input and try again.")
        );
    }
    
    @Test
    void testHandleRuntimeError() {
        // Given
        RuntimeException exception = new RuntimeException("Some runtime error");
        
        // When
        boolean result = errorHandler.handleRuntimeError(exception);
        
        // Then
        assertFalse(result, "Runtime errors should not be recoverable");
        verify(consoleInterface).displayCriticalError(
            eq("An unexpected error occurred: Some runtime error"),
            eq("Please try again. If the problem persists, contact support.")
        );
    }
    
    @Test
    void testHandleStartupError() {
        // Given
        Exception exception = new Exception("Startup failed");
        
        // When
        boolean result = errorHandler.handleStartupError(exception);
        
        // Then
        assertFalse(result, "Startup errors should not be recoverable");
        verify(consoleInterface).displayCriticalError(
            eq("Failed to start the application: Startup failed"),
            eq("Please check the application configuration and ensure all required files are present.")
        );
    }
    
    @Test
    void testHandleConfigurationError() {
        // Given
        Exception exception = new Exception("Configuration is invalid");
        
        // When
        boolean result = errorHandler.handleConfigurationError(exception);
        
        // Then
        assertFalse(result, "Configuration errors should not be recoverable");
        verify(consoleInterface).displayCriticalError(
            eq("Configuration error: Configuration is invalid"),
            eq("Please check the application.properties file and ensure all settings are correct.")
        );
    }
    
    @Test
    void testHandleUnexpectedError() {
        // Given
        Exception exception = new Exception("Unexpected error");
        
        // When
        boolean result = errorHandler.handleUnexpectedError(exception);
        
        // Then
        assertFalse(result, "Unexpected errors should not be recoverable");
        verify(consoleInterface).displayCriticalError(
            eq("An unexpected error occurred. Please contact support with the following details: Exception - Unexpected error"),
            eq("Try restarting the application. If the problem persists, please report this issue.")
        );
    }
    
    @Test
    void testDisplayTroubleshootingGuide() {
        // When
        errorHandler.displayTroubleshootingGuide();
        
        // Then - verify that the guide is displayed (no specific verification needed as it prints to System.out)
        // This test ensures the method doesn't throw exceptions
        assertDoesNotThrow(() -> errorHandler.displayTroubleshootingGuide());
    }
}