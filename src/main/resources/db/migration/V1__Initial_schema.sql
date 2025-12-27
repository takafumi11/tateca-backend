-- ============================================================================
-- Migration: V1__Initial_schema.sql
-- Purpose: Complete production schema baseline (as of 2025-12-27)
-- Source: Production database dump from Railway
-- Note: This schema exactly matches production, including duplicate indexes
--       on transaction_obligations table (will be cleaned up in future)
-- ============================================================================

--
-- Table structure for table `auth_users`
--

CREATE TABLE IF NOT EXISTS `auth_users` (
  `uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_time` timestamp NULL DEFAULT NULL,
  `total_login_count` int DEFAULT '0',
  `last_app_review_dialog_shown_at` timestamp NULL DEFAULT NULL,
  `app_review_status` enum('PENDING','COMPLETED','PERMANENTLY_DECLINED') COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  PRIMARY KEY (`uid`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `currency_names`
--

CREATE TABLE IF NOT EXISTS `currency_names` (
  `currency_code` char(3) NOT NULL,
  `jp_currency_name` varchar(50) NOT NULL,
  `eng_currency_name` varchar(50) NOT NULL,
  `jp_country_name` varchar(50) NOT NULL,
  `eng_country_name` varchar(50) NOT NULL,
  `is_active` tinyint NOT NULL,
  `currency_symbol` varchar(10) DEFAULT NULL,
  `symbol_position` enum('PREFIX','SUFFIX') DEFAULT NULL,
  PRIMARY KEY (`currency_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `exchange_rates`
--

CREATE TABLE IF NOT EXISTS `exchange_rates` (
  `date` date NOT NULL,
  `currency_code` char(3) NOT NULL,
  `exchange_rate` decimal(18,6) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`date`,`currency_code`),
  KEY `currency_code` (`currency_code`),
  CONSTRAINT `exchange_rates_ibfk_1` FOREIGN KEY (`currency_code`) REFERENCES `currency_names` (`currency_code`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `groups`
--

CREATE TABLE IF NOT EXISTS `groups` (
  `uuid` binary(16) NOT NULL,
  `uuid_text` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`uuid`)) VIRTUAL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `join_token` binary(16) DEFAULT NULL,
  `join_token_text` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`join_token`)) VIRTUAL,
  `token_expires` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `uuid` binary(16) NOT NULL,
  `uuid_text` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`uuid`)) VIRTUAL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `auth_user_uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  KEY `auth_user_uid` (`auth_user_uid`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`auth_user_uid`) REFERENCES `auth_users` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `user_groups`
--

CREATE TABLE IF NOT EXISTS `user_groups` (
  `user_uuid` binary(16) NOT NULL,
  `group_uuid` binary(16) NOT NULL,
  `user_uuid_text` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`user_uuid`)) VIRTUAL,
  `group_uuid_text` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`group_uuid`)) VIRTUAL,
  PRIMARY KEY (`user_uuid`,`group_uuid`),
  KEY `group_uuid` (`group_uuid`),
  CONSTRAINT `user_groups_ibfk_1` FOREIGN KEY (`user_uuid`) REFERENCES `users` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `user_groups_ibfk_2` FOREIGN KEY (`group_uuid`) REFERENCES `groups` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `transaction_history`
--

CREATE TABLE IF NOT EXISTS `transaction_history` (
  `uuid` binary(16) NOT NULL,
  `uuid_text` char(36) GENERATED ALWAYS AS (bin_to_uuid(`uuid`)) VIRTUAL,
  `transaction_type` enum('LOAN','REPAYMENT') NOT NULL,
  `group_uuid` binary(16) DEFAULT NULL,
  `group_uuid_text` char(36) GENERATED ALWAYS AS (bin_to_uuid(`group_uuid`)) VIRTUAL,
  `title` varchar(50) DEFAULT NULL,
  `amount` int NOT NULL,
  `currency_code` char(3) NOT NULL,
  `exchange_rate_date` date NOT NULL,
  `transaction_date` timestamp NOT NULL,
  `payer_id` binary(16) NOT NULL,
  `payer_id_text` char(36) GENERATED ALWAYS AS (bin_to_uuid(`payer_id`)) VIRTUAL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  KEY `idx_transaction_history_group_uuid` (`group_uuid`),
  KEY `idx_transaction_history_payer_id` (`payer_id`),
  CONSTRAINT `transaction_history_ibfk_1` FOREIGN KEY (`group_uuid`) REFERENCES `groups` (`uuid`),
  CONSTRAINT `transaction_history_ibfk_2` FOREIGN KEY (`payer_id`) REFERENCES `users` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `transaction_obligations`
--
-- NOTE: This table has duplicate indexes as they exist in production:
-- - transaction_uuid: 3 indexes (user_uuid, idx_transaction_uuid, idx_transaction_obligations_transaction_uuid)
-- - user_uuid: 2 indexes (user_uuid, idx_transaction_obligations_user_uuid)
-- These duplicates are preserved to match production exactly and can be cleaned up in a future migration

CREATE TABLE IF NOT EXISTS `transaction_obligations` (
  `uuid` binary(16) NOT NULL,
  `uuid_text` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`uuid`)) VIRTUAL,
  `transaction_uuid` binary(16) NOT NULL,
  `transaction_uuid_text` char(36) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`transaction_uuid`)) VIRTUAL,
  `user_uuid` binary(16) NOT NULL,
  `user_uuid_text` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (bin_to_uuid(`user_uuid`)) VIRTUAL,
  `amount` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  KEY `user_uuid` (`user_uuid`),
  KEY `idx_transaction_uuid` (`transaction_uuid`),
  KEY `idx_transaction_obligations_transaction_uuid` (`transaction_uuid`),
  KEY `idx_transaction_obligations_user_uuid` (`user_uuid`),
  CONSTRAINT `transaction_obligations_ibfk_1` FOREIGN KEY (`transaction_uuid`) REFERENCES `transaction_history` (`uuid`),
  CONSTRAINT `transaction_obligations_ibfk_2` FOREIGN KEY (`user_uuid`) REFERENCES `users` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
