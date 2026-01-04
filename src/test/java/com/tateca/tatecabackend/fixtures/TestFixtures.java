package com.tateca.tatecabackend.fixtures;

import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.model.SymbolPosition;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Test fixtures using Object Mother pattern.
 *
 * <p>Provides standardized test data for entities with sensible defaults.</p>
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Currencies
 * CurrencyEntity jpy = TestFixtures.Currencies.jpy();
 * CurrencyEntity usd = TestFixtures.Currencies.usd();
 *
 * // AuthUsers
 * AuthUserEntity authUser = TestFixtures.AuthUsers.defaultAuthUser();
 *
 * // Groups
 * GroupEntity group = TestFixtures.Groups.defaultGroup();
 * GroupEntity group = TestFixtures.Groups.withName("My Group");
 *
 * // Users
 * UserEntity user = TestFixtures.Users.userWithAuthUser(authUser);
 * UserEntity user = TestFixtures.Users.userWithoutAuthUser("John Doe");
 *
 * // UserGroups
 * UserGroupEntity userGroup = TestFixtures.UserGroups.create(user, group);
 *
 * // Exchange Rate API Responses
 * ExchangeRateClientResponse response = TestFixtures.ExchangeRateApiResponses.success();
 * </pre>
 */
public class TestFixtures {
    // ========== Object Mother: Currencies ==========

    public static class Currencies {
        public static CurrencyEntity jpy() {
            return CurrencyEntity.builder()
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

        public static CurrencyEntity usd() {
            return CurrencyEntity.builder()
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

        public static CurrencyEntity eur() {
            return CurrencyEntity.builder()
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
    }

    // ========== Object Mother: ExchangeRateApiResponses ==========

    public static class ExchangeRateApiResponses {
        public static ExchangeRateClientResponse success() {
            java.util.Map<String, Double> rates = new java.util.HashMap<>();
            rates.put("JPY", 1.0);
            rates.put("USD", 0.0067);
            rates.put("EUR", 0.0061);
            return new ExchangeRateClientResponse("success", "1704067200", rates);
        }

        public static ExchangeRateClientResponse withRates(
                java.util.Map<String, Double> rates) {
            return new ExchangeRateClientResponse("success", "1704067200", rates);
        }
    }

    // ========== Object Mother: AuthUsers ==========

    public static class AuthUsers {
        /**
         * Creates a default AuthUserEntity with unique UID.
         * Uses timestamp to ensure uniqueness across test runs.
         */
        public static AuthUserEntity defaultAuthUser() {
            return AuthUserEntity.builder()
                    .uid("test-uid-" + System.currentTimeMillis())
                    .name("Test Auth User")
                    .email("test@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }
    }

    // ========== Object Mother: Groups ==========

    public static class Groups {
        /**
         * Creates a default GroupEntity with unique UUID and join token.
         * Token expires in 1 day from now.
         */
        public static GroupEntity defaultGroup() {
            Instant now = Instant.now();
            return GroupEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test Group")
                    .joinToken(UUID.randomUUID())
                    .tokenExpires(now.plus(1, ChronoUnit.DAYS))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        /**
         * Creates a GroupEntity with a custom groupName.
         */
        public static GroupEntity withName(String name) {
            Instant now = Instant.now();
            return GroupEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name(name)
                    .joinToken(UUID.randomUUID())
                    .tokenExpires(now.plus(1, ChronoUnit.DAYS))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }
    }

    // ========== Object Mother: Users ==========

    public static class Users {
        /**
         * Creates a UserEntity linked to an AuthUserEntity.
         */
        public static UserEntity userWithAuthUser(AuthUserEntity authUser) {
            Instant now = Instant.now();
            return UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name("Test User")
                    .authUser(authUser)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        /**
         * Creates a UserEntity without an AuthUserEntity (participant).
         */
        public static UserEntity userWithoutAuthUser(String name) {
            Instant now = Instant.now();
            return UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name(name)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }
    }

    // ========== Object Mother: UserGroups ==========

    public static class UserGroups {
        /**
         * Creates a UserGroupEntity linking a user to a group.
         */
        public static UserGroupEntity create(UserEntity user, GroupEntity group) {
            return UserGroupEntity.builder()
                    .userUuid(user.getUuid())
                    .groupUuid(group.getUuid())
                    .user(user)
                    .group(group)
                    .build();
        }
    }
}
