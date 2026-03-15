# Testing Strategy

## Overview

A strategy that classifies tests by **the specification they verify**, ensuring each test type provides unique value.

```
┌────────────────────────────────────────────────────────────────────┐
│                  External Specification Tests                      │
│         Verifying specifications shared with API consumers         │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Scenario Test (Acceptance Test)                              │  │
│  │ Verifies: requirements.md ACs (within service boundary)     │  │
│  │ SUT: All layers combined    Approach: Black-box / API only  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ E2E Test (Integration Test)                                 │  │
│  │ Verifies: requirements.md ACs (actual side effects of       │  │
│  │           external services)                                │  │
│  │ SUT: All services combined  Approach: Staging / QA team     │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Controller Web Test (Contract Test)                         │  │
│  │ Verifies: OpenAPI spec                                      │  │
│  │ SUT: Web layer only         Approach: Service mocked        │  │
│  └──────────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────────┤
│                  Internal Specification Tests                      │
│         Quality assurance within the development team              │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Service Unit Test (Domain Test)                             │  │
│  │ Verifies: Domain logic                                      │  │
│  │ SUT: Service layer only     Approach: All dependencies      │  │
│  │                              mocked                         │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Integration Test (Infrastructure Test)                      │  │
│  │ Verifies: Infrastructure-specific behavior                  │  │
│  │ SUT: Service + Infra        Approach: Real DB / WireMock    │  │
│  └──────────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────────┤
│  Cross-cutting: Security / API Client / Repository / Utility      │
└────────────────────────────────────────────────────────────────────┘
```

---

## Testing Philosophy

### Principle 1: Tests Verify Specifications

In SDD, tests are the means of verifying specifications. Every feature test has a corresponding SDD artifact.

| SDD Artifact | Test Type |
|-------------|-----------|
| requirements.md (ACs) — verifiable within service boundary | Scenario Test |
| requirements.md (ACs) — requires verification of actual side effects from external services | E2E Test |
| OpenAPI spec | Controller Web Test |
| Domain rules (derived from requirements.md) | Service Unit Test |
| Infrastructure requirements (derived from design.md) | Integration Test |

### Principle 2: Classification Is Determined by the Specification Under Test

Test classification is determined by **what is being verified (the target specification)**. The testing technique (use of mocks, DB connection, etc.) is not a classification criterion.

### Principle 3: External vs. Internal Is Determined by Specification Visibility

- **External specification tests** — Verify specifications published to API consumers
- **Internal specification tests** — Quality assurance within the development team

Controller Web Tests mock the Service layer, but since they verify the OpenAPI spec (an external specification published to the frontend), they are classified as external specification tests.

### Principle 4: Each Test Type Provides Unique Value

Each test type focuses on "things that can only be verified at that layer." Business outcomes already verified by higher-level tests are not re-verified in lower-level tests, and logic branches already verified by lower-level tests are not re-verified in higher-level tests.

### What a Test Failure Indicates

| Test Type | A Failure Indicates |
|-----------|-------------------|
| Scenario Test | A business requirement is not met (within service boundary) |
| E2E Test | Inter-service integration is not working as expected |
| Controller Web Test | The HTTP contract has been broken |
| Service Unit Test | There is a bug in the domain logic |
| Integration Test | Infrastructure behavior differs from expectations |

---

## AC Verification Boundaries

Each AC in requirements.md is routed to either a Scenario Test or an E2E Test based on whether verification requires actual side effects from external services.

| AC Characteristic | Test Type | Example |
|-------------------|-----------|---------|
| Verification can be completed with the service's own response and DB state | Scenario Test | "Balance is deducted", "Error is returned" |
| Verifying that the correct request is sent to an external service | Scenario Test (WireMock verify) | "A Sync Invest request is sent to PPG" |
| Verification requires actual DB state or side effects from external services | E2E Test | "Standard points on the PPG side actually increase" |

When writing ACs in requirements.md, explicitly identify which boundary each AC belongs to. ACs that are only subject to E2E Test verification are tagged with `[E2E]` in requirements.md.

### Limits of Shift-Left

Scenario Tests verify against external services stubbed with WireMock. If the WireMock stubs diverge from the actual behavior of external services, the divergence cannot be detected locally.

1. **Scenario Tests (WireMock verify)** prevent "request construction errors" and "flow omissions" — the majority of bugs can be detected here
2. **E2E Tests (staging environment)** detect "divergence from actual behavior"
3. **Future improvement:** Introduction of Contract Testing will enable local detection of divergence between WireMock stubs and actual APIs

---

## Test Type Definitions

### Scenario Test (Acceptance Test)

| Item | Details |
|------|---------|
| Specification under test | requirements.md ACs (without `[E2E]` tag) |
| Perspective | API consumer's black-box perspective |
| Infrastructure | `AbstractIntegrationTest` + `@AutoConfigureMockMvc` |
| Naming | `{Feature}ScenarioTest` — `src/test/java/.../scenario/` |
| Owner | Developer |

**Responsibilities:** Business flow verification within service boundary, correctness of requests sent to external services (WireMock verify)

**Not responsible for:** Response JSON schema (→ Controller Web Test), domain logic branching (→ Service Unit Test), actual side effects of external services (→ E2E Test)

### E2E Test (Integration Test)

| Item | Details |
|------|---------|
| Specification under test | requirements.md ACs (with `[E2E]` tag) |
| Perspective | Inter-service integration |
| Infrastructure | Staging environment + real microservice cluster |
| Start timing | Case preparation can begin from SDD Step 3 (after OpenAPI is finalized). Execution happens after staging deployment |
| Owner | QA team |

**Responsibilities:** Verification of actual DB state and side effects of external services, detection of divergence between WireMock stubs and actual APIs, detection of environment-specific issues

**Not responsible for:** Business logic within service boundary (→ Scenario Test), exhaustive HTTP contract verification (→ Controller Web Test)

### Controller Web Test (Contract Test)

| Item | Details |
|------|---------|
| Specification under test | OpenAPI spec |
| Perspective | HTTP interface boundary |
| Infrastructure | `@WebMvcTest` + `@MockitoBean` |
| Naming | `{Controller}WebTest` — `src/test/java/.../controller/` |
| Owner | Developer |

**Responsibilities:** Status codes and response structure, Bean Validation, Service exception → HTTP mapping, correct delegation to Service

**Not responsible for:** Correctness of business logic (→ Service Unit Test), data persistence (→ Integration Test)

### Service Unit Test (Domain Test)

| Item | Details |
|------|---------|
| Specification under test | Domain rules derived from requirements.md |
| Perspective | White-box view of domain logic |
| Infrastructure | `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks` |
| Naming | `{Service}UnitTest` — `src/test/java/.../service/` |
| Owner | Developer |

**Responsibilities:** Exhaustive coverage of domain rule branches, repository interaction verification

**Not responsible for:** HTTP response format (→ Controller Web Test), actual DB persistence behavior (→ Integration Test)

### Integration Test (Infrastructure Test)

| Item | Details |
|------|---------|
| Specification under test | Infrastructure-specific behavior |
| Perspective | Real-environment behavior that cannot be verified with Unit Tests |
| Infrastructure | `AbstractIntegrationTest` (Testcontainers MySQL + WireMock) |
| Naming | `{Service}IntegrationTest` — `src/test/java/.../service/` |
| Owner | Developer |

**Responsibilities:** JPA lifecycle, DB encoding, transaction boundaries, custom queries, external API client retry and fallback

**Not responsible for:** Correctness of domain logic (→ Service Unit Test), business flow verification (→ Scenario Test)

---

## Responsibility Boundaries Between Tests

| Verification Item | Scenario | E2E | Controller Web | Service Unit | Integration |
|-------------------|:--------:|:---:|:--------------:|:------------:|:-----------:|
| Business requirements (ACs) — within service boundary | **◎** | | | | |
| Business requirements (ACs) — actual side effects of external services | | **◎** | | | |
| Requests sent to external services | **◎** ※1 | **◎** | | | |
| HTTP status codes | △ ※2 | | **◎** | | |
| Response JSON schema | | | **◎** | | |
| Bean Validation firing | | | **◎** | | |
| Exception → response mapping | | | **◎** | | |
| Domain logic branching | | | | **◎** | |
| Repository call verification | | | | **◎** | |
| Actual DB persistence | | | | | **◎** |
| JPA lifecycle | | | | | **◎** |
| Character encoding | | | | | **◎** |
| Retry and fallback | | | | | **◎** |
| WireMock stub vs. actual API divergence | | **◎** | | | |
| Environment-specific issues | | **◎** | | | |

※1 Verified via WireMock verify. Confirms the correctness of request content, not actual transmission to the real API.
※2 Scenario Tests expect the correct status code as a result of a business flow, but exhaustive verification of all status codes is the responsibility of Controller Web Tests.
