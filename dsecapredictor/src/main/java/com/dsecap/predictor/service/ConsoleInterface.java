package com.dsecap.predictor.service;

import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.model.CollegeRecommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Scanner;
import java.util.List;

/**
 * Console interface component for handling all user input/output operations.
 * Provides interactive prompts for student data collection and formatted output for recommendations.
 */
@Component
public class ConsoleInterface {
    
    private final ValidationService validationService;
    private Scanner scanner;
    
    @Autowired
    public ConsoleInterface(ValidationService validationService) {
        this.validationService = validationService;
        initializeScanner();
    }
    
    /**
     * Initialize scanner with proper configuration
     */
    private void initializeScanner() {
        try {
            this.scanner = new Scanner(System.in);
            // Set delimiter to handle different line endings
            this.scanner.useDelimiter(System.lineSeparator());
        } catch (Exception e) {
            System.err.println("Warning: Could not initialize scanner properly: " + e.getMessage());
            this.scanner = new Scanner(System.in);
        }
    }
    
    /**
     * Collects student input through interactive console prompts.
     * Validates each input and re-prompts on validation errors.
     * 
     * @return Student object with validated data
     * @throws IOException if input reading fails
     */
    public Student collectStudentInput() throws IOException {
        System.out.println("=== DSE College Predictor ===");
        System.out.println("Please provide your details for college recommendations:\n");
        
        // Test if input is working
        testInputSystem();
        
        Double percentage = collectPercentage();
        String branch = collectBranch();
        String category = collectCategory();
        String gender = collectGender();
        String district = collectDistrict();
        Boolean isPwd = collectPwdStatus();
        
        return Student.builder()
                .percentage(percentage)
                .branch(branch)
                .category(category)
                .gender(gender)
                .district(district)
                .isPwd(isPwd)
                .build();
    }
    
    /**
     * Collects and validates student percentage.
     * 
     * @return validated percentage
     * @throws IOException if input reading fails
     */
    private Double collectPercentage() throws IOException {
        System.out.println("Examples: 85.5, 92.0, 78.25");
        
        int attempts = 0;
        while (attempts < 5) { // Limit attempts to prevent infinite loop
            System.out.print("Enter your final year percentage or CGPA (0-100): ");
            System.out.flush(); // Ensure the prompt is displayed immediately
            
            try {
                String input = readInputSafely();
                
                if (input == null || input.trim().isEmpty()) {
                    displayErrorMessage("Please enter a value.");
                    attempts++;
                    continue;
                }
                
                input = input.trim();
                
                try {
                    Double percentage = Double.parseDouble(input);
                    validationService.validatePercentage(percentage);
                    return percentage;
                } catch (NumberFormatException e) {
                    displayErrorMessage("Invalid number format. Please enter a valid decimal number (e.g., 85.5).");
                } catch (ValidationException e) {
                    displayErrorMessage(e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Error reading input (attempt " + (attempts + 1) + "): " + e.getMessage());
                
                // Try to reinitialize scanner
                if (attempts < 3) {
                    try {
                        initializeScanner();
                        System.out.println("Scanner reinitialized. Please try again.");
                    } catch (Exception reinitException) {
                        System.err.println("Could not reinitialize scanner: " + reinitException.getMessage());
                    }
                }
            }
            attempts++;
        }
        
        throw new IOException("Failed to read percentage input after multiple attempts");
    }
    
    /**
     * Safely read input with multiple fallback methods
     */
    private String readInputSafely() throws IOException {
        try {
            // Method 1: Try normal nextLine
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            }
            
            // Method 2: Try with next() if nextLine fails
            if (scanner.hasNext()) {
                return scanner.next();
            }
            
            // Method 3: Try BufferedReader as fallback
            return readWithBufferedReader();
            
        } catch (Exception e) {
            // Method 4: Last resort - try BufferedReader
            System.out.println("Falling back to alternative input method...");
            return readWithBufferedReader();
        }
    }
    
    /**
     * Fallback input method using BufferedReader
     */
    private String readWithBufferedReader() throws IOException {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            return reader.readLine();
        } catch (Exception e) {
            throw new IOException("All input methods failed: " + e.getMessage());
        }
    }
    
    /**
     * Test the input system to ensure it's working
     */
    private void testInputSystem() {
        System.out.println("Input system status:");
        System.out.println("- Scanner available: " + (scanner != null));
        System.out.println("- System.in available: " + (System.in != null));
        
        try {
            System.out.println("- Scanner has next: " + scanner.hasNext());
        } catch (Exception e) {
            System.out.println("- Scanner test failed: " + e.getMessage());
        }
        
        System.out.println("Ready to collect input...\n");
    }
    
    /**
     * Collects and validates engineering branch.
     * 
     * @return validated branch
     * @throws IOException if input reading fails
     */
    private String collectBranch() throws IOException {
        while (true) {
            System.out.print("Enter your engineering branch (e.g., Computer Engineering, Mechanical Engineering): ");
            System.out.flush();
            
            try {
                String input = readInputSafely();
                if (input == null) {
                    throw new IOException("End of input stream reached");
                }
                
                String branch = input.trim();
                if (branch.isEmpty()) {
                    displayErrorMessage("Please enter a branch name.");
                    continue;
                }
                
                try {
                    validationService.validateBranch(branch);
                    return branch;
                } catch (ValidationException e) {
                    displayErrorMessage(e.getMessage());
                }
            } catch (Exception e) {
                throw new IOException("Error reading input: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Collects and validates student category.
     * 
     * @return validated category
     * @throws IOException if input reading fails
     */
    private String collectCategory() throws IOException {
        System.out.println("Valid categories: " + String.join(", ", validationService.getValidCategories()));
        
        while (true) {
            System.out.print("Enter your caste/category: ");
            
            try {
                String category = scanner.nextLine().trim();
                
                try {
                    validationService.validateCategory(category);
                    return category;
                } catch (ValidationException e) {
                    displayErrorMessage(e.getMessage());
                }
            } catch (Exception e) {
                throw new IOException("Error reading input: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Collects and validates student gender.
     * 
     * @return validated gender
     * @throws IOException if input reading fails
     */
    private String collectGender() throws IOException {
        System.out.println("Valid options: " + String.join(", ", validationService.getValidGenders()));
        
        while (true) {
            System.out.print("Enter your gender: ");
            
            try {
                String gender = scanner.nextLine().trim();
                
                try {
                    validationService.validateGender(gender);
                    return gender;
                } catch (ValidationException e) {
                    displayErrorMessage(e.getMessage());
                }
            } catch (Exception e) {
                throw new IOException("Error reading input: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Collects and validates student district.
     * 
     * @return validated district
     * @throws IOException if input reading fails
     */
    private String collectDistrict() throws IOException {
        while (true) {
            System.out.print("Enter your home district/region: ");
            
            try {
                String district = scanner.nextLine().trim();
                
                try {
                    validationService.validateDistrict(district);
                    return district;
                } catch (ValidationException e) {
                    displayErrorMessage(e.getMessage());
                }
            } catch (Exception e) {
                throw new IOException("Error reading input: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Collects and validates PWD status.
     * 
     * @return validated PWD status
     * @throws IOException if input reading fails
     */
    private Boolean collectPwdStatus() throws IOException {
        while (true) {
            System.out.print("Are you a Person with Disability (PWD)? (true/false or yes/no): ");
            
            try {
                String input = scanner.nextLine().trim().toLowerCase();
                
                try {
                    Boolean isPwd;
                    if ("true".equals(input) || "yes".equals(input) || "y".equals(input)) {
                        isPwd = true;
                    } else if ("false".equals(input) || "no".equals(input) || "n".equals(input)) {
                        isPwd = false;
                    } else {
                        throw new ValidationException("Please enter 'true/false' or 'yes/no'");
                    }
                    
                    validationService.validatePwdStatus(isPwd);
                    return isPwd;
                } catch (ValidationException e) {
                    displayErrorMessage(e.getMessage());
                }
            } catch (Exception e) {
                throw new IOException("Error reading input: " + e.getMessage(), e);
            }
        }
    }    

    /**
     * Displays college recommendations in a formatted, user-friendly manner.
     * Shows recommendations ranked by confidence and match quality.
     * 
     * @param recommendations list of college recommendations to display
     */
    public void displayRecommendations(List<CollegeRecommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            displayNoRecommendationsMessage();
            return;
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                    COLLEGE RECOMMENDATIONS");
        System.out.println("=".repeat(80));
        System.out.printf("Found %d college recommendation(s) for you:\n\n", recommendations.size());
        
        for (int i = 0; i < recommendations.size(); i++) {
            CollegeRecommendation recommendation = recommendations.get(i);
            displaySingleRecommendation(i + 1, recommendation);
        }
        
        System.out.println("=".repeat(80));
        displayRecommendationLegend();
    }
    
    /**
     * Displays a single college recommendation with formatted output.
     * 
     * @param rank the ranking number of the recommendation
     * @param recommendation the college recommendation to display
     */
    private void displaySingleRecommendation(int rank, CollegeRecommendation recommendation) {
        String confidenceIcon = getConfidenceIcon(recommendation.getConfidence());
        String confidenceColor = getConfidenceDescription(recommendation.getConfidence());
        
        System.out.printf("%d. %s %s\n", rank, confidenceIcon, recommendation.getCollege().getName());
        System.out.printf("   Branch: %s\n", recommendation.getCollege().getBranch());
        System.out.printf("   District: %s\n", recommendation.getCollege().getDistrict());
        System.out.printf("   Cutoff: %.2f%% (%d, %s)\n", 
            recommendation.getCollege().getCutoffPercent(),
            recommendation.getCollege().getYear(),
            recommendation.getCollege().getRound());
        System.out.printf("   Confidence: %s (Match Score: %.1f)\n", 
            confidenceColor, recommendation.getMatchScore());
        
        if (recommendation.getCollege().getChoiceCode() != null && 
            !recommendation.getCollege().getChoiceCode().trim().isEmpty()) {
            System.out.printf("   Choice Code: %s\n", recommendation.getCollege().getChoiceCode());
        }
        
        System.out.println();
    }
    
    /**
     * Gets the icon representation for confidence level.
     * 
     * @param confidence the confidence level
     * @return icon string
     */
    private String getConfidenceIcon(CollegeRecommendation.ConfidenceLevel confidence) {
        switch (confidence) {
            case STRONG:
                return "🟢";
            case SAFE:
                return "🟡";
            case STRETCH:
                return "🔴";
            default:
                return "⚪";
        }
    }
    
    /**
     * Gets the description for confidence level.
     * 
     * @param confidence the confidence level
     * @return description string
     */
    private String getConfidenceDescription(CollegeRecommendation.ConfidenceLevel confidence) {
        switch (confidence) {
            case STRONG:
                return "Strong Match - You meet or exceed the cutoff";
            case SAFE:
                return "Safe Match - You're close to the cutoff";
            case STRETCH:
                return "Stretch Match - Requires improvement or luck";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Displays the legend explaining confidence levels and icons.
     */
    private void displayRecommendationLegend() {
        System.out.println("LEGEND:");
        System.out.println("🟢 Strong Match - Your percentage meets or exceeds the historical cutoff");
        System.out.println("🟡 Safe Match - Your percentage is close to the cutoff (within range)");
        System.out.println("🔴 Stretch Match - Your percentage is below cutoff, consider as backup");
        System.out.println("\nNote: Recommendations are based on historical data and actual cutoffs may vary.");
        System.out.println("Consider applying to a mix of Strong, Safe, and Stretch colleges.");
    }
    
    /**
     * Displays message when no recommendations are found.
     */
    private void displayNoRecommendationsMessage() {
        displayNoMatchesFoundMessage();
    }
    
    /**
     * Displays detailed message when no college matches are found for a student profile.
     * Provides comprehensive guidance on alternative approaches and next steps.
     */
    public void displayNoMatchesFoundMessage() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    NO COLLEGE MATCHES FOUND");
        System.out.println("=".repeat(70));
        System.out.println("Unfortunately, no colleges match your profile within the ±10% range.");
        System.out.println();
        System.out.println("This could happen for several reasons:");
        System.out.println("• Your percentage may be outside the typical range for your branch/category");
        System.out.println("• Limited historical data for your specific branch-category combination");
        System.out.println("• Your branch might have different naming conventions in our data");
        System.out.println();
        System.out.println("RECOMMENDED NEXT STEPS:");
        System.out.println();
        System.out.println("1. 📚 BROADEN YOUR SEARCH:");
        System.out.println("   • Try related engineering branches (e.g., 'Computer' instead of 'Computer Engineering')");
        System.out.println("   • Consider similar branches (Mechanical, Electrical, Civil, etc.)");
        System.out.println("   • Look into colleges in neighboring districts");
        System.out.println();
        System.out.println("2. 🎯 ALTERNATIVE STRATEGIES:");
        System.out.println("   • Research colleges that accept lower cutoffs in later rounds");
        System.out.println("   • Consider management quota or institute-level admissions");
        System.out.println("   • Look into private colleges with different admission criteria");
        System.out.println();
        System.out.println("3. 📈 IMPROVE YOUR PROFILE:");
        System.out.println("   • If possible, retake exams to improve your percentage");
        System.out.println("   • Gather additional certificates or achievements");
        System.out.println("   • Consider gap year for better preparation");
        System.out.println();
        System.out.println("4. 🤝 SEEK GUIDANCE:");
        System.out.println("   • Consult with academic counselors at your current institution");
        System.out.println("   • Contact admission offices of target colleges directly");
        System.out.println("   • Join student forums for peer advice and experiences");
        System.out.println();
        System.out.println("5. 🔄 TRY AGAIN:");
        System.out.println("   • Run the predictor again with different branch names");
        System.out.println("   • Try variations like 'IT' instead of 'Information Technology'");
        System.out.println("   • Consider checking multiple category options if eligible");
        System.out.println();
        System.out.println("💡 Remember: This predictor is based on historical data. Actual admission");
        System.out.println("   scenarios can vary, and new opportunities may be available!");
        System.out.println("=".repeat(70));
    }
    
    /**
     * Displays error messages in a user-friendly format.
     * 
     * @param message the error message to display
     */
    public void displayErrorMessage(String message) {
        System.err.println("❌ Error: " + message);
        System.err.println("Please try again.\n");
    }
    
    /**
     * Displays critical error messages that may terminate the application.
     * 
     * @param message the critical error message
     * @param suggestion optional suggestion for resolution
     */
    public void displayCriticalError(String message, String suggestion) {
        System.err.println("\n" + "!".repeat(60));
        System.err.println("                    CRITICAL ERROR");
        System.err.println("!".repeat(60));
        System.err.println("❌ " + message);
        
        if (suggestion != null && !suggestion.trim().isEmpty()) {
            System.err.println("\n💡 Suggestion: " + suggestion);
        }
        
        System.err.println("!".repeat(60));
    }
    
    /**
     * Displays a welcome message when the application starts.
     */
    public void displayWelcomeMessage() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              WELCOME TO DSE COLLEGE PREDICTOR");
        System.out.println("=".repeat(80));
        System.out.println("This application helps diploma engineering students predict suitable");
        System.out.println("colleges for Direct Second Year (DSE) admission based on historical");
        System.out.println("cutoff data from the past 5 years.");
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * Displays a completion message when the application finishes successfully.
     */
    public void displayCompletionMessage() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Thank you for using DSE College Predictor!");
        System.out.println("Best of luck with your college applications! 🎓");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Displays a loading message during data processing.
     * 
     * @param operation the operation being performed
     */
    public void displayLoadingMessage(String operation) {
        System.out.println("⏳ " + operation + "...");
    }
    
    /**
     * Displays a success message for completed operations.
     * 
     * @param message the success message
     */
    public void displaySuccessMessage(String message) {
        System.out.println("✅ " + message);
    }
    
    /**
     * Prompts user to press Enter to continue.
     * 
     * @throws IOException if input reading fails
     */
    public void waitForUserInput() throws IOException {
        System.out.print("\nPress Enter to continue...");
        try {
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
        } catch (Exception e) {
            throw new IOException("Error reading input: " + e.getMessage(), e);
        }
    }
    
    /**
     * Closes the scanner resources.
     */
    public void close() {
        try {
            if (scanner != null) {
                scanner.close();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not close scanner properly");
        }
    }
}