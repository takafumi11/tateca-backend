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

### GitHub Integration Tools
- **GitHub CLI**: Use the [GitHub CLI](https://cli.github.com/manual/) for command-line GitHub operations such as creating pull requests, managing issues, and repository interactions
- **GitHub MCP Server**: Alternatively, use the [GitHub MCP Server](https://github.com/github/github-mcp-server) for enhanced integration capabilities and automated GitHub workflows