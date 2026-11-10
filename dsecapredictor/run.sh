#!/bin/bash
# DSE College Predictor - Unix/Linux Shell Script
# Usage: ./run.sh [profile]
# Examples:
#   ./run.sh          - Run with default profile
#   ./run.sh dev      - Run with development profile
#   ./run.sh prod     - Run with production profile

echo "========================================"
echo "DSE College Predictor"
echo "========================================"

if [ -z "$1" ]; then
    echo "Running with default profile..."
    mvn spring-boot:run
else
    echo "Running with profile: $1"
    mvn spring-boot:run -Dspring-boot.run.profiles="$1"
fi

if [ $? -ne 0 ]; then
    echo ""
    echo "Error: Failed to run the application"
    echo "Make sure Maven is installed and the project is compiled"
    echo "Try running: mvn clean compile"
    exit 1
fi