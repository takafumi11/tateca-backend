# Tateca Backend

Spring Boot 3.5.4 / Java 21 application for group expense management with Firebase authentication.

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Database | MySQL 8 |
| Authentication | Firebase Authentication |
| CI/CD | GitHub Actions |
| Container Registry | GitHub Container Registry (GHCR) |
| Deployment | Railway (image-based) |
| Monitoring | Better Stack |
| API Documentation | GitHub Pages ([Swagger UI](https://tateca.github.io/tateca-backend/swagger.html) / [Redoc](https://tateca.github.io/tateca-backend/)) |

## Getting Started

### Prerequisites

- JDK 21
- Docker (for local MySQL)

### Setup

```bash
# Start MySQL
docker-compose up -d

# Run application
./gradlew bootRun

# Run tests
./gradlew test
```

### Environment Variables

See `CLAUDE.md` for the full list of required environment variables. Dev profile provides default values for local development.

## Development Methodology

This project follows **Specification-Driven Development (SDD)**.

```
Step 1: Requirements + HLD
Step 2: OpenAPI
Step 3: Scenario Test (RED)
Step 4: LLD (optional)
Step 5: TDD Implementation (GREEN)
```

- Process guide: [docs/sdd-process.md](docs/sdd-process.md)
- Testing strategy: [docs/testing.md](docs/testing.md)

## CI/CD

| Pipeline | Trigger | Purpose |
|----------|---------|---------|
| CI (`ci.yml`) | PR to main, push to main | Test, build Docker image, validate OpenAPI |
| CD (`cd.yml`) | After CI on main | Retag image, deploy docs to GitHub Pages, trigger Railway redeploy |

Docker images are built once in CI and reused in CD (Build Once, Deploy Anywhere).

## API Documentation

- **Swagger UI:** https://tateca.github.io/tateca-backend/swagger.html
- **Redoc:** https://tateca.github.io/tateca-backend/
- **OpenAPI specs:** `openapi/` directory (Doc-First, single source of truth)

## Project Structure

```
docs/
├── sdd-process.md      ← SDD process guide
├── testing.md           ← Testing strategy
└── specs/{feature}/     ← Feature specifications

openapi/                 ← OpenAPI specs (HTTP contract source of truth)

src/main/java/.../
├── controller/          ← REST endpoints
├── service/             ← Business logic
├── accessor/            ← Data access layer
├── repository/          ← JPA repositories
└── entity/              ← Domain models

src/test/java/.../
├── scenario/            ← Scenario Tests (Acceptance)
├── controller/          ← Controller Web Tests
└── service/             ← Unit Tests + Integration Tests
```
