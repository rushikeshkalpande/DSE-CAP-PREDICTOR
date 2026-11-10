package com.dsecap.predictor.service;

/**
 * Exception thrown when there are issues loading or parsing CSV data.
 * This includes file not found, malformed data, and other I/O related errors.
 */
public class DataLoadingException extends Exception {
    
    /**
     * Constructs a new DataLoadingException with the specified detail message.
     * 
     * @param message the detail message
     */
    public DataLoadingException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new DataLoadingException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public DataLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}