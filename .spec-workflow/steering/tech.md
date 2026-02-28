# Technology Stack

## Project Type

RESTful API service for group expense management with multi-currency support, Firebase authentication, and automated settlement calculations.

## Core Technologies

### Primary Language(s)
- **Language**: Java 25
- **Build Tool**: Gradle 9.2.0 with Kotlin DSL and Version Catalog
- **Runtime**: Eclipse Temurin JRE 25

### Key Dependencies

**Framework:**
- **Spring Boot 3.5.8**: Web, Security, JPA, Validation, AOP, Actuator
- **Spring Security**: Firebase JWT + API key authentication

**Database:**
- **MySQL 8.0+**: Primary storage (Railway managed)
- **Spring Data JPA + Hibernate**: ORM
- **Flyway**: Schema migrations
- **HikariCP**: Connection pooling

**Authentication:**
- **Firebase Admin SDK 9.4.2**: JWT token verification
- **Custom TatecaAuthenticationFilter**: Path-based auth routing

**API & Documentation:**
- **SpringDoc OpenAPI 2.8.1**: Code-first spec generation
- **Swagger UI**: Interactive documentation
- **Redoc**: Static documentation (CI/CD generated)

**Observability:**
- **Logstash Logback Encoder 7.4**: JSON logging for Better Stack
- **Datasource Proxy 1.10**: SQL query logging
- **Spring Boot Actuator**: Health checks and metrics

**Testing:**
- **JUnit 5 + Mockito + AssertJ**: Unit testing
- **REST Assured 5.5.0**: Contract testing
- **Testcontainers 1.21.3**: Integration testing with MySQL
- **WireMock 3.9.1**: External API mocking

**Other:**
- **Resilience4j 2.2.0**: Retry policies for external APIs
- **Lombok 1.18.42**: Boilerplate reduction

### Application Architecture

Layered monolithic architecture:
```
Controller → Service → Accessor → Repository → Entity
```

**Key Patterns:**
- Dependency Injection (constructor-based)
- Repository Pattern (JPA)
- DTO Pattern (API/domain separation)
- AOP (retry, logging, transactions)

**Authentication Strategy:**
- `/internal/**` endpoints: API key (constant-time comparison)
- All other endpoints: Firebase JWT (RS256)
- `@UId` annotation for user ID extraction

### Data Storage

- **Database**: MySQL 8.0+ with Binary(16) UUIDs
- **Migrations**: Flyway (`src/main/resources/db/migration`)
- **Time Zone**: UTC Instant timestamps
- **Formats**: JSON (API), Binary(16) (UUIDs), ISO 8601 with timezone (dates)

**Key Entities:**
- `TransactionHistoryEntity`: Loans and repayments
- `GroupEntity`: User groups with invitation tokens
- `UserEntity`, `AuthUserEntity`: User management
- `ExchangeRateEntity`: Daily currency rates
- `TransactionObligationEntity`: Loan obligations

### External Integrations

- **Exchange Rate API**: Daily updates at 00:01 UTC
- **Firebase**: JWT authentication
- **Better Stack**: Monitoring and log aggregation
- **Railway**: Production hosting
- **GitHub Container Registry**: Docker images

## Development Environment

### Build & Development Tools

- **Build**: Gradle 9.2.0 with Kotlin DSL
- **Local DB**: Docker Compose with MySQL 8.0
- **Hot Reload**: Spring Boot DevTools
- **Env Management**: spring-dotenv for .env files

**Key Commands:**
```bash
./gradlew build         # Full build with quality checks
./gradlew bootRun       # Start application
./gradlew test          # Run tests
./gradlew qualityGate   # Run all quality checks
docker-compose up -d    # Start MySQL
```

### Code Quality Tools

**Static Analysis:**
- **Checkstyle 10.21.0**: Style enforcement (150 char limit)
- **SpotBugs 4.8.6**: Bug detection
- **Status**: Warning-only mode (gradual improvement)

**Code Coverage:**
- **JaCoCo 0.8.14**: Coverage measurement
- **Current**: 50% baseline
- **Target**: 90% line, 95% branch

**Testing:**
- **Frameworks**: JUnit 5, Mockito, AssertJ, REST Assured, Testcontainers, WireMock
- **Test Classes**: AbstractIntegrationTest, AbstractControllerIntegrationTest, AbstractContractTest

### Version Control

- **VCS**: Git with feature branch workflow
- **Branch Protection**: Require PR + CI checks before merge
- **Branch Naming**: `<type>/<issue-number>-<description>`
- **Commit Style**: English, single line, conventional format

## Deployment & Distribution

**Platform:**
- **Production**: Railway (Docker image-based)
- **Registry**: GitHub Container Registry
- **Base Image**: eclipse-temurin:25-jre (~200MB)

**Build & Deployment Flow:**
```
1. PR → CI builds → Tags: version, sha-<hash>, pr-<number>
2. Merge → CI rebuilds → Tags: version, sha-<hash>, latest
3. CD → Retag + Railway redeploy
```

**Environment Variables:**
- Database: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
- Auth: `FIREBASE_SERVICE_ACCOUNT_KEY`, `FIREBASE_PROJECT_ID`, `LAMBDA_API_KEY`
- External: `EXCHANGE_RATE_API_KEY`

**CI/CD:**
- **CI** (.github/workflows/ci.yml): Tests, Docker build, API validation (~10-15 min)
- **CD** (.github/workflows/cd.yml): Retag, docs deploy, Railway trigger (~2-3 min)
- **Dependency Updates**: Dependabot (weekly Monday 09:00 JST)

## Technical Requirements

### Performance
- **Memory**: 256MB heap (Xms/Xmx)
- **GC**: G1GC with string deduplication
- **JVM**: Tiered compilation, lazy initialization, JMX disabled
- **Target Response Time**: < 500ms for 95th percentile

### Compatibility
- **Java**: 25 (toolchain enforced)
- **Spring Boot**: 3.5.8
- **MySQL**: 8.0+
- **OS**: Linux containers (production), macOS/Windows (development)

### Security
- **Authentication**: Firebase JWT (RS256), API key (constant-time)
- **Encryption**: HTTPS/TLS
- **Data Protection**: PII masking in logs
- **Input Validation**: Jakarta Bean Validation
- **SQL Injection**: Parameterized JPA queries

### Standards
- **OpenAPI 3.0**: API specification
- **ISO 4217**: Currency codes
- **ISO 8601**: Date/time with timezone
- **JWT**: RS256 Firebase tokens

## Technical Decisions

1. **Java 25**: Modern features (virtual threads, pattern matching), future-proof
2. **Code-First API**: Docs always in sync with code, eliminates manual maintenance
3. **Image-Based Deployment**: Zero Railway build costs, faster deploys, pre-tested artifacts
4. **Gradle Version Catalog**: Centralized dependencies, type-safe, Dependabot compatible
5. **Gradual Quality**: Enable checks without blocking, measure before enforce
6. **Testcontainers**: Real MySQL behavior, production parity, catch DB-specific issues
7. **Firebase Auth**: Managed service, free tier, mobile SDK support
8. **Multi-Stage Docker**: Smaller images (~200MB), no build tools in production

## Known Limitations

- **Static Analysis**: Warning-only mode, ~367 Checkstyle warnings
- **Test Coverage**: 50% current vs 90%/95% target
- **Single Region**: Latency for distant users (acceptable for MVP)
- **No Caching**: Repeated DB queries (acceptable load for MVP)
- **Manual Versioning**: Requires manual build.gradle.kts edits
- **Limited Observability**: Basic logging, no distributed tracing
