package com.tateca.tatecabackend.fixtures;

import com.tateca.tatecabackend.entity.AppReviewStatus;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.model.SymbolPosition;

import java.time.Instant;
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

    // ========== Object Mother: Users ==========

    public static class Users {
        public static UserEntity standard() {
            UserEntity user = UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test User")
                    .currencyName(Currencies.jpy())
                    .build();
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            return user;
        }
    }

    // ========== Object Mother: AuthUsers ==========

    public static class AuthUsers {
        public static AuthUserEntity standard() {
            AuthUserEntity authUser = AuthUserEntity.builder()
                    .uid("test-auth-uid")
                    .name("Test Auth User")
                    .email("test@example.com")
                    .totalLoginCount(1)
                    .lastLoginTime(Instant.now())
                    .appReviewStatus(AppReviewStatus.PENDING)
                    .build();
            authUser.setCreatedAt(Instant.now());
            authUser.setUpdatedAt(Instant.now());
            return authUser;
        }
    }

}
