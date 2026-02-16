#!/bin/bash

# ANSI color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Store PIDs for cleanup
BACKEND_PID=""
FRONTEND_PID=""
CLEANUP_DONE=0

BACKEND_PREFIX="$(printf "${BLUE}[BACKEND]${NC} ")"
FRONTEND_PREFIX="$(printf "${GREEN}[FRONTEND]${NC} ")"
BACKEND_DIR="$(find . -maxdepth 1 -mindepth 1 -type d -name '*backend' | head -n 1)"
FRONTEND_DIR="$(find . -maxdepth 1 -mindepth 1 -type d -name '*frontend' | head -n 1)"

prefix_logs() {
    local prefix="$1"
    awk -v prefix="$prefix" '{ print prefix $0; fflush(); }'
}

# Cleanup function to kill background processes
cleanup() {
    if [ "$CLEANUP_DONE" -eq 1 ]; then
        return
    fi
    CLEANUP_DONE=1

    echo -e "\n${RED}Shutting down services...${NC}"
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo "Stopping backend..."
        kill "$BACKEND_PID" 2>/dev/null || true
        wait "$BACKEND_PID" 2>/dev/null || true
    fi
    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo "Stopping frontend..."
        kill "$FRONTEND_PID" 2>/dev/null || true
        wait "$FRONTEND_PID" 2>/dev/null || true
    fi
}

handle_shutdown() {
    cleanup
    exit 0
}

# Trap SIGINT/SIGTERM for shutdown and EXIT for one-time cleanup
trap handle_shutdown SIGINT SIGTERM
trap cleanup EXIT

# Check if required commands exist
if ! command -v mvn &> /dev/null && ! command -v ./mvnw &> /dev/null; then
    echo -e "${RED}Error: Maven/mvnw not found${NC}"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo -e "${RED}Error: npm not found${NC}"
    exit 1
fi

if [ -z "$BACKEND_DIR" ] || [ -z "$FRONTEND_DIR" ]; then
    echo -e "${RED}Error: backend/frontend directories not found${NC}"
    exit 1
fi

# Function to run the backend
run_backend() {
    trap '' INT
    cd "$BACKEND_DIR" || exit 1
    exec ./mvnw spring-boot:run > >(prefix_logs "${BACKEND_PREFIX}") 2>&1
}

# Function to run the frontend
run_frontend() {
    trap '' INT
    cd "$FRONTEND_DIR" || exit 1
    exec npm run dev > >(prefix_logs "${FRONTEND_PREFIX}") 2>&1
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
