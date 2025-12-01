package com.tateca.tatecabackend.dto.response;

import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserInfoDTO Unit Tests")
class UserInfoDTOUnitTest {

    @Test
    @DisplayName("from() should convert UserEntity to UserInfoDTO correctly")
    void fromShouldConvertUserEntityCorrectly() {
        // Given
        CurrencyNameEntity currency = TestFixtures.Currencies.jpy();
        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid("test-uid")
                .name("Auth Name")
                .email("test@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .uuid(userId)
                .name("Test User")
                .currencyName(currency)
                .authUser(authUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // When
        UserInfoDTO result = UserInfoDTO.from(user);

        // Then
        assertThat(result.uuid()).isEqualTo(userId.toString());
        assertThat(result.userName()).isEqualTo("Test User");
        assertThat(result.currency().getCurrencyCode()).isEqualTo("JPY");
        assertThat(result.authUser().getUid()).isEqualTo("test-uid");
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("from() should handle null authUser")
    void fromShouldHandleNullAuthUser() {
        // Given
        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name("Test User")
                .currencyName(TestFixtures.Currencies.jpy())
                .authUser(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // When
        UserInfoDTO result = UserInfoDTO.from(user);

        // Then
        assertThat(result.authUser()).isNull();
    }
}
