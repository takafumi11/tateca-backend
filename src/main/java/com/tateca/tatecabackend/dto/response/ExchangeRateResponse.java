package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExchangeRateResponse {
    @JsonProperty("exchange_rate")
    List<ExchangeRateResponseDTO> exchangeRateResponseList;

    public static ExchangeRateResponse from(List<ExchangeRateEntity> exchangeRateEntityList) {
        return new ExchangeRateResponse(exchangeRateEntityList.stream().map(ExchangeRateResponseDTO::from).toList());
    }
}
