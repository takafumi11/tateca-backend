-- Drop currency_code column from users table
-- This column exists in production but is not used by the client

-- Drop foreign key constraint if it exists
SET @fk_exists = (SELECT COUNT(*)
                  FROM information_schema.TABLE_CONSTRAINTS
                  WHERE CONSTRAINT_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'users'
                    AND CONSTRAINT_NAME = 'fk_users_currency_code'
                    AND CONSTRAINT_TYPE = 'FOREIGN KEY');

SET @drop_fk_query = IF(@fk_exists > 0,
                         'ALTER TABLE users DROP FOREIGN KEY fk_users_currency_code',
                         'SELECT "Foreign key does not exist"');

PREPARE stmt FROM @drop_fk_query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop column if it exists
SET @column_exists = (SELECT COUNT(*)
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                        AND TABLE_NAME = 'users'
                        AND COLUMN_NAME = 'currency_code');

SET @drop_column_query = IF(@column_exists > 0,
                             'ALTER TABLE users DROP COLUMN currency_code',
                             'SELECT "Column does not exist"');

PREPARE stmt FROM @drop_column_query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
