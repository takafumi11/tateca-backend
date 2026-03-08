# Testing Strategy

## Overview

テストを **外部仕様テスト** と **内部仕様テスト** に分類し、各レイヤーの責務を明確に分離する。
テスト間で検証内容を重複させず、各テストが「そのレイヤーでしか検証できないこと」に集中する。

```
┌─────────────────────────────────────────────────┐
│         外部仕様テスト（ブラックボックス）            │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │ Scenario Test (Acceptance)                │  │
│  │ → ビジネス要件の充足をクライアント視点で検証   │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │ Controller Web Test (@WebMvcTest)         │  │
│  │ → HTTP インターフェース契約の準拠を検証      │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
├─────────────────────────────────────────────────┤
│         内部仕様テスト（ホワイトボックス）            │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │ Service Unit Test                         │  │
│  │ → ドメインロジックの正しさを検証             │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │ Integration Test (Persistence)            │  │
│  │ → 永続化レイヤーの振る舞いを実 DB で検証     │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

## 外部仕様テスト

### Scenario Test (Acceptance)

**視点:** クライアントのブラックボックス視点

**責務:**
- `requirements.md` の Acceptance Criteria が HTTP レベルで充足されることを検証する
- クライアントが実際に行う API 呼び出しフローを忠実に再現する

**非責務:**
- レスポンスの JSON スキーマ構造検証（Controller Web Test の責務）
- 内部実装の詳細（Repository 呼び出し回数、内部例外の型など）

**原則:**
- セットアップと検証は全て HTTP エンドポイント経由で行う（Repository 直接参照禁止）
- **マスタデータの例外:** `currencies` テーブルおよび `exchange_rates` テーブルのみ、対応する API が存在しないため `@BeforeEach` 内で Repository 経由の直接投入を許可する。これら以外のテーブルへの直接アクセスは禁止
- テスト名は Req/AC に 1:1 でマッピングする
- requirements.md の AC に対応しない技術的なエッジケースは `TechnicalEdgeCases` に分離する

**基盤:** `AbstractIntegrationTest` + `@AutoConfigureMockMvc`（フルスタック起動）

**命名規則:** `{Feature}ScenarioTest`

**ディレクトリ:** `src/test/java/.../scenario/`

---

### Controller Web Test

**視点:** HTTP インターフェースの契約

**責務:**
- レスポンスが OpenAPI スキーマに構造的に準拠していることを検証する
- 各ステータスコードのレスポンス構造（必須フィールド、`error_code`、`errors` 配列）を検証する
- Bean Validation が正しく発火すること（DTO の正規化を含む）を検証する
- Service が例外を投げた場合に正しい HTTP レスポンスに変換されることを検証する
- Service への正しい委譲（引数、呼び出し回数）を検証する

**非責務:**
- ビジネスロジックの正しさ（Service はモック）
- データの永続化確認
- 認証フィルターの動作確認

**DTO 単体テストについて:**
DTO 単体テストは設けない。

**基盤:** `@WebMvcTest` + `@MockitoBean`（Web 層のみ起動）

**命名規則:** `{Controller}WebTest`

**ディレクトリ:** `src/test/java/.../controller/`

---

## 内部仕様テスト

### Service Unit Test

**視点:** ドメインロジックのホワイトボックス

**責務:**
- Service 層のドメインロジック（認可、存在確認、冪等判定、状態遷移）が正しく機能することを検証する
- Repository との相互作用（呼び出し有無、引数、順序）を検証する

**非責務:**
- HTTP レスポンス形式
- 認証
- データベース永続化の実挙動
- 正規化・バリデーション（DTO 層 → Controller Web Test の責務）

**基盤:** Mockito（`@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`）

**命名規則:** `{ServiceImpl}Test` または `{Service}UnitTest`

**ディレクトリ:** `src/test/java/.../service/impl/` または `src/test/java/.../service/`

---

### Integration Test (Persistence)

**視点:** インフラストラクチャ結合

**責務:**
- Unit Test では検証不可能な永続化レイヤーの振る舞いを実 DB で検証する

**具体的な検証対象:**
- `@PreUpdate` / `@PrePersist` によるタイムスタンプ自動更新
- save スキップ時にタイムスタンプが変更されないことの実 DB 証明
- マルチバイト文字・絵文字の DB エンコーディング
- エンティティリレーションの保持
- トランザクション境界の正しさ
- 外部 API クライアントのリトライ・フォールバック動作（WireMock）

**非責務:**
- ドメインロジックの正しさ（Service Unit Test で完全カバー済みのケースを重複しない）
- HTTP レスポンス形式

**重複排除の原則:**
Service Unit Test で検証済みの以下のケースは Integration Test に含めない:
- 例外送出
- 入力値のバウンダリ（min/max length など）
- モック可能なビジネスロジック分岐

**基盤:** `AbstractIntegrationTest`（Testcontainers MySQL + WireMock）

**命名規則:** `{Service}IntegrationTest`

**ディレクトリ:** `src/test/java/.../service/`

---

## テスト間の責務境界

各検証項目がどのテストレイヤーに属するかの判断基準(例えば):

| 問い | 該当テスト |
|------|-----------|
| クライアントが AC を満たせるか？ | Scenario Test |
| HTTP レスポンスが OpenAPI に準拠するか？ | Controller Web Test |
| Bean Validation が正しく発火するか？ | Controller Web Test |
| ドメインロジックの分岐は正しいか？ | Service Unit Test |
| Repository が正しく呼ばれるか？ | Service Unit Test |
| 実 DB で永続化が正しいか？ | Integration Test |
| `@PreUpdate` が期待通り動くか？ | Integration Test |
| 文字エンコーディングは正しいか？ | Integration Test |

**重複テストを避ける原則:** 上位テスト（Scenario）で検証済みのビジネス結果を下位テスト（Integration）で再検証しない。下位テスト（Unit）で検証済みのロジック分岐を上位テスト（Integration）で再検証しない。各テストは「そのレイヤーでしか検証できないこと」に集中する。

---

## テスト基盤

### AbstractIntegrationTest

Scenario Test と Integration Test の共通基盤。Testcontainers で MySQL と WireMock を起動する。

- `@SpringBootTest` でフルコンテキスト起動
- `@Isolated` で他のテストクラスとの並列実行を防止（全サブクラスが同一の Testcontainers MySQL を共有するため）
- `flushAndClear()` で Hibernate キャッシュを排除し、DB の実状態を検証

**`@Transactional` の使い分け:**

`AbstractIntegrationTest` 自体には `@Transactional` を付けない。テストの種別によって使い分ける。

| テスト種別 | データ隔離方式 | 理由 |
|-----------|-------------|------|
| Service を直接呼ぶ Integration Test | 個別クラスに `@Transactional` を付与 | Service メソッドがテストと同じトランザクション内で実行されるため、ロールバックが有効 |
| MockMvc ベースの Scenario Test | `DatabaseCleaner` で `@BeforeEach` に DELETE | MockMvc は HTTP リクエストごとに独立したトランザクションをコミットするため、テストの `@Transactional` ロールバックが効かない |

### DatabaseCleaner

MockMvc ベースのテスト（Scenario Test 等）でテスト間のデータ隔離を保証するユーティリティ。

- `@BeforeEach` で `databaseCleaner.clean()` を呼び出す
- FK 依存順序で `DELETE FROM` を実行する（`TRUNCATE` ではなく `DELETE` を使用。`TRUNCATE` は DDL であり、MySQL InnoDB で並行トランザクションのテーブルメタデータを無効化してデッドロックを引き起こすため）
- マスタデータ（`currencies`, `exchange_rates`）も DELETE 対象に含まれるため、必要なマスタデータはテスト側の `@BeforeEach` で再投入する

```java
@Autowired private DatabaseCleaner databaseCleaner;

@BeforeEach
void setUp() throws Exception {
    databaseCleaner.clean();
    // インスタンスフィールドを全て null に初期化
    // API 経由でテストデータをセットアップ
}
```

### TestSecurityConfig

`@WebMvcTest` 環境で認証をバイパスするテスト用セキュリティ設定。
パスベースで `FirebaseAuthentication`（一般エンドポイント）と `ApiKeyAuthentication`（`/internal/**`）を切り替える。

### TestFixtures

Object Mother パターンによるテストデータ生成。エンティティの一貫したテストデータを提供する。

---

## 命名規約まとめ

| テスト種別 | クラス名パターン | ディレクトリ |
|-----------|----------------|-------------|
| Scenario | `{Feature}ScenarioTest` | `scenario/` |
| Controller Web | `{Controller}WebTest` | `controller/` |
| Service Unit | `{ServiceImpl}Test` / `{Service}UnitTest` | `service/impl/` or `service/` |
| Integration | `{Service}IntegrationTest` | `service/` |
