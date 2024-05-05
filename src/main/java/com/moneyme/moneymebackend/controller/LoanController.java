package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.request.CreateLoanRequest;
import com.moneyme.moneymebackend.dto.response.CreateLoanResponse;
import com.moneyme.moneymebackend.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.moneyme.moneymebackend.constants.ApiConstants.HEADER_AUTHORIZATION;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_GROUPS;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_LOANS;

@RestController
@RequestMapping(PATH_GROUPS + "/{groupId}" + PATH_LOANS)
@RequiredArgsConstructor
public class LoanController {
    private final LoanService service;

    @PostMapping
    public ResponseEntity<CreateLoanResponse> createLoan(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @RequestBody CreateLoanRequest request,
            @PathVariable("groupId") UUID groupId
    ) {
       CreateLoanResponse response = service.createLoan(request, groupId);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
