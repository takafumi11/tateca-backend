package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/exchange-rate")
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "Currency exchange rate operations")
public class ExchangeRateController {
    private final ExchangeRateService service;

    @GetMapping("/{date}")
    @Operation(summary = "Get exchange rates for a specific date")
    public ResponseEntity<ExchangeRateResponseDTO> getExchangeRate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(service.getExchangeRate(date));
    }
}
