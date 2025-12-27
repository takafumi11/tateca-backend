-- Migration: V3__Add_settlement_performance_indexes.sql
-- Purpose: Add database indexes to optimize settlement endpoint performance
-- Impact: Improves JOIN and WHERE clause performance for settlement queries
-- Related to: Settlement endpoint GET /groups/{groupId}/transactions/settlement

-- Index for JOIN on transaction_obligations.transaction_uuid
-- Used by: JOIN FETCH o.transaction in ObligationRepository.findByGroupId
-- Benefit: Speeds up obligation-to-transaction joins
CREATE INDEX idx_transaction_obligations_transaction_uuid
ON transaction_obligations(transaction_uuid);

-- Index for WHERE clause on transaction_history.group_uuid
-- Used by: WHERE t.group.uuid = :groupId in settlement queries
-- Benefit: Fast filtering by group in settlement calculation
CREATE INDEX idx_transaction_history_group_uuid
ON transaction_history(group_uuid);

-- Index for JOIN on transaction_history.payer_id
-- Used by: JOIN FETCH t.payer in ObligationRepository.findByGroupId
-- Benefit: Speeds up transaction-to-payer user joins
CREATE INDEX idx_transaction_history_payer_id
ON transaction_history(payer_id);

-- Index for JOIN on transaction_obligations.user_uuid
-- Used by: JOIN FETCH o.user in ObligationRepository.findByGroupId
-- Benefit: Speeds up obligation-to-user joins
CREATE INDEX idx_transaction_obligations_user_uuid
ON transaction_obligations(user_uuid);
