package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {
    private final ExchangeRateAccessor accessor;

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDTO getExchangeRate(LocalDate date) {
        return ExchangeRateResponseDTO.from(accessor.findAllActiveByDate(date));
    }
}
