-- ============================================================================
-- Migration: V2__Rename_currency_names_to_currencies.sql
-- Purpose: Rename currency_names table to currencies for better naming convention
-- Date: 2026-01-02
-- ============================================================================

-- Rename table (automatically updates foreign key references in MySQL)
RENAME TABLE currency_names TO currencies;
