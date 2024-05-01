package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.constants.ApiConstants;
import com.moneyme.moneymebackend.dto.response.GetTransactionsResponse;
import com.moneyme.moneymebackend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.moneyme.moneymebackend.constants.ApiConstants.HEADER_AUTHORIZATION;

@RestController
@RequestMapping(ApiConstants.PATH_TRANSACTIONS)
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    @GetMapping
    public ResponseEntity<List<GetTransactionsResponse>> getTransactions(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam("group_id") String groupId
    ) {
        List<GetTransactionsResponse> response = service.getTransactions(count, groupId);
     return ResponseEntity.ok(response);
    }
}
