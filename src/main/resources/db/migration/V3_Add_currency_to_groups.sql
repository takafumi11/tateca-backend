ALTER TABLE `groups`
  ADD COLUMN currency_code CHAR(3)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
    NOT NULL DEFAULT 'JPY';

-- その後に外部キーを追加
ALTER TABLE `groups`
  ADD CONSTRAINT fk_groups_currency
  FOREIGN KEY (currency_code)
  REFERENCES currency_names(currency_code);