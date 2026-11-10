# DSE College Predictor

A Java console application that helps diploma engineering students in Maharashtra predict suitable engineering colleges for Direct Second Year (DSE) admission based on historical cutoff data.

## Overview

The DSE College Predictor analyzes historical cutoff data from the past 5 years and provides personalized college recommendations based on student credentials and preferences. The application uses the ±10% rule to filter colleges and ranks them by match quality and confidence level.

## Features

- **Personalized Recommendations**: Get college suggestions based on your percentage, branch, category, and other preferences
- **Historical Data Analysis**: Uses 5 years of cutoff data for accurate predictions
- **Smart Filtering**: Applies ±10% rule to find colleges within your reach
- **Confidence Scoring**: Categorizes recommendations as Strong, Safe, or Stretch matches
- **Multiple Categories**: Supports OPEN, OBC, SC, ST, NT, EWS, SEBC categories
- **Console Interface**: Easy-to-use command-line interface

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd dsecapredictor
```

2. Build the application:
```bash
mvn clean compile
```

## Running the Application

### Using Maven:
```bash
mvn spring-boot:run
```

### Using Java directly:
```bash
mvn clean package
java -jar target/dsecapredictor-0.0.1-SNAPSHOT.jar
```

## Usage

1. **Start the application** using one of the methods above
2. **Enter your details** when prompted:
   - Final year percentage or CGPA (0-100)
   - Engineering branch (e.g., Computer Engineering, Mechanical Engineering)
   - Caste category (OPEN, OBC, SC, ST, NT, EWS, SEBC)
   - Gender (Male/Female/Other)
   - Home district/region
   - PWD status (true/false)

3. **Review recommendations** displayed in ranked order with:
   - College name and district
   - Branch and historical cutoff percentage
   - Year and round information
   - Confidence level (Strong/Safe/Stretch)

## Sample Input/Output

```
=== DSE College Predictor ===
Enter your final year percentage (0-100): 85.5
Enter your engineering branch: Computer Engineering
Enter your category (OPEN/OBC/SC/ST/NT/EWS/SEBC): OBC
Enter your gender (Male/Female/Other): Male
Enter your home district: Pune
Are you PWD? (true/false): false

=== College Recommendations ===
1. Walchand College of Engineering Sangli – Branch: Computer Engineering – District: Sangli – Cutoff: 86.8% (2023) – Confidence: Safe
2. Government College of Engineering Aurangabad – Branch: Computer Engineering – District: Aurangabad – Cutoff: 84.2% (2023) – Confidence: Strong
3. Government College of Engineering Nashik – Branch: Computer Engineering – District: Nashik – Cutoff: 82.8% (2023) – Confidence: Strong
```

## Configuration

The application can be configured through `src/main/resources/application.properties`:

```properties
# DSE Predictor Configuration
dse.predictor.csv-file-path=src/main/resources/data/sample-cutoff-data.csv
dse.predictor.max-recommendations=15
dse.predictor.percentage-range=10.0
dse.predictor.enable-detailed-logging=false
dse.predictor.min-year=2019

# Logging Configuration
logging.level.com.dsecap.predictor=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Configuration Options:

- `csv-file-path`: Path to the CSV file containing cutoff data
- `max-recommendations`: Maximum number of colleges to recommend (default: 15)
- `percentage-range`: Range for the ±% rule (default: 10.0)
- `enable-detailed-logging`: Enable detailed logging for debugging
- `min-year`: Minimum year to consider for historical data

## Data Format

The application expects CSV data in the following format:

```csv
name,district,choiceCode,branch,year,round,category,cutoffPercent
"College Name",District,CODE001,Computer Engineering,2023,CAP Round 1,OPEN,95.5
```

### CSV Fields:
- **name**: College name
- **district**: District where college is located
- **choiceCode**: Unique choice code for the college-branch combination
- **branch**: Engineering branch (e.g., Computer Engineering, Mechanical Engineering)
- **year**: Admission year
- **round**: CAP round (e.g., CAP Round 1, CAP Round 2)
- **category**: Student category (OPEN, OBC, SC, ST, NT, EWS, SEBC)
- **cutoffPercent**: Cutoff percentage for that year and category

## Testing

Run the test suite:

```bash
mvn test
```

Run specific test classes:

```bash
mvn test -Dtest=PredictionServiceTest
mvn test -Dtest=CsvDataLoaderTest
```

## Architecture

The application follows a layered architecture:

- **Console Interface Layer**: Handles user input/output
- **Business Logic Layer**: Prediction and validation services
- **Data Access Layer**: CSV loading and college repository
- **Domain Models**: Student, College, and recommendation models

## Troubleshooting

### Common Issues:

1. **CSV file not found**:
   - Ensure the CSV file exists at the configured path
   - Check file permissions

2. **No recommendations found**:
   - Try broadening the percentage range in configuration
   - Check if your branch exists in the data
   - Verify category spelling

3. **Invalid input errors**:
   - Ensure percentage is between 0-100
   - Use exact category names (OPEN, OBC, SC, ST, NT, EWS, SEBC)
   - Check branch name spelling

### Logging:

Enable detailed logging by setting:
```properties
dse.predictor.enable-detailed-logging=true
logging.level.com.dsecap.predictor=DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Check the troubleshooting section above
- Review the application logs
- Create an issue in the repository

---

**Note**: This application uses sample data for demonstration. For production use, replace the sample CSV file with actual historical cutoff data from Maharashtra's CAP rounds.