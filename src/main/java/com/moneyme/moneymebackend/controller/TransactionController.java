package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.annotation.ValidBearerToken;
import com.moneyme.moneymebackend.dto.response.TransactionsSettlementResponse;
import com.moneyme.moneymebackend.dto.response.TransactionsHistoryResponse;
import com.moneyme.moneymebackend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.moneyme.moneymebackend.constants.ApiConstants.HEADER_AUTHORIZATION;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_GROUPS;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_HISTORY;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_SETTLEMENT;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_TRANSACTIONS;

@RestController
@RequestMapping(PATH_GROUPS + "/{groupId}" + PATH_TRANSACTIONS)
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    @GetMapping(PATH_HISTORY)
    public ResponseEntity<TransactionsHistoryResponse> getTransactions(
            @ValidBearerToken String uid,
            @RequestParam(defaultValue = "10") int count,
            @PathVariable UUID groupId) {
        TransactionsHistoryResponse response = service.getTransactions(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(PATH_SETTLEMENT)
    public ResponseEntity<TransactionsSettlementResponse> getGroupSettlements(
            @ValidBearerToken String uid,
            @PathVariable UUID groupId) {
        TransactionsSettlementResponse response = service.getSettlements(groupId);
        return ResponseEntity.ok(response);
    }
}
