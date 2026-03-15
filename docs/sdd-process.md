# Spec Driven Development (SDD) Process Guide

## Overview

This document defines the complete process for Spec Driven Development (SDD).
It serves as a step-by-step guide for developing new features or modifying existing ones, starting from specifications and progressing through test-driven implementation.

Execution details for each step are defined in the corresponding Cursor skills. This document focuses on the overall process overview, inter-step dependencies, the review framework, and cross-cutting insights.

## Core Principles

- **Specification first, implementation second:** Finalize business requirements → design → HTTP contract before writing code
- **Separation of concerns:** Requirements (WHAT), design (HOW), contract (INTERFACE), and implementation (CODE) are clearly separated
- **No duplication across documents:** Do not write the same information in multiple documents. Each document serves as a Single Source of Truth
- **Test-driven:** Write tests first (RED), then make them pass with implementation (GREEN)

## Process Overview

```
Step 1: Requirements (Domain requirements + ACs)
    ↓                           ← Specification phase
Step 2: Design — HLD (Processing flows, state transitions, Domain models)
    ↓
Step 3: OpenAPI (HTTP interface contract)
    ↓                     ┌──→ QA: E2E Test case preparation begins (in parallel)
Step 4: Scenario Test — RED (Acceptance tests, ACs within service boundary)
    ↓                           ← Implementation phase
Step 5: TDD Implementation (Complete when all tests are GREEN)
    ↓
    └──→ Staging deployment → QA: E2E Test execution (verifies actual side effects of external services)
```

---

## Step 1: Requirements

**Purpose:** Finalize domain-level business requirements and acceptance criteria. No technical concerns are included.

**Skill:** `sdd-requirements`

**Artifact:** `docs/specs/{feature}/requirements.md`

**Reviewers:**
- **PDM:** Validity of business requirements and ACs, Out of Scope decisions
- **QA:** Completeness of ACs, testability, identification of ambiguities
- **Tech Lead:** Technical feasibility of business requirements

**Responsibilities of requirements.md:** Describes only business requirements, domain rules, and acceptance criteria. Technical concerns such as processing flows, state transitions, integration patterns, and HTTP status codes are not included. These are defined from Step 2 (design.md) onward.

---

## Step 2: Design — HLD (High-Level Design)

**Purpose:** Design how to realize the business requirements at the architecture level. Before defining the OpenAPI (HTTP contract), finalize processing flows, state transitions, domain models, and error strategies.

**Skill:** `sdd-design`

**Artifact:** `docs/specs/{feature}/design.md`

**Reviewers:**
- **Tech Lead:** Validity of processing flows, accuracy of state transitions, appropriateness of integration patterns
- **Developers:** Feasibility of processing flows, consistency with existing patterns

**Decision Flow:** If any of the following is YES → design.md is recommended. If all are NO → design.md is not needed (proceed to Step 3: OpenAPI).

```
Q1: Are there multi-step processing flows? (staged processing, Saga, etc.)
Q2: Are there state transitions? (status management, lifecycle)
Q3: Is there integration with external systems? (API calls, event processing)
Q4: Is an error recovery strategy needed? (retry, compensating transactions)
Q5: Are there DB schema changes? (new tables, column additions, FK constraint changes)
```

**On LLD (Low-Level Design):** This process does not create LLD as a separate document. Spring Boot's layered architecture (Controller → Service → Repository) and the test code (created through TDD) serve the role of LLD. Irreversible architectural decisions are recorded in `docs/ADR.md`.

---

## Step 3: OpenAPI (HTTP Interface Contract)

**Purpose:** Define the Single Source of Truth for the HTTP interface. Translate the processing flows and error strategies from design.md into an HTTP contract.

**Skill:** `sdd-openapi`

**Artifacts:**
- `openapi/paths/{feature}.yaml`
- `openapi/components/schemas/requests/{Request}.yaml`
- `openapi/components/schemas/responses/{Response}.yaml`
- `openapi/components/examples/errors/{ERROR_CODE}.yaml`

**Reviewers:**
- **Tech Lead:** Error code taxonomy, appropriateness of status codes, consistency with existing APIs
- **Frontend:** Usability of request/response, field naming, ease of error handling implementation

---

## Step 4: Scenario Test — RED + E2E Test Preparation

**Purpose:** Express the ACs from requirements.md as test code and finalize them in a RED state. Can be written with just the ACs and OpenAPI — internal design is not needed.

**Routing ACs by verification boundary:** Each AC in requirements.md is routed to either a Scenario Test or an E2E Test based on whether verification requires actual side effects from external services. See `testing.md` for detailed routing rules.

| AC Characteristic | Test Type | Owner | Execution Environment |
|-------------------|-----------|-------|-----------------------|
| Verification can be completed with the service's own response and DB state | Scenario Test | Developer | Local (Testcontainers + WireMock) |
| Verifying that the correct request is sent to an external service | Scenario Test (WireMock verify) | Developer | Local |
| Verification requires actual DB state or side effects from external services | E2E Test | QA team | Staging environment |

ACs tagged with `[E2E]` in requirements.md are only subject to E2E Test verification.

**Skill:** `sdd-scenario-test`

**Artifacts:**
- `src/test/java/.../scenario/{Feature}ScenarioTest.java` (Developer)
- E2E test cases (QA team — preparation can begin after Step 3 OpenAPI is finalized)

**Reviewers:**
- **Developers (code review):** Correctness of AC mapping in Scenario Tests, test isolation, helper method design
- **QA:** Completeness of E2E test cases

**Test state at this point:**
- Scenario Test: **RED**
- E2E Test: Case preparation in progress (execution happens after staging deployment)

---

## Step 5: TDD Implementation

**Purpose:** Write tests first, then make them pass with implementation. Build bottom-up and make all tests GREEN.

**Skills:** `sdd-tdd` (orchestration), `sdd-controller-web-test`, `sdd-service-unit-test`, `sdd-integration-test`

**Reviewers:**
- **Developers (code review):** Implementation quality, test coverage, adherence to layer responsibilities

TDD execution order, RED→GREEN transition rules, and stub creation timing are defined in the `sdd-tdd` skill.

**After Step 5 completion:** Deploy to the staging environment where the QA team executes E2E Tests. This confirms that external integrations verified via WireMock in Scenario Tests work as expected in the real environment.

---

## Document Responsibility Separation

| Document | Defines | Does Not Define |
|----------|---------|-----------------|
| `domain-glossary.md` | Project-wide business terms | Design terms, technical patterns (→ ADR.md) |
| `requirements.md` | Domain requirements, business rules, ACs, feature-specific terms | Processing flows, state transitions, HTTP details, implementation terms, internal status names |
| `design.md` | HLD: processing flows, state transitions, data models, integration patterns, error handling strategies | HTTP details (error codes, mapping tables → OpenAPI), DDL syntax (→ Flyway), layer implementation details / logging policies (→ code) |
| OpenAPI specs | HTTP contract, JSON schemas, error examples | Domain requirements, processing flows |
| `testing.md` | Testing strategy, test type responsibilities, naming conventions | Test items for individual features |
| Code + Tests | LLD: layer implementation, transaction boundaries, DB access | — |

### Information Flow

```
requirements.md (Defines WHAT)
    │
    ├──→ design.md (Defines HOW at the architecture level)
    │       │
    │       └──→ OpenAPI (Defines INTERFACE)
    │               │
    │               └──→ E2E Test case preparation (QA team, in parallel)
    │
    └──→ Scenario Test (Transforms ACs within service boundary into executable form)
```

---

## Artifact Directory Structure

```
docs/
├── sdd-process.md          ← This document (overall process)
├── domain-glossary.md      ← Project-wide domain glossary (business terms only)
├── testing.md              ← Testing strategy (shared across all features)
├── STEERING.md             ← Project overview
├── ADR.md                  ← Architecture Decision Records
└── specs/
    └── {feature}/
        ├── requirements.md ← Domain requirements + ACs (only feature-specific terms defined)
        └── design.md       ← High-level design (processing flows, state transitions, integration patterns)

openapi/
├── paths/
│   └── {feature}.yaml      ← Path definitions
└── components/
    ├── schemas/
    │   ├── requests/        ← Request schemas
    │   └── responses/       ← Response schemas
    └── examples/
        └── errors/          ← Error response examples

src/test/java/.../
├── scenario/                ← Scenario Tests (Acceptance)
├── controller/              ← Controller Web Tests
└── service/
    ├── impl/                ← Service Unit Tests
    └── {Service}IntegrationTest.java  ← Integration Tests
```