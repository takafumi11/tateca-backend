# Specification-Driven Development (SDD) Process Guide

## Overview

本ドキュメントは、仕様駆動開発（Specification-Driven Development）の完全なプロセスを定義する。
新機能の追加や既存機能の変更において、仕様を起点とし、テスト駆動で実装を進めるためのステップバイステップガイドである。

各ステップの実行詳細は対応する Cursor スキルに定義されている。本ドキュメントはプロセス全体の俯瞰、ステップ間の依存関係、レビュー体制、横断的な知見に集中する。

## 基本原則

- **仕様が先、実装が後:** コードを書く前にドメイン要件と HTTP 契約を確定する
- **外部契約が先、内部設計が後:** フロントエンドとの合意（OpenAPI）とブラックボックステストを先に確定し、内部の HOW は後で決める
- **各レイヤーの責務分離:** 要件定義、API 契約、設計、テスト、実装は明確に分離する
- **ドキュメント間の重複排除:** 同じ情報を複数のドキュメントに書かない。各ドキュメントには Single Source of Truth がある
- **テスト駆動:** テストを先に書き（RED）、実装で通す（GREEN）

## プロセス全体像

```
Step 1: Requirements + HLD（ドメイン要件 + 技術方向性 + AC）
    ↓                           ← 仕様フェーズ
Step 2: OpenAPI（HTTP インターフェース契約）
    ↓
Step 3: Scenario Test — RED（受け入れテスト）
    ↓                           ← 実装フェーズ
Step 4: LLD — 任意（不可逆な技術判断がある場合のみ）
    ↓
Step 5: TDD Implementation（全テスト GREEN で完了）
```

---

## Step 1: Requirements + HLD（要件定義 + 高レベル設計）

**目的:** ドメインレベルのビジネス要件と受け入れ基準を確定する。技術的な方向性（High-Level Design）も同時に収束させる。

**スキル:** `sdd-requirements`

**成果物:** `docs/specs/{feature}/requirements.md`

**レビュアー:**
- **PDM:** ビジネス要件と AC の妥当性、Out of Scope の判断
- **QA:** AC の網羅性、テスト可能性、曖昧さの指摘
- **Tech Lead:** ビジネス要件の技術的なレビュー、HLD の方向性の妥当性

**HLD について:** 削除戦略（物理削除 vs ソフトデリート）、既存機能との関係、データモデルの方向性など、AC の書き方に影響する技術的判断は Requirements と同時に議論・確定する。HLD と Requirements は同時並行で収束するものであり、分離しない。requirements.md の Scope / Out of Scope、Domain Definitions セクションが HLD の記載場所となる。

---

## Step 2: OpenAPI（HTTP インターフェース契約）

**目的:** HTTP インターフェースの Single Source of Truth を定義する。外部契約を内部設計より先に確定する。

**スキル:** `sdd-openapi`

**成果物:**
- `openapi/paths/{feature}.yaml`
- `openapi/components/schemas/requests/{Request}.yaml`
- `openapi/components/schemas/responses/{Response}.yaml`
- `openapi/components/examples/errors/{ERROR_CODE}.yaml`

**レビュアー:**
- **Tech Lead:** エラーコード体系、ステータスコードの適切さ、既存 API との一貫性
- **Frontend:** リクエスト/レスポンスの使いやすさ、フィールド命名、エラーハンドリングの実装容易性

---

## Step 3: Scenario Test — RED

**目的:** requirements.md の AC をテストコードとして表現し、RED 状態で確定する。AC と OpenAPI があれば書ける。内部設計は不要。

**スキル:** `sdd-scenario-test`

**成果物:** `src/test/java/.../scenario/{Feature}ScenarioTest.java`

**レビュアー:**
- **開発者（コードレビュー）:** AC マッピングの正しさ、テスト隔離、ヘルパーメソッドの設計

**この時点でのテスト状態:**
- Scenario Test: **RED**

---

## Step 4: LLD（低レベル設計）— 任意

**目的:** 不可逆な技術判断（DB スキーマ、トランザクション境界等）を実装前にレビューする。外部契約とブラックボックステストは既に確定済み。

**スキル:** `sdd-design`

**成果物:** `docs/specs/{feature}/design.md`

**レビュアー:**
- **Tech Lead:** DB スキーマ変更、FK 制約・CASCADE 設計、トランザクション境界の判断
- **開発者:** レイヤー責務の配置、処理フローの実現可能性

**HLD との違い:** Step 1 の HLD は「何を作るか」に影響する技術方向性（削除戦略の方針、既存機能との関係）。LLD は「どう作るか」の具体的な技術判断（Flyway マイグレーション、エンティティリレーション、`@Transactional` スコープ）。LLD の必要性の判定フローはスキル `sdd-design` に定義されている。

---

## Step 5: TDD Implementation

**目的:** テストを先に書き、実装で通す。ボトムアップで積み上げ、全テストを GREEN にする。

**スキル:** `sdd-tdd`（オーケストレーション）、`sdd-controller-web-test`、`sdd-service-unit-test`、`sdd-integration-test`

**レビュアー:**
- **開発者（コードレビュー）:** 実装品質、テスト網羅性、レイヤー責務の遵守

TDD の実行順序、RED→GREEN の遷移ルール、スタブ作成のタイミングはスキル `sdd-tdd` に定義されている。

---

## ドキュメント間の責務分離

| ドキュメント | 定義するもの | 定義しないもの |
|------------|------------|--------------|
| `requirements.md` | ドメイン要件、ビジネスルール、AC、HLD（技術方向性） | HTTP 詳細、実装用語、JSON フィールド名、具体的な技術実装 |
| OpenAPI specs | HTTP 契約、JSON スキーマ、エラー例 | ドメイン要件、内部設計 |
| `design.md`（任意） | LLD: レイヤー責務、処理フロー、DB スキーマ、トランザクション境界 | HTTP スキーマ詳細（→ OpenAPI）、テスト戦略（→ testing.md） |
| `testing.md` | テスト戦略、各テスト責務、命名規約 | 個別機能のテスト項目 |

### 情報の流れ

```
requirements.md（WHAT + HLD を定義）
    │
    ├──→ OpenAPI（HTTP 契約として INTERFACE を定義）
    │
    ├──→ Scenario Test（AC を実行可能な形式に変換）
    │
    └──→ design.md（任意: 実装直前に LLD を定義）
```

---

## 成果物ディレクトリ構成

```
docs/
├── sdd-process.md          ← 本ドキュメント（プロセス全体）
├── testing.md              ← テスト戦略（全機能共通）
└── specs/
    └── {feature}/
        ├── requirements.md ← ドメイン要件 + AC
        └── design.md       ← 技術設計 + 責務マッピング（任意）

openapi/
├── paths/
│   └── {feature}.yaml      ← パス定義
└── components/
    ├── schemas/
    │   ├── requests/        ← リクエストスキーマ
    │   └── responses/       ← レスポンススキーマ
    └── examples/
        └── errors/          ← エラーレスポンス例

src/test/java/.../
├── scenario/                ← Scenario Test（Acceptance）
├── controller/              ← Controller Web Test
└── service/
    ├── impl/                ← Service Unit Test
    └── {Service}IntegrationTest.java  ← Integration Test
```

---

## 既存コードがある場合の SDD 適用

既存コードの状態と目的に応じて、3 つのフローを使い分ける。

**スキル:** `sdd-reverse`

### フロー選択

| ケース | フロー | 目的 |
|-------|-------|------|
| 新規 API を作る（既存コードに影響なし） | **通常 SDD** | 新規開発 |
| 既存 API に機能を追加する | **部分的 Reverse → 通常 SDD** | リグレッション防止 + 新規開発 |
| 既存機能をリファクタリングする | **フル Reverse SDD** | 既存コードの保護 |

### 部分的 Reverse（既存 API への機能追加時）

既存 API に新機能を追加する場合、変更対象の API に限定して既存の振る舞いを保護する。

```
Step 0: 変更対象 API の既存 Scenario Test + requirements.md を作成（部分的 Reverse）
    → 既存の AC を抽出し、Scenario Test が GREEN であることを確認
    ↓
Step 1〜5: 通常 SDD フロー
    → requirements.md に新規 AC を追加
    → 新規 AC の Scenario Test を RED で追加
    → TDD Implementation
    → 全テスト（既存 + 新規）が GREEN で完了
```

**フル Reverse SDD との違い:**
- 変更対象の API のみ（プロジェクト全体ではない）
- Scenario Test + requirements.md のみ（design.md、Controller Web Test、Unit Test は不要）
- 目的はリグレッション防止（仕様の完全な文書化ではない）

### フル Reverse SDD（リファクタリング前の仕様保護）

実装が先に存在する既存機能に対して、SDD 仕様群とテストスイートをフルセットで後付け生成する。

| | 通常 SDD | フル Reverse SDD |
|---|---------|------------|
| 起点 | 要件（仕様が先） | 実装（コードが先） |
| Scenario Test | RED → GREEN | **最初から GREEN** |
| Controller Web Test | RED → GREEN | **最初から GREEN** |
| ユーザー確認 | 仕様作成時 | **リバース後に確認** |
| テストが RED | 実装が未完了 | **バグ発見** |

### Reverse SDD の優先順位

| 優先度 | 基準 |
|--------|------|
| 高 | リファクタリング対象・変更予定がある機能 |
| 高 | 金融計算・決済などビジネスクリティカルな機能 |
| 中 | エンティティ結合が多く複雑な機能 |
| 低 | 安定しており変更予定のない機能 |

