package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private CurrencyNameEntity jpyCurrency;
    private AuthUserEntity testAuthUser;

    @BeforeEach
    void setUp() {
        // Setup currencies
        jpyCurrency = TestFixtures.Currencies.jpy();
        CurrencyNameEntity usdCurrency = TestFixtures.Currencies.usd();
        CurrencyNameEntity eurCurrency = TestFixtures.Currencies.eur();

        entityManager.persist(jpyCurrency);
        entityManager.persist(usdCurrency);
        entityManager.persist(eurCurrency);

        // Setup auth user
        testAuthUser = AuthUserEntity.builder()
                .uid("test-auth-uid")
                .name("Test Auth User")
                .email("test@example.com")
                .build();
        entityManager.persist(testAuthUser);

        flushAndClear();
    }

    @Nested
    @DisplayName("PATCH /users/{userId}")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user name successfully")
        void shouldUpdateUserNameSuccessfully() throws Exception {
            // Given
            UserEntity user = createAndPersistUser("Original Name", jpyCurrency, testAuthUser);
            UUID userId = user.getUuid();

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.currency.currency_code").value("JPY"));
        }

        @Test
        @DisplayName("Should update currency code successfully")
        void shouldUpdateCurrencyCodeSuccessfully() throws Exception {
            // Given
            UserEntity user = createAndPersistUser("Test User", jpyCurrency, testAuthUser);
            UUID userId = user.getUuid();

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currency_code": "USD"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value("Test User"))
                    .andExpect(jsonPath("$.currency.currency_code").value("USD"));
        }

        @Test
        @DisplayName("Should update both name and currency code successfully")
        void shouldUpdateBothNameAndCurrencyCodeSuccessfully() throws Exception {
            // Given
            UserEntity user = createAndPersistUser("Original Name", jpyCurrency, testAuthUser);
            UUID userId = user.getUuid();

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name",
                                        "currency_code": "EUR"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.currency.currency_code").value("EUR"));
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when request body is empty")
        void shouldReturnBadRequestWhenRequestBodyEmpty() throws Exception {
            // Given
            UserEntity user = createAndPersistUser("Original Name", jpyCurrency, testAuthUser);
            UUID userId = user.getUuid();

            // When & Then
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when user does not exist")
        void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(patch("/users/{userId}", nonExistentUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when currency code does not exist")
        void shouldReturnNotFoundWhenCurrencyCodeDoesNotExist() throws Exception {
            // Given
            UserEntity user = createAndPersistUser("Test User", jpyCurrency, testAuthUser);
            UUID userId = user.getUuid();

            // When & Then - "XXX" is valid format (3 uppercase letters) but doesn't exist in DB
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currency_code": "XXX"
                                    }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when userId is invalid UUID format")
        void shouldReturnBadRequestWhenInvalidUuidFormat() throws Exception {
            // When & Then
            mockMvc.perform(patch("/users/{userId}", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "New Name"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when currency code format is invalid")
        void shouldReturnBadRequestWhenCurrencyCodeFormatInvalid() throws Exception {
            // Given
            UserEntity user = createAndPersistUser("Test User", jpyCurrency, testAuthUser);
            UUID userId = user.getUuid();

            // When & Then - "INVALID" is not 3 uppercase letters
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "currency_code": "INVALID"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should persist changes to database")
        void shouldPersistChangesToDatabase() throws Exception {
            // Given
            UserEntity user = createAndPersistUser("Original Name", jpyCurrency, testAuthUser);
            UUID userId = user.getUuid();

            // When
            mockMvc.perform(patch("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "user_name": "Persisted Name",
                                        "currency_code": "USD"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // Then - Verify the changes are persisted
            flushAndClear();
            UserEntity updatedUser = entityManager.find(UserEntity.class, userId);
            assertThat(updatedUser.getName()).isEqualTo("Persisted Name");
            assertThat(updatedUser.getCurrencyName().getCurrencyCode()).isEqualTo("USD");
        }
    }

    private UserEntity createAndPersistUser(String name, CurrencyNameEntity currency, AuthUserEntity authUser) {
        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(name)
                .currencyName(currency)
                .authUser(authUser)
                .build();
        entityManager.persist(user);
        flushAndClear();
        return user;
    }
}
