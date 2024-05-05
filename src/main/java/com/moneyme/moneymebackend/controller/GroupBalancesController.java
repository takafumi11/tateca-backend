package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.response.UserBalanceResponse;
import com.moneyme.moneymebackend.service.UserBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
public class UserBalanceController {
    private final UserBalanceService service;

    @GetMapping("/{groupId}")
    public ResponseEntity<UserBalanceResponse> getUserBalances(
            @RequestHeader("Authorization") String token,
            @PathVariable("group_id") String groupId
    ) {
        UserBalanceResponse response = service.getUserBalances(groupId);
        return ResponseEntity.ok(response);
    }
}
