package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Exchange rate list response")
public class ExchangeRateListResponseDTO {
    @JsonProperty("exchange_rate")
    @Schema(description = "List of exchange rates")
    List<ExchangeRateResponseDTO> exchangeRateResponseList;

    public static ExchangeRateListResponseDTO from(List<ExchangeRateEntity> exchangeRateEntityList) {
        return new ExchangeRateListResponseDTO(exchangeRateEntityList.stream().map(ExchangeRateResponseDTO::from).toList());
    }
}
