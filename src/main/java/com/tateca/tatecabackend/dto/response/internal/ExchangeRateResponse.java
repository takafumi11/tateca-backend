package com.tateca.tatecabackend.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.model.SymbolPosition;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exchange rate information for a currency")
public record ExchangeRateResponse(
        @JsonProperty("currency_code")
        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currencyCode,

        @JsonProperty("jp_currency_name")
        @Schema(description = "Currency name in Japanese", example = "米ドル")
        String jpCurrencyName,

        @JsonProperty("eng_currency_name")
        @Schema(description = "Currency name in English", example = "US Dollar")
        String engCurrencyName,

        @JsonProperty("jp_country_name")
        @Schema(description = "Country name in Japanese", example = "アメリカ合衆国")
        String jpCountryName,

        @JsonProperty("eng_country_name")
        @Schema(description = "Country name in English", example = "United States")
        String engCountryName,

        @JsonProperty("currency_symbol")
        @Schema(description = "Currency symbol", example = "$")
        String currencySymbol,

        @JsonProperty("symbol_position")
        @Schema(description = "Position of currency symbol")
        SymbolPosition symbolPosition,

        @JsonProperty("exchange_rate")
        @Schema(description = "Exchange rate to JPY", example = "150.25")
        String exchangeRate
) {
    public static ExchangeRateResponse from(ExchangeRateEntity exchangeRateEntity) {
        CurrencyNameEntity currencyNameEntity = exchangeRateEntity.getCurrencyName();

        return new ExchangeRateResponse(
                currencyNameEntity.getCurrencyCode(),
                currencyNameEntity.getJpCurrencyName(),
                currencyNameEntity.getEngCurrencyName(),
                currencyNameEntity.getJpCountryName(),
                currencyNameEntity.getEngCountryName(),
                currencyNameEntity.getCurrencySymbol(),
                currencyNameEntity.getSymbolPosition(),
                exchangeRateEntity.getExchangeRate().toString()
        );
    }
}
