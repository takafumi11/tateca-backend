package com.tateca.tatecabackend.fixtures;

import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.model.SymbolPosition;

/**
 * Test fixtures using Object Mother pattern.
 *
 * <p>Provides standardized test data for entities and DTOs with sensible defaults
 * and easy customization through fluent builders.</p>
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Object Mother: Standard predefined objects
 * CurrencyNameEntity jpy = TestFixtures.Currencies.jpy();
 * CurrencyNameEntity usd = TestFixtures.Currencies.usd();
 * </pre>
 */
public class TestFixtures {
    // ========== Object Mother: Currencies ==========

    public static class Currencies {
        public static CurrencyNameEntity jpy() {
            return CurrencyNameEntity.builder()
                    .currencyCode("JPY")
                    .jpCurrencyName("日本円")
                    .engCurrencyName("Japanese Yen")
                    .jpCountryName("日本")
                    .engCountryName("Japan")
                    .isActive(true)
                    .currencySymbol("¥")
                    .symbolPosition(SymbolPosition.PREFIX)
                    .build();
        }

        public static CurrencyNameEntity usd() {
            return CurrencyNameEntity.builder()
                    .currencyCode("USD")
                    .jpCurrencyName("米ドル")
                    .engCurrencyName("US Dollar")
                    .jpCountryName("アメリカ")
                    .engCountryName("United States")
                    .isActive(true)
                    .currencySymbol("$")
                    .symbolPosition(SymbolPosition.PREFIX)
                    .build();
        }

        public static CurrencyNameEntity eur() {
            return CurrencyNameEntity.builder()
                    .currencyCode("EUR")
                    .jpCurrencyName("ユーロ")
                    .engCurrencyName("Euro")
                    .jpCountryName("欧州")
                    .engCountryName("Europe")
                    .isActive(true)
                    .currencySymbol("€")
                    .symbolPosition(SymbolPosition.PREFIX)
                    .build();
        }

        public static CurrencyNameEntity inactive(String currencyCode) {
            return CurrencyNameEntity.builder()
                    .currencyCode(currencyCode)
                    .jpCurrencyName("非アクティブ通貨")
                    .engCurrencyName("Inactive Currency")
                    .jpCountryName("なし")
                    .engCountryName("None")
                    .isActive(false)
                    .currencySymbol("X")
                    .symbolPosition(SymbolPosition.PREFIX)
                    .build();
        }
    }

    // ========== Object Mother: ExchangeRateApiResponses ==========

    public static class ExchangeRateApiResponses {
        public static com.tateca.tatecabackend.api.response.ExchangeRateClientResponse success() {
            com.tateca.tatecabackend.api.response.ExchangeRateClientResponse response =
                new com.tateca.tatecabackend.api.response.ExchangeRateClientResponse();
            response.setResult("success");
            response.setTimeLastUpdateUnix("1704067200");
            java.util.Map<String, Double> rates = new java.util.HashMap<>();
            rates.put("JPY", 1.0);
            rates.put("USD", 0.0067);
            rates.put("EUR", 0.0061);
            response.setConversionRates(rates);
            return response;
        }

        public static com.tateca.tatecabackend.api.response.ExchangeRateClientResponse withRates(
                java.util.Map<String, Double> rates) {
            com.tateca.tatecabackend.api.response.ExchangeRateClientResponse response = success();
            response.setConversionRates(rates);
            return response;
        }

        public static com.tateca.tatecabackend.api.response.ExchangeRateClientResponse error() {
            com.tateca.tatecabackend.api.response.ExchangeRateClientResponse response =
                new com.tateca.tatecabackend.api.response.ExchangeRateClientResponse();
            response.setResult("error");
            response.setConversionRates(new java.util.HashMap<>());
            return response;
        }
    }

}
