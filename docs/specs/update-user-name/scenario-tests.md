# Scenario Test Specification

## Purpose

本書は `update-user-name` の受け入れシナリオを定義する。
ここで定義した **P0 シナリオがすべて通過した場合、`requirements.md` の要件を満たしたと判定する**。

契約の参照元:
- Path: `openapi/paths/users-userId-update-user-name.yaml`
- Request: `openapi/components/schemas/requests/UpdateUserNameRequest.yaml`
- Error examples: `openapi/components/examples/errors/*.yaml`

## Modern Best-Practice Test Scope

- シナリオは「観測可能な振る舞い」に限定する
- 内部実装（Repository 呼び出し回数など）はシナリオテストで検証しない
- P0（必須）と P1（拡張）を分離し、まず P0 を安定化する
- 過剰な組み合わせ爆発を避け、境界値と代表ケースを選ぶ

## P0 Scenarios (Must Pass)

### SCN-001 正常更新

Given:
- 既存ユーザー `userId` が存在し、現在の名前が `"Alice"` である
- 認証済みリクエストである

When:
- `PATCH /users/{userId}` に `{"user_name":"Bob"}` を送信する

Then:
- レスポンスは成功
- 返却されたユーザー名は `"Bob"`
- 再取得時もユーザー名は `"Bob"`

### SCN-002 前後空白トリム

Given:
- 既存ユーザー `userId` の現在名が `"Alice"` である
- 認証済みリクエストである

When:
- `PATCH /users/{userId}` に `{"user_name":"   Bob   "}` を送信する

Then:
- レスポンスは成功
- 返却されたユーザー名は `"Bob"`（trim 後）
- 再取得時もユーザー名は `"Bob"`

### SCN-003 同値更新の冪等

Given:
- 既存ユーザー `userId` の現在名が `"Alice"` である
- 認証済みリクエストである

When:
- `PATCH /users/{userId}` に `{"user_name":"Alice"}` を送信する

Then:
- レスポンスは成功
- ユーザー名は `"Alice"` のまま
- 観測可能な不整合（重複レコード作成など）が発生しない

### SCN-004 バリデーション: 空値

Given:
- 既存ユーザー `userId` が存在する
- 認証済みリクエストである

When:
- `PATCH /users/{userId}` に空値（`""` または空白のみ）を送信する

Then:
- レスポンスは `400`
- `error_code` は `VALIDATION.FAILED`

### SCN-005 バリデーション: 長さ超過

Given:
- 既存ユーザー `userId` が存在する
- 認証済みリクエストである

When:
- 正規化後 51 文字以上となる `user_name` を送信する

Then:
- レスポンスは `400`
- `error_code` は `VALIDATION.FAILED`

### SCN-006 未認証拒否

Given:
- 既存ユーザー `userId` が存在する
- 認証情報がない、または不正である

When:
- `PATCH /users/{userId}` を呼び出す

Then:
- レスポンスは `401`
- `error_code` は以下のいずれか
  - `AUTH.MISSING_CREDENTIALS`
  - `AUTH.INVALID_FORMAT`
  - `AUTH.INVALID_TOKEN`

### SCN-007 不正JSON

Given:
- 既存ユーザー `userId` が存在する
- 認証済みリクエストである

When:
- `PATCH /users/{userId}` に不正 JSON を送信する

Then:
- レスポンスは `400`
- `error_code` は `REQUEST.MALFORMED_JSON`

### SCN-008 ユーザー不存在

Given:
- 指定した `userId` が存在しない
- 認証済みリクエストである

When:
- `PATCH /users/{userId}` を呼び出す

Then:
- レスポンスは `404`
- `error_code` は `USER.NOT_FOUND`

## P1 Scenarios (Optional)

- `415`（Content-Type 不正）で `REQUEST.UNSUPPORTED_MEDIA_TYPE` を返す
- Unicode / Emoji を含む名前の更新
- 境界値:
  - 正規化後 1 文字（受理）
  - 正規化後 50 文字（受理）
  - 正規化後 51 文字（拒否）

## Automation Mapping

- API受け入れテスト（HTTP）:
  - SCN-001..008 を実装
- Service integration test:
  - 正規化後バリデーションと同値更新ロジックを補強
- Web test:
  - malformed JSON / media type / validation の入り口保証

## Definition of Done for This Feature

- P0 シナリオ 8 件が自動化され、CI で常時グリーン
- OpenAPI の error examples と実際のレスポンスが一致
- `requirements.md` / `design.md` / 実装 / テストの差分が解消されている
