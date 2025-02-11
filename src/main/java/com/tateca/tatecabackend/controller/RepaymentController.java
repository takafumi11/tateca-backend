package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.RepaymentCreationRequest;
import com.tateca.tatecabackend.dto.response.RepaymentCreationResponse;
import com.tateca.tatecabackend.service.RepaymentService;
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
@RequestMapping(ApiConstants.PATH_GROUPS + "/{groupId}" + ApiConstants.PATH_REPAYMENTS)
@RequiredArgsConstructor
public class RepaymentController {
    private final RepaymentService service;

    @PostMapping
    public ResponseEntity<RepaymentCreationResponse> createRepayment(
            @RequestHeader(ApiConstants.HEADER_AUTHORIZATION) String idToken,
            @RequestBody RepaymentCreationRequest request,
            @PathVariable("groupId") UUID groupId
    ) {
        RepaymentCreationResponse response = service.createRepayment(request, groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{repaymentId}")
    public ResponseEntity<RepaymentCreationResponse> getRepayment(
            @RequestHeader(ApiConstants.HEADER_AUTHORIZATION) String idToken,
            @PathVariable("groupId") UUID groupId,
            @PathVariable("repaymentId") UUID repaymentId
    ) {
        RepaymentCreationResponse response = service.getRepayment(repaymentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{repaymentId}")
    public ResponseEntity<Void> deleteRepayment(
            @PathVariable("repaymentId") UUID repaymentId
    ) {
        service.deleteRepayment(repaymentId);
        return ResponseEntity.noContent().build();
    }
}
