package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.response.ExchangeRateListResponseDTO;

import java.time.LocalDate;

public interface ExchangeRateService {
    ExchangeRateListResponseDTO getExchangeRate(LocalDate date);
}
