package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrencyResponseDTO {
    @JsonProperty("currency_code")
    String currencyCode;
    @JsonProperty("jp_currency_name")
    String jpCurrencyName;
    @JsonProperty("eng_currency_name")
    String engCurrencyName;

    @JsonProperty("currency_symbol")
    String currencySymbol;

    @JsonProperty("symbol_position")
    String symbolPosition;

    @JsonProperty("exchange_rate")
    String exchangeRate;

    public static CurrencyResponseDTO from(ExchangeRateEntity exchangeRateEntity) {
        return CurrencyResponseDTO.builder()
                .currencyCode(exchangeRateEntity.getCurrencyCode())
                .jpCurrencyName(exchangeRateEntity.getCurrencyName().getJpCurrencyName())
                .engCurrencyName(exchangeRateEntity.getCurrencyName().getEngCurrencyName())
                .currencySymbol(exchangeRateEntity.getCurrencyName().getCurrencySymbol())
                .symbolPosition(exchangeRateEntity.getCurrencyName().getSymbolPosition().name())
                .exchangeRate(exchangeRateEntity.getExchangeRate().toString())
                .build();
    }
}
