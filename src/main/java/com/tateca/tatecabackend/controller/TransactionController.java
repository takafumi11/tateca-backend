package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups/{groupId}/transactions")
@Tag(name = "Transactions", description = "Transaction management operations")
public class TransactionController {
    private final TransactionService service;

    @GetMapping("/history")
    @Operation(summary = "Get transaction history for a group")
    public ResponseEntity<TransactionHistoryResponseDTO> getTransactionHistory(
            @RequestParam(defaultValue = "5") int count,
            @PathVariable UUID groupId
    ) {
        TransactionHistoryResponseDTO response = service.getTransactionHistory(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settlement")
    @Operation(summary = "Get settlement information for a group in JPY")
    public ResponseEntity<TransactionSettlementResponseDTO> getTransactionSettlement(
            @PathVariable UUID groupId
    ) {
        TransactionSettlementResponseDTO response = service.getSettlements(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<CreateTransactionResponseDTO> createTransaction(
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody CreateTransactionRequestDTO request
    ) {
        CreateTransactionResponseDTO response = service.createTransaction(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<CreateTransactionResponseDTO> getTransactionDetail(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId
    ) {
        CreateTransactionResponseDTO response = service.getTransactionDetail(transactionId);
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
