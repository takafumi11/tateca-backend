package com.tateca.tatecabackend.fixtures;

import com.tateca.tatecabackend.entity.AppReviewStatus;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.model.SymbolPosition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Test fixtures using Object Mother pattern.
 *
 * <p>Provides standardized test data for entities and DTOs with sensible defaults
 * and easy customization through fluent builders.</p>
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Object Mother: Standard predefined objects
 * GroupEntity group = TestFixtures.Groups.standard();
 * UserEntity user = TestFixtures.Users.standard();
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

    // ========== Object Mother: ExchangeRates ==========

    public static class ExchangeRates {
        public static ExchangeRateEntity jpy(LocalDate date) {
            return ExchangeRateEntity.builder()
                    .currencyCode("JPY")
                    .date(date)
                    .currencyName(Currencies.jpy())
                    .exchangeRate(BigDecimal.ONE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        public static ExchangeRateEntity usd(LocalDate date, BigDecimal rate) {
            return ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(date)
                    .currencyName(Currencies.usd())
                    .exchangeRate(rate)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        public static ExchangeRateEntity eur(LocalDate date, BigDecimal rate) {
            return ExchangeRateEntity.builder()
                    .currencyCode("EUR")
                    .date(date)
                    .currencyName(Currencies.eur())
                    .exchangeRate(rate)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        public static ExchangeRateEntity withCurrency(LocalDate date, CurrencyNameEntity currency, BigDecimal rate) {
            return ExchangeRateEntity.builder()
                    .currencyCode(currency.getCurrencyCode())
                    .date(date)
                    .currencyName(currency)
                    .exchangeRate(rate)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
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

    // ========== Object Mother: AuthUsers ==========

    public static class AuthUsers {
        public static AuthUserEntity standard() {
            return AuthUserEntity.builder()
                    .uid("test-auth-uid")
                    .name("Test Auth User")
                    .email("test@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(Instant.now())
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
        }
    }

    // ========== Object Mother: Groups ==========

    public static class Groups {
        public static GroupEntity standard() {
            return GroupEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test Group")
                    .joinToken(UUID.randomUUID())
                    .build();
        }
    }

    // ========== Object Mother: Users ==========

    public static class Users {
        public static UserEntity standard() {
            return UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test User")
                    .currencyName(Currencies.jpy())
                    .build();
        }
    }

}
