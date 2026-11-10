package com.dsecap.predictor.service;

import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.model.CollegeRecommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.util.List;
import java.util.Scanner;

/**
 * Simple console interface using basic System.console() and Scanner as fallback
 */
@Component("simpleConsoleInterface")
public class SimpleConsoleInterface {
    
    private final ValidationService validationService;
    
    @Autowired
    public SimpleConsoleInterface(ValidationService validationService) {
        this.validationService = validationService;
    }
    
    /**
     * Collects student input using the most basic input methods
     */
    public Student collectStudentInput() {
        System.out.println("=== DSE College Predictor (Simple Input) ===");
        System.out.println("Please provide your details for college recommendations:\n");
        
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
    
    private Double collectPercentage() {
        System.out.println("Examples: 85.5, 92.0, 78.25");
        
        while (true) {
            System.out.print("Enter your final year percentage or CGPA (0-100): ");
            System.out.flush();
            
            String input = readInput();
            
            if (input == null || input.trim().isEmpty()) {
                System.out.println("❌ Please enter a value.");
                continue;
            }
            
            input = input.trim();
            
            try {
                Double percentage = Double.parseDouble(input);
                validationService.validatePercentage(percentage);
                System.out.println("✅ Percentage accepted: " + percentage);
                return percentage;
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid number format. Please enter a valid decimal number (e.g., 85.5).");
            } catch (ValidationException e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }
    
    private String collectBranch() {
        while (true) {
            System.out.print("Enter your engineering branch (e.g., Computer Engineering): ");
            System.out.flush();
            
            String input = readInput();
            
            if (input == null || input.trim().isEmpty()) {
                System.out.println("❌ Please enter a branch name.");
                continue;
            }
            
            String branch = input.trim();
            
            try {
                validationService.validateBranch(branch);
                System.out.println("✅ Branch accepted: " + branch);
                return branch;
            } catch (ValidationException e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }   
 
    private String collectCategory() {
        System.out.println("Valid categories: " + String.join(", ", validationService.getValidCategories()));
        
        while (true) {
            System.out.print("Enter your caste/category: ");
            System.out.flush();
            
            String input = readInput();
            
            if (input == null || input.trim().isEmpty()) {
                System.out.println("❌ Please enter a category.");
                continue;
            }
            
            String category = input.trim();
            
            try {
                validationService.validateCategory(category);
                System.out.println("✅ Category accepted: " + category);
                return category;
            } catch (ValidationException e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }
    
    private String collectGender() {
        System.out.println("Valid options: " + String.join(", ", validationService.getValidGenders()));
        
        while (true) {
            System.out.print("Enter your gender: ");
            System.out.flush();
            
            String input = readInput();
            
            if (input == null || input.trim().isEmpty()) {
                System.out.println("❌ Please enter a gender.");
                continue;
            }
            
            String gender = input.trim();
            
            try {
                validationService.validateGender(gender);
                System.out.println("✅ Gender accepted: " + gender);
                return gender;
            } catch (ValidationException e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }
    
    private String collectDistrict() {
        while (true) {
            System.out.print("Enter your home district/region: ");
            System.out.flush();
            
            String input = readInput();
            
            if (input == null || input.trim().isEmpty()) {
                System.out.println("❌ Please enter a district.");
                continue;
            }
            
            String district = input.trim();
            
            try {
                validationService.validateDistrict(district);
                System.out.println("✅ District accepted: " + district);
                return district;
            } catch (ValidationException e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }
    
    private Boolean collectPwdStatus() {
        while (true) {
            System.out.print("Are you a Person with Disability (PWD)? (true/false or yes/no): ");
            System.out.flush();
            
            String input = readInput();
            
            if (input == null || input.trim().isEmpty()) {
                System.out.println("❌ Please enter yes/no or true/false.");
                continue;
            }
            
            input = input.trim().toLowerCase();
            
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
                System.out.println("✅ PWD status accepted: " + (isPwd ? "Yes" : "No"));
                return isPwd;
            } catch (ValidationException e) {
                System.out.println("❌ " + e.getMessage());
            }
        }
    }
    
    /**
     * Read input using multiple fallback methods
     */
    private String readInput() {
        try {
            // Method 1: Try System.console() first (most reliable)
            Console console = System.console();
            if (console != null) {
                return console.readLine();
            }
            
            // Method 2: Fallback to Scanner
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
            return null;
        }
    }  
  
    // ========== Display Methods ==========
    
    public void displayWelcomeMessage() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    DSE College Predictor                    ║");
        System.out.println("║              Direct Second Year Admission Helper            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Welcome! This tool helps diploma engineering students predict");
        System.out.println("suitable colleges for Direct Second Year (DSE) admission based");
        System.out.println("on historical cutoff data and your academic profile.");
        System.out.println();
    }
    
    public void displayLoadingMessage(String message) {
        System.out.println("⏳ " + message + "...");
    }
    
    public void displaySuccessMessage(String message) {
        System.out.println("✅ " + message);
    }
    
    public void displayRecommendations(List<CollegeRecommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            System.out.println("\n❌ No college matches found for your profile.");
            System.out.println("\n💡 Suggestions:");
            System.out.println("   • Try broadening your search criteria");
            System.out.println("   • Consider alternative branches");
            System.out.println("   • Check if your percentage is within typical ranges");
            return;
        }
        
        System.out.println("\n🎓 College Recommendations for Your Profile:");
        System.out.println("═".repeat(80));
        
        for (int i = 0; i < recommendations.size(); i++) {
            var rec = recommendations.get(i);
            var college = rec.getCollege();
            
            System.out.printf("%2d. %s%n", i + 1, college.getName());
            System.out.printf("    Branch: %s | District: %s%n", 
                college.getBranch(), college.getDistrict());
            System.out.printf("    Cutoff: %.1f%% (%d) | Confidence: %s%n", 
                college.getCutoffPercent(), college.getYear(), rec.getConfidence());
            System.out.printf("    Match Score: %.2f | Round: %s%n", 
                rec.getMatchScore(), college.getRound());
            
            if (i < recommendations.size() - 1) {
                System.out.println("    " + "─".repeat(60));
            }
        }
        
        System.out.println("═".repeat(80));
        System.out.println("\n📊 Legend:");
        System.out.println("   STRONG: High chance of admission");
        System.out.println("   SAFE: Good chance of admission");
        System.out.println("   STRETCH: Lower chance, but worth trying");
    }
    
    public void displayCompletionMessage() {
        System.out.println("\n🎯 Prediction Complete!");
        System.out.println("\n💡 Next Steps:");
        System.out.println("   1. Research the recommended colleges thoroughly");
        System.out.println("   2. Check current admission requirements and deadlines");
        System.out.println("   3. Prepare necessary documents for application");
        System.out.println("   4. Consider visiting college websites for detailed information");
        System.out.println("\n📞 For more assistance, contact the respective college admission offices.");
        System.out.println("\nThank you for using DSE College Predictor! Good luck with your admissions! 🍀");
    }
    
    public void displayErrorMessage(String message) {
        System.out.println("❌ " + message);
    }
    
    public void close() {
        // Nothing to close for this simple implementation
        System.out.println("Session ended.");
    }
}