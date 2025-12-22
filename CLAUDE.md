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

**GitHub Actions:**
- Runs on every pull request to `main`
- Build and test pipeline with JDK 21
- Can be triggered manually via `workflow_dispatch`

**Dependabot:**
- Automatic dependency updates weekly (Monday 09:00 JST)
- Grouped updates for Spring Boot, testing libraries, and security dependencies
- GitHub Actions and Docker updates managed separately

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

**Test Structure:**
- **Unit Tests**: Test logic in isolation with mocked dependencies (Mockito)
- **Integration Tests**: Test component integration with real infrastructure (WireMock, Testcontainers)

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

**Test Infrastructure:**
- `AbstractIntegrationTest` - Base class with MySQL and WireMock containers
- `AbstractServiceUnitTest` - Base class with Mockito support
- `TestFixtures` - Object Mother pattern for test data

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

### Overview
API specifications and Postman collections are managed in the `api-specs/` directory, following a **Spec-First** approach where OpenAPI specifications serve as the single source of truth.

### Directory Structure
```
api-specs/
├── openapi/              # OpenAPI 3.0 specification (modular)
├── postman/              # Postman collections and environments
├── docs/                 # Generated documentation
├── dist/                 # Bundled specifications
└── scripts/              # Automation scripts
```

### Quick Start

**Setup:**
```bash
cd api-specs
npm install
```

**Development Workflow:**
```bash
npm run lint              # Validate OpenAPI spec
npm run bundle            # Bundle modular spec
npm run build-docs        # Generate HTML documentation
npm run generate:postman  # Generate Postman collection from OpenAPI
npm run dev              # Full workflow: bundle + docs + serve
```

**View Documentation:**
```bash
npm run preview          # Interactive preview
npm run serve           # Serve generated docs at http://localhost:8080
```

### API Specification Workflow

**1. Editing OpenAPI Specifications:**
- Edit modular files in `api-specs/openapi/paths/` or `api-specs/openapi/components/`
- Never edit bundled files in `api-specs/dist/` (auto-generated)
- Validate changes with `npm run lint`

**2. Generating Documentation:**
- Run `npm run build-docs` to generate HTML documentation
- Documentation is served via `npm run serve` at port 8080
- CI/CD automatically deploys to GitHub Pages on merge

**3. Postman Collections:**
- Manual collection: `postman/collections/Tateca Backend.postman_collection.json`
- Auto-generated: `postman/collections/tateca-api-generated.postman_collection.json` (from OpenAPI)
- Environments: `postman/environments/*.postman_environment.json`
- Run `npm run generate:postman` to sync with OpenAPI spec

**4. CI/CD Integration:**
- OpenAPI validation runs on every PR
- Documentation auto-deploys on merge to main
- Breaking changes are detected automatically

### NPM Scripts Reference

**Validation:**
- `npm run lint` - Validate OpenAPI specification
- `npm run validate` - JSON format validation (CI/CD)
- `npm run stats` - Show specification statistics

**Build:**
- `npm run bundle` - Bundle modular spec to single file
- `npm run build-docs` - Generate HTML documentation
- `npm run build:all` - Clean + bundle + docs

**Development:**
- `npm run preview` - Interactive documentation preview
- `npm run dev` - Full dev workflow (bundle + docs + serve)
- `npm test` - CI/CD test (lint + bundle)

**Postman:**
- `npm run generate:postman` - Generate collection from OpenAPI
- `npm run postman:validate` - Validate collections with Newman

### Best Practices

1. **Spec-First Development:**
   - Update OpenAPI spec before implementing endpoints
   - Use spec as contract between frontend and backend
   - Generate Postman collections from spec for consistency

2. **Version Control:**
   - Commit OpenAPI source files only
   - Exclude generated files (dist/, docs/, node_modules/)
   - Auto-generated Postman collections are gitignored

3. **Documentation:**
   - Keep OpenAPI descriptions comprehensive
   - Use examples for all schemas and responses
   - Run `npm run preview` to verify documentation appearance

4. **Testing:**
   - Use Postman environments for different stages (local/production)
   - Validate API behavior with Newman: `npm run postman:validate`
   - Ensure OpenAPI spec matches actual API implementation

### Integration with Backend

The API specifications are tightly integrated with the Spring Boot backend:
- Specifications live alongside code in same repository
- CI/CD validates both code and specs together
- Breaking API changes are caught in PR validation
- Documentation deploys automatically with releases

For detailed API specification workflows, see `api-specs/README.md`

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