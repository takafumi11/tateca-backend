package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.service.InternalExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/exchange-rates")
@RequiredArgsConstructor
public class InternalExchangeRateController {
    private static final Logger logger = LoggerFactory.getLogger(InternalExchangeRateController.class);

    private final InternalExchangeRateService exchangeRateService;

    @PostMapping
    public ResponseEntity<Void> updateExchangeRates() {
        logger.info("Exchange rate update triggered via HTTP endpoint");

        int ratesUpdated = exchangeRateService.fetchAndStoreLatestExchangeRate();

        logger.info("Exchange rate update completed successfully. Stored {} total rates (today + tomorrow)", ratesUpdated);

        return ResponseEntity.noContent().build();
    }
}
