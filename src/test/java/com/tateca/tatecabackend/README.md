# Testing Guidelines

このディレクトリには、Tateca Backendプロジェクトのすべてのテストコードが含まれています。

## テストルール

### ✅ アサーションライブラリ

**AssertJを使用してください（JUnitのアサーションは使用しない）**

```java
// ✅ 正しい（AssertJ）
import static org.assertj.core.api.Assertions.assertThat;

assertThat(result).isNotNull();
assertThat(result.getName()).isEqualTo("Test");
assertThat(result.getAmount()).isPositive();
assertThat(list).hasSize(3);
assertThat(list).contains(expected);

// ❌ 間違い（JUnit）
import static org.junit.jupiter.api.Assertions.*;

assertEquals("Test", result.getName());
assertTrue(result.getAmount() > 0);
```

**理由:**
- より読みやすい流暢なAPI
- 失敗時のエラーメッセージが詳細
- 豊富なアサーションメソッド

### 📋 テスト構成

#### Repository層
- `@DataJpaTest` + Testcontainers (MySQL)
- カスタムクエリ（@Query, Native Query）のみテスト

#### Service層
- **Unit Test**: Mockitoで依存をモック化
  - 目標: Conditional Coverage（分岐網羅率）95-100%
- **Integration Test**: Testcontainers + 実際のDB

#### Controller層
- **Integration Test**: @SpringBootTest + MockMvc + Testcontainers

### 🏗️ テスト構造（AAA パターン）

```java
@Test
@DisplayName("取引作成が成功する")
void createTransaction_Success() {
    // Arrange（準備）
    GroupEntity group = TestFixtures.Groups.standard();
    TransactionCreationRequestDTO request = createRequest();

    // Act（実行）
    TransactionDetailResponseDTO result = service.createTransaction(
        group.getUuid(),
        request
    );

    // Assert（検証）
    assertThat(result).isNotNull();
    assertThat(result.getAmount()).isEqualTo(1000);
}
```

### 🎨 命名規則

#### テストクラス名
```
<対象クラス名> + Test/IT
例: TransactionServiceTest, TransactionServiceIT
```

#### テストメソッド名
```
<メソッド名>_<条件>_<期待結果>
例: createTransaction_WhenValidRequest_ReturnsTransaction
    createTransaction_WhenInvalidAmount_ThrowsException
```

#### @DisplayName
```java
@DisplayName("日本語で分かりやすく記述")
```

### 🧰 テストユーティリティ

#### TestFixtures
標準的なテストデータを提供：

```java
// Object Mother パターン
GroupEntity group = TestFixtures.Groups.standard();
UserEntity user = TestFixtures.Users.standard();
CurrencyNameEntity jpy = TestFixtures.Currencies.jpy();
```

#### AbstractIntegrationTest
Integration Testの基底クラス：

```java
class MyIntegrationTest extends AbstractIntegrationTest {
    // Testcontainersが自動的にセットアップされる
}
```

### 📊 カバレッジ目標

- **Service層 Unit Test**: Branch Coverage 95-100%
- **Integration Test**: 主要フロー60%以上
- **全体**: 70%以上

### 🔧 テスト実行

```bash
# 全テスト実行
./gradlew test

# 特定のテストクラス実行
./gradlew test --tests "*TransactionServiceTest"

# カバレッジレポート生成
./gradlew test jacocoTestReport

# カバレッジレポート確認
open build/reports/jacoco/test/html/index.html
```

### 📝 その他のベストプラクティス

1. **各テストは独立している**
   - テスト間で状態を共有しない
   - 実行順序に依存しない

2. **テストは高速**
   - Unit Testは数ミリ秒で完了
   - 外部依存はモック化

3. **テストは読みやすい**
   - 1テスト1検証
   - @DisplayNameで意図を明確に

4. **失敗メッセージは明確**
   - AssertJを使って詳細なエラーメッセージ

## ディレクトリ構造

```
src/test/java/com/tateca/tatecabackend/
├── config/
│   ├── AbstractIntegrationTest.java  # Integration Test基底クラス
│   └── TestConfig.java                # テスト用Bean設定
├── fixtures/
│   └── TestFixtures.java              # テストデータファクトリ
├── repository/
│   └── *RepositoryTest.java           # Repository層テスト
├── service/
│   ├── *ServiceTest.java              # Service層Unit Test
│   └── *ServiceIT.java                # Service層Integration Test
├── controller/
│   └── *ControllerIT.java             # Controller層Integration Test
└── TatecaBackendApplicationTests.java # 基本テスト
```

## 参考リンク

- [AssertJ Documentation](https://assertj.github.io/doc/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers Documentation](https://testcontainers.com/)
