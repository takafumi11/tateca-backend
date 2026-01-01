package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.ExchangeRateResponse;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Exchange rate list response")
public record ExchangeRateResponseDTO(
        @JsonProperty("exchange_rate")
        @Schema(description = "List of exchange rates")
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
