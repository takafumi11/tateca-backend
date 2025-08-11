# Tateca Backend

Spring Boot 3.2.5 Java 17 application for group expense management with Firebase authentication.

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
- External: `EXCHANGE_RATE_API_KEY


### GitHub Integration Tools
- **GitHub CLI**: Use the [GitHub CLI](https://cli.github.com/manual/) for command-line GitHub operations such as creating pull requests, managing issues, and repository interactions
- **GitHub MCP Server**: Alternatively, use the [GitHub MCP Server](https://github.com/github/github-mcp-server) for enhanced integration capabilities and automated GitHub workflows