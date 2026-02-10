# Testing Best Practices

## Test Structure

### Controller Tests

**ControllerWebTest** (MockMvc)
- HTTP layer testing (request/response)
- Validation, serialization, status codes
- Mock service layer
- No database, no business logic

**Example**: `UserControllerWebTest`

---

### Service Tests

**ServiceUnitTest** (Mockito)
- Internal behavior verification (mock dependencies)
- Repository call order, entity mutation, logging
- No database, no actual integration

**ServiceIntegrationTest** (Testcontainers)
- End-to-end service behavior with real infrastructure
- Database persistence, DTO mapping, transactions
- Exception handling, boundary values

**Rule**: UnitTest only tests what IntegrationTest cannot verify

**Example**:
```
UnitTest: Repository call order, logging
IntegrationTest: DB persistence, DTO mapping, exceptions
```

---

## Test Naming

### Integration/WebTest
```
givenCondition_whenAction_thenResult()
```
Example: `givenUserExists_whenUpdatingName_thenShouldUpdateCorrectly()`

### UnitTest (with @Nested)
```
@Nested class Behavior
@Test void shouldDoSomething()
```
Example: `@Nested class RepositoryInteractionBehavior`

---

## @Nested Usage

**Use @Nested when**:
- Multiple endpoints in controller
- Testing different behavior aspects (UnitTest)

**Don't use @Nested when**:
- Single endpoint
- Simple test structure

---

## Test Consolidation

**Avoid over-splitting**:
```
❌ Bad: 3 tests for basic update (name, relationship, timestamp)
✅ Good: 1 test verifying all aspects
```

**Separate when**:
- Different test conditions (boundary values, error cases)
- Different behavior aspects (UnitTest categories)

---

## Role Separation

| Test Type | Purpose | Uses |
|-----------|---------|------|
| WebTest | HTTP layer | MockMvc, Mock Service |
| UnitTest | Internal behavior | Mockito, LogAppender |
| IntegrationTest | Real integration | Testcontainers, Real DB |

**No duplication**: If IntegrationTest covers it, remove from UnitTest.
