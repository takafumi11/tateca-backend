CREATE TABLE IF NOT EXISTS users (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) UNIQUE,
    auth_user_id VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
    PRIMARY KEY (user_uuid, group_uuid),
    FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
    FOREIGN KEY (group_uuid) REFERENCES `groups`(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS loans (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    group_uuid BINARY(16),
    title VARCHAR(50),
    amount DECIMAL(10,2) NOT NULL,
    date DATETIME NOT NULL,
    payer_id BINARY(16) NOT NULL,
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
    amount DECIMAL(10,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_uuid) REFERENCES loans(uuid),
    FOREIGN KEY (user_uuid) REFERENCES users(uuid)
);

CREATE TABLE IF NOT EXISTS repayment_history (
    uuid BINARY(16) PRIMARY KEY,
    uuid_text CHAR(36) AS (BIN_TO_UUID(uuid)) VIRTUAL,
    group_uuid BINARY(16),
    title VARCHAR(50),
    amount DECIMAL(10,2) NOT NULL,
    date DATETIME NOT NULL,
    payer_id BINARY(16) NOT NULL,
    recipient_user_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (payer_id) REFERENCES users(uuid),
    FOREIGN KEY (recipient_user_id) REFERENCES users(uuid),
    FOREIGN KEY (group_uuid) REFERENCES `groups`(uuid)
);