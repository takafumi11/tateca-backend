package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.response.GetGroupTransactionsResponse;
import com.moneyme.moneymebackend.dto.response.GetTransactionsResponse;
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
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_SETTLEMENTS;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_TRANSACTIONS;

@RestController
@RequestMapping(PATH_GROUPS + "/{groupId}" + PATH_TRANSACTIONS)
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    @GetMapping
    public ResponseEntity<GetTransactionsResponse> getTransactions(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @RequestParam(defaultValue = "10") int count,
            @PathVariable("groupId") UUID groupId) {
        GetTransactionsResponse response = service.getTransactions(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(PATH_SETTLEMENTS)
    public ResponseEntity<GetGroupTransactionsResponse> getGroupSettlements(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @PathVariable("groupId") UUID groupId) {
        GetGroupTransactionsResponse response = service.getGroupBalances(groupId);
        return ResponseEntity.ok(response);
    }
}
