-- Add currency_code column to users table
-- This links users to their preferred currency

ALTER TABLE users
ADD COLUMN currency_code CHAR(3) NOT NULL DEFAULT 'JPY',
ADD CONSTRAINT fk_users_currency_code
    FOREIGN KEY (currency_code) REFERENCES currency_names(currency_code);

-- Create index for better join performance
CREATE INDEX idx_users_currency_code ON users(currency_code);
