package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.ExchangeRateResponse;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;

import java.util.List;

public record ExchangeRateResponseDTO(
        @JsonProperty("exchange_rate")
        List<ExchangeRateResponse> exchangeRateResponseResponseList
) {
    public static ExchangeRateResponseDTO from(List<ExchangeRateEntity> exchangeRateEntityList) {
        return new ExchangeRateResponseDTO(
                exchangeRateEntityList.stream()
                        .map(ExchangeRateResponse::from)
                        .toList()
        );
    }
}
