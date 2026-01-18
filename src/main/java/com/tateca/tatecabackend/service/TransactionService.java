package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;

import java.util.UUID;

/**
 * Service interface for managing transactions, settlements, and transaction history.
 */
public interface TransactionService {

    /**
     * Retrieves transaction history for a group.
     *
     * @param count the maximum number of transactions to retrieve
     * @param groupId the UUID of the group
     * @return transaction history response containing the list of transactions
     */
    TransactionHistoryResponseDTO getTransactionHistory(int count, UUID groupId);

    /**
     * Calculates settlements for a group in JPY.
     *
     * @param groupId the UUID of the group
     * @return settlement response containing optimized transactions in JPY
     */
    TransactionSettlementResponseDTO getSettlements(UUID groupId);

    /**
     * Creates a new transaction (LOAN or REPAYMENT).
     *
     * @param groupId the UUID of the group
     * @param request the transaction creation request containing type, amount, currency, and details
     * @return transaction detail response with created transaction and obligations
     */
    CreateTransactionResponseDTO createTransaction(UUID groupId, CreateTransactionRequestDTO request);

    /**
     * Retrieves detailed information about a specific transaction.
     *
     * @param transactionId the UUID of the transaction
     * @return transaction detail response including transaction info and obligations
     */
    CreateTransactionResponseDTO getTransactionDetail(UUID transactionId);

    /**
     * Deletes a transaction and its associated obligations.
     *
     * @param transactionId the UUID of the transaction to delete
     */
    void deleteTransaction(UUID transactionId);

    /**
     * Updates an existing LOAN transaction.
     * Only LOAN transactions can be updated; REPAYMENT transactions are immutable.
     *
     * @param transactionId the UUID of the transaction to update
     * @param request the transaction update request containing new LOAN values
     * @return transaction detail response with updated transaction and obligations
     * @throws IllegalArgumentException if attempting to update a REPAYMENT transaction
     * @throws com.tateca.tatecabackend.exception.domain.EntityNotFoundException if transaction not found
     */
    CreateTransactionResponseDTO updateTransaction(UUID transactionId, UpdateTransactionRequestDTO request);
}
