package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateAccessor accessor;

    public ExchangeRateResponse getExchangeRate(LocalDate date) {
        return ExchangeRateResponse.from(accessor.findAllActiveByDate(date));
    }
}
