-- Inserting into users
INSERT INTO users(uuid, name, email, auth_user_id)
VALUES
(@uuid100 := UUID_TO_BIN(UUID(), 1), '山田太郎', 'taro.yamada@example.com', 'authIdFromFirebase'),
(@uuid101 := UUID_TO_BIN(UUID(), 1), '佐藤花子', NULL, NULL);

-- Inserting into groups
INSERT INTO `groups`(uuid, name)
VALUES
(@uuid200 := UUID_TO_BIN(UUID(), 1), 'バックエンド開発');

-- Inserting into user_groups
INSERT INTO user_groups(user_uuid, group_uuid)
VALUES
(@uuid100, @uuid200),
(@uuid101, @uuid200);

-- Inserting into loans
INSERT INTO loans(uuid, group_uuid, title, amount, currency_code, currency_rate, date, payer_id)
VALUES
(@uuid300 := UUID_TO_BIN(UUID(), 1), @uuid200, 'リゴレット', 3600, 'JPY', 1.0, '2024-01-02 03:12:38', @uuid100);

-- Inserting into loan_obligations
INSERT INTO loan_obligations(uuid, loan_uuid, user_uuid, amount)
VALUES
(@uuid400 := UUID_TO_BIN(UUID(), 1), @uuid300, @uuid101, 1800);

-- Inserting into repayments
INSERT INTO repayments(uuid, group_uuid, title, amount, currency_code, currency_rate, date, payer_id, recipient_user_id)
VALUES
(@uuid500 := UUID_TO_BIN(UUID(), 1), @uuid200, '一部返済', 1000, 'KRW', 8.7472, '2024-01-03 03:12:38', @uuid101, @uuid100);
