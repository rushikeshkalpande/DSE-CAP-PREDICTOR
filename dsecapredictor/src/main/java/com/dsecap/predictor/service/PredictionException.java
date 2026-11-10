package com.dsecap.predictor.service;

/**
 * Exception thrown when there are issues during the college prediction process.
 * This includes business logic errors, data inconsistencies, and prediction failures.
 */
public class PredictionException extends Exception {
    
    /**
     * Constructs a new PredictionException with the specified detail message.
     * 
     * @param message the detail message
     */
    public PredictionException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new PredictionException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public PredictionException(String message, Throwable cause) {
        super(message, cause);
    }
}