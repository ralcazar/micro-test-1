#!/bin/bash

# Form Platform CLI Client Runner Script
# Runs the CLI from the formplatform module

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the directory where the script is located (project root)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    exit 1
fi

# Compile the formplatform module if needed
if [ ! -d "formplatform/target/classes" ] || [ ! -f "formplatform/target/classes/com/formplatform/infrastructure/adapter/input/cli/FormCliClient.class" ]; then
    echo -e "${YELLOW}Compiling formplatform module...${NC}"
    mvn -pl formplatform compile -q
    if [ $? -ne 0 ]; then
        echo -e "${RED}Compilation failed${NC}"
        exit 1
    fi
    echo -e "${GREEN}Compilation successful${NC}"
fi

# Run the CLI client (classpath from formplatform module)
java -cp "formplatform/target/classes:$(mvn -pl formplatform dependency:build-classpath -q -DincludeScope=compile -Dmdep.outputFile=/dev/stdout)" \
    com.formplatform.infrastructure.adapter.input.cli.FormCliClient "$@"
