package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExchangeRateListResponseDTO {
    @JsonProperty("exchange_rate")
    List<ExchangeRateResponseDTO> exchangeRateResponseList;

    public static ExchangeRateListResponseDTO from(List<ExchangeRateEntity> exchangeRateEntityList) {
        return new ExchangeRateListResponseDTO(exchangeRateEntityList.stream().map(ExchangeRateResponseDTO::from).toList());
    }
}
