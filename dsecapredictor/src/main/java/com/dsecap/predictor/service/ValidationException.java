package com.dsecap.predictor.service;

/**
 * Exception thrown when input validation fails.
 * Provides specific error messages for different validation scenarios.
 */
public class ValidationException extends Exception {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}