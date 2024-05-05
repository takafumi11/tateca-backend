package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.response.GetGroupTransactionsResponse;
import com.moneyme.moneymebackend.service.GroupBalancesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupBalancesController {
    private final GroupBalancesService service;

    @GetMapping("/{group_id}/balances")
    public ResponseEntity<GetGroupTransactionsResponse> getUserBalances(
            @RequestHeader("Authorization") String token,
            @PathVariable("group_id") String groupId
    ) {
        GetGroupTransactionsResponse response = service.getGroupBalances(groupId);
        return ResponseEntity.ok(response);
    }
}
