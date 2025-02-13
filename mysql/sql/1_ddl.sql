CREATE TABLE IF NOT EXISTS auth_users (
    uid VARCHAR(128) PRIMARY KEY,
    name VARCHAR(50),
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    total_login_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS users (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    name VARCHAR(50) NOT NULL,
    auth_user_uid VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (auth_user_uid) REFERENCES auth_users(uid)
);

CREATE TABLE IF NOT EXISTS `groups` (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    name VARCHAR(50) NOT NULL,
    join_token BINARY(16),
    join_token_text CHAR(36) AS (BIN_TO_UUID(join_token)) VIRTUAL,
    token_expires TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_groups (
    user_uuid BINARY(16),
    group_uuid BINARY(16),
    user_uuid_text CHAR(36) AS (BIN_TO_UUID(user_uuid)) VIRTUAL,
    group_uuid_text CHAR(36) AS (BIN_TO_UUID(group_uuid)) VIRTUAL,
    PRIMARY KEY (user_uuid, group_uuid),
    FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
    FOREIGN KEY (group_uuid) REFERENCES `groups`(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_history (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    transaction_type ENUM('LOAN', 'REPAYMENT') NOT NULL,
    group_uuid BINARY(16),
    group_uuid_text CHAR(36) AS (BIN_TO_UUID(group_uuid)) VIRTUAL,
    title VARCHAR(50),
    amount INT NOT NULL,
    currency_code CHAR(3) NOT NULL,
    currency_rate DECIMAL(9, 6) NOT NULL,
    date TIMESTAMP NOT NULL,
    payer_id BINARY(16) NOT NULL,
    payer_id_text CHAR(36) AS (BIN_TO_UUID(payer_id)) VIRTUAL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (group_uuid) REFERENCES `groups`(uuid),
    FOREIGN KEY (payer_id) REFERENCES users(uuid),
);

CREATE TABLE IF NOT EXISTS transaction_obligations (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    transaction_uuid BINARY(16) NOT NULL,
    transaction_uuid_text CHAR(36) AS (BIN_TO_UUID(transaction_uuid)) VIRTUAL,
    user_uuid BINARY(16) NOT NULL,
    user_uuid_text CHAR(36) AS (BIN_TO_UUID(user_uuid)) VIRTUAL,
    amount INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_uuid) REFERENCES transaction_history(uuid),
    FOREIGN KEY (user_uuid) REFERENCES users(uuid)
);

CREATE TABLE IF NOT EXISTS currency_names (
    currency_code CHAR(3) PRIMARY KEY,  -- 通貨コード (例: USD, EUR)
    jp_currency_name VARCHAR(50) NOT NULL, -- 日本語表示名 (例: "米ドル", "ユーロ")
    eng_currency_name VARCHAR(50) NOT NULL, -- 英語表示名 (例: "United States Dollar", "Euro")
    jp_country_name VARCHAR(50) NOT NULL, -- 日本語の国名 (例: "アメリカ合衆国", "日本")
    eng_country_name VARCHAR(50) NOT NULL,  -- 英語の国名 (例: "United States", "Japan")
    is_active TINYINT(1) NOT NULL -- 有効/無効を示すフラグ (1: 有効, 0: 無効)
);

CREATE TABLE IF NOT EXISTS exchange_rates (
    date DATE NOT NULL,                      -- 為替レートが適用される日付 (yyyy-mm-dd)
    currency_code CHAR(3) NOT NULL,          -- 通貨コード (例: "USD", "EUR")
    exchange_rate DECIMAL(18, 6) NOT NULL,   -- 換算レート (JPY に対するレート)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- レコード作成時刻
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- レコード更新時刻
    PRIMARY KEY (date, currency_code),        -- 複合主キー: 日付と通貨コードの組み合わせ
    FOREIGN KEY (currency_code)               -- 外部キー制約: 通貨コードがcurrency_namesテーブルに存在することを保証
    REFERENCES currency_names (currency_code)
    ON DELETE CASCADE                         -- 通貨名が削除されると、関連する為替レートも削除
);