#!/bin/bash

# Form Platform CLI Client Runner Script
# This script compiles and runs the CLI client

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    exit 1
fi

# Compile the project if needed
if [ ! -d "target/classes" ] || [ ! -f "target/classes/com/formplatform/infrastructure/adapter/input/cli/FormCliClient.class" ]; then
    echo -e "${YELLOW}Compiling project...${NC}"
    mvn compile -q
    if [ $? -ne 0 ]; then
        echo -e "${RED}Compilation failed${NC}"
        exit 1
    fi
    echo -e "${GREEN}Compilation successful${NC}"
fi

# Run the CLI client
java -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=compile -Dmdep.outputFile=/dev/stdout)" \
    com.formplatform.infrastructure.adapter.input.cli.FormCliClient "$@"
