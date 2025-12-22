# Tateca API Specifications

このディレクトリには、Tateca APIのOpenAPI仕様とPostmanコレクションが含まれています。

## 📁 ディレクトリ構成

```
api-specs/
├── openapi/                          # OpenAPI仕様（モジュラー構造）
│   ├── openapi.yaml                  # メインOpenAPIファイル
│   ├── components/                   # 再利用可能コンポーネント
│   │   ├── info.yaml                # API情報
│   │   ├── security.yaml            # セキュリティスキーム
│   │   ├── parameters/              # パラメータ定義
│   │   ├── responses/               # レスポンス定義
│   │   └── schemas/                 # データモデル
│   └── paths/                       # エンドポイント定義
├── postman/                         # Postmanコレクションと環境設定
│   ├── collections/                 # コレクションファイル
│   │   ├── Tateca Backend.postman_collection.json    # 手動管理コレクション
│   │   └── tateca-api-generated.postman_collection.json  # 自動生成（Git管理外）
│   └── environments/                # 環境変数
│       ├── local.postman_environment.json         # ローカル環境
│       └── production.postman_environment.json    # 本番環境
├── docs/                            # 生成されたドキュメント（Git管理外）
├── dist/                            # バンドル済み仕様（Git管理外）
├── scripts/                         # ユーティリティスクリプト
│   └── generate-postman.js          # OpenAPI→Postman変換
├── .redocly.yaml                    # Redocly設定
└── package.json                     # npm依存関係とスクリプト
```

## 🚀 セットアップ

### 1. 依存関係のインストール

```bash
cd api-specs
npm install
```

### 2. OpenAPI仕様の検証

```bash
npm run lint
```

## 📝 開発ワークフロー

### OpenAPI仕様の編集

1. **仕様ファイルを編集**
   ```bash
   # エンドポイントの追加/変更
   vim openapi/paths/groups-groupId.yaml

   # スキーマの追加/変更
   vim openapi/components/schemas/GroupDto.yaml
   ```

2. **仕様を検証**
   ```bash
   npm run lint
   ```

3. **仕様をバンドル**
   ```bash
   npm run bundle
   ```

4. **ドキュメントを生成**
   ```bash
   npm run build-docs
   ```

5. **Postmanコレクションを自動生成**
   ```bash
   npm run generate:postman
   ```

### ドキュメントのプレビュー

```bash
# インタラクティブプレビュー
npm run preview

# 静的ドキュメントを生成してサーブ
npm run dev
```

ブラウザで `http://localhost:8080` を開くとドキュメントが表示されます。

## 🔧 利用可能なnpmスクリプト

### OpenAPI関連

- `npm run lint` - OpenAPI仕様の検証
- `npm run bundle` - モジュラー仕様を単一YAMLにバンドル
- `npm run validate` - JSON形式で検証結果を出力（CI/CD用）
- `npm run stats` - 仕様の統計情報を表示

### ドキュメント生成

- `npm run build-docs` - HTMLドキュメント生成
- `npm run build-docs:redoc` - Redoc形式ドキュメント生成
- `npm run preview` - インタラクティブプレビュー
- `npm run serve` - 生成済みドキュメントをサーブ
- `npm run dev` - フルワークフロー（bundle + docs + serve）

### Postman関連

- `npm run generate:postman` - OpenAPIからPostmanコレクション生成
- `npm run postman:validate` - Postmanコレクションの検証（Newman使用）

### ユーティリティ

- `npm run clean` - 生成ファイルのクリーンアップ
- `npm run build:all` - すべてをビルド（clean + bundle + docs）
- `npm test` - CI/CD用テスト（lint + bundle）
- `npm run ci` - CI/CD完全ワークフロー

## 🔄 CI/CDとの統合

GitHub Actionsワークフローが以下を自動実行します：

1. **プルリクエスト時**
   - OpenAPI仕様の検証（lint）
   - 仕様のバンドル
   - ドキュメント生成
   - Postmanコレクション生成

2. **mainブランチマージ時**
   - 上記すべて
   - GitHub Pagesへのドキュメントデプロイ

## 📦 Postmanコレクションの使用

### インポート方法

1. Postmanを開く
2. **Import** をクリック
3. `postman/collections/Tateca Backend.postman_collection.json` を選択
4. 環境設定をインポート：
   - ローカル開発: `postman/environments/local.postman_environment.json`
   - 本番環境: `postman/environments/production.postman_environment.json`

### 環境変数

各環境で以下の変数が利用可能です：

**ローカル環境 (local)**
- `baseUrl`: `http://localhost:8080`
- `GroupId`: テスト用グループID
- `transactionId`: テスト用トランザクションID
- `userId`: テスト用ユーザーID
- `apiKey`: テスト用APIキー

**本番環境 (production)**
- `baseUrl`: `https://api.tateca.com`
- その他の変数は環境に応じて設定

## 🎯 ベストプラクティス

### 1. OpenAPI仕様の変更時

- モジュラーファイル（`openapi/paths/`, `openapi/components/`）を直接編集
- バンドル済みファイル（`dist/`）は編集しない（自動生成される）
- 変更後は必ず `npm run lint` で検証

### 2. Postmanコレクション

- 手動コレクション (`Tateca Backend.postman_collection.json`) は必要に応じて更新
- 自動生成コレクション (`*-generated.postman_collection.json`) は編集しない
- 環境変数は機密情報を含まないように注意

### 3. バージョン管理

- OpenAPI仕様のソースファイルのみコミット
- 生成ファイル（`dist/`, `docs/`, `node_modules/`）は`.gitignore`で除外
- 自動生成Postmanコレクションも除外

## 🔍 トラブルシューティング

### `npm run lint` が失敗する

```bash
# 詳細なエラー情報を確認
npm run lint -- --format=stylish

# 特定のルールを無効化したい場合は .redocly.yaml を編集
```

### ドキュメント生成が失敗する

```bash
# まずバンドルを確認
npm run bundle

# バンドル済みファイルが正しく生成されているか確認
cat dist/tateca-api.yaml
```

### Postman自動生成が失敗する

```bash
# バンドルファイルが存在するか確認
ls -la dist/tateca-api.yaml

# 存在しない場合はバンドルを実行
npm run bundle && npm run generate:postman
```

## 📚 参考リンク

- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Redocly CLI Documentation](https://redocly.com/docs/cli/)
- [Postman Collection Format](https://www.postman.com/collection/)
- [Newman (Postman CLI)](https://learning.postman.com/docs/running-collections/using-newman-cli/command-line-integration-with-newman/)

## 🤝 コントリビューション

API仕様の変更時は以下の手順に従ってください：

1. フィーチャーブランチを作成
2. OpenAPI仕様を編集
3. `npm run lint` で検証
4. `npm run build:all` ですべてをビルド
5. プルリクエストを作成
6. CI/CDパイプラインが成功することを確認
