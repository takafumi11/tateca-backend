package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.dto.response.ExchangeRateListResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateAccessor accessor;

    public ExchangeRateListResponseDTO getExchangeRate(LocalDate date) {
        return ExchangeRateListResponseDTO.from(accessor.findAllActiveByDate(date));
    }
}
