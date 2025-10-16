package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.model.SymbolPosition;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CurrencyNameDTO {
    @JsonProperty("currency_code")
    String currencyCode;

    @JsonProperty("jp_currency_name")
    String jpCurrencyName;

    @JsonProperty("eng_currency_name")
    String engCurrencyName;

    @JsonProperty("jp_country_name")
    String jpCountryName;

    @JsonProperty("eng_country_name")
    String engCountryName;

    @JsonProperty("currency_symbol")
    String currencySymbol;

    @JsonProperty("symbol_position")
    SymbolPosition symbolPosition;

    public static CurrencyNameDTO from(CurrencyNameEntity currencyName) {
        return CurrencyNameDTO.builder()
                .currencyCode(currencyName.getCurrencyCode())
                .jpCurrencyName(currencyName.getJpCurrencyName())
                .engCurrencyName(currencyName.getEngCurrencyName())
                .jpCountryName(currencyName.getJpCountryName())
                .engCountryName(currencyName.getEngCountryName())
                .currencySymbol(currencyName.getCurrencySymbol())
                .symbolPosition(currencyName.getSymbolPosition())
                .build();
    }
}
