# Campus Operations System

Full-stack campus scheduling platform built as a solo capstone project for
COMP 4431/4432 (Advanced Project) at Lakehead University.

The goal is to demonstrate production-style full-stack engineering:
domain modeling, validation, optimization, testing, and deployable structure.

## Current Scope (V1)

V1 focuses on admin and instructor workflows:

- Campus resource management (buildings, rooms, courses, instructors, time slots)
- Schedule creation with conflict detection
- Instructor change request workflow
- Utilization analytics dashboard
- Auto-scheduling with Timefold solver and live progress updates

Student-facing workflows are planned for V2.

## Demo Auth Model (Intentional for V1)

This project uses a demo role-switching model in the UI (admin/instructor)
instead of real signup/login. This keeps demos fast and allows testing of
role-specific UX without account setup overhead.

Real authentication/authorization is planned for a future version.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, TypeScript, Vue Router, Tailwind CSS 4, Vite |
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Spring Validation |
| Optimization | Timefold Solver |
| Realtime | STOMP over SockJS WebSocket |
| Database (dev) | H2 (file-based) |
| Database (runtime option) | PostgreSQL driver included |
| Testing | JUnit 5, Mockito, MockMvc, Playwright |

## Monorepo Structure

```text
campus-scheduler/
  campus-scheduler-backend/    # Spring Boot API + solver
  campus-scheduler-frontend/   # Vue app
  run.sh                       # Starts backend + frontend together
```

## Key Features

### Admin

- CRUD for all core entities
- Manual schedule creation and deletion
- Solver controls (start/stop/save)
- Review and approve/reject instructor change requests
- Utilization analytics (rooms/buildings/peak hours)

### Instructor

- View own schedule
- Submit schedule change requests
- Receive validation feedback and workflow status

### Platform

- Conflict detection (capacity and room/time clashes)
- Impact analysis for requested schedule changes
- Research-based demo data generation presets
- Live solver status via WebSocket

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+ and npm
- Unix-like shell (macOS/Linux) for `run.sh`

### 1) Install Frontend Dependencies

```bash
cd campus-scheduler-frontend
npm ci
cd ..
```

### 2) Run the Full Stack (Recommended)

```bash
./run.sh
```

This starts:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`

### 3) Manual Run (Alternative)

Backend:

```bash
cd campus-scheduler-backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd campus-scheduler-frontend
npm run dev
```

## API and Local Tools

- OpenAPI / Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/campusdb`
  - Username: `sa`
  - Password: *(blank)*

## Testing

Backend test suite:

```bash
cd campus-scheduler-backend
./mvnw test
```

Frontend E2E tests:

```bash
cd campus-scheduler-frontend
npm run test:e2e
```

Note: current Playwright tests rely heavily on API mocking for deterministic UI flow validation.

## Data and Privacy

All data in this project is simulated or generated for academic/demo purposes.
No real student or institutional private data is used.
