package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;

public record CurrencyResponseDTO(
        @JsonProperty("currency_code")
        String currencyCode,

        @JsonProperty("jp_currency_name")
        String jpCurrencyName,

        @JsonProperty("eng_currency_name")
        String engCurrencyName
) {
    public static CurrencyResponseDTO from(ExchangeRateEntity exchangeRate) {
        return new CurrencyResponseDTO(
                exchangeRate.getCurrencyCode(),
                exchangeRate.getCurrencyName().getJpCurrencyName(),
                exchangeRate.getCurrencyName().getEngCurrencyName()
        );
    }
}
