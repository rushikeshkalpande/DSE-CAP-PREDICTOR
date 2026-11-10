package com.dsecap.predictor;

import com.dsecap.predictor.config.PredictorConfig;
import com.dsecap.predictor.model.CollegeRecommendation;
import com.dsecap.predictor.model.Student;
import com.dsecap.predictor.repository.CollegeRepository;
import com.dsecap.predictor.service.*;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Main Spring Boot application class for DSE College Predictor.
 * Implements CommandLineRunner to provide console-based interaction.
 */
@SpringBootApplication
@Slf4j
public class DseCapPredictorApplication implements CommandLineRunner {

	private final CsvDataLoader csvDataLoader;
	private final CollegeRepository collegeRepository;
	private final SimpleConsoleInterface consoleInterface;
	private final PredictionService predictionService;
	private final PredictorConfig config;
	private final ErrorHandler errorHandler;
	private final PerformanceMonitor performanceMonitor;
	private final CacheService cacheService;
	private final Environment environment;

	@Autowired
	public DseCapPredictorApplication(
			CsvDataLoader csvDataLoader,
			CollegeRepository collegeRepository,
			SimpleConsoleInterface consoleInterface,
			PredictionService predictionService,
			PredictorConfig config,
			ErrorHandler errorHandler,
			PerformanceMonitor performanceMonitor,
			CacheService cacheService,
			Environment environment) {
		this.csvDataLoader = csvDataLoader;
		this.collegeRepository = collegeRepository;
		this.consoleInterface = consoleInterface;
		this.predictionService = predictionService;
		this.config = config;
		this.errorHandler = errorHandler;
		this.performanceMonitor = performanceMonitor;
		this.cacheService = cacheService;
		this.environment = environment;
	}

	public static void main(String[] args) {
		SpringApplication.run(DseCapPredictorApplication.class, args);
	}

	/**
	 * Main application execution flow.
	 * Implements the complete workflow: data loading → user input → prediction → output
	 */
	@Override
	public void run(String... args) throws Exception {
		// Check if running in test environment and skip console interaction
		String[] activeProfiles = environment.getActiveProfiles();
		boolean isTestEnvironment = java.util.Arrays.asList(activeProfiles).contains("test");
		
		if (isTestEnvironment) {
			// In test environment, just load data and return
			loadCollegeData();
			log.info("Application running in test mode - skipping console interaction");
			return;
		}
		
		// Start overall application performance monitoring
		performanceMonitor.startOperation("APPLICATION_EXECUTION");
		
		try {
			// Step 1: Display welcome message
			consoleInterface.displayWelcomeMessage();
			
			// Step 2: Load and validate CSV data
			if (!loadCollegeData()) {
				return; // Exit if data loading fails
			}
			
			// Step 3: Collect student input with error handling
			Student student = collectStudentInputWithRetry();
			if (student == null) {
				return; // Exit if input collection fails
			}
			
			// Step 4: Generate predictions with error handling
			consoleInterface.displayLoadingMessage("Analyzing your profile and generating recommendations");
			List<CollegeRecommendation> recommendations = generatePredictionsWithErrorHandling(student);
			
			// Step 5: Display results (recommendations could be empty if no matches found)
			if (recommendations != null) {
				consoleInterface.displayRecommendations(recommendations);
			}
			
			// Step 6: Display completion message
			consoleInterface.displayCompletionMessage();
			
		} catch (Exception e) {
			log.error("Application execution failed", e);
			if (!errorHandler.handleUnexpectedError(e)) {
				// If error handler indicates non-recoverable error, show troubleshooting guide
				errorHandler.displayTroubleshootingGuide();
			}
		} finally {
			// End performance monitoring and display summary
			long totalTime = performanceMonitor.endOperation("APPLICATION_EXECUTION");
			log.info("Application completed in {} ms", totalTime);
			
			// Display performance summary and cache statistics
			performanceMonitor.logPerformanceSummary();
			logCacheStatistics();
			
			// Clean up resources
			consoleInterface.close();
		}
	}
	
	/**
	 * Loads college data from CSV file with startup validation.
	 * 
	 * @return true if data loading is successful, false otherwise
	 */
	private boolean loadCollegeData() {
		try {
			consoleInterface.displayLoadingMessage("Loading historical college cutoff data");
			
			// Load CSV data
			var colleges = csvDataLoader.loadFromCsv(config.getCsvFilePath());
			
			// Validate that we have data
			if (colleges.isEmpty()) {
				DataLoadingException e = new DataLoadingException("No college data found in CSV file: " + config.getCsvFilePath());
				return !errorHandler.handleDataLoadingError(e);
			}
			
			// Load data into repository
			collegeRepository.loadColleges(colleges);
			
			// Confirm successful loading
			consoleInterface.displaySuccessMessage(
				String.format("Successfully loaded %d college records", colleges.size())
			);
			
			// Log configuration details
			log.info("Application started with configuration:");
			log.info("- CSV File Path: {}", config.getCsvFilePath());
			log.info("- Max Recommendations: {}", config.getMaxRecommendations());
			log.info("- Percentage Range: ±{}%", config.getPercentageRange());
			log.info("- Colleges Loaded: {}", colleges.size());
			
			return true;
			
		} catch (DataLoadingException e) {
			return !errorHandler.handleDataLoadingError(e);
		} catch (Exception e) {
			return !errorHandler.handleStartupError(e);
		}
	}
	
	/**
	 * Collects student input with retry mechanism for recoverable errors.
	 * 
	 * @return Student object if successful, null if unrecoverable error occurs
	 */
	private Student collectStudentInputWithRetry() {
		try {
			consoleInterface.displayLoadingMessage("Preparing input collection");
			return consoleInterface.collectStudentInput();
			
		} catch (Exception e) {
			if (!errorHandler.handleUnexpectedError(e)) {
				return null; // Unexpected errors are typically not recoverable
			}
		}
		
		return null;
	}
	
	/**
	 * Generates predictions with comprehensive error handling.
	 * 
	 * @param student the student profile
	 * @return list of recommendations, or null if prediction fails
	 */
	private List<CollegeRecommendation> generatePredictionsWithErrorHandling(Student student) {
		try {
			return predictionService.predictColleges(student);
			
		} catch (PredictionException e) {
			// Handle prediction-specific errors (including no matches found)
			if (errorHandler.handlePredictionError(e)) {
				// If the error is recoverable (like no matches found), return empty list
				// The error handler will have displayed appropriate messaging
				return List.of(); // Return empty list instead of null
			} else {
				// Non-recoverable prediction error
				return null;
			}
		} catch (Exception e) {
			// Handle unexpected errors during prediction
			if (!errorHandler.handleUnexpectedError(e)) {
				return null;
			}
			return List.of(); // Return empty list for recoverable unexpected errors
		}
	}
	
	/**
	 * Logs cache statistics for performance monitoring.
	 */
	private void logCacheStatistics() {
		CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
		log.info("Cache Statistics: College Filter Cache: {}, Percentage Filter Cache: {}, Total Utilization: {:.1f}%",
				stats.getCollegeFilterCacheSize(),
				stats.getPercentageFilterCacheSize(),
				stats.getCacheUtilization());
	}
}
