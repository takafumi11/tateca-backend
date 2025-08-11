package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.model.SymbolPosition;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExchangeRateResponseDTO {
    @JsonProperty("currency_code") String currencyCode;
    @JsonProperty("jp_currency_name") String jpCurrencyName;
    @JsonProperty("eng_currency_name") String engCurrencyName;
    @JsonProperty("jp_country_name") String jpCountryName;
    @JsonProperty("eng_country_name") String engCountryName;
    @JsonProperty("currency_symbol") String currencySymbol;
    @JsonProperty("symbol_position")
    SymbolPosition symbolPosition;
    @JsonProperty("exchange_rate") String exchangeRate;

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