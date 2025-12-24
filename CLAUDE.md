# Tateca Backend

Spring Boot 3.5.4 Java 21 application for group expense management with Firebase authentication.

## Commands

**Build & Test:**
- `./gradlew build` - Build project
- `./gradlew test` - Run tests  
- `./gradlew bootRun` - Start application

**Database:**
- `docker-compose up -d` - Start MySQL
- `docker-compose down` - Stop MySQL

## Architecture

**Core Entities:**
- `TransactionHistoryEntity` - Expenses, loans, repayments
- `GroupEntity` - User groups for shared expenses
- `UserEntity` - Firebase-authenticated users
- `ExchangeRateEntity` - Currency rates

**Structure:**
- `/controller` - REST endpoints (`/groups/{groupId}/transactions`)
- `/service` - Business logic (transactions, settlements, groups)
- `/accessor` + `/repository` - Data access with JPA
- `/entity` - Domain models

**Authentication:**
- Firebase JWT tokens via `BearerTokenInterceptor`
- Lambda API keys for system endpoints
- `@UId` annotation extracts user ID

**Integrations:**
- Exchange rates updated daily at 00:01 UTC
- Resilience4j retry for external APIs

## Environment

**Profiles:**
- `dev` (default local)
- `prod` (Railway deployment)

**Required Variables:**
- Database: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
- Auth: `FIREBASE_SERVICE_ACCOUNT_KEY`, `LAMBDA_API_KEY`
- External: `EXCHANGE_RATE_API_KEY`

## CI/CD

### Overview

Modern CI/CD pipeline following **Build Once, Deploy Anywhere** principle with strict separation of concerns:

```
Pull Request → CI (Test & Build) → Merge → CD (Retag & Deploy)
```

**Key Principles:**
- ✅ **Immutable Artifacts**: Docker images built once in CI, reused in CD
- ✅ **Shift-Left Testing**: All validation happens during PR (before merge)
- ✅ **Fast Deployments**: CD only retags images (~30s) instead of rebuilding (3-4min)
- ✅ **Branch Protection**: All code flows through PR → CI → Main → CD

### Prerequisites

**Branch Protection Rules** (Required):
- Navigate to: GitHub Repository → Settings → Branches → Branch protection rules
- Enable for `main` branch:
  - ✅ Require pull request before merging
  - ✅ Require status checks to pass (CI pipeline)
  - ✅ Prevent direct pushes to main
- **Why**: Ensures all code goes through CI validation before reaching production

### Workflows

#### 1. CI Pipeline (`.github/workflows/ci.yml`)

**Trigger:** Pull requests to `main` branch only

**Purpose:** Validate code quality, build artifacts, generate documentation

**Jobs:**

| Job | Duration | Description |
|-----|----------|-------------|
| `java-test` | 2-3 min | JDK 21 build & test execution |
| `docker-build-push` | 3-4 min | Multi-stage Docker build → GHCR push |
| `api-validation` | 5-7 min | OpenAPI generation, validation, documentation |

**Job Details:**

1. **Java Tests** (`java-test`)
   - Builds project with Gradle
   - Runs all unit, integration, and contract tests
   - Required for subsequent jobs

2. **Docker Build & Push** (`docker-build-push`)
   - **Depends on:** `java-test`
   - Builds Docker image with multi-stage build (Gradle + JRE)
   - Pushes to GitHub Container Registry (GHCR)
   - **Tags:** `sha-<commit-hash>` (NOT `latest` - that's CD's job)
   - Uses GitHub Actions cache for faster subsequent builds
   - Comments on PR with image information

3. **API Validation & Generation** (`api-validation`)
   - Builds and starts Spring Boot application with minimal environment
   - Fetches OpenAPI spec from `/v3/api-docs` endpoint (Code-First)
   - Validates spec with Spectral CLI
   - Generates HTML documentation with Redocly
   - Uploads spec artifacts to GitHub Actions
   - Comments on PR with specification statistics

**Total CI Time:** ~10-15 minutes

**Why CI Only Runs on PRs:**
- PRs are the only way code enters `main` (via branch protection)
- Eliminates duplicate builds when merging
- Clear separation: CI = validation, CD = deployment

---

#### 2. CD Pipeline (`.github/workflows/cd.yml`)

**Trigger:** Push to `main` branch (PR merges only, direct pushes disabled)

**Purpose:** Deploy validated artifacts to production environments

**Jobs:** Run in sequence with dependencies

```
docker-retag → [deploy-docs, railway-redeploy]
```

**Job Details:**

1. **Docker Retag** (`docker-retag`)
   - **Duration:** ~30 seconds
   - Pulls pre-built image from CI: `ghcr.io/<repo>:sha-<commit>`
   - Retags as `latest` for production: `ghcr.io/<repo>:latest`
   - Pushes `latest` tag to GHCR
   - **No rebuild** - uses exact image tested in PR
   - **Optimization:** 3-4 min saved vs rebuilding

2. **Deploy API Documentation** (`deploy-docs`)
   - **Duration:** ~2-3 minutes
   - **Depends on:** `docker-retag`
   - **Optimized:** Uses pre-built `latest` image (no Gradle/JDK setup)
   - Starts application container with MySQL service
   - Fetches OpenAPI spec from running container
   - Generates Swagger UI for interactive testing
   - Generates Redoc for static documentation
   - Deploys to GitHub Pages
   - **Optimization:** 3 min saved by using Docker image vs building app

3. **Railway Redeploy** (`railway-redeploy`)
   - **Duration:** ~30 seconds (trigger only, actual deploy by Railway)
   - **Depends on:** `docker-retag`
   - Triggers Railway to pull `latest` image from GHCR
   - Railway deploys pre-built image (no build step on Railway)
   - **Optimization:** 5 min saved vs Railway building from Dockerfile

**Total CD Time:** ~3-4 minutes (vs 8-10 min without optimizations)

**Documentation URLs:**
- Base: https://tateca.github.io/tateca-backend
- Swagger UI: `/swagger.html` - Interactive API testing
- Redoc: `/` - Beautiful API documentation
- Resources: `/downloads.html` - Download OpenAPI specs

---

### Docker Image Management

**Registry:** GitHub Container Registry (GHCR) at `ghcr.io/<owner>/<repo>`

**Build Strategy:**
- Multi-stage Dockerfile (builder: JDK 21 + Gradle, runtime: JRE 21)
- Built **once** in CI, **reused** in CD
- No rebuilds in CD pipeline

**Image Tags:**

| Tag | Created By | Purpose | Example |
|-----|-----------|---------|---------|
| `sha-<commit>` | CI | Immutable reference to specific commit | `sha-a1b2c3d` |
| `latest` | CD | Production deployment tag | `latest` |

**Tag Lifecycle:**
```
1. PR created → CI builds → ghcr.io/repo:sha-a1b2c3d
2. PR merged  → CD retags → ghcr.io/repo:latest (points to sha-a1b2c3d)
3. Railway deploys ghcr.io/repo:latest
```

**Image Specifications:**
- Size: ~200MB (JRE-based, optimized for production)
- Base: eclipse-temurin:21-jre
- Caching: GitHub Actions cache for faster builds
- Storage: All SHA-tagged images retained indefinitely

**Why SHA Tags in CI, Latest in CD:**
- ✅ `sha-<commit>` is immutable (never changes)
- ✅ `latest` always points to current production (`main` branch)
- ✅ Prevents PR images from overwriting production `latest`
- ✅ Enables easy rollback (retag any `sha-xxx` as `latest`)

---

### Railway Deployment

**Configuration:** Image-based deployment (Settings → Build → Image)

**Image URL:** `ghcr.io/tateca/tateca-backend:latest`

**Deployment Flow:**
```
1. CI: Build image → ghcr.io/repo:sha-abc123
2. CD: Retag sha-abc123 → latest
3. CD: Trigger railway redeploy
4. Railway: Pull latest from GHCR → Deploy
```

**Benefits:**
- ⚡ **Fast:** Railway deployment ~30s (no build, just pull)
- 🔒 **Consistent:** Exact same image tested in PR
- 💰 **Cost-effective:** Zero Railway build minutes used
- ✅ **Reliable:** Pre-tested artifacts, immutable images

**No `railway.toml` Required:**
- Railway configured via Web Console (not file-based)
- Image URL set directly in Railway settings
- Dockerfile in repo is NOT used by Railway

---

### API Documentation (Code-First)

**Philosophy:** Source code is the single source of truth for API specification

**Implementation:**
- SpringDoc annotations on controllers define API spec
- OpenAPI spec auto-generated at runtime from `/v3/api-docs` endpoint
- No manual YAML/JSON maintenance required

**Generation Process:**
1. **PR (ci.yml):** Generate spec for validation and review
2. **Main (cd.yml):** Generate spec and deploy documentation to GitHub Pages

**Tools:**
- SpringDoc OpenAPI: Annotation-based spec generation
- Spectral: Linting and validation
- Redocly: Static documentation site generation
- Swagger UI: Interactive API testing interface

**Benefits:**
- ✅ Code and docs always in sync (impossible to be out of date)
- ✅ Breaking changes caught automatically in PRs
- ✅ Zero manual spec maintenance
- ✅ Interactive testing without Postman

**Usage:**
- **Swagger UI:** Test endpoints in browser with "Try it out"
- **Redoc:** Browse beautiful, responsive documentation
- **Downloads:** Get OpenAPI YAML/JSON for Postman, Insomnia, etc.

---

### Additional Features

**API Breaking Change Detection (Disabled):**
- **Status:** Currently disabled, not in active use
- **Tool:** [oasdiff](https://github.com/Tufin/oasdiff) for spec comparison
- **Capability:** Automated detection of breaking API changes in PRs
- **Future:** Can be re-enabled when API versioning becomes critical
- **Examples:** Removed endpoints, new required fields, type changes

**Dependabot:**
- Weekly dependency updates (Monday 09:00 JST)
- Grouped updates for Spring Boot, testing libraries, security patches
- Separate groups for GitHub Actions and Docker base images

## Code Quality

**Static Analysis Tools:**
- **Checkstyle**: Code style enforcement (gradual adoption)
- **SpotBugs**: Bug pattern detection
- **JaCoCo**: Code coverage measurement (90% line, 95% branch)
- **OWASP**: Dependency vulnerability scanning

**Current Status:**
- Checkstyle: Warning-only mode (existing code has ~50 warnings)
- Goal: Fix warnings incrementally, then enforce strict mode
- Priority: UnusedImports > Whitespace > LineLength

**Running Checks:**
```bash
./gradlew checkstyleMain checkstyleTest  # Style check
./gradlew spotbugsMain spotbugsTest      # Bug detection
./gradlew jacocoTestReport               # Coverage report
./gradlew dependencyCheckAnalyze         # Security scan
```

## Testing Guidelines

**Test Pyramid:**
```
┌─────────────────────────────────┐
│ E2E Tests (Manual/Automated)    │ ← QA Team
├─────────────────────────────────┤
│ Contract Tests                  │ ← API specification compliance
├─────────────────────────────────┤
│ Integration Tests               │ ← Component integration (DB, APIs)
├─────────────────────────────────┤
│ Unit Tests                      │ ← Business logic (most numerous)
└─────────────────────────────────┘
```

**Test Structure:**
- **Unit Tests**: Test logic in isolation with mocked dependencies (Mockito)
- **Integration Tests**: Test component integration with real infrastructure (WireMock, Testcontainers)
- **Contract Tests**: Verify API responses match OpenAPI specification (REST Assured + MockMvc)

**Unit Test Principles:**
- Mock external dependencies
- Focus on business logic validation
- Fast execution
- No external infrastructure required

**Integration Test Principles:**
- Use BDD style (Given/When/Then with nested classes)
- Test actual component integration (e.g., Resilience4j AOP, Spring configuration)
- WireMock for external API simulation
- Focus on behavior that cannot be tested in unit tests

**Integration Test Strategy:**
- **API Client Layer**: Test retry logic, fallback behavior, error handling with WireMock scenarios
- **HTTP Client Layer**: Unit tests are sufficient; avoid redundant integration tests for framework features
- **True Integration Tests**: Test full flow (Controller → Service → Client) at higher levels

**BDD Test Structure Example:**
```java
@Nested
@DisplayName("Given external API is temporarily unavailable")
class WhenExternalApiIsTemporarilyUnavailable {

    @Test
    @DisplayName("Then should retry and eventually succeed")
    void thenShouldRetryAndEventuallySucceed() {
        // Given: Setup WireMock scenario
        givenExternalApiFailsTwiceThenSucceeds();

        // When: Execute operation
        var response = apiClient.fetchLatestExchangeRate();

        // Then: Verify behavior
        assertThat(response).isNotNull();
        verifyApiWasCalledThreeTimes();
    }
}
```

**Contract Test Principles:**
- Verify API responses match OpenAPI specification
- Focus on critical public APIs (not all endpoints)
- Test both success and error scenarios
- Use real database (Testcontainers) for accurate responses

**Contract Test Strategy:**
- Write contract tests for:
  - ✅ Public APIs consumed by frontend
  - ✅ APIs with strict versioning requirements
  - ✅ Critical business endpoints (transactions, settlements)
- Skip contract tests for:
  - ❌ Internal/development-only endpoints
  - ❌ Simple CRUD operations covered by integration tests
  - ❌ Frequently changing experimental APIs

**Contract Test Example:**
```java
@DisplayName("Exchange Rate API Contract Tests")
class ExchangeRateContractTest extends AbstractContractTest {

    @Test
    @DisplayName("Should return exchange rates with correct schema")
    void shouldReturnExchangeRatesWithCorrectSchema() {
        given()
                .contentType("application/json")
        .when()
                .get("/exchange-rate/{date}", testDate.toString())
        .then()
                .statusCode(HttpStatus.OK.value())
                .contentType("application/json")
                .body("exchange_rate", notNullValue())
                .body("exchange_rate[0].currency_code", notNullValue())
                .body("exchange_rate[0].exchange_rate", notNullValue());
    }
}
```

**Test Infrastructure:**
- `AbstractIntegrationTest` - Base class with MySQL and WireMock containers (Testcontainers)
- `AbstractControllerIntegrationTest` - Base class with MockMvc for controller tests
- `AbstractContractTest` - Base class with REST Assured for contract tests
- `AbstractServiceUnitTest` - Base class with Mockito support
- `TestFixtures` - Object Mother pattern for test data

**Test Execution:**
```bash
./gradlew test                          # Run all tests
./gradlew test --tests "*UnitTest"      # Unit tests only
./gradlew test --tests "*IntegrationTest"  # Integration tests only
./gradlew test --tests "*ContractTest"  # Contract tests only
```

## Development Workflow

**Branch Naming Convention:**
```
<type>/<issue-number>-<description>
```

**Types:**
- `feature/` - New features
- `fix/` - Bug fixes
- `hotfix/` - Production emergency fixes
- `chore/` - Build, config, dependencies updates, **test code additions**
- `refactor/` - Code improvements (no functionality change)
- `docs/` - Documentation only
- `perf/` - Performance improvements

**Examples:**
- `feature/123-add-user-authentication`
- `fix/456-resolve-payment-validation`
- `chore/update-gitignore`
- `chore/add-api-client-tests`

**Commit Message Guidelines:**
- Write commit messages in English only
- Keep messages to a single line (no multi-line descriptions)
- Do not include AI attribution or co-authorship tags
- Follow conventional commit format: `type: brief description`
- Examples:
  - `fix: Add workflow file to docs deployment trigger paths`
  - `feat: Add user authentication with Firebase`
  - `chore: Update dependencies to latest versions`

### GitHub Integration Tools
- **GitHub CLI**: Use the [GitHub CLI](https://cli.github.com/manual/) for command-line GitHub operations such as creating pull requests, managing issues, and repository interactions
- **GitHub MCP Server**: Alternatively, use the [GitHub MCP Server](https://github.com/github/github-mcp-server) for enhanced integration capabilities and automated GitHub workflows

## API Specification Management

### Current Approach: Code-First

This project uses **Code-First** approach for API documentation:

**Implementation:**
- SpringDoc annotations on controllers define API specification
- OpenAPI spec auto-generated at runtime from `/v3/api-docs` endpoint
- CI/CD automatically generates and deploys documentation

**Documentation:** See [CI/CD → API Documentation (Code-First)](#api-documentation-code-first) section above

**Artifacts:**
- Live docs: https://tateca.github.io/tateca-backend
- Swagger UI: Interactive API testing
- Redoc: Static documentation
- OpenAPI specs: Download YAML/JSON

### Legacy: Spec-First (Deprecated)

**⚠️ Migration Notice:**
The `api-specs/` directory contains legacy Spec-First specifications. This approach is **deprecated** and will be removed once migration to Code-First is complete.

**If you need the old tooling:**
- See `api-specs/README.md` for npm scripts and workflows
- Note: These specs are no longer actively maintained
- Do not use for new development

## Refactoring Best Practices

When performing major refactoring (e.g., HTTP client migration, database layer changes):

### Recommended Workflow

**Step 1: Write tests BEFORE refactoring**
- Capture current behavior as baseline
- Establish expected behavior through tests
- Use appropriate test doubles (mocks, stubs, fakes)

**Step 2: Perform refactoring incrementally**
- Make small, focused changes
- Keep all tests green throughout the process
- Commit frequently

**Step 3: Verify tests still pass**
- Ensures behavior hasn't changed
- Validates refactoring correctness
- Catches regressions early

### Example: HTTP Client Migration

**Ideal approach:**
```
1. Ensure comprehensive tests exist for current implementation
2. Verify all tests pass
3. Perform migration incrementally
4. Verify tests still pass after each change
5. Confirm behavior is unchanged
```

**Lesson learned:**
- Tests should be comprehensive BEFORE major refactoring
- Tests act as safety net during refactoring
- Incremental changes with passing tests reduce risk