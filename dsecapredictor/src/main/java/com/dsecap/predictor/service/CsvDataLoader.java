package com.dsecap.predictor.service;

import com.dsecap.predictor.model.College;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Component responsible for loading and parsing CSV files containing historical college cutoff data.
 * Handles file I/O operations and converts CSV rows to College objects.
 * Includes performance monitoring and memory optimization.
 */
@Component
@Slf4j
public class CsvDataLoader {
    
    private final PerformanceMonitor performanceMonitor;
    
    private static final int EXPECTED_FIELD_COUNT = 8;
    private static final String CSV_DELIMITER = ",";
    
    public CsvDataLoader(PerformanceMonitor performanceMonitor) {
        this.performanceMonitor = performanceMonitor;
    }
    
    /**
     * Loads college data from a CSV file.
     * Expected CSV format: name,district,choiceCode,branch,year,round,category,cutoffPercent
     * 
     * @param filePath the path to the CSV file
     * @return list of College objects parsed from the CSV
     * @throws DataLoadingException if file cannot be read or contains invalid data
     */
    public List<College> loadFromCsv(String filePath) throws DataLoadingException {
        log.info("Starting to load college data from CSV file: {}", filePath);
        
        // Start performance monitoring
        performanceMonitor.startOperation("CSV_LOADING");
        
        List<College> colleges = new ArrayList<>();
        int lineNumber = 0;
        int successfullyParsed = 0;
        int skippedRows = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            
            // Skip header row if present
            if ((line = reader.readLine()) != null) {
                lineNumber++;
                if (isHeaderRow(line)) {
                    log.debug("Skipping header row: {}", line);
                } else {
                    // First row is data, process it
                    try {
                        College college = parseCollegeFromCsvRow(line, lineNumber);
                        colleges.add(college);
                        successfullyParsed++;
                    } catch (Exception e) {
                        log.warn("Failed to parse line {}: {} - Error: {}", lineNumber, line, e.getMessage());
                        skippedRows++;
                    }
                }
            }
            
            // Process remaining rows
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                try {
                    College college = parseCollegeFromCsvRow(line, lineNumber);
                    colleges.add(college);
                    successfullyParsed++;
                } catch (Exception e) {
                    log.warn("Failed to parse line {}: {} - Error: {}", lineNumber, line, e.getMessage());
                    skippedRows++;
                }
            }
            
        } catch (FileNotFoundException e) {
            String errorMessage = String.format("CSV file not found: %s. Please ensure the file exists and the path is correct.", filePath);
            log.error(errorMessage, e);
            throw new DataLoadingException(errorMessage, e);
        } catch (SecurityException e) {
            String errorMessage = String.format("Access denied to CSV file: %s. Please check file permissions.", filePath);
            log.error(errorMessage, e);
            throw new DataLoadingException(errorMessage, e);
        } catch (IOException e) {
            String errorMessage = String.format("Error reading CSV file: %s. The file may be corrupted or in use by another application.", filePath);
            log.error(errorMessage, e);
            throw new DataLoadingException(errorMessage, e);
        } catch (OutOfMemoryError e) {
            String errorMessage = String.format("Insufficient memory to load CSV file: %s. The file may be too large.", filePath);
            log.error(errorMessage, e);
            throw new DataLoadingException(errorMessage, new Exception(e));
        }
        
        // End performance monitoring
        long loadingTime = performanceMonitor.endOperation("CSV_LOADING");
        
        log.info("CSV loading completed in {} ms. Successfully parsed: {}, Skipped rows: {}, Total lines processed: {}", 
                loadingTime, successfullyParsed, skippedRows, lineNumber);
        
        if (colleges.isEmpty()) {
            throw new DataLoadingException("No valid college data found in CSV file: " + filePath);
        }
        
        // Log memory efficiency metrics
        long memoryPerRecord = estimateMemoryPerRecord(colleges);
        log.info("Memory efficiency: ~{} bytes per college record, total estimated: {} KB", 
                memoryPerRecord, (memoryPerRecord * colleges.size()) / 1024);
        
        return colleges;
    }
    
    /**
     * Parses a single CSV row into a College object.
     * 
     * @param csvRow the CSV row to parse
     * @param lineNumber the line number for error reporting
     * @return College object created from the CSV row
     * @throws IllegalArgumentException if the row format is invalid
     */
    private College parseCollegeFromCsvRow(String csvRow, int lineNumber) {
        if (csvRow == null || csvRow.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV row is empty or null");
        }
        
        String[] fields = parseCsvRow(csvRow);
        
        validateCsvData(fields, lineNumber);
        
        try {
            return College.builder()
                    .name(cleanField(fields[0]))
                    .district(cleanField(fields[1]))
                    .choiceCode(cleanField(fields[2]))
                    .branch(cleanField(fields[3]))
                    .year(parseYear(fields[4], lineNumber))
                    .round(cleanField(fields[5]))
                    .category(cleanField(fields[6]))
                    .cutoffPercent(parsePercentage(fields[7], lineNumber))
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Failed to create College object from line %d: %s", lineNumber, e.getMessage()), e);
        }
    }
    
    /**
     * Validates CSV data fields for completeness and format.
     * 
     * @param csvFields array of CSV fields
     * @param lineNumber line number for error reporting
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCsvData(String[] csvFields, int lineNumber) {
        if (csvFields.length != EXPECTED_FIELD_COUNT) {
            throw new IllegalArgumentException(
                String.format("Line %d: Expected %d fields but found %d. Row: %s", 
                    lineNumber, EXPECTED_FIELD_COUNT, csvFields.length, String.join(",", csvFields)));
        }
        
        // Check for required fields (name, district, branch, category cannot be empty)
        String[] requiredFields = {csvFields[0], csvFields[1], csvFields[3], csvFields[6]};
        String[] fieldNames = {"name", "district", "branch", "category"};
        
        for (int i = 0; i < requiredFields.length; i++) {
            String cleanedField = cleanField(requiredFields[i]);
            if (cleanedField == null || cleanedField.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Line %d: Required field '%s' is empty", lineNumber, fieldNames[i]));
            }
        }
    }
    
    /**
     * Cleans and trims a CSV field, removing quotes if present.
     * 
     * @param field the field to clean
     * @return cleaned field value
     */
    private String cleanField(String field) {
        if (field == null) {
            return "";
        }
        
        String cleaned = field.trim();
        
        // Remove surrounding quotes if present
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        return cleaned;
    }
    
    /**
     * Parses year field from CSV.
     * 
     * @param yearField the year field to parse
     * @param lineNumber line number for error reporting
     * @return parsed year as Integer
     * @throws IllegalArgumentException if year format is invalid
     */
    private Integer parseYear(String yearField, int lineNumber) {
        String cleanYear = cleanField(yearField);
        
        if (cleanYear.isEmpty()) {
            throw new IllegalArgumentException(String.format("Line %d: Year field is empty", lineNumber));
        }
        
        try {
            int year = Integer.parseInt(cleanYear);
            if (year < 2000 || year > 2030) {
                throw new IllegalArgumentException(
                    String.format("Line %d: Year %d is not in valid range (2000-2030)", lineNumber, year));
            }
            return year;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Line %d: Invalid year format '%s'", lineNumber, cleanYear), e);
        }
    }
    
    /**
     * Parses percentage field from CSV.
     * 
     * @param percentField the percentage field to parse
     * @param lineNumber line number for error reporting
     * @return parsed percentage as Double
     * @throws IllegalArgumentException if percentage format is invalid
     */
    private Double parsePercentage(String percentField, int lineNumber) {
        String cleanPercent = cleanField(percentField);
        
        if (cleanPercent.isEmpty()) {
            throw new IllegalArgumentException(String.format("Line %d: Cutoff percentage field is empty", lineNumber));
        }
        
        try {
            double percentage = Double.parseDouble(cleanPercent);
            if (percentage < 0.0 || percentage > 100.0) {
                throw new IllegalArgumentException(
                    String.format("Line %d: Cutoff percentage %.2f is not in valid range (0.0-100.0)", 
                        lineNumber, percentage));
            }
            return percentage;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Line %d: Invalid percentage format '%s'", lineNumber, cleanPercent), e);
        }
    }
    
    /**
     * Parses a CSV row handling quoted fields properly.
     * This method correctly handles commas within quoted fields.
     * 
     * @param csvRow the CSV row to parse
     * @return array of parsed fields
     */
    private String[] parseCsvRow(String csvRow) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < csvRow.length(); i++) {
            char c = csvRow.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < csvRow.length() && csvRow.charAt(i + 1) == '"') {
                    // Handle escaped quotes ("")
                    currentField.append('"');
                    i++; // Skip the next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator found outside quotes
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Add the last field
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }
    
    /**
     * Determines if a line is a header row based on common header patterns.
     * 
     * @param line the line to check
     * @return true if the line appears to be a header
     */
    private boolean isHeaderRow(String line) {
        String lowerLine = line.toLowerCase();
        return lowerLine.contains("name") && lowerLine.contains("district") && 
               lowerLine.contains("branch") && lowerLine.contains("cutoff");
    }
    
    /**
     * Estimates memory usage per college record for performance monitoring.
     * 
     * @param colleges List of colleges to analyze
     * @return Estimated bytes per record
     */
    private long estimateMemoryPerRecord(List<College> colleges) {
        if (colleges.isEmpty()) {
            return 0;
        }
        
        // Sample a few records to estimate average memory usage
        int sampleSize = Math.min(10, colleges.size());
        long totalEstimatedBytes = 0;
        
        for (int i = 0; i < sampleSize; i++) {
            College college = colleges.get(i);
            totalEstimatedBytes += estimateCollegeMemoryUsage(college);
        }
        
        return totalEstimatedBytes / sampleSize;
    }
    
    /**
     * Estimates memory usage for a single college object.
     * 
     * @param college College object to analyze
     * @return Estimated bytes for this college
     */
    private long estimateCollegeMemoryUsage(College college) {
        long bytes = 0;
        
        // Object overhead (approximately 16 bytes for object header)
        bytes += 16;
        
        // String fields (each string has overhead + character data)
        bytes += estimateStringMemory(college.getName());
        bytes += estimateStringMemory(college.getDistrict());
        bytes += estimateStringMemory(college.getChoiceCode());
        bytes += estimateStringMemory(college.getBranch());
        bytes += estimateStringMemory(college.getRound());
        bytes += estimateStringMemory(college.getCategory());
        
        // Primitive fields
        bytes += 4; // Integer year
        bytes += 8; // Double cutoffPercent
        
        return bytes;
    }
    
    /**
     * Estimates memory usage for a string.
     * 
     * @param str String to analyze
     * @return Estimated bytes for this string
     */
    private long estimateStringMemory(String str) {
        if (str == null) {
            return 8; // Reference size
        }
        
        // String object overhead + char array + length
        return 40 + (str.length() * 2); // Each char is 2 bytes in UTF-16
    }
}