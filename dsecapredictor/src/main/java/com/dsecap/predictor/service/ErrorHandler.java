package com.dsecap.predictor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Centralized error handler component for managing exceptions and providing
 * user-friendly error messages with resolution guidance.
 */
@Component
@Slf4j
public class ErrorHandler {
    
    private final ConsoleInterface consoleInterface;
    
    @Autowired
    public ErrorHandler(ConsoleInterface consoleInterface) {
        this.consoleInterface = consoleInterface;
    }
    
    /**
     * Handles data loading exceptions with appropriate user messaging and logging.
     * 
     * @param e the DataLoadingException to handle
     * @return true if the error is recoverable, false if application should terminate
     */
    public boolean handleDataLoadingError(DataLoadingException e) {
        log.error("Data loading error occurred", e);
        
        String message = e.getMessage();
        String suggestion = null;
        
        // Provide specific suggestions based on error type
        if (message.contains("file not found") || message.contains("CSV file not found")) {
            suggestion = "Please ensure the CSV file exists at the specified path and you have read permissions.";
        } else if (message.contains("No valid college data found")) {
            suggestion = "Please check that the CSV file contains valid college records with proper formatting.";
        } else if (message.contains("Error reading CSV file")) {
            suggestion = "Please verify the CSV file is not corrupted and is accessible.";
        } else {
            suggestion = "Please check the CSV file format and ensure it matches the expected structure.";
        }
        
        consoleInterface.displayCriticalError(message, suggestion);
        return false; // Data loading errors are typically not recoverable
    }
    
    /**
     * Handles validation exceptions with user-friendly messaging.
     * 
     * @param e the ValidationException to handle
     * @return true if the error is recoverable (user can retry input)
     */
    public boolean handleValidationError(ValidationException e) {
        log.debug("Validation error: {}", e.getMessage());
        
        String message = e.getMessage();
        String enhancedMessage = enhanceValidationMessage(message);
        
        consoleInterface.displayErrorMessage(enhancedMessage);
        return true; // Validation errors are recoverable - user can re-enter data
    }
    
    /**
     * Handles prediction exceptions with appropriate messaging.
     * 
     * @param e the PredictionException to handle
     * @return true if the error is recoverable, false otherwise
     */
    public boolean handlePredictionError(PredictionException e) {
        log.error("Prediction error occurred", e);
        
        String message = e.getMessage();
        String suggestion = null;
        
        // Provide specific suggestions based on error type
        if (message.contains("no matches found") || message.contains("No close matches")) {
            return handleNoMatchesFound(e);
        } else if (message.toLowerCase().contains("insufficient data")) {
            suggestion = "The system may need more historical data to make accurate predictions.";
        } else {
            suggestion = "Please try again or contact support if the problem persists.";
        }
        
        consoleInterface.displayCriticalError(message, suggestion);
        return false;
    }
    
    /**
     * Handles I/O exceptions that occur during user input collection.
     * 
     * @param e the IOException to handle
     * @return true if the error is recoverable, false otherwise
     */
    public boolean handleInputOutputError(IOException e) {
        log.error("I/O error during user input", e);
        
        String message;
        String suggestion;
        
        if (e.getMessage() != null && e.getMessage().contains("End of input stream")) {
            message = "Input stream was unexpectedly closed.";
            suggestion = "Please restart the application and try again.";
        } else {
            message = "An error occurred while reading your input.";
            suggestion = "Please ensure your console supports interactive input and try again.";
        }
        
        consoleInterface.displayCriticalError(message, suggestion);
        return false; // I/O errors are typically not recoverable
    }
    
    /**
     * Handles general runtime exceptions with generic error messaging.
     * 
     * @param e the RuntimeException to handle
     * @return true if the error might be recoverable, false otherwise
     */
    public boolean handleRuntimeError(RuntimeException e) {
        log.error("Runtime error occurred", e);
        
        String message = "An unexpected error occurred: " + e.getMessage();
        String suggestion = "Please try again. If the problem persists, contact support.";
        
        consoleInterface.displayCriticalError(message, suggestion);
        return false; // Most runtime errors are not recoverable
    }
    
    /**
     * Handles the specific case when no college matches are found for a student.
     * Provides detailed guidance on alternative approaches.
     * 
     * @param e the PredictionException indicating no matches
     * @return true (this is a recoverable scenario)
     */
    private boolean handleNoMatchesFound(PredictionException e) {
        log.info("No matches found for student profile: {}", e.getMessage());
        
        // Display the no matches message through console interface
        consoleInterface.displayNoMatchesFoundMessage();
        
        return true; // User can potentially try different criteria
    }
    
    /**
     * Enhances validation error messages with more helpful context.
     * 
     * @param originalMessage the original validation error message
     * @return enhanced message with additional guidance
     */
    private String enhanceValidationMessage(String originalMessage) {
        String lowerMessage = originalMessage.toLowerCase();
        
        if (lowerMessage.contains("percentage") && lowerMessage.contains("range")) {
            return originalMessage + ". Please enter a value between 0 and 100.";
        } else if (lowerMessage.contains("category")) {
            return originalMessage + ". Please use one of the valid categories: OPEN, OBC, SC, ST, NT, EWS, SEBC.";
        } else if (lowerMessage.contains("gender")) {
            return originalMessage + ". Please enter Male, Female, or Other.";
        } else if (lowerMessage.contains("branch")) {
            return originalMessage + ". Please enter a valid engineering branch name.";
        } else if (lowerMessage.contains("district")) {
            return originalMessage + ". Please enter your home district or region name.";
        } else {
            return originalMessage;
        }
    }
    
    /**
     * Handles application startup errors with appropriate messaging.
     * 
     * @param e the exception that occurred during startup
     * @return false (startup errors are not recoverable)
     */
    public boolean handleStartupError(Exception e) {
        log.error("Application startup error", e);
        
        String message = "Failed to start the application: " + e.getMessage();
        String suggestion = "Please check the application configuration and ensure all required files are present.";
        
        consoleInterface.displayCriticalError(message, suggestion);
        return false;
    }
    
    /**
     * Handles configuration-related errors.
     * 
     * @param e the exception related to configuration
     * @return false (configuration errors typically require restart)
     */
    public boolean handleConfigurationError(Exception e) {
        log.error("Configuration error", e);
        
        String message = "Configuration error: " + e.getMessage();
        String suggestion = "Please check the application.properties file and ensure all settings are correct.";
        
        consoleInterface.displayCriticalError(message, suggestion);
        return false;
    }
    
    /**
     * Logs and handles unexpected exceptions that don't fit other categories.
     * 
     * @param e the unexpected exception
     * @return false (unexpected errors are typically not recoverable)
     */
    public boolean handleUnexpectedError(Exception e) {
        log.error("Unexpected error occurred", e);
        
        String message = "An unexpected error occurred. Please contact support with the following details: " + 
                        e.getClass().getSimpleName() + " - " + e.getMessage();
        String suggestion = "Try restarting the application. If the problem persists, please report this issue.";
        
        consoleInterface.displayCriticalError(message, suggestion);
        return false;
    }
    
    /**
     * Provides a summary of common error resolution steps.
     */
    public void displayTroubleshootingGuide() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    TROUBLESHOOTING GUIDE");
        System.out.println("=".repeat(70));
        System.out.println("Common issues and solutions:");
        System.out.println();
        System.out.println("1. CSV File Not Found:");
        System.out.println("   • Ensure the CSV file exists in the specified path");
        System.out.println("   • Check file permissions (read access required)");
        System.out.println("   • Verify the file path in application.properties");
        System.out.println();
        System.out.println("2. No College Matches Found:");
        System.out.println("   • Try different engineering branches");
        System.out.println("   • Consider expanding your search to nearby districts");
        System.out.println("   • Check if your percentage is within reasonable range");
        System.out.println();
        System.out.println("3. Input Validation Errors:");
        System.out.println("   • Ensure percentage is between 0-100");
        System.out.println("   • Use valid category codes (OPEN, OBC, SC, ST, NT, EWS, SEBC)");
        System.out.println("   • Enter complete branch names (e.g., 'Computer Engineering')");
        System.out.println();
        System.out.println("4. Application Startup Issues:");
        System.out.println("   • Check Java version compatibility");
        System.out.println("   • Ensure all required dependencies are available");
        System.out.println("   • Verify application.properties configuration");
        System.out.println("=".repeat(70));
    }
}