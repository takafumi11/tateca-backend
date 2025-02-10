package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponse;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponse;
import com.tateca.tatecabackend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<TransactionsHistoryResponse> getTransactions(
            @RequestParam(defaultValue = "10") int count,
            @PathVariable UUID groupId) {
        TransactionsHistoryResponse response = service.getTransactions(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(PATH_SETTLEMENT)
    public ResponseEntity<TransactionsSettlementResponse> getGroupSettlements(
            @PathVariable UUID groupId) {
        TransactionsSettlementResponse response = service.getSettlements(groupId);
        return ResponseEntity.ok(response);
    }
}
