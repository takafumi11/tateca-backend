# Tateca Backend

Spring Boot 3.5.8 / Java 25 — グループ経費管理 REST API。詳細は `docs/STEERING.md` を参照。

## Commands

```bash
./gradlew build                           # Build
./gradlew test                            # Test (all)
./gradlew test --tests "*UnitTest"        # Unit tests only
./gradlew test --tests "*IntegrationTest" # Integration tests only
./gradlew test --tests "*ScenarioTest"    # Scenario tests only
./gradlew bootRun                         # Start application
./gradlew qualityGate                     # All quality checks (test + checkstyle + spotbugs + jacoco)
docker-compose up -d                      # Start MySQL
docker-compose down                       # Stop MySQL
```

## Development Methodology

This project follows **Specification-Driven Development (SDD)**.

- Process guide: `docs/sdd-process.md`
- Testing strategy: `docs/testing.md`
- SDD skills: `.cursor/skills/sdd-*`
- New features MUST follow the SDD process: Requirements + HLD → OpenAPI → Scenario Test (RED) → TDD Implementation (GREEN)
- Existing APIs without documentation require partial or full Reverse SDD before modification (see `sdd-reverse` skill)

## Architecture

**Structure:**
- `/controller` — REST endpoints
- `/service` — Business logic
- `/repository` — Data access with JPA
- `/entity` — Domain models

**Authentication:**
- Firebase JWT via `TatecaAuthenticationFilter` (user endpoints)
- X-API-Key header for `/internal/**` endpoints (Lambda/EventBridge)
- `@UId` annotation extracts user ID or system ID

**Key patterns:**
- OpenAPI specs in `openapi/` are the single source of truth for HTTP contracts
- Resilience4j retry for external API calls
- Dependencies managed via Gradle Version Catalog (`gradle/libs.versions.toml`)
- Static analysis: Checkstyle + SpotBugs (warning-only mode)

## Environment

**Profiles:** `dev` (default local), `prod` (Railway)

**Required Variables:**
- Database: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
- Auth: `FIREBASE_SERVICE_ACCOUNT_KEY`, `FIREBASE_PROJECT_ID`, `LAMBDA_API_KEY`
- External: `EXCHANGE_RATE_API_KEY`

Dev profile provides default values for Firebase and Lambda API key for local testing.

## Development Workflow

**Branch naming:** `<type>/<issue-number>-<description>`
- Types: `feature/`, `fix/`, `hotfix/`, `chore/`, `refactor/`, `docs/`, `perf/`

**Commit messages:**
- English only, single line, no AI attribution tags
- Conventional commit format: `type: brief description`

**GitHub tools:** GitHub CLI or GitHub MCP Server for PR/issue operations.

## Testing Conventions

- **Scenario Tests** — Verify requirements.md ACs via HTTP endpoints (black-box)
- **Controller Web Tests** — Verify OpenAPI contract (`@WebMvcTest`)
- **Service Unit Tests** — Domain logic with mocked dependencies (`@ExtendWith(MockitoExtension.class)`)
- **Integration Tests** — Infrastructure behavior with real DB/WireMock (`AbstractIntegrationTest`)

**Test infrastructure:**
- `AbstractIntegrationTest` — Base class with Testcontainers MySQL + WireMock, `@ResourceLock("DATABASE")`
- `TestFixtures` — Object Mother pattern for test data
- `cleanup.sql` — TRUNCATE script for scenario test cleanup via `@Sql`
- Parallel execution: classes concurrent, methods sequential; DB tests serialized via `@ResourceLock`

## Refactoring

Write tests BEFORE refactoring → refactor incrementally → keep all tests green throughout.

## Reference Docs

Read relevant docs before starting a task:
- `docs/STEERING.md` — Project overview, domain model, tech stack, API endpoints, CI/CD details
- `docs/sdd-process.md` — SDD process steps and artifact structure
- `docs/testing.md` — Test type definitions, responsibility boundaries, AC routing
- `docs/specs/{feature}/` — Feature-specific requirements and design
