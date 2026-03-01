# Requirements Document

## Introduction

本ドキュメントは、ユーザー自身の表示名更新機能（`update-user-name`）の要件を確定するための仕様書です。
目的は、ドメインとビジネスルールを明確化し、後続の `design.md` でインターフェース契約と実装方針へ展開することです。

## Scope

- 本書は、`userId` で特定されるユーザー表示名更新におけるドメイン要件とビジネスルールを定義する。
- HTTP ステータス、JSON 形式、エンドポイント定義などのインターフェース詳細は `design.md` で定義する。
- 楽観ロック競合、禁止語フィルタ、重複制約は本スコープ外とする。

## Domain Definitions

- Display Name:
  - ユーザーの表示名
  - 保存前に前後空白を除去した値を正規値とする
- Same-Value Update:
  - 現在の正規値と、更新要求の正規値が一致する更新

## Requirements

### Requirement 1: 表示名の更新（正常系）

**User Story:** ユーザーとして表示名を変更したい。プロフィール情報を最新状態に保つため。

#### Acceptance Criteria

1. WHEN 認証済みリクエストで有効な表示名が指定される THEN システム SHALL 対象ユーザーの表示名を更新する
2. WHEN 表示名更新が確定する THEN システム SHALL 以降の参照で更新後の表示名を返す

---

### Requirement 2: 入力正規化とバリデーション

**User Story:** API クライアントとして、不正入力を早期に検知し、許容入力は一貫した形式で保存してほしい。

#### Acceptance Criteria

1. WHEN 表示名に前後空白が含まれる THEN システム SHALL 前後空白を除去して保存する
2. WHEN 表示名が未指定、null、空文字、または空白のみのとき THEN システム SHALL 入力不備として拒否する
3. WHEN 正規化後の表示名が 50 文字を超えるとき THEN システム SHALL 入力不備として拒否する

---

### Requirement 3: 認証

**User Story:** システム管理者として、認証されていないアクセスからユーザーデータを保護したい。

#### Acceptance Criteria

1. WHEN 未認証の要求である THEN システム SHALL 更新処理を拒否する
2. WHEN 認証情報が不正である THEN システム SHALL 更新処理を拒否する

---

### Requirement 4: 冪等性（同値更新）

**User Story:** API クライアントとして、同じ表示名を再送しても状態不整合を起こしたくない。

#### Acceptance Criteria

1. WHEN 現在値と同じ表示名で更新要求する THEN システム SHALL 成功として扱う
2. WHEN 同値更新が行われる THEN システム SHALL 重複レコード作成など観測可能な不整合を発生させない

---

### Requirement 5: リソース不存在

**User Story:** API クライアントとして、対象ユーザー情報が存在しない場合に適切なエラーを受け取りたい。

#### Acceptance Criteria

1. WHEN 認証済みだがアプリ内ユーザーレコードが存在しない THEN システム SHALL リソース不在として拒否する
