# Tateca Backend

Spring Boot 3.2.5 Java 17 application for group expense management with Firebase authentication.

## Commands

**Build & Test:**
- `./gradlew build` - Build project
- `./gradlew test` - Run tests  
- `./gradlew bootRun` - Start application

**Database:**
- `docker compose up` - Start MySQL
- `docker compose down` - Stop MySQL

## Architecture

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

## Testing

**Test Commands:**
- `./gradlew test` - Run all tests
- `./gradlew test --tests "*ServiceUnitTest"` - Run specific test class
- `./gradlew test jacocoTestReport` - Generate coverage report

**Test Structure:**
- **Service Unit Test**: `AbstractServiceUnitTest` + Mockito
- **Controller Unit Test**: `AbstractControllerWebTest` + @WebMvcTest
- **Controller Integration Test**: `AbstractControllerIntegrationTest` + Testcontainers

**Naming Convention:**
- Unit Test: `*UnitTest.java`
- Integration Test: `*IntegrationTest.java`

**Key Rules:**
- Use AssertJ (not JUnit assertions)
- Follow AAA pattern (Arrange-Act-Assert)
- Use `TestFixtures.Currencies.jpy()` for test data
- DTO with `from()`: Create dedicated unit test

**Base Classes:**
- `AbstractServiceUnitTest` - Mockito extension for service unit tests
- `AbstractControllerWebTest` - @WebMvcTest with mocked Firebase auth
- `AbstractControllerIntegrationTest` - Full context with Testcontainers MySQL

**Coverage Target:**
- Service Unit Test: Branch Coverage 95-100%
- Integration Test: Main flows 60%+
- Overall: 70%+

### GitHub Integration Tools
- **GitHub CLI**: Use the [GitHub CLI](https://cli.github.com/manual/) for command-line GitHub operations such as creating pull requests, managing issues, and repository interactions
- **GitHub MCP Server**: Alternatively, use the [GitHub MCP Server](https://github.com/github/github-mcp-server) for enhanced integration capabilities and automated GitHub workflows