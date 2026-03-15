# Tateca Backend

グループ経費管理 REST API。複数ユーザーでの経費・貸し借り・返済を記録し、多通貨対応の精算を算出する。

| | |
|---|---|
| **Stack** | Java 25 / Spring Boot 3.5.8 / MySQL 8 / Firebase Auth |
| **Deployment** | Railway (image-based) via GHCR |
| **API Docs** | [docs.tateca.net](https://docs.tateca.net) |
| **CI/CD** | GitHub Actions (`ci.yml` / `cd.yml`) |

## Getting Started

**Prerequisites:** JDK 25, Docker

```bash
docker-compose up -d    # Start MySQL
./gradlew bootRun       # Start application
./gradlew test          # Run tests
```

Dev profile provides default values for local development. See [CLAUDE.md](CLAUDE.md) for all commands and environment variables.

## Documentation

| Document | 内容 |
|----------|------|
| [CLAUDE.md](CLAUDE.md) | AI エージェント向け指示書 — コマンド、規約、テスト手法 |
| [docs/STEERING.md](docs/STEERING.md) | プロジェクト全体像 — ドメインモデル、アーキテクチャ、CI/CD、API 一覧 |
| [docs/sdd-process.md](docs/sdd-process.md) | SDD プロセスガイド |
| [docs/testing.md](docs/testing.md) | テスト戦略 |
| [docs/specs/](docs/specs/) | フィーチャー別の要件・設計 |
| [openapi/](openapi/) | OpenAPI スペック (HTTP 契約の Single Source of Truth) |

## Development Methodology

This project follows **Specification-Driven Development (SDD)**.

```
Requirements + HLD → OpenAPI → Scenario Test (RED) → TDD Implementation (GREEN)
```

## Project Structure

```
src/main/java/.../
├── controller/     ← REST endpoints
├── service/        ← Business logic
├── repository/     ← JPA data access
└── entity/         ← Domain models

src/test/java/.../
├── scenario/       ← Scenario Tests (Acceptance)
├── controller/     ← Controller Web Tests
└── service/        ← Unit + Integration Tests
```
