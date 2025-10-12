-- Add currency_code column to users table
ALTER TABLE users
  ADD COLUMN currency_code CHAR(3)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
    NOT NULL DEFAULT 'JPY';

-- Add foreign key constraint for users.currency_code
ALTER TABLE users
  ADD CONSTRAINT fk_users_currency
  FOREIGN KEY (currency_code)
  REFERENCES currency_names(currency_code);

-- Remove foreign key constraint from groups table
ALTER TABLE `groups`
  DROP FOREIGN KEY fk_groups_currency;

-- Remove currency_code column from groups table
ALTER TABLE `groups`
  DROP COLUMN currency_code;
