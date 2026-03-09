# Tateca Backend

Spring Boot 3.5.4 Java 21 application for group expense management with Firebase authentication.

## Development Methodology

This project follows **Specification-Driven Development (SDD)**.

- Process guide: `docs/sdd-process.md`
- Testing strategy: `docs/testing.md`
- SDD skills: `.cursor/skills/sdd-*`
- New features MUST follow the SDD process: Requirements + HLD → OpenAPI → Scenario Test (RED) → LLD (optional) → TDD Implementation (GREEN)
- Existing APIs without documentation require partial or full Reverse SDD before modification (see `sdd-reverse` skill)

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
- Firebase JWT tokens via `TatecaAuthenticationFilter` (user endpoints)
- X-API-Key header for `/internal/**` endpoints (Lambda/EventBridge)
- Path-based routing: `/internal/**` = API key, others = Firebase JWT
- Constant-time API key comparison to prevent timing attacks
- `@UId` annotation extracts user ID or system ID

**Integrations:**
- Exchange rates updated daily at 00:01 UTC
- Resilience4j retry for external APIs

## Environment

**Profiles:**
- `dev` (default local)
- `prod` (Railway deployment)

**Required Variables:**
- Database: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
- Auth: `FIREBASE_SERVICE_ACCOUNT_KEY`, `FIREBASE_PROJECT_ID`, `LAMBDA_API_KEY`
- External: `EXCHANGE_RATE_API_KEY`

**Note:** Dev profile provides default values for Firebase and Lambda API key for local testing.

## CI/CD

### Overview

2つのワークフローでシンプルに構成:

```
PR → CI (Test + OpenAPI Preview)  ※テスト必須でマージブロック
Main merge → CD (OpenAPI Deploy + Docker Build latest + Railway Redeploy)
```

### Prerequisites

**Branch Protection Rules** (Required):
- GitHub Repository → Settings → Branches → Branch protection rules
- `main` ブランチに対して:
  - Require pull request before merging
  - Require status checks to pass: `Test` ジョブを必須に設定
  - Prevent direct pushes to main

**Required Secrets:**
- `RAILWAY_TOKEN` - Railway API token
- `RAILWAY_SERVICE_ID` - Railway service ID

### Workflows

#### 1. CI Pipeline (`.github/workflows/ci.yml`)

**Trigger:** Pull requests to `main` (opened, synchronize, reopened, closed)

**Jobs (PR open/sync/reopen):**

| Job | Description | 依存関係 |
|-----|-------------|---------|
| `test` | JDK 25 テスト実行 + JaCoCo カバレッジ | なし (マージブロッカー) |
| `openapi-lint` | Redocly による OpenAPI スペック検証 | なし |
| `openapi-preview` | ブランチ名パスで GitHub Pages にプレビューデプロイ | `openapi-lint` |

**Jobs (PR close):**

| Job | Description |
|-----|-------------|
| `cleanup-preview` | gh-pages からプレビューフォルダを削除 |

**OpenAPI Preview URL:** `https://<owner>.github.io/<repo>/<branch-name>/`
- ブランチ名の `/` は `-` に変換 (例: `feature/foo` → `feature-foo`)
- PR にコメントでプレビュー URL を自動投稿

---

#### 2. CD Pipeline (`.github/workflows/cd.yml`)

**Trigger:** Push to `main` (= PR マージ) + workflow_dispatch

**Jobs:**

| Job | Description | 依存関係 |
|-----|-------------|---------|
| `deploy-docs` | OpenAPI ドキュメントを GitHub Pages ルートにデプロイ | なし |
| `docker-build-push` | Docker Build → GHCR に `latest` + `sha-<hash>` タグでプッシュ | なし |
| `railway-redeploy` | Railway に `latest` イメージの再デプロイをトリガー | `docker-build-push` |

**Documentation URL:** `https://<owner>.github.io/<repo>/`

---

### Docker Image Management

**Registry:** GitHub Container Registry (GHCR) at `ghcr.io/<owner>/<repo>`

**Tags:**

| Tag | When | Purpose |
|-----|------|---------|
| `latest` | main merge | Railway がプルするプロダクションポインタ |
| `sha-<commit>` | main merge | 不変のコミット参照 (監査用) |

**Deployment Flow:**
```
1. PR merge to main
2. CD: Docker Build → ghcr.io/repo:latest, sha-<hash>
3. CD: railway redeploy → Railway pulls latest → Deploy
```

---

### Railway Deployment

**Configuration:** Image-based deployment (Settings → Build → Image)

**Image URL:** `ghcr.io/tateca/tateca-backend:latest`

- Railway は `latest` タグのイメージを自動プル
- `railway redeploy` コマンドでトリガー
- Dockerfile は Railway では使用しない (CI でビルド済み)

---

### API Documentation

**Single Source of Truth:** `openapi/` ディレクトリの OpenAPI スペックファイル

**Tools:** Redocly (Lint + Build + Bundle)

**Deploy タイミング:**
- **PR:** ブランチ名パスでプレビューデプロイ (CI)
- **Main merge:** ルートパスで本番デプロイ (CD)

**Documentation URL:** https://tateca.github.io/tateca-backend

---

### Additional Features

**Dependabot:**
- Weekly dependency updates (Monday 09:00 JST)
- Grouped updates for Spring Boot, testing libraries, security patches
- Separate groups for GitHub Actions and Docker base images

## Code Quality

### Gradle Version Catalog

Dependencies are managed using Gradle Version Catalog (`gradle/libs.versions.toml`):
- **Benefits**: Centralized version management, type-safe accessors, easier updates
- **Usage**: `libs.firebase.admin`, `libs.bundles.spring.boot.starters`
- **Automatic**: Gradle 9.2+ auto-detects `gradle/libs.versions.toml`

### Static Analysis Tools

**Checkstyle** (Code Style Enforcement):
- Configuration: `config/checkstyle/checkstyle.xml`
- Version: 10.21.0
- Focus: UnusedImports, Whitespace, LineLength (150 chars)
- Status: Warning-only mode (gradual adoption)
- Current: ~367 warnings detected

**SpotBugs** (Bug Pattern Detection):
- Configuration: `config/spotbugs/spotbugs-exclude.xml`
- Version: 4.8.6
- Effort: Maximum, Report Level: Medium
- Status: Warning-only mode (gradual adoption)
- Excludes: Lombok/Spring false positives

**JaCoCo** (Code Coverage):
- Version: 0.8.12
- Reports: Always generated (XML, HTML, CSV)
- Verification: Disabled by default (enable with `JACOCO_VERIFICATION_ENABLED=true`)
- Current Thresholds: 50% line/branch coverage (baseline)
- Target: 90% line coverage, 95% branch coverage

### Running Quality Checks

```bash
# Run all quality checks (tests + checkstyle + spotbugs + jacoco)
./gradlew qualityGate

# Run standard checks (includes static analysis)
./gradlew check

# Individual checks
./gradlew checkstyleMain checkstyleTest  # Code style
./gradlew spotbugsMain spotbugsTest      # Bug detection
./gradlew jacocoTestReport               # Coverage report

# Enable JaCoCo verification locally
JACOCO_VERIFICATION_ENABLED=true ./gradlew test
```

### Dependency Versions (Updated)

- **Firebase Admin**: 9.4.2 (was 9.2.0)
- **WireMock**: 3.9.1 (was 2.35.0) - Maven artifact changed, Java packages unchanged
- **Testcontainers**: 1.20.4 (was 1.19.3)
- **REST Assured**: 5.5.0 (was 5.4.0)

### Gradual Improvement Strategy

**Phase 1 (Current)**: Warning-only mode, collect metrics
- All checks run but don't fail the build
- Identify high-priority issues

**Phase 2 (Future)**: Fix critical issues
- Remove unused imports
- Fix security warnings
- Target: 50% warning reduction

**Phase 3 (Future)**: Fix style issues
- Clean up whitespace
- Fix line length violations
- Target: 80% warning reduction

**Phase 4 (Future)**: Enable strict mode
- Set `ignoreFailures = false`
- Enable JaCoCo verification in CI
- Enforce quality standards for new code

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
- `AbstractIntegrationTest` - Base class with MySQL and WireMock containers (Testcontainers), `@ResourceLock("DATABASE")` for parallel safety
- `TestFixtures` - Object Mother pattern for test data
- `cleanup.sql` - TRUNCATE script for scenario test cleanup via `@Sql`

**Test Parallel Execution:**
- JUnit 5 parallel execution enabled: classes run concurrently, methods within a class run sequentially
- Unit tests + WebMvc tests: fully parallel (no shared state)
- DB-dependent tests (Integration + Scenario): serialized via `@ResourceLock("DATABASE")` on `AbstractIntegrationTest`
- Integration tests: `@Transactional` rollback for data isolation
- Scenario tests: `@Sql("/cleanup.sql")` TRUNCATE for data cleanup before each test method

**Test Execution:**
```bash
./gradlew test                          # Run all tests
./gradlew test --tests "*UnitTest"      # Unit tests only
./gradlew test --tests "*IntegrationTest"  # Integration tests only
./gradlew test --tests "*ScenarioTest"  # Scenario tests only
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

OpenAPI specs in `openapi/` directory are the single source of truth for HTTP interface contracts.
See the [Development Methodology](#development-methodology) section for the full SDD process.

**Documentation:**
- Live docs: https://tateca.github.io/tateca-backend
- Swagger UI: Interactive API testing
- Redoc: Static documentation
- OpenAPI specs: Download YAML/JSON

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