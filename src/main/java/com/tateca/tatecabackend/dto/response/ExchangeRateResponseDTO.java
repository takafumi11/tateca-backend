package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.model.SymbolPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Exchange rate information for a currency")
public class ExchangeRateResponseDTO {
    @JsonProperty("currency_code")
    @Schema(description = "ISO 4217 currency code", example = "USD")
    String currencyCode;

    @JsonProperty("jp_currency_name")
    @Schema(description = "Currency name in Japanese", example = "米ドル")
    String jpCurrencyName;

    @JsonProperty("eng_currency_name")
    @Schema(description = "Currency name in English", example = "US Dollar")
    String engCurrencyName;

    @JsonProperty("jp_country_name")
    @Schema(description = "Country name in Japanese", example = "アメリカ合衆国")
    String jpCountryName;

    @JsonProperty("eng_country_name")
    @Schema(description = "Country name in English", example = "United States")
    String engCountryName;

    @JsonProperty("currency_symbol")
    @Schema(description = "Currency symbol", example = "$")
    String currencySymbol;

    @JsonProperty("symbol_position")
    @Schema(description = "Position of currency symbol")
    SymbolPosition symbolPosition;

    @JsonProperty("exchange_rate")
    @Schema(description = "Exchange rate to JPY", example = "150.25")
    String exchangeRate;

    public static ExchangeRateResponseDTO from(ExchangeRateEntity exchangeRateEntity) {
        CurrencyNameEntity currencyNameEntity = exchangeRateEntity.getCurrencyName();

        return ExchangeRateResponseDTO.builder()
                .currencyCode(currencyNameEntity.getCurrencyCode())
                .jpCurrencyName(currencyNameEntity.getJpCurrencyName())
                .engCurrencyName(currencyNameEntity.getEngCurrencyName())
                .jpCountryName(currencyNameEntity.getJpCountryName())
                .engCountryName(currencyNameEntity.getEngCountryName())
                .currencySymbol(currencyNameEntity.getCurrencySymbol())
                .symbolPosition(currencyNameEntity.getSymbolPosition())
                .exchangeRate(exchangeRateEntity.getExchangeRate().toString())
                .build();
    }
}