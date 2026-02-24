#!/bin/bash

# ANSI color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

BACKEND_PID=""
FRONTEND_PID=""
CLEANUP_DONE=0

BACKEND_DIR="$(find . -maxdepth 1 -mindepth 1 -type d -name '*backend' | head -n 1)"
FRONTEND_DIR="$(find . -maxdepth 1 -mindepth 1 -type d -name '*frontend' | head -n 1)"

prefix_logs() {
    local prefix="$1"
    while IFS= read -r line; do
        printf "%b%s\n" "$prefix" "$line"
    done
}

cleanup() {
    [[ "$CLEANUP_DONE" -eq 1 ]] && return
    CLEANUP_DONE=1

    echo -e "\n${RED}Shutting down services...${NC}"
    for pid_var in BACKEND_PID FRONTEND_PID; do
        local pid="${!pid_var}"
        if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
            echo "Stopping ${pid_var%_PID}..."
            kill -- -"$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
            wait "$pid" 2>/dev/null || true
        fi
    done
}

trap 'cleanup; exit 0' SIGINT SIGTERM
trap cleanup EXIT

# Validate dependencies
if ! command -v mvn &>/dev/null && [[ ! -x "${BACKEND_DIR}/mvnw" ]]; then
    echo -e "${RED}Error: Maven/mvnw not found${NC}"
    exit 1
fi

if ! command -v npm &>/dev/null; then
    echo -e "${RED}Error: npm not found${NC}"
    exit 1
fi

if [[ -z "$BACKEND_DIR" || -z "$FRONTEND_DIR" ]]; then
    echo -e "${RED}Error: backend/frontend directories not found${NC}"
    exit 1
fi

# Kill existing processes on dev ports
echo "Killing any existing services on ports 5173 and 8080..."
if [[ "$(uname -s)" == "Linux" ]]; then
    fuser -k 5173/tcp 2>/dev/null || true
    fuser -k 8080/tcp 2>/dev/null || true
else
    # macOS
    lsof -ti tcp:5173 | xargs kill -9 2>/dev/null || true
    lsof -ti tcp:8080 | xargs kill -9 2>/dev/null || true
fi

echo "Starting services..."

# Start backend
(
    cd "$BACKEND_DIR" || exit 1
    ./mvnw spring-boot:run 2>&1
) | prefix_logs "$(printf "${BLUE}[BACKEND]${NC} ")" &
BACKEND_PID=$!

# Start frontend
(
    cd "$FRONTEND_DIR" || exit 1
    npm run dev 2>&1
) | prefix_logs "$(printf "${GREEN}[FRONTEND]${NC} ")" &
FRONTEND_PID=$!

wait -n 2>/dev/null || wait
cleanup
