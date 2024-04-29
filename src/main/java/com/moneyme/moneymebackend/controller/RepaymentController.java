package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.request.CreateRepaymentRequest;
import com.moneyme.moneymebackend.dto.response.CreateRepaymentResponse;
import com.moneyme.moneymebackend.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/repayments")
@RequiredArgsConstructor
public class RepaymentController {
    private final RepaymentService service;

    @PostMapping
    public ResponseEntity<CreateRepaymentResponse> creteRepayment(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateRepaymentRequest request
    ) {
        CreateRepaymentResponse response = service.createRepayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
