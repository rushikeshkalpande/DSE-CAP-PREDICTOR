@echo off
REM DSE College Predictor - Windows Batch Script
REM Usage: run.bat [profile]
REM Examples:
REM   run.bat          - Run with default profile
REM   run.bat dev      - Run with development profile
REM   run.bat prod     - Run with production profile

echo ========================================
echo DSE College Predictor
echo ========================================

if "%1"=="" (
    echo Running with default profile...
    mvn spring-boot:run
) else (
    echo Running with profile: %1
    mvn spring-boot:run -Dspring-boot.run.profiles=%1
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error: Failed to run the application
    echo Make sure Maven is installed and the project is compiled
    echo Try running: mvn clean compile
    pause
)