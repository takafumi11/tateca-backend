package com.tateca.tatecabackend.fixtures;

import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    // ========== Object Mother: Groups ==========

    public static class Groups {
        public static GroupEntity standard() {
            return GroupEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test Group")
                    .joinToken(UUID.randomUUID())
                    .tokenExpires(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build();
        }

        public static GroupEntity withExpiredToken() {
            GroupEntity group = standard();
            group.setTokenExpires(Instant.now().minus(1, ChronoUnit.DAYS));
            return group;
        }

        public static GroupEntity withName(String name) {
            GroupEntity group = standard();
            group.setName(name);
            return group;
        }
    }

    // ========== Object Mother: Users ==========

    public static class Users {
        public static UserEntity standard() {
            return UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test User")
                    .authUser(AuthUsers.standard())
                    .currencyName(Currencies.jpy())
                    .build();
        }

        public static UserEntity withName(String name) {
            UserEntity user = standard();
            user.setName(name);
            return user;
        }

        public static UserEntity withCurrency(CurrencyNameEntity currency) {
            UserEntity user = standard();
            user.setCurrencyName(currency);
            return user;
        }
    }

    // ========== Object Mother: AuthUsers ==========

    public static class AuthUsers {
        public static AuthUserEntity standard() {
            return AuthUserEntity.builder()
                    .uid("test-auth-uid-" + UUID.randomUUID())
                    .build();
        }

        public static AuthUserEntity withUid(String uid) {
            return AuthUserEntity.builder()
                    .uid(uid)
                    .build();
        }
    }

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
                    .build();
        }
    }

    // ========== Object Mother: Exchange Rates ==========

    public static class ExchangeRates {
        public static ExchangeRateEntity jpy() {
            return ExchangeRateEntity.builder()
                    .currencyCode("JPY")
                    .date(LocalDate.now())
                    .exchangeRate(BigDecimal.ONE)
                    .currencyName(Currencies.jpy())
                    .build();
        }

        public static ExchangeRateEntity usd() {
            return ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(LocalDate.now())
                    .exchangeRate(BigDecimal.valueOf(150.00))
                    .currencyName(Currencies.usd())
                    .build();
        }

        public static ExchangeRateEntity withCurrency(CurrencyNameEntity currency) {
            return ExchangeRateEntity.builder()
                    .currencyCode(currency.getCurrencyCode())
                    .date(LocalDate.now())
                    .exchangeRate(BigDecimal.ONE)
                    .currencyName(currency)
                    .build();
        }

        public static ExchangeRateEntity withRate(String currencyCode, BigDecimal rate) {
            return ExchangeRateEntity.builder()
                    .currencyCode(currencyCode)
                    .date(LocalDate.now())
                    .exchangeRate(rate)
                    .build();
        }
    }

    // ========== Object Mother: Transactions ==========

    public static class Transactions {
        public static TransactionHistoryEntity loan(GroupEntity group, UserEntity payer, ExchangeRateEntity exchangeRate) {
            return TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .group(group)
                    .title("Test Loan")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(payer)
                    .exchangeRate(exchangeRate)
                    .build();
        }

        public static TransactionHistoryEntity repayment(GroupEntity group, UserEntity payer, ExchangeRateEntity exchangeRate) {
            return TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.REPAYMENT)
                    .group(group)
                    .title("Test Repayment")
                    .amount(3000)
                    .transactionDate(Instant.now())
                    .payer(payer)
                    .exchangeRate(exchangeRate)
                    .build();
        }
    }

}
