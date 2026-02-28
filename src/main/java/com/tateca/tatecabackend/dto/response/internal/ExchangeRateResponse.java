package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.model.SymbolPosition;

public record ExchangeRateResponse(
        @JsonProperty("currency_code")
        String currencyCode,

        @JsonProperty("jp_currency_name")
        String jpCurrencyName,

        @JsonProperty("eng_currency_name")
        String engCurrencyName,

        @JsonProperty("jp_country_name")
        String jpCountryName,

        @JsonProperty("eng_country_name")
        String engCountryName,

        @JsonProperty("currency_symbol")
        String currencySymbol,

        @JsonProperty("symbol_position")
        SymbolPosition symbolPosition,

        @JsonProperty("exchange_rate")
        String exchangeRate
) {
    public static ExchangeRateResponse from(ExchangeRateEntity exchangeRateEntity) {
        CurrencyEntity currencyEntity = exchangeRateEntity.getCurrency();

        return new ExchangeRateResponse(
                currencyEntity.getCurrencyCode(),
                currencyEntity.getJpCurrencyName(),
                currencyEntity.getEngCurrencyName(),
                currencyEntity.getJpCountryName(),
                currencyEntity.getEngCountryName(),
                currencyEntity.getCurrencySymbol(),
                currencyEntity.getSymbolPosition(),
                exchangeRateEntity.getExchangeRate().toString()
        );
    }
}
