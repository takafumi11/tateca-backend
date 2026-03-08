# Testing Strategy

## 概要

テストを **検証対象の仕様** によって分類し、各テスト種別が固有の価値を提供する戦略を定義する。

```
┌────────────────────────────────────────────────────────────────────┐
│                     外部仕様テスト                                   │
│         API 利用者と共有する仕様の検証                                │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Scenario Test（受け入れテスト）                                │  │
│  │ 検証対象: requirements.md AC                                  │  │
│  │ SUT: 全レイヤー結合    技法: ブラックボックス / API 経由のみ    │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Controller Web Test（契約テスト）                              │  │
│  │ 検証対象: OpenAPI spec                                        │  │
│  │ SUT: Web 層のみ        技法: Service モック                   │  │
│  └──────────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────────┤
│                     内部仕様テスト                                   │
│         開発チーム内部の品質保証                                      │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Service Unit Test（ドメインテスト）                            │  │
│  │ 検証対象: ドメインロジック                                      │  │
│  │ SUT: Service 層のみ    技法: 全依存モック                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Integration Test（インフラテスト）                             │  │
│  │ 検証対象: インフラストラクチャ固有の振る舞い                     │  │
│  │ SUT: Service + インフラ 技法: 実 DB / WireMock                │  │
│  └──────────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────────┤
│  横断的テスト: Security / API Client / Repository / Utility         │
└────────────────────────────────────────────────────────────────────┘
```

---

## テスティングフィロソフィ

### 原則 1: テストは仕様を検証する

SDD においてテストは仕様の検証手段である。全てのフィーチャーテストは対応する SDD 成果物を持つ。

| SDD 成果物 | テスト種別 |
|-----------|----------|
| requirements.md（AC） | Scenario Test |
| OpenAPI spec | Controller Web Test |
| ドメインルール（requirements.md から導出） | Service Unit Test |
| インフラ要件（design.md から導出） | Integration Test |

### 原則 2: 分類は検証対象仕様で決まる

テストの分類は **何を検証するか（検証対象仕様）** で決まる。テスト技法（モックの有無、DB 接続の有無）は分類基準ではない。

### 原則 3: 外部/内部は仕様の公開範囲で決まる

- **外部仕様テスト** — API 利用者に公開する仕様の検証
- **内部仕様テスト** — 開発チーム内部の品質保証

Controller Web Test は Service をモックするが、検証対象は OpenAPI（フロントエンドに公開する外部仕様）であるため外部仕様テストに分類する。モックはテスト対象を分離するための技法であり、検証対象仕様の公開範囲を変えるものではない。

### 原則 4: 各テストは固有の価値を提供する

各テスト種別は「そのレイヤーでしか検証できないこと」に集中する。上位テストで検証済みのビジネス結果を下位テストで再検証せず、下位テストで検証済みのロジック分岐を上位テストで再検証しない。

### テスト失敗が示すもの

| テスト種別 | 失敗が示すもの |
|-----------|------------|
| Scenario Test | ビジネス要件が満たされていない |
| Controller Web Test | HTTP 契約が破られている |
| Service Unit Test | ドメインロジックにバグがある |
| Integration Test | インフラの振る舞いが想定と異なる |

---

## 外部仕様テスト

### Scenario Test（受け入れテスト）

**検証対象仕様:** requirements.md の Acceptance Criteria

**視点:** API 利用者のブラックボックス視点。クライアントが実際に行う API 呼び出しフローを忠実に再現する。

**テスト設計原則:**

- セットアップと検証は全て HTTP エンドポイント経由（Repository 直接参照禁止）
- マスタデータの例外: `currencies`, `exchange_rates` テーブルのみ API 未提供のため `@BeforeEach` での直接投入を許可
- テスト名は requirements.md の Req/AC に 1:1 でマッピングする
- AC に対応しない技術的エッジケースは `TechnicalEdgeCases` ネストクラスに分離する

**責務:**

- ビジネスフローの正常完了（正常系 AC）
- ビジネスルール違反時のリジェクト（異常系 AC）
- 複数 API を跨ぐシナリオの検証

**非責務:**

- レスポンス JSON のスキーマ構造検証 → Controller Web Test
- Bean Validation の発火確認 → Controller Web Test
- ドメインロジックの分岐網羅 → Service Unit Test
- DB エンコーディング、タイムスタンプ精度 → Integration Test

**基盤:** `AbstractIntegrationTest` + `@AutoConfigureMockMvc`（フルスタック起動）

**命名:** `{Feature}ScenarioTest` — `src/test/java/.../scenario/`

---

### Controller Web Test（契約テスト）

**検証対象仕様:** OpenAPI spec

**視点:** HTTP インターフェースの境界。OpenAPI に定義されたステータスコード、レスポンススキーマ、エラー形式が正しく実装されていることを保証する。

**なぜ外部仕様テストか:** Service をモックするが、検証対象は OpenAPI（フロントエンドに公開する外部仕様）である。モックは検証対象を Web 層に分離するための技法であり、検証対象仕様の公開範囲を変えるものではない。

**テスト設計原則:**

- Service は `@MockitoBean` でモックする
- HTTP リクエスト/レスポンスの形式のみをテストする
- ビジネスロジックの正しさは検証しない（Service のモックが正しい値を返す前提）
- DTO 単体テストは設けない（Controller Web Test に統合）

**責務:**

- 各ステータスコードのレスポンス構造（フィールド存在、`error_code`、`errors` 配列）
- Bean Validation の発火（リクエストボディ、パスパラメータの制約）
- Service 例外 → HTTP レスポンスへのマッピング
- Service への正しい委譲（引数、呼び出し回数）
- Content-Type ネゴシエーション

**非責務:**

- ビジネスロジックの正しさ → Service Unit Test
- データの永続化確認 → Integration Test
- 認証フィルターの動作 → Security Tests

**基盤:** `@WebMvcTest` + `@MockitoBean`（Web 層のみ起動）

**命名:** `{Controller}WebTest` — `src/test/java/.../controller/`

---

## 内部仕様テスト

### Service Unit Test（ドメインテスト）

**検証対象仕様:** requirements.md から導出されるドメインルール

**視点:** ドメインロジックのホワイトボックス。Service 層の分岐・例外・状態遷移が正しく機能することを検証する。

**テスト設計原則:**

- 全ての依存を `@Mock` で置換し、Service を完全に分離する
- ドメインルールの分岐を網羅する（正常分岐、異常分岐、境界値）
- Repository との相互作用（呼び出し有無、引数、順序）を検証する

**責務:**

- 認可チェック（メンバーシップ確認、権限検証）
- 存在確認とエラー送出
- ビジネスルールの適用（上限チェック、冪等判定、状態遷移）
- Repository への正しいデータ受け渡し

**非責務:**

- HTTP レスポンス形式 → Controller Web Test
- 認証 → Security Tests
- DB 永続化の実挙動 → Integration Test
- DTO の正規化・バリデーション → Controller Web Test

**基盤:** `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`

**命名:** `{Service}UnitTest` または `{ServiceImpl}Test` — `src/test/java/.../service/`

---

### Integration Test（インフラテスト）

**検証対象仕様:** インフラストラクチャ固有の振る舞い

**視点:** インフラストラクチャ結合。Unit Test では検証不可能な、実環境でしか確認できない振る舞いを検証する。

**テスト設計原則:**

- Unit Test で検証済みのドメインロジック分岐は重複しない
- 「実 DB でしか確認できないこと」に集中する
- 外部 API クライアントのリトライ・フォールバックは WireMock で検証する

**責務:**

- `@PreUpdate` / `@PrePersist` によるタイムスタンプ自動更新
- マルチバイト文字・絵文字の DB エンコーディング
- エンティティリレーションの保持
- トランザクション境界の正しさ
- カスタムクエリの正しさ
- 外部 API クライアントのリトライ・フォールバック（WireMock）

**非責務:**

- ドメインロジックの正しさ → Service Unit Test
- HTTP レスポンス形式 → Controller Web Test
- ビジネスフローの検証 → Scenario Test

**重複排除:** Service Unit Test で検証済みの以下は含めない:

- 例外送出の分岐
- 入力値のバウンダリ（min/max length など）
- モック可能なビジネスロジック

**基盤:** `AbstractIntegrationTest`（Testcontainers MySQL + WireMock）

**命名:** `{Service}IntegrationTest` — `src/test/java/.../service/`

---

## 横断的テスト

フィーチャーの requirements.md に直接マッピングされない、インフラストラクチャコンポーネントのテスト。SDD の Req/AC 命名規約には従わず、テスト対象コンポーネントの責務に基づいた命名を使用する。

| テスト対象 | テスト技法 | 検証内容 |
|-----------|----------|---------|
| 認証フィルター | Unit (Mockito) | JWT 検証、UID 抽出、エラーレスポンス |
| API キー認証 | Integration (TestRestTemplate) | 認証フロー全体の結合動作 |
| API クライアント | Unit (Mockito) + Integration (WireMock) | レスポンス変換、リトライ・フォールバック |
| Repository カスタムクエリ | Integration (Testcontainers) | JPQL/Native クエリの正しさ |
| ユーティリティ | Unit | ヘルパーメソッドの正しさ |
| アノテーション | Unit | カスタムアノテーションの解決 |

---

## テスト間の責務境界

| 検証項目 | Scenario | Controller Web | Service Unit | Integration |
|---------|:--------:|:--------------:|:------------:|:-----------:|
| ビジネス要件 (AC) の充足 | **◎** | | | |
| HTTP ステータスコード | △ ※ | **◎** | | |
| レスポンス JSON スキーマ | | **◎** | | |
| Bean Validation 発火 | | **◎** | | |
| 例外→レスポンス変換 | | **◎** | | |
| ドメインロジック分岐 | | | **◎** | |
| Repository 呼び出し検証 | | | **◎** | |
| 実 DB 永続化 | | | | **◎** |
| JPA ライフサイクル | | | | **◎** |
| 文字エンコーディング | | | | **◎** |
| リトライ・フォールバック | | | | **◎** |

※ Scenario Test はビジネスフローの結果として正しいステータスコードを期待するが、全ステータスコードの網羅的検証は Controller Web Test の責務。

---

## テスト基盤

### AbstractIntegrationTest

Scenario Test と Integration Test の共通基盤。Testcontainers で MySQL と WireMock を起動する。

- `@SpringBootTest` でフルコンテキスト起動
- `@Isolated` で並列実行防止（共有 Testcontainers MySQL のデッドロック防止）
- `flushAndClear()` で Hibernate キャッシュを排除し、DB の実状態を検証

**`@Transactional` の使い分け:**

| テスト種別 | データ隔離方式 | 理由 |
|-----------|-------------|------|
| Integration Test（Service 直接呼び出し） | `@Transactional` ロールバック | Service がテストと同一トランザクション内で実行される |
| Scenario Test（MockMvc 経由） | `DatabaseCleaner` | MockMvc は HTTP ごとに独立トランザクションをコミットする |

### DatabaseCleaner

MockMvc ベーステスト（Scenario Test 等）のデータ隔離ユーティリティ。

- `@BeforeEach` で `databaseCleaner.clean()` を呼び出す
- FK 依存順序で `DELETE FROM` を実行（`TRUNCATE` は MySQL InnoDB でデッドロックの原因となるため不使用）
- マスタデータも削除されるため、テスト側 `@BeforeEach` で再投入する

```java
@Autowired private DatabaseCleaner databaseCleaner;

@BeforeEach
void setUp() throws Exception {
    databaseCleaner.clean();
    // API 経由でテストデータをセットアップ
}
```

### TestSecurityConfig

`@WebMvcTest` 環境で認証をバイパスするテスト用セキュリティ設定。パスベースで Firebase 認証（一般エンドポイント）と API Key 認証（`/internal/**`）を切り替える。

### TestFixtures

Object Mother パターンによるテストデータ生成。エンティティの一貫したテストデータを提供する。

---

## SDD プロセスとの対応

```
通常 SDD:
  requirements.md → Scenario Test (RED)
  OpenAPI spec    → Controller Web Test (RED)
  domain rules    → Service Unit Test (RED)     ┐
  infrastructure  → Integration Test (RED)      ┘ TDD Implementation → GREEN

フル Reverse SDD:
  Implementation → requirements.md → Scenario Test (GREEN = 挙動保護)
                 → OpenAPI review  → Controller Web Test (GREEN)
                 → domain rules    → Service Unit Test (GREEN)
                 → infrastructure  → Integration Test (GREEN)
                                     (RED = バグ発見)
```

---

## 命名規約

| テスト種別 | クラス名パターン | ディレクトリ |
|-----------|----------------|-------------|
| Scenario | `{Feature}ScenarioTest` | `scenario/` |
| Controller Web | `{Controller}WebTest` | `controller/` |
| Service Unit | `{Service}UnitTest` / `{ServiceImpl}Test` | `service/` |
| Integration | `{Service}IntegrationTest` | `service/` |
| Security | `{Component}Test` / `{Component}UnitTest` | `security/` |
| Repository | `{Repository}Test` | `repository/` |
| API Client | `{Client}UnitTest` / `{Client}IntegrationTest` | `api/client/` |
| Utility | `{Class}Test` | 対応パッケージ |

---

## アンチパターン

| アンチパターン | 正しいアプローチ |
|-------------|--------------|
| Scenario Test で JSON スキーマの全フィールドを検証する | Controller Web Test でスキーマ検証する |
| Controller Web Test でビジネスロジックの正しさを検証する | Service Unit Test で検証する |
| Integration Test で Unit Test 済みのドメイン分岐を重複検証する | Unit Test でカバー済みなら含めない |
| Service Unit Test で Bean Validation を検証する | Controller Web Test（Web 層）で検証する |
| Scenario Test で Repository を直接使用してセットアップする | API 経由でセットアップする |
| テスト名に実装詳細（クラス名、メソッド名）を含める | 仕様用語（ドメイン言語）で命名する |
| 1 つのテストメソッドで複数の AC を検証する | AC ごとにテストメソッドを分離する |
