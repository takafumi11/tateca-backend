package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.TransactionCreationRequestDTO;
import com.tateca.tatecabackend.dto.response.TransactionDetailResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO;

import java.util.UUID;

/**
 * Service interface for managing transactions, settlements, and transaction history.
 *
 * <p>This service handles:
 * <ul>
 *   <li>Transaction creation (LOAN and REPAYMENT)</li>
 *   <li>Settlement calculation with rounding error adjustment</li>
 *   <li>Transaction history retrieval</li>
 *   <li>Transaction detail queries</li>
 *   <li>Transaction deletion</li>
 * </ul>
 */
public interface TransactionService {

    /**
     * Retrieves transaction history for a group.
     *
     * @param count the maximum number of transactions to retrieve
     * @param groupId the UUID of the group
     * @return transaction history response containing the list of transactions
     */
    TransactionsHistoryResponseDTO getTransactionHistory(int count, UUID groupId);

    /**
     * Calculates settlements for a group in JPY.
     *
     * <p>This method performs the following:
     * <ul>
     *   <li>Fetches all obligations for the group</li>
     *   <li>Converts all amounts to JPY</li>
     *   <li>Adjusts rounding errors by assigning the difference to the maximum debtor</li>
     *   <li>Optimizes settlement transactions to minimize the number of transfers</li>
     * </ul>
     *
     * <p><strong>Rounding Error Adjustment:</strong><br>
     * When converting foreign currency loans to JPY, individual
     * obligation amounts may not sum to exactly the total loan amount due to rounding.
     * This implementation groups obligations by transaction and adjusts the difference
     * to the maximum debtor, ensuring perfect repayment without ±1 cent discrepancies.
     *
     * @param groupId the UUID of the group
     * @return settlement response containing optimized transactions in JPY
     */
    TransactionsSettlementResponseDTO getSettlements(UUID groupId);

    /**
     * Creates a new transaction (LOAN or REPAYMENT).
     *
     * @param groupId the UUID of the group
     * @param request the transaction creation request containing type, amount, currency, and details
     * @return transaction detail response with created transaction and obligations
     */
    TransactionDetailResponseDTO createTransaction(UUID groupId, TransactionCreationRequestDTO request);

    /**
     * Retrieves detailed information about a specific transaction.
     *
     * @param transactionId the UUID of the transaction
     * @return transaction detail response including transaction info and obligations
     */
    TransactionDetailResponseDTO getTransactionDetail(UUID transactionId);

    /**
     * Deletes a transaction and its associated obligations.
     *
     * @param transactionId the UUID of the transaction to delete
     */
    void deleteTransaction(UUID transactionId);
}
