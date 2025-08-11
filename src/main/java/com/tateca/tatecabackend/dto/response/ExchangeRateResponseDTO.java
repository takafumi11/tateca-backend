package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExchangeRateResponseDTO {
    @JsonProperty("currency_name") String currencyName;
    @JsonProperty("rate") BigDecimal rate;

    public static ExchangeRateResponseDTO from(ExchangeRateEntity entity) {
        return ExchangeRateResponseDTO.builder()
                .currencyName(entity.getCurrencyName().getEngCurrencyName())
                .rate(entity.getExchangeRate())
                .build();
    }
}