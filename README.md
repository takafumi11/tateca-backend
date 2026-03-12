# Tateca Backend

Spring Boot 3.5.8 / Java 25 application for group expense management with Firebase authentication.

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 3.5.8 |
| Database | MySQL 8 |
| Authentication | Firebase Authentication |
| CI/CD | GitHub Actions |
| Container Registry | GitHub Container Registry (GHCR) |
| Deployment | Railway (image-based) |
| Monitoring | Better Stack |
| API Documentation | [Redoc](https://docs.tateca.net) |

## Getting Started

### Prerequisites

- JDK 25
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
| CI (`ci.yml`) | PR to main | Test, OpenAPI lint, preview deploy to GitHub Pages |
| CD (`cd.yml`) | Push to main (= PR merge) | Deploy API docs, Docker build & push to GHCR, Railway redeploy |

## API Documentation

- **Redoc:** https://docs.tateca.net
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
