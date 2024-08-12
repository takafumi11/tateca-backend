CREATE TABLE IF NOT EXISTS auth_users (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    name VARCHAR(50),
    email VARCHAR(255) UNIQUE,
    uid VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    name VARCHAR(50) NOT NULL,
    FOREIGN KEY (auth_user_uuid) REFERENCES auth_users(uuid),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_auth_users (
    user_uuid BINARY(16),
    auth_user_uuid BINARY(16),
    user_uuid_text CHAR(36) AS (BIN_TO_UUID(user_uuid)) VIRTUAL,
    auth_user_uuid_text CHAR(36) AS (BIN_TO_UUID(auth_user_uuid)) VIRTUAL,
    PRIMARY KEY (user_uuid, auth_user_uuid),
    FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
    FOREIGN KEY (auth_user_uuid) REFERENCES auth_users(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `groups` (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    name VARCHAR(50) NOT NULL,
    join_token BINARY(16),
    join_token_text CHAR(36) AS (BIN_TO_UUID(join_token)) VIRTUAL,
    token_expires DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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

CREATE TABLE IF NOT EXISTS loans (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    group_uuid BINARY(16),
    group_uuid_text CHAR(36) AS (BIN_TO_UUID(group_uuid)) VIRTUAL,
    title VARCHAR(50),
    amount INT NOT NULL,
    currency_code CHAR(3) NOT NULL,
    currency_rate DECIMAL(9, 6) NOT NULL,
    date DATETIME NOT NULL,
    payer_id BINARY(16) NOT NULL,
    payer_id_text CHAR(36) AS (BIN_TO_UUID(payer_id)) VIRTUAL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (payer_id) REFERENCES users(uuid),
    FOREIGN KEY (group_uuid) REFERENCES `groups`(uuid)
);

CREATE TABLE IF NOT EXISTS loan_obligations (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    loan_uuid BINARY(16) NOT NULL,
    user_uuid BINARY(16) NOT NULL,
    loan_uuid_text CHAR(36) AS (BIN_TO_UUID(loan_uuid)) VIRTUAL,
    user_uuid_text CHAR(36) AS (BIN_TO_UUID(user_uuid)) VIRTUAL,
    amount INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_uuid) REFERENCES loans(uuid),
    FOREIGN KEY (user_uuid) REFERENCES users(uuid)
);

CREATE TABLE IF NOT EXISTS repayments (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    group_uuid BINARY(16),
    group_uuid_text CHAR(36) AS (BIN_TO_UUID(group_uuid)) VIRTUAL,
    title VARCHAR(50),
    amount INT NOT NULL,
    currency_code CHAR(3) NOT NULL,
    currency_rate DECIMAL(9, 6) NOT NULL,
    date DATETIME NOT NULL,
    payer_id BINARY(16) NOT NULL,
    recipient_user_id BINARY(16) NOT NULL,
    payer_id_text CHAR(36) AS (BIN_TO_UUID(payer_id)) VIRTUAL,
    recipient_user_id_text CHAR(36) AS (BIN_TO_UUID(recipient_user_id)) VIRTUAL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (payer_id) REFERENCES users(uuid),
    FOREIGN KEY (recipient_user_id) REFERENCES users(uuid),
    FOREIGN KEY (group_uuid) REFERENCES `groups`(uuid)
);