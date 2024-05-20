package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.request.RepaymentCreationRequest;
import com.moneyme.moneymebackend.dto.response.RepaymentCreationResponse;
import com.moneyme.moneymebackend.service.RepaymentService;
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

import static com.moneyme.moneymebackend.constants.ApiConstants.HEADER_AUTHORIZATION;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_GROUPS;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_REPAYMENTS;

@RestController
@RequestMapping(PATH_GROUPS + "/{groupId}" + PATH_REPAYMENTS)
@RequiredArgsConstructor
public class RepaymentController {
    private final RepaymentService service;

    @PostMapping
    public ResponseEntity<RepaymentCreationResponse> createRepayment(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @RequestBody RepaymentCreationRequest request,
            @PathVariable("groupId") UUID groupId
    ) {
        RepaymentCreationResponse response = service.createRepayment(request, groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{repaymentId}")
    public ResponseEntity<RepaymentCreationResponse> getRepayment(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @PathVariable("groupId") UUID groupId,
            @PathVariable("repaymentId") UUID repaymentId
    ) {
        RepaymentCreationResponse response = service.getRepayment(repaymentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{repaymentId}")
    public ResponseEntity<RepaymentCreationResponse> updateRepayment(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @PathVariable("groupId") UUID groupId,
            @PathVariable("repaymentId") UUID repaymentId,
            @RequestBody RepaymentCreationRequest request
    ) {
        RepaymentCreationResponse response = service.updateRepayment(groupId, repaymentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{repaymentId}")
    public ResponseEntity<Void> deleteRepayment(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @PathVariable("groupId") UUID groupId,
            @PathVariable("repaymentId") UUID repaymentId
    ) {
        service.deleteRepayment(groupId, repaymentId);
        return ResponseEntity.noContent().build();
    }
}
