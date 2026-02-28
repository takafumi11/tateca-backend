package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.service.TransactionService;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/groups/{groupId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService service;

    @GetMapping("/history")
    public ResponseEntity<TransactionHistoryResponseDTO> getTransactionHistory(
            @RequestParam(defaultValue = "5") int count,
            @PathVariable UUID groupId
    ) {
        TransactionHistoryResponseDTO response = service.getTransactionHistory(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settlement")
    public ResponseEntity<TransactionSettlementResponseDTO> getTransactionSettlement(
            @PathVariable UUID groupId
    ) {
        TransactionSettlementResponseDTO response = service.getSettlements(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateTransactionResponseDTO> createTransaction(
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody CreateTransactionRequestDTO request
    ) {
        logger.info("Creating transaction: groupId={}, type={}, amount={}, currency={}",
                PiiMaskingUtil.maskUuid(groupId),
                request.transactionType(),
                request.amount(),
                request.currencyCode());

        CreateTransactionResponseDTO response = service.createTransaction(groupId, request);

        if (response != null && response.id() != null) {
            logger.info("Transaction created successfully: transactionId={}, groupId={}",
                    PiiMaskingUtil.maskUuid(UUID.fromString(response.id())),
                    PiiMaskingUtil.maskUuid(groupId));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<CreateTransactionResponseDTO> getTransactionDetail(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId
    ) {
        CreateTransactionResponseDTO response = service.getTransactionDetail(transactionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId
    ) {
        logger.info("Deleting transaction: transactionId={}, groupId={}",
                PiiMaskingUtil.maskUuid(transactionId),
                PiiMaskingUtil.maskUuid(groupId));

        service.deleteTransaction(transactionId);

        logger.info("Transaction deleted successfully: transactionId={}, groupId={}",
                PiiMaskingUtil.maskUuid(transactionId),
                PiiMaskingUtil.maskUuid(groupId));

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateTransactionResponseDTO> updateTransaction(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId,
            @Valid @RequestBody UpdateTransactionRequestDTO request
    ) {
        logger.info("Updating transaction: transactionId={}, groupId={}, amount={}, currency={}",
                PiiMaskingUtil.maskUuid(transactionId),
                PiiMaskingUtil.maskUuid(groupId),
                request.amount(),
                request.currencyCode());

        CreateTransactionResponseDTO response = service.updateTransaction(transactionId, request);

        logger.info("Transaction updated successfully: transactionId={}, groupId={}",
                PiiMaskingUtil.maskUuid(transactionId),
                PiiMaskingUtil.maskUuid(groupId));

        return ResponseEntity.ok(response);
    }
}
