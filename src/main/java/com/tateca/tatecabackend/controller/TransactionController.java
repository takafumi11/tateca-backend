package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.TransactionCreationRequestDTO;
import com.tateca.tatecabackend.dto.response.TransactionDetailResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponse;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponse;
import com.tateca.tatecabackend.service.TransactionService;
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
public class TransactionController {
    private final TransactionService service;

    @GetMapping(PATH_HISTORY)
    public ResponseEntity<TransactionsHistoryResponse> getTransactionHistory(
            @RequestParam(defaultValue = "5") int count,
            @PathVariable UUID groupId
    ) {
        TransactionsHistoryResponse response = service.getTransactionHistory(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(PATH_SETTLEMENT)
    public ResponseEntity<TransactionsSettlementResponse> getTransactionSettlement(
            @PathVariable UUID groupId
    ) {
        TransactionsSettlementResponse response = service.getSettlements(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TransactionDetailResponseDTO> createTransaction(
            @PathVariable("groupId") UUID groupId,
            @RequestBody TransactionCreationRequestDTO request
    ) {
        TransactionDetailResponseDTO response = service.createTransaction(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDetailResponseDTO> getTransactionDetail(
            @PathVariable("transactionId") UUID transactionId
    ) {
        TransactionDetailResponseDTO response = service.getTransactionDetail(transactionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteLoan(
            @PathVariable("transactionId") UUID transactionId
    ) {
        service.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }
}
