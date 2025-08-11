package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.constants.ApiConstants;
import com.tateca.tatecabackend.dto.response.ExchangeRateListResponseDTO;
import com.tateca.tatecabackend.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(ApiConstants.PATH_EXCHANGE_RATE)
@RequiredArgsConstructor
public class ExchangeRateController {
    private final ExchangeRateService service;

    @GetMapping("/{date}")
    public ResponseEntity<ExchangeRateListResponseDTO> getExchangeRate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(service.getExchangeRate(date));
    }
}
