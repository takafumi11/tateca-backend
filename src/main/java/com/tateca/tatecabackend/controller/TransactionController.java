package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.TransactionCreationRequestDTO;
import com.tateca.tatecabackend.dto.response.TransactionDetailResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO;
import com.tateca.tatecabackend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tateca.tatecabackend.constants.ApiConstants.PATH_GROUPS;
import static com.tateca.tatecabackend.constants.ApiConstants.PATH_HISTORY;
import static com.tateca.tatecabackend.constants.ApiConstants.PATH_SETTLEMENT;
import static com.tateca.tatecabackend.constants.ApiConstants.PATH_TRANSACTIONS;

@RestController
@RequiredArgsConstructor
@RequestMapping(PATH_GROUPS + "/{groupId}" + PATH_TRANSACTIONS)
@Tag(name = "Transactions", description = "Transaction management operations")
public class TransactionController {
    private final TransactionService service;

    @GetMapping(PATH_HISTORY)
    @Operation(summary = "Get transaction history for a group")
    public ResponseEntity<TransactionsHistoryResponseDTO> getTransactionHistory(
            @RequestParam(defaultValue = "5") int count,
            @PathVariable UUID groupId
    ) {
        TransactionsHistoryResponseDTO response = service.getTransactionHistory(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(PATH_SETTLEMENT)
    @Operation(summary = "Get settlement information for a group")
    public ResponseEntity<TransactionsSettlementResponseDTO> getTransactionSettlement(
            @PathVariable UUID groupId
    ) {
        TransactionsSettlementResponseDTO response = service.getSettlements(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<TransactionDetailResponseDTO> createTransaction(
            @PathVariable("groupId") UUID groupId,
            @RequestBody TransactionCreationRequestDTO request
    ) {
        TransactionDetailResponseDTO response = service.createTransaction(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<TransactionDetailResponseDTO> getTransactionDetail(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId
    ) {
        TransactionDetailResponseDTO response = service.getTransactionDetail(transactionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<Void> deleteLoan(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId
    ) {
        service.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }
}
