-- Add app review dialog management columns to auth_users table
ALTER TABLE auth_users 
ADD COLUMN last_app_review_dialog_shown_at TIMESTAMP NULL,
ADD COLUMN app_review_status ENUM('PENDING', 'COMPLETED', 'PERMANENTLY_DECLINED') DEFAULT 'PENDING';