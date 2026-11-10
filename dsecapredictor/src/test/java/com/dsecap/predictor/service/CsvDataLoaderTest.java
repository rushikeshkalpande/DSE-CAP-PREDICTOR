package com.dsecap.predictor.service;

import com.dsecap.predictor.model.College;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvDataLoader component.
 * Tests CSV parsing functionality, error handling, and data validation.
 */
class CsvDataLoaderTest {
    
    private CsvDataLoader csvDataLoader;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        PerformanceMonitor performanceMonitor = new PerformanceMonitor();
        csvDataLoader = new CsvDataLoader(performanceMonitor);
    }
    
    @Test
    void testLoadFromCsv_ValidFile_ShouldReturnColleges() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "Government College of Engineering Pune",Pune,GCP001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
            "Walchand College of Engineering Sangli",Sangli,WCE002,Mechanical Engineering,2023,CAP Round 1,OBC,88.2
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(2, colleges.size());
        
        College firstCollege = colleges.get(0);
        assertEquals("Government College of Engineering Pune", firstCollege.getName());
        assertEquals("Pune", firstCollege.getDistrict());
        assertEquals("GCP001", firstCollege.getChoiceCode());
        assertEquals("Computer Engineering", firstCollege.getBranch());
        assertEquals(2023, firstCollege.getYear());
        assertEquals("CAP Round 1", firstCollege.getRound());
        assertEquals("OPEN", firstCollege.getCategory());
        assertEquals(95.5, firstCollege.getCutoffPercent());
        
        College secondCollege = colleges.get(1);
        assertEquals("Walchand College of Engineering Sangli", secondCollege.getName());
        assertEquals("Sangli", secondCollege.getDistrict());
        assertEquals("WCE002", secondCollege.getChoiceCode());
        assertEquals("Mechanical Engineering", secondCollege.getBranch());
        assertEquals(2023, secondCollege.getYear());
        assertEquals("CAP Round 1", secondCollege.getRound());
        assertEquals("OBC", secondCollege.getCategory());
        assertEquals(88.2, secondCollege.getCutoffPercent());
    }
    
    @Test
    void testLoadFromCsv_FileNotFound_ShouldThrowDataLoadingException() {
        // Given
        String nonExistentFile = "non-existent-file.csv";
        
        // When & Then
        DataLoadingException exception = assertThrows(DataLoadingException.class, 
            () -> csvDataLoader.loadFromCsv(nonExistentFile));
        
        assertTrue(exception.getMessage().contains("CSV file not found"));
        assertTrue(exception.getMessage().contains(nonExistentFile));
    }
    
    @Test
    void testLoadFromCsv_EmptyFile_ShouldThrowDataLoadingException() throws Exception {
        // Given
        Path emptyFile = createTempCsvFile("");
        
        // When & Then
        DataLoadingException exception = assertThrows(DataLoadingException.class, 
            () -> csvDataLoader.loadFromCsv(emptyFile.toString()));
        
        assertTrue(exception.getMessage().contains("No valid college data found"));
    }
    
    @Test
    void testLoadFromCsv_HeaderOnly_ShouldThrowDataLoadingException() throws Exception {
        // Given
        String headerOnlyContent = "name,district,choiceCode,branch,year,round,category,cutoffPercent";
        Path headerOnlyFile = createTempCsvFile(headerOnlyContent);
        
        // When & Then
        DataLoadingException exception = assertThrows(DataLoadingException.class, 
            () -> csvDataLoader.loadFromCsv(headerOnlyFile.toString()));
        
        assertTrue(exception.getMessage().contains("No valid college data found"));
    }
    
    @Test
    void testLoadFromCsv_MalformedData_ShouldSkipInvalidRowsAndContinue() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "Valid College 1",Pune,VC001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
            "Invalid Fields",Pune,IF002
            "Valid College 2",Mumbai,VC003,Mechanical Engineering,2022,CAP Round 2,OBC,88.2
            "Invalid Year",Delhi,IY004,Electronics Engineering,invalid_year,CAP Round 1,OPEN,85.0
            "Valid College 3",Nashik,VC005,Civil Engineering,2023,CAP Round 1,SC,75.8
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(3, colleges.size()); // Only valid rows should be loaded
        assertEquals("Valid College 1", colleges.get(0).getName());
        assertEquals("Valid College 2", colleges.get(1).getName());
        assertEquals("Valid College 3", colleges.get(2).getName());
    }
    
    @Test
    void testLoadFromCsv_InvalidPercentage_ShouldSkipRow() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "Valid College",Pune,VC001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
            "Invalid Percentage High",Mumbai,IPH002,Mechanical Engineering,2023,CAP Round 1,OBC,150.0
            "Invalid Percentage Low",Delhi,IPL003,Electronics Engineering,2022,CAP Round 2,SC,-5.0
            "Invalid Percentage Format",Nashik,IPF004,Civil Engineering,2023,CAP Round 1,ST,abc
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(1, colleges.size()); // Only the valid row should be loaded
        assertEquals("Valid College", colleges.get(0).getName());
        assertEquals(95.5, colleges.get(0).getCutoffPercent());
    }
    
    @Test
    void testLoadFromCsv_InvalidYear_ShouldSkipRow() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "Valid College",Pune,VC001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
            "Invalid Year Format",Mumbai,IYF002,Mechanical Engineering,abc,CAP Round 1,OBC,88.2
            "Invalid Year Range Low",Delhi,IYRL003,Electronics Engineering,1999,CAP Round 2,SC,85.0
            "Invalid Year Range High",Nashik,IYRH004,Civil Engineering,2031,CAP Round 1,ST,82.5
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(1, colleges.size()); // Only the valid row should be loaded
        assertEquals("Valid College", colleges.get(0).getName());
        assertEquals(2023, colleges.get(0).getYear());
    }
    
    @Test
    void testLoadFromCsv_EmptyRequiredFields_ShouldSkipRow() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "Valid College",Pune,VC001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
            "",Mumbai,EN002,Mechanical Engineering,2023,CAP Round 1,OBC,88.2
            "Empty District",,ED003,Electronics Engineering,2022,CAP Round 2,SC,85.0
            "Empty Branch",Delhi,EB004,,2023,CAP Round 1,ST,82.5
            "Empty Category",Nashik,EC005,Civil Engineering,2023,CAP Round 1,,80.0
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(1, colleges.size()); // Only the valid row should be loaded
        assertEquals("Valid College", colleges.get(0).getName());
    }
    
    @Test
    void testLoadFromCsv_WithQuotedFields_ShouldHandleCorrectly() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "Government College of Engineering, Pune",Pune,GCP001,"Computer Engineering",2023,"CAP Round 1",OPEN,95.5
            Walchand College of Engineering Sangli,"Sangli",WCE002,Mechanical Engineering,2023,CAP Round 1,"OBC",88.2
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(2, colleges.size());
        assertEquals("Government College of Engineering, Pune", colleges.get(0).getName());
        assertEquals("Computer Engineering", colleges.get(0).getBranch());
        assertEquals("CAP Round 1", colleges.get(0).getRound());
        
        assertEquals("Walchand College of Engineering Sangli", colleges.get(1).getName());
        assertEquals("Sangli", colleges.get(1).getDistrict());
        assertEquals("OBC", colleges.get(1).getCategory());
    }
    
    @Test
    void testLoadFromCsv_WithEmptyLines_ShouldSkipEmptyLines() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "College 1",Pune,C001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
            
            "College 2",Mumbai,C002,Mechanical Engineering,2023,CAP Round 1,OBC,88.2
            
            
            "College 3",Delhi,C003,Electronics Engineering,2022,CAP Round 2,SC,85.0
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(3, colleges.size());
        assertEquals("College 1", colleges.get(0).getName());
        assertEquals("College 2", colleges.get(1).getName());
        assertEquals("College 3", colleges.get(2).getName());
    }
    
    @Test
    void testLoadFromCsv_NoHeader_ShouldTreatFirstRowAsData() throws Exception {
        // Given
        String csvContent = """
            "Government College of Engineering Pune",Pune,GCP001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
            "Walchand College of Engineering Sangli",Sangli,WCE002,Mechanical Engineering,2023,CAP Round 1,OBC,88.2
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(2, colleges.size());
        assertEquals("Government College of Engineering Pune", colleges.get(0).getName());
        assertEquals("Walchand College of Engineering Sangli", colleges.get(1).getName());
    }
    
    @Test
    void testLoadFromCsv_BoundaryPercentageValues_ShouldHandleCorrectly() throws Exception {
        // Given
        String csvContent = """
            name,district,choiceCode,branch,year,round,category,cutoffPercent
            "Zero Percent College",Pune,ZPC001,Computer Engineering,2023,CAP Round 1,OPEN,0.0
            "Perfect Score College",Mumbai,PSC002,Mechanical Engineering,2023,CAP Round 1,OBC,100.0
            "Decimal College",Delhi,DC003,Electronics Engineering,2022,CAP Round 2,SC,85.75
            """;
        Path csvFile = createTempCsvFile(csvContent);
        
        // When
        List<College> colleges = csvDataLoader.loadFromCsv(csvFile.toString());
        
        // Then
        assertEquals(3, colleges.size());
        assertEquals(0.0, colleges.get(0).getCutoffPercent());
        assertEquals(100.0, colleges.get(1).getCutoffPercent());
        assertEquals(85.75, colleges.get(2).getCutoffPercent());
    }
    
    /**
     * Helper method to create a temporary CSV file with the given content.
     */
    private Path createTempCsvFile(String content) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, content);
        return csvFile;
    }
}