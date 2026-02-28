# Project Structure

## Directory Organization

```
tateca-backend/
├── src/
│   ├── main/
│   │   ├── java/com/tateca/tatecabackend/
│   │   │   ├── annotation/          # Custom annotations (@UId)
│   │   │   ├── api/                 # External API clients
│   │   │   │   ├── client/          # API client implementations
│   │   │   │   └── response/        # API response models
│   │   │   ├── config/              # Spring configuration classes
│   │   │   ├── constants/           # Application constants
│   │   │   ├── controller/          # REST API endpoints
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/         # Request DTOs
│   │   │   │   └── response/        # Response DTOs
│   │   │   ├── entity/              # JPA entities (domain models)
│   │   │   ├── exception/           # Custom exceptions
│   │   │   │   └── domain/          # Domain-specific exceptions
│   │   │   ├── interceptor/         # HTTP interceptors
│   │   │   ├── logging/             # Logging components
│   │   │   ├── model/               # Domain models (enums, value objects)
│   │   │   ├── repository/          # JPA repositories
│   │   │   ├── security/            # Security filters and configurations
│   │   │   ├── service/             # Business logic
│   │   │   │   └── impl/            # Service implementations
│   │   │   ├── util/                # Utility classes
│   │   │   └── validation/          # Custom validators
│   │   └── resources/
│   │       ├── db/migration/        # Flyway SQL migrations
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/com/tateca/tatecabackend/
│       │   ├── actuator/            # Actuator endpoint tests
│       │   ├── annotation/          # Annotation tests
│       │   ├── api/client/          # API client tests
│       │   ├── config/              # Configuration tests
│       │   ├── controller/          # Controller integration tests
│       │   ├── fixtures/            # Test data fixtures
│       │   ├── repository/          # Repository tests
│       │   ├── security/            # Security filter tests
│       │   ├── service/             # Service unit/integration tests
│       │   └── util/                # Utility tests
│       └── resources/
│           └── logback-test.xml
├── config/                          # Quality tool configurations
│   ├── checkstyle/
│   └── spotbugs/
├── .github/workflows/               # CI/CD pipelines
│   ├── ci.yml
│   └── cd.yml
├── gradle/                          # Gradle wrapper and version catalog
│   └── libs.versions.toml
├── build.gradle.kts                 # Gradle build configuration
├── docker-compose.yml               # Local MySQL setup
├── Dockerfile                       # Multi-stage Docker build
└── CLAUDE.md                        # Project documentation
```

## Naming Conventions

### Files
- **Controllers**: `{Entity}Controller.java` (e.g., `TransactionController.java`)
- **Services**: `{Entity}Service.java` interface, `{Entity}ServiceImpl.java` implementation
- **Repositories**: `{Entity}Repository.java` (e.g., `GroupRepository.java`)
- **Entities**: `{Entity}Entity.java` (e.g., `TransactionHistoryEntity.java`)
- **DTOs**: `{Operation}{Entity}{Type}DTO.java` (e.g., `CreateTransactionRequestDTO.java`)
- **Tests**: `{ClassName}{TestType}.java` (e.g., `TransactionServiceUnitTest.java`, `TransactionControllerIntegrationTest.java`)

### Code
- **Classes/Interfaces**: PascalCase (e.g., `TransactionService`, `UserRepository`)
- **Methods/Functions**: camelCase (e.g., `getTransactionHistory`, `createGroup`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_GROUP_SIZE`, `DEFAULT_TRANSACTION_COUNT`)
- **Variables**: camelCase (e.g., `groupId`, `exchangeRate`)
- **Packages**: lowercase (e.g., `com.tateca.tatecabackend.service`)

## Import Patterns

### Import Order
1. Java standard library imports (`java.*`, `javax.*`)
2. Third-party library imports (Spring, Lombok, etc.)
3. Internal project imports (`com.tateca.tatecabackend.*`)

### Package Organization
- **Absolute imports**: All imports use fully qualified package names
- **No wildcard imports**: Explicit imports for each class
- **Layer isolation**: Controllers don't import repositories directly (use services)

## Code Structure Patterns

### Class Organization
1. **Annotations**: Class-level annotations (@RestController, @Service, etc.)
2. **Static fields**: Logger, constants
3. **Instance fields**: Dependencies (injected via constructor)
4. **Constructor**: Constructor-based dependency injection
5. **Public methods**: API methods
6. **Private methods**: Helper methods
7. **Static nested classes**: Inner DTOs or builders

### Controller Structure
```java
@RestController
@RequestMapping("/endpoint")
public class EntityController {
    private static final Logger logger = ...;
    private final EntityService service;

    // Constructor injection
    public EntityController(EntityService service) { ... }

    // Public API methods with OpenAPI annotations
    @GetMapping
    @Operation(summary = "...")
    public ResponseEntity<DTO> getEntity() { ... }
}
```

### Service Structure
```java
public interface EntityService {
    ResponseDTO operation(RequestDTO request);
}

@Service
public class EntityServiceImpl implements EntityService {
    private final EntityRepository repository;

    // Constructor injection
    public EntityServiceImpl(EntityRepository repository) { ... }

    @Override
    @Transactional
    public ResponseDTO operation(RequestDTO request) {
        // Input validation
        // Business logic
        // Data persistence
        // Response mapping
    }
}
```

### Entity Structure
```java
@Entity
@Table(name = "table_name")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityName {
    @Id
    private UUID id;

    // Fields with JPA annotations

    @PrePersist
    protected void onCreate() { ... }

    @PreUpdate
    protected void onUpdate() { ... }

    // Static factory methods
    public static EntityName from(...) { ... }
}
```

## Code Organization Principles

1. **Layered Architecture**: Clear separation between Controller, Service, Repository, Entity
2. **Dependency Direction**: Controllers → Services → Repositories → Entities
3. **DTO Pattern**: DTOs for API contracts, Entities for domain models (no direct exposure)
4. **Interface Segregation**: Services define interfaces, implementations are separate
5. **Single Responsibility**: Each class has one clear purpose
6. **Immutability**: Use `@Builder` for entity creation, prefer immutable DTOs (Java records)

## Module Boundaries

### Layer Responsibilities
- **Controller**: HTTP request/response handling, validation, OpenAPI documentation
- **Service**: Business logic, transaction management, orchestration
- **Repository**: Data access, JPA queries
- **Entity**: Domain model, JPA mappings, lifecycle hooks

### Boundary Rules
- ❌ Controllers cannot access Repositories directly
- ❌ Repositories cannot access Services
- ❌ Entities should not contain business logic
- ✅ Services orchestrate between layers
- ✅ DTOs transform between layers
- ✅ Exceptions bubble up from Repository → Service → Controller

### Authentication Boundaries
- **User Endpoints**: Firebase JWT authentication (all endpoints except `/internal/**`)
- **Internal Endpoints**: API key authentication (`/internal/**`)
- **Public Endpoints**: None (all endpoints require authentication)

## Code Size Guidelines

- **File Size**: Prefer < 500 lines per file
- **Method Size**: Prefer < 50 lines per method
- **Line Length**: 150 characters maximum (Checkstyle enforced)
- **Nesting Depth**: Prefer < 3 levels of nesting
- **Class Complexity**: Monitor with SpotBugs, refactor when flagged

## Test Structure

### Test Types
- **Unit Tests**: `*UnitTest.java` - Test single class with mocked dependencies
- **Integration Tests**: `*IntegrationTest.java` - Test component integration (DB, APIs)
- **Contract Tests**: `*ContractTest.java` - Verify API responses match OpenAPI spec

### Test Organization
- **Base Classes**:
  - `AbstractIntegrationTest` - MySQL + WireMock containers
  - `AbstractControllerIntegrationTest` - MockMvc setup
  - `AbstractContractTest` - REST Assured + OpenAPI validation
  - `AbstractServiceUnitTest` - Mockito support
- **Fixtures**: `TestFixtures` class for test data creation
- **Parallel Execution**: Class-level parallelism enabled

### Test Naming
```java
@DisplayName("Given {precondition}")
class When{Action} {
    @Test
    @DisplayName("Then {expected outcome}")
    void then{ExpectedBehavior}() { ... }
}
```

## Configuration Management

### Environment Profiles
- **dev**: Local development (default)
- **prod**: Production (Railway)
- **test**: Test execution

### Configuration Files
- `application.properties`: Common configuration
- `application-dev.properties`: Development overrides
- `application-prod.properties`: Production overrides
- `application-observability.properties`: Monitoring configuration (optional import)

### Externalized Configuration
- All secrets via environment variables
- No hardcoded credentials
- Railway secrets for production
- `.env` file for local development (gitignored)

## Database Migrations

### Migration Naming
```
V{version}__{description}.sql
```
Examples:
- `V1__create_users_table.sql`
- `V2__add_group_join_token.sql`

### Migration Rules
- **Never modify existing migrations** - Flyway validates checksums
- **Always test migrations locally** before committing
- **Use transactions** for data migrations
- **Provide rollback scripts** for complex migrations

## Documentation Standards

- **Public APIs**: OpenAPI annotations (@Operation, @ApiResponse)
- **Complex Logic**: Inline comments explaining "why", not "what"
- **CLAUDE.md**: Project-level documentation (commands, architecture, workflows)
- **Javadoc**: Not actively used (SpringDoc preferred for API docs)

## Quality Gates

### Pre-Commit
- No automatic checks (developer responsibility)

### CI Pipeline
- All tests must pass
- Checkstyle warnings reported (not enforced)
- SpotBugs warnings reported (not enforced)
- JaCoCo coverage reports generated
- OpenAPI spec validation

### Code Review
- GitHub Pull Requests required
- CI checks must pass
- Manual review before merge
