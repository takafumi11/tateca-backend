# STEERING — Tateca Backend

プロジェクトの全体像を素早く把握するための「地図」。
新規参画の開発者や、別の Cursor セッションがコンテキストを得るために参照する。

## Document Map

```
README.md          — 人間向けの玄関。概要 + Getting Started + リンク集
CLAUDE.md          — AI エージェントへの指示書。コマンド、規約、テスト手法（150行以下）
docs/STEERING.md   — プロジェクトの地図（本文書）。プロダクト、ドメイン、アーキテクチャ、運用の詳細
docs/sdd-process.md — SDD プロセスガイド
docs/testing.md     — テスト戦略・テストタイプ定義
docs/specs/{feature}/ — フィーチャー別の要件・設計
```

---

## Product Overview

**Tateca** は、グループ経費管理 (Group Expense Management) の REST API バックエンド。

複数ユーザーが共有するグループ内で、経費 (Expense)・貸し借り (Loan)・返済 (Repayment) を記録し、多通貨に対応した精算 (Settlement) を算出する。

| 項目 | 値 |
|------|-----|
| API 名 | Tateca API |
| バージョン | 1.0.0 |
| API ドキュメント | https://docs.tateca.net |
| 本番 API | https://api.tateca.net |
| ステージング API | https://staging-api.tateca.net |

---

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Java | 25 |
| Framework | Spring Boot | 3.5.8 |
| Build Tool | Gradle (Kotlin DSL) | 9.2.0 |
| Database | MySQL | 8 |
| DB Migration | Flyway | (Spring Boot managed) |
| Authentication | Firebase Authentication | Admin SDK 9.4.2 |
| Resilience | Resilience4j | 2.2.0 |
| Observability | Logstash + Micrometer + Better Stack | — |
| Container Registry | GitHub Container Registry (GHCR) | — |
| Deployment | Railway (image-based) | — |
| API Documentation | Redocly (OpenAPI 3.1) | — |

バージョンの実態は `gradle/libs.versions.toml` を参照。

---

## Domain Model

### Core Entities

```
┌──────────────┐     N:M      ┌──────────────┐
│  AuthUser    │──────────────│    Group      │
│  (Firebase)  │  UserGroup   │              │
└──────┬───────┘              └──────┬───────┘
       │ 1:1                         │ 1:N
┌──────┴───────┐              ┌──────┴───────┐
│    User      │              │ Transaction  │
│  (Profile)   │              │   History    │
└──────────────┘              └──────┬───────┘
                                     │ 1:N
                              ┌──────┴───────┐
                              │ Transaction  │
                              │  Obligation  │
                              └──────────────┘

┌──────────────┐     1:N      ┌──────────────┐
│   Currency   │──────────────│ ExchangeRate │
└──────────────┘              └──────────────┘
```

| Entity | 役割 |
|--------|------|
| `AuthUserEntity` | Firebase UID と紐づく認証ユーザー。レビュー設定を保持 |
| `UserEntity` | 表示名などのプロファイル情報 |
| `GroupEntity` | ユーザーが所属するグループ。招待コード (invite code) でメンバーを追加 |
| `UserGroupEntity` | ユーザーとグループの多対多中間テーブル |
| `TransactionHistoryEntity` | 経費・貸し借り・返済の履歴 |
| `TransactionObligationEntity` | トランザクションに対する各ユーザーの負担額 |
| `CurrencyEntity` | 通貨マスタ (JPY, USD, EUR 等) |
| `ExchangeRateEntity` | 日次の為替レート |

### Transaction Types

| Type | 説明 |
|------|------|
| `EXPENSE` | グループ経費 (割り勘) |
| `LOAN` | メンバー間の貸し借り |
| `REPAYMENT` | 借りたお金の返済 |

---

## Architecture Overview

### Layered Architecture

```
HTTP Request
    │
    ▼
┌─────────────────────────┐
│  Security Filter        │  Firebase JWT / API Key 認証
│  (TatecaAuthFilter)     │
├─────────────────────────┤
│  Controller             │  HTTP ⇔ DTO 変換、Bean Validation
├─────────────────────────┤
│  Service                │  ビジネスロジック、ドメインルール
├─────────────────────────┤
│  Repository             │  JPA によるデータアクセス
├─────────────────────────┤
│  MySQL                  │  永続化 (Flyway マイグレーション)
└─────────────────────────┘
```

### Authentication

2 つの認証方式がパスベースでルーティングされる:

| パス | 認証方式 | 用途 |
|------|----------|------|
| `/internal/**` | X-API-Key ヘッダー | Lambda / EventBridge からの内部呼び出し |
| その他 | Firebase JWT (Bearer token) | フロントエンドからのユーザー操作 |

- Constant-time API key comparison to prevent timing attacks
- `@UId` annotation extracts user ID or system ID from the security context

### External Integrations

| 連携先 | 用途 | 頻度 |
|--------|------|------|
| Firebase Authentication | ユーザー認証・トークン検証 | リクエストごと |
| Exchange Rate API | 為替レート取得 | 日次 (00:01 UTC) |
| Better Stack | ログ集約・モニタリング | リアルタイム |

---

## API Endpoints Overview

### User / Auth

| Method | Path | 機能 |
|--------|------|------|
| POST | `/auth/users` | 認証ユーザー登録 |
| GET | `/auth/users/{uid}` | 認証ユーザー取得 |
| DELETE | `/auth/users/{uid}` | 認証ユーザー削除 |
| PATCH | `/auth/users/review-preferences` | レビュー設定更新 |
| PUT | `/users/{userId}` | 表示名更新 |

### Group

| Method | Path | 機能 |
|--------|------|------|
| POST | `/groups` | グループ作成 |
| GET | `/groups/list` | グループ一覧取得 |
| GET | `/groups/{groupId}` | グループ詳細取得 |
| PUT | `/groups/{groupId}` | グループ名更新 |
| POST | `/groups/{groupId}/members` | メンバー追加 (招待コード) |
| DELETE | `/groups/{groupId}/members/{userUuid}` | メンバー削除 |
| DELETE | `/groups/{groupId}/users/{userUuid}` | グループ脱退 |

### Transaction

| Method | Path | 機能 |
|--------|------|------|
| POST | `/groups/{groupId}/transactions` | トランザクション作成 |
| GET | `/groups/{groupId}/transactions/{transactionId}` | トランザクション詳細取得 |
| PUT | `/groups/{groupId}/transactions/{transactionId}` | トランザクション更新 |
| DELETE | `/groups/{groupId}/transactions/{transactionId}` | トランザクション削除 |
| GET | `/groups/{groupId}/transactions/history` | 取引履歴一覧取得 |
| GET | `/groups/{groupId}/transactions/settlement` | 精算情報取得 |

### Exchange Rate

| Method | Path | 機能 |
|--------|------|------|
| GET | `/exchange-rate/{date}` | 指定日の為替レート取得 |
| POST | `/internal/exchange-rates` | 為替レート更新 (内部 API) |

---

## CI/CD

### Overview

```
PR → CI (Test + OpenAPI Lint + Preview)  ※テスト必須でマージブロック
Main merge → CD (OpenAPI Deploy + Docker Build + Railway Redeploy)
```

### CI Pipeline (`.github/workflows/ci.yml`)

**Trigger:** Pull requests to `main`

| Job | Description |
|-----|-------------|
| `test` | JDK 25 テスト実行 + JaCoCo カバレッジ (マージブロッカー) |
| `openapi-lint` | Redocly による OpenAPI スペック検証 |
| `openapi-preview` | ブランチ名パスで GitHub Pages にプレビューデプロイ |
| `cleanup-preview` | PR close 時に gh-pages からプレビューフォルダを削除 |

OpenAPI Preview URL: `https://<owner>.github.io/<repo>/<branch-name>/`

### CD Pipeline (`.github/workflows/cd.yml`)

**Trigger:** Push to `main` (= PR マージ) + workflow_dispatch

| Job | Description |
|-----|-------------|
| `deploy-docs` | OpenAPI ドキュメントを GitHub Pages ルートにデプロイ |
| `docker-build-push` | Docker Build → GHCR に `latest` + `sha-<hash>` タグでプッシュ |
| `railway-redeploy` | Railway に `latest` イメージの再デプロイをトリガー |

### Deployment

- **Registry:** GHCR (`ghcr.io/tateca/tateca-backend`)
- **Tags:** `latest` (本番ポインタ) + `sha-<commit>` (不変参照)
- **Railway:** image-based deployment。`latest` タグを自動プル

### Required Secrets

- `RAILWAY_TOKEN`, `RAILWAY_SERVICE_ID`

### Branch Protection Rules

`main` ブランチ: PR 必須、`Test` ジョブ必須、direct push 禁止

---

## Code Quality

### Static Analysis (Warning-only mode)

| Tool | Config | Focus |
|------|--------|-------|
| Checkstyle | `config/checkstyle/checkstyle.xml` | UnusedImports, Whitespace, LineLength (150 chars) |
| SpotBugs | `config/spotbugs/spotbugs-exclude.xml` | Bug patterns (Effort: Max, Level: Medium) |
| JaCoCo | `build.gradle.kts` | Coverage reports (verification via `JACOCO_VERIFICATION_ENABLED=true`) |

### Gradual Improvement Roadmap

1. **Phase 1 (Current):** Warning-only mode、メトリクス収集
2. **Phase 2:** 未使用 import 削除、セキュリティ警告修正 (50% 削減目標)
3. **Phase 3:** Whitespace・LineLength 修正 (80% 削減目標)
4. **Phase 4:** Strict mode 有効化 (`ignoreFailures = false`)

### Dependabot

- Weekly dependency updates (Monday 09:00 JST)
- Grouped updates: Spring Boot, testing libraries, security patches, GitHub Actions, Docker base images

---

## SDD Feature Specifications

実装済み機能の仕様書は `docs/specs/` 配下にフィーチャー単位で管理:

| Feature | 仕様ディレクトリ |
|---------|-----------------|
| 認証ユーザー作成 | `docs/specs/create-auth-user/` |
| 認証ユーザー取得 | `docs/specs/get-auth-user/` |
| 認証ユーザー削除 | `docs/specs/delete-auth-user/` |
| レビュー設定更新 | `docs/specs/update-review-preferences/` |
| 表示名更新 | `docs/specs/update-user-name/` |
| グループ作成 | `docs/specs/create-group/` |
| グループ一覧取得 | `docs/specs/get-group-list/` |
| グループ詳細取得 | `docs/specs/get-group-detail/` |
| グループ名更新 | `docs/specs/update-group-name/` |
| グループ参加 | `docs/specs/join-group/` |
| グループ脱退 | `docs/specs/leave-group/` |
| メンバー追加 | `docs/specs/add-member/` |
| メンバー削除 | `docs/specs/remove-member/` |
| トランザクション作成 | `docs/specs/create-transaction/` |
| トランザクション詳細取得 | `docs/specs/get-transaction-detail/` |
| トランザクション更新 | `docs/specs/update-transaction/` |
| トランザクション削除 | `docs/specs/delete-transaction/` |
| 取引履歴取得 | `docs/specs/get-transaction-history/` |
| 精算情報取得 | `docs/specs/get-transaction-settlement/` |
| 為替レート取得 | `docs/specs/get-exchange-rate/` |
| 為替レート更新 | `docs/specs/update-exchange-rate/` |
