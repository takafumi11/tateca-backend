# Tateca Backend

[![CI](https://github.com/YOUR_USERNAME/tateca-backend/workflows/CI/badge.svg)](https://github.com/YOUR_USERNAME/tateca-backend/actions)

Spring Boot 3.5.4 Java 21 application for group expense management with Firebase authentication.

## Tech Stack

- **Framework**: Spring Boot 3.5.4, Java 21
- **Database**: MySQL 8.0
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, Mockito, Testcontainers
- **CI/CD**: GitHub Actions, Railway
- **Quality**: JaCoCo (95% branch, 90% line), Checkstyle, SpotBugs, OWASP

## Quick Start

```bash
# 1. Start MySQL
docker compose up -d

# 2. Create .env file with required environment variables
# (MYSQLHOST, MYSQLUSER, MYSQLPASSWORD, FIREBASE_SERVICE_ACCOUNT_KEY, etc.)

# 3. Run application
./gradlew bootRun
```

Application starts at `http://localhost:8080`

## Development

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Code quality checks
./gradlew checkstyleMain spotbugsMain

# Coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Testing Strategy

- **Unit Tests** (`*UnitTest.java`): Service logic with Mockito
- **Controller Tests** (`*ControllerUnitTest.java`): @WebMvcTest
- **Integration Tests** (`*IntegrationTest.java`): Testcontainers
- **Coverage**: 95% branch, 90% line minimum

## CI/CD

GitHub Actions runs on every PR to `main`:
- Build & Test
- Code Quality (Checkstyle, SpotBugs)
- Security Scanning (OWASP, Trivy)
- Automated dependency updates (Dependabot)

## Architecture

```
controller/   → REST endpoints
service/      → Business logic
accessor/     → Data access
repository/   → JPA repositories
entity/       → Domain models
dto/          → Request/Response
```

## Deployment

Automatically deployed to Railway on merge to `main`
