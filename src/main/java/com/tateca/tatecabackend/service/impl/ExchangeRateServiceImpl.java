package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.dto.response.ExchangeRateListResponseDTO;
import com.tateca.tatecabackend.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {
    private final ExchangeRateAccessor accessor;

    @Override
    public ExchangeRateListResponseDTO getExchangeRate(LocalDate date) {
        return ExchangeRateListResponseDTO.from(accessor.findAllActiveByDate(date));
    }
}
