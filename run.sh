#!/bin/bash

# ANSI color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Store PIDs for cleanup
BACKEND_PID=""
FRONTEND_PID=""

# Cleanup function to kill background processes
cleanup() {
    echo -e "\n${RED}Shutting down services...${NC}"
    if [ ! -z "$BACKEND_PID" ]; then
        echo "Stopping backend..."
        kill $BACKEND_PID 2>/dev/null
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        echo "Stopping frontend..."
        kill $FRONTEND_PID 2>/dev/null
    fi
    exit 0
}

# Trap SIGINT (Ctrl+C) and EXIT to run cleanup
trap cleanup SIGINT SIGTERM EXIT

# Check if required commands exist
if ! command -v mvn &> /dev/null && ! command -v ./mvnw &> /dev/null; then
    echo -e "${RED}Error: Maven/mvnw not found${NC}"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo -e "${RED}Error: npm not found${NC}"
    exit 1
fi

# Function to run the backend
run_backend() {
    cd campus-scheduler-backend || exit 1
    ./mvnw spring-boot:run 2>&1 | sed "s/^/$(printf "${BLUE}[BACKEND]${NC} ")/"
}

# Function to run the frontend
run_frontend() {
    cd campus-scheduler-frontend || exit 1
    npm run dev 2>&1 | sed "s/^/$(printf "${GREEN}[FRONTEND]${NC} ")/"
}

# Kill any existing processes on ports 5173 and 8080
echo "Killing any existing services on ports 5173 and 8080..."
kill -9 $(lsof -ti:5173) 2>/dev/null || true
kill -9 $(lsof -ti:8080) 2>/dev/null || true

echo "Starting services..."

# Run both services in the background and store PIDs
run_backend &
BACKEND_PID=$!

run_frontend &
FRONTEND_PID=$!

# Wait for both processes to finish
wait
