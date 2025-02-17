package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    public static CurrencyResponseDTO from(ExchangeRateEntity exchangeRate) {
        return CurrencyResponseDTO.builder()
                .currencyCode(exchangeRate.getCurrencyCode())
                .jpCurrencyName(exchangeRate.getCurrencyName().getJpCurrencyName())
                .engCurrencyName(exchangeRate.getCurrencyName().getEngCurrencyName())
                .build();
    }
}
