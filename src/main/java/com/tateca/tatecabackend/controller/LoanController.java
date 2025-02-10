package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.LoanCreationRequest;
import com.tateca.tatecabackend.dto.response.LoanCreationResponse;
import com.tateca.tatecabackend.service.LoanService;
import com.tateca.tatecabackend.constants.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.PATH_GROUPS + "/{groupId}" + ApiConstants.PATH_LOANS)
@RequiredArgsConstructor
public class LoanController {
    private final LoanService service;

    @PostMapping
    public ResponseEntity<LoanCreationResponse> createLoan(
            @RequestHeader(ApiConstants.HEADER_AUTHORIZATION) String idToken,
            @RequestBody LoanCreationRequest request,
            @PathVariable("groupId") UUID groupId
    ) {
       LoanCreationResponse response = service.createLoan(request, groupId);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<LoanCreationResponse> getLoan(
            @RequestHeader(ApiConstants.HEADER_AUTHORIZATION) String idToken,
            @PathVariable("groupId") UUID groupId,
            @PathVariable("loanId") UUID loanId
    ) {
        LoanCreationResponse response = service.getLoan(loanId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{loanId}")
    public ResponseEntity<LoanCreationResponse> updateLoan(
            @RequestHeader(ApiConstants.HEADER_AUTHORIZATION) String idToken,
            @RequestBody LoanCreationRequest request,
            @PathVariable("groupId") UUID groupId,
            @PathVariable("loanId") UUID loanId
    ) {
        LoanCreationResponse response = service.updateLoan(groupId, loanId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{loanId}")
    public ResponseEntity<Void> deleteLoan(
            @PathVariable("loanId") UUID loanId
    ) {
        service.deleteLoan(loanId);
        return ResponseEntity.noContent().build();
    }
}
