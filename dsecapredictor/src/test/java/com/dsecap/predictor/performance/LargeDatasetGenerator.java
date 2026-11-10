package com.dsecap.predictor.performance;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating large datasets for performance testing.
 * Creates CSV files with realistic college cutoff data for scalability testing.
 */
public class LargeDatasetGenerator {
    
    private static final String[] COLLEGE_NAMES = {
        "Government College of Engineering Pune",
        "Walchand College of Engineering Sangli", 
        "College of Engineering Pune",
        "Veermata Jijabai Technological Institute Mumbai",
        "Sardar Patel College of Engineering Mumbai",
        "Government College of Engineering Aurangabad",
        "Shri Guru Gobind Singhji Institute of Engineering and Technology Nanded",
        "Government College of Engineering Nagpur",
        "Yeshwantrao Chavan College of Engineering Nagpur",
        "Rajarambapu Institute of Technology Rajaramnagar",
        "Bharati Vidyapeeth College of Engineering Pune",
        "MIT College of Engineering Pune",
        "Sinhgad College of Engineering Pune",
        "Pune Institute of Computer Technology",
        "Vishwakarma Institute of Technology Pune",
        "Dr. Babasaheb Ambedkar Technological University Lonere",
        "Konkan Gyanpeeth College of Engineering Karjat",
        "Thakur College of Engineering and Technology Mumbai",
        "Fr. Conceicao Rodrigues College of Engineering Mumbai",
        "Atharva College of Engineering Mumbai"
    };
    
    private static final String[] DISTRICTS = {
        "Pune", "Mumbai", "Nagpur", "Aurangabad", "Nashik", "Kolhapur", 
        "Sangli", "Satara", "Solapur", "Ahmednagar", "Nanded", "Latur",
        "Osmanabad", "Beed", "Jalna", "Parbhani", "Hingoli", "Washim",
        "Akola", "Amravati", "Buldhana", "Yavatmal", "Wardha", "Gadchiroli",
        "Chandrapur", "Gondia", "Bhandara", "Raigad", "Thane", "Palghar",
        "Ratnagiri", "Sindhudurg"
    };
    
    private static final String[] BRANCHES = {
        "Computer Engineering", "Information Technology", "Electronics Engineering",
        "Mechanical Engineering", "Civil Engineering", "Electrical Engineering",
        "Electronics and Telecommunication", "Instrumentation Engineering",
        "Chemical Engineering", "Automobile Engineering", "Production Engineering",
        "Biomedical Engineering", "Environmental Engineering", "Mining Engineering",
        "Metallurgical Engineering", "Textile Engineering", "Food Technology",
        "Biotechnology", "Agricultural Engineering", "Aerospace Engineering"
    };
    
    private static final String[] CATEGORIES = {
        "OPEN", "OBC", "SC", "ST", "NT", "EWS", "SEBC"
    };
    
    private static final String[] ROUNDS = {
        "CAP Round 1", "CAP Round 2", "CAP Round 3", "Institutional Round"
    };
    
    private static final int[] YEARS = {2019, 2020, 2021, 2022, 2023};
    
    private final Random random = new Random(42); // Fixed seed for reproducible results
    
    /**
     * Generates a large CSV dataset for performance testing.
     * 
     * @param filename Output filename
     * @param recordCount Number of records to generate
     * @throws IOException if file writing fails
     */
    public void generateLargeDataset(String filename, int recordCount) throws IOException {
        System.out.println("Generating large dataset with " + recordCount + " records...");
        
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.write("name,district,choiceCode,branch,year,round,category,cutoffPercent\n");
            
            // Generate records
            for (int i = 0; i < recordCount; i++) {
                String record = generateRandomRecord(i);
                writer.write(record + "\n");
                
                // Progress indicator
                if (i % 10000 == 0 && i > 0) {
                    System.out.println("Generated " + i + " records...");
                }
            }
        }
        
        System.out.println("Dataset generation completed: " + filename);
    }
    
    /**
     * Generates a single random college record.
     * 
     * @param index Record index for unique choice codes
     * @return CSV formatted record
     */
    private String generateRandomRecord(int index) {
        String collegeName = getRandomElement(COLLEGE_NAMES);
        String district = getRandomElement(DISTRICTS);
        String choiceCode = generateChoiceCode(collegeName, index);
        String branch = getRandomElement(BRANCHES);
        int year = getRandomElement(YEARS);
        String round = getRandomElement(ROUNDS);
        String category = getRandomElement(CATEGORIES);
        double cutoffPercent = generateRealisticCutoff(branch, category);
        
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",%.2f",
                collegeName, district, choiceCode, branch, year, round, category, cutoffPercent);
    }
    
    /**
     * Generates a realistic cutoff percentage based on branch and category.
     * 
     * @param branch Engineering branch
     * @param category Student category
     * @return Realistic cutoff percentage
     */
    private double generateRealisticCutoff(String branch, String category) {
        // Base cutoff ranges by branch popularity
        double baseCutoff;
        switch (branch) {
            case "Computer Engineering":
            case "Information Technology":
                baseCutoff = 85.0 + random.nextDouble() * 12.0; // 85-97%
                break;
            case "Electronics Engineering":
            case "Electronics and Telecommunication":
                baseCutoff = 80.0 + random.nextDouble() * 15.0; // 80-95%
                break;
            case "Mechanical Engineering":
            case "Electrical Engineering":
                baseCutoff = 75.0 + random.nextDouble() * 18.0; // 75-93%
                break;
            case "Civil Engineering":
            case "Chemical Engineering":
                baseCutoff = 70.0 + random.nextDouble() * 20.0; // 70-90%
                break;
            default:
                baseCutoff = 65.0 + random.nextDouble() * 25.0; // 65-90%
                break;
        }
        
        // Adjust for category (reserved categories typically have lower cutoffs)
        switch (category) {
            case "OPEN":
                // No adjustment
                break;
            case "OBC":
            case "EWS":
                baseCutoff -= 3.0 + random.nextDouble() * 4.0; // 3-7% lower
                break;
            case "SC":
            case "ST":
                baseCutoff -= 8.0 + random.nextDouble() * 7.0; // 8-15% lower
                break;
            case "NT":
            case "SEBC":
                baseCutoff -= 5.0 + random.nextDouble() * 5.0; // 5-10% lower
                break;
        }
        
        // Ensure cutoff is within valid range
        return Math.max(45.0, Math.min(100.0, baseCutoff));
    }
    
    /**
     * Generates a unique choice code for a college.
     * 
     * @param collegeName College name
     * @param index Record index
     * @return Unique choice code
     */
    private String generateChoiceCode(String collegeName, int index) {
        String prefix = collegeName.replaceAll("[^A-Z]", "").substring(0, Math.min(4, collegeName.replaceAll("[^A-Z]", "").length()));
        return String.format("%s%03d", prefix, (index % 999) + 1);
    }
    
    /**
     * Gets a random element from an array.
     * 
     * @param array Array to select from
     * @return Random element
     */
    private <T> T getRandomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
    
    /**
     * Gets a random element from an int array.
     * 
     * @param array Array to select from
     * @return Random element
     */
    private int getRandomElement(int[] array) {
        return array[random.nextInt(array.length)];
    }
    
    /**
     * Main method for standalone dataset generation.
     */
    public static void main(String[] args) {
        LargeDatasetGenerator generator = new LargeDatasetGenerator();
        
        try {
            // Generate datasets of different sizes for testing
            generator.generateLargeDataset("src/test/resources/performance/small-dataset.csv", 1000);
            generator.generateLargeDataset("src/test/resources/performance/medium-dataset.csv", 10000);
            generator.generateLargeDataset("src/test/resources/performance/large-dataset.csv", 50000);
            generator.generateLargeDataset("src/test/resources/performance/xlarge-dataset.csv", 100000);
            
            System.out.println("All datasets generated successfully!");
            
        } catch (IOException e) {
            System.err.println("Error generating datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }
}