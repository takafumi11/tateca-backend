# Product Overview

## Product Purpose

Tateca is a group expense management system that simplifies tracking and settling shared expenses among multiple users. It solves the common pain point of managing group finances by providing real-time expense tracking, multi-currency support, and automated settlement calculations. The system eliminates the complexity of manually calculating "who owes whom" in group settings such as trips, shared housing, or team events.

## Target Users

**Primary Users**: Groups of 2-10 people who share expenses and need to track and settle financial obligations.

**User Needs and Pain Points**:
- Need to track expenses across multiple currencies (e.g., international trips)
- Want to avoid complex manual calculations for group settlements
- Require clear visibility into who paid what and who owes whom
- Need secure authentication to protect financial data
- Want to easily join and manage multiple groups simultaneously
- Need historical transaction records for reference

**Use Cases**:
- Travel groups tracking expenses during international trips
- Roommates managing shared household expenses
- Friend groups splitting costs for events or activities
- Teams managing shared project costs

## Key Features

1. **Group Management**: Create and manage expense groups with 2-10 members, join groups via invitation tokens, and track multiple groups simultaneously (up to 10 per user)

2. **Multi-Type Transaction Tracking**: Record two types of transactions:
   - LOAN: One-time loans between members with customizable split ratios (obligations)
   - REPAYMENT: Debt repayments between members

3. **Multi-Currency Support**: Support for multiple currencies with automatic exchange rate updates (daily at 00:01 UTC), currency conversion for settlement calculations, and historical exchange rate tracking

4. **Automated Settlement Calculation**: Real-time calculation of who owes whom, settlement amounts converted to JPY, and optimized payment paths to minimize transactions

5. **Transaction History**: View recent transactions (configurable count, default: 5 items), detailed transaction information including payer, amount, currency, and obligations, and ability to update LOAN transactions after creation

6. **Secure Authentication**: Firebase JWT authentication for user endpoints, API key authentication for internal endpoints (Lambda/EventBridge), and constant-time comparison to prevent timing attacks

7. **RESTful API**: Well-documented OpenAPI specification with code-first approach, comprehensive validation and error handling, and interactive API documentation (Swagger UI + Redoc)

## Business Objectives

- Provide reliable, production-grade group expense management service
- Maintain high availability and data integrity for financial records
- Support scalability to handle multiple concurrent groups and users
- Ensure security and privacy of financial data
- Deliver accurate multi-currency calculations using up-to-date exchange rates
- Minimize operational costs through efficient CI/CD and deployment automation

## Success Metrics

- **Code Quality**:
  - Target: 90% line coverage, 95% branch coverage (JaCoCo goals defined in CLAUDE.md)
  - Current baseline: 50% line/branch coverage
  - Zero critical security vulnerabilities (SpotBugs medium effort detection)
  - Checkstyle compliance with 150 character line length limit
- **Deployment Efficiency**: CI/CD pipeline completion target < 15 minutes from PR to production (typical: ~10-15 minutes based on CI/CD workflow configuration)
- **Cost Efficiency**: Zero Railway build minutes through image-based deployment strategy

## Product Principles

1. **Code-First API Documentation**: Source code is the single source of truth for API specification. OpenAPI spec is auto-generated at runtime from SpringDoc annotations, ensuring documentation and implementation are always in sync.

2. **Build Once, Deploy Anywhere**: Docker image is built once during CI, tagged with semantic version from build.gradle.kts, and reused across all environments. No rebuilds in production deployment pipeline.

3. **Shift-Left Quality**: All validation happens during pull request phase before merge. Tests, static analysis, API validation, and documentation generation must pass before code reaches production.

4. **Auditable Financial Records**: Financial transactions can be created, updated (LOAN transactions), and deleted. All operations are logged to ensure traceability and auditability.

5. **Security by Default**: Firebase authentication for user endpoints, API key authentication for system endpoints, constant-time comparison for sensitive operations, and PII masking in all logs.

6. **Fail-Fast Validation**: Comprehensive input validation at API boundary using Jakarta Bean Validation. Clear, actionable error messages for all validation failures.

7. **Gradual Quality Improvement**: Static analysis tools (Checkstyle, SpotBugs, JaCoCo) run in warning-only mode initially, allowing incremental improvement without blocking development.

## Monitoring & Visibility

- **Dashboard Type**: Web-based documentation and monitoring
  - API Documentation: GitHub Pages (Swagger UI + Redoc)
  - Deployment Status: Railway dashboard
  - CI/CD Pipeline: GitHub Actions workflow visualizations

- **Real-time Updates**:
  - API documentation auto-deployed on main branch merge
  - Exchange rates updated daily at 00:01 UTC
  - Railway deployment triggered automatically after CI completion

- **Key Metrics Displayed**:
  - API endpoint health and response times (via Better Stack monitoring and Railway logs)
  - Application performance metrics and uptime (Better Stack)
  - Test coverage metrics (JaCoCo reports)
  - Static analysis warnings (Checkstyle, SpotBugs)
  - Docker image tags and versions (GitHub Container Registry)
  - CI/CD pipeline status and duration (GitHub Actions)

- **Sharing Capabilities**:
  - Public API documentation at https://docs.tateca.net/
  - Downloadable OpenAPI specs (YAML/JSON) for client generation
  - PR comments with version info, image tags, and API spec statistics
