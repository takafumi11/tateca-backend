package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;

import java.time.LocalDate;

public interface ExchangeRateService {
    ExchangeRateResponseDTO getExchangeRate(LocalDate date);
}
