package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.ExchangeRateUpdateRequestDTO;
import com.tateca.tatecabackend.service.ExchangeRateUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateLambdaController {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateLambdaController.class);

    private final ExchangeRateUpdateService exchangeRateUpdateService;

    @PostMapping
    public ResponseEntity<Void> updateExchangeRates(
            @Valid @RequestBody ExchangeRateUpdateRequestDTO requestDTO) {

        logger.info("Exchange rate update triggered via HTTP endpoint for date: {}", requestDTO.targetDate());

        int ratesUpdated = exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(requestDTO.targetDate());

        logger.info("Exchange rate update completed successfully. Updated {} rates for date: {}",
                ratesUpdated, requestDTO.targetDate());

        return ResponseEntity.noContent().build();
    }
}
