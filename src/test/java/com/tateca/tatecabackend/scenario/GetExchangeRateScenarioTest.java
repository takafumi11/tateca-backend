package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.model.SymbolPosition;
import com.tateca.tatecabackend.repository.CurrencyRepository;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Get Exchange Rate — Acceptance Scenario Tests")
class GetExchangeRateScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";
    private static final String BASE_ENDPOINT = "/exchange-rate";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DatabaseCleaner databaseCleaner;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;

    private LocalDate testDate;
    private String userUid;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();
        testDate = LocalDate.of(2025, 6, 15);
        userUid = "user-uid-" + System.nanoTime();
    }

    private void setupActiveCurrenciesAndRates() {
        CurrencyEntity usd = TestFixtures.Currencies.usd();
        CurrencyEntity eur = TestFixtures.Currencies.eur();
        currencyRepository.save(usd);
        currencyRepository.save(eur);

        exchangeRateRepository.save(ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(testDate)
                .exchangeRate(new BigDecimal("150.25"))
                .build());
        exchangeRateRepository.save(ExchangeRateEntity.builder()
                .currencyCode("EUR")
                .date(testDate)
                .exchangeRate(new BigDecimal("165.50"))
                .build());
    }

    private CurrencyEntity createInactiveCurrency() {
        CurrencyEntity inactive = CurrencyEntity.builder()
                .currencyCode("XXX")
                .jpCurrencyName("無効通貨")
                .engCurrencyName("Inactive Currency")
                .jpCountryName("なし")
                .engCountryName("None")
                .isActive(false)
                .currencySymbol("X")
                .symbolPosition(SymbolPosition.PREFIX)
                .build();
        return currencyRepository.save(inactive);
    }

    @Nested
    @DisplayName("Req1: 為替レート一覧の取得")
    class Req1_GetExchangeRateList {

        @Test
        @DisplayName("AC1: 有効通貨の為替レートと通貨情報を含む一覧を返却する")
        void ac1_shouldReturnExchangeRatesWithCurrencyInfo() throws Exception {
            setupActiveCurrenciesAndRates();

            MvcResult result = mockMvc.perform(get(BASE_ENDPOINT + "/{date}", testDate)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode rates = response.path("exchange_rate");
            assertThat(rates.isArray()).isTrue();
            assertThat(rates.size()).isEqualTo(2);

            boolean foundUsd = false;
            for (JsonNode rate : rates) {
                if ("USD".equals(rate.path("currency_code").asText())) {
                    foundUsd = true;
                    assertThat(rate.path("jp_currency_name").asText()).isEqualTo("米ドル");
                    assertThat(rate.path("eng_currency_name").asText()).isEqualTo("US Dollar");
                    assertThat(rate.path("jp_country_name").asText()).isNotEmpty();
                    assertThat(rate.path("eng_country_name").asText()).isNotEmpty();
                    assertThat(rate.path("currency_symbol").asText()).isEqualTo("$");
                    assertThat(rate.path("exchange_rate").asText()).isNotEmpty();
                }
            }
            assertThat(foundUsd).isTrue();
        }

        @Test
        @DisplayName("AC2: 為替レートが存在しない日付では空の一覧を返却する")
        void ac2_shouldReturnEmptyListWhenNoRatesExist() throws Exception {
            LocalDate emptyDate = LocalDate.of(2099, 12, 31);

            MvcResult result = mockMvc.perform(get(BASE_ENDPOINT + "/{date}", emptyDate)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode rates = response.path("exchange_rate");
            assertThat(rates.isArray()).isTrue();
            assertThat(rates.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("AC3: 有効通貨のレートのみを返却し、無効通貨を除外する")
        void ac3_shouldReturnOnlyActiveCurrencyRates() throws Exception {
            setupActiveCurrenciesAndRates();
            createInactiveCurrency();
            exchangeRateRepository.save(ExchangeRateEntity.builder()
                    .currencyCode("XXX")
                    .date(testDate)
                    .exchangeRate(new BigDecimal("999.99"))
                    .build());

            MvcResult result = mockMvc.perform(get(BASE_ENDPOINT + "/{date}", testDate)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode rates = response.path("exchange_rate");
            assertThat(rates.size()).isEqualTo(2);

            for (JsonNode rate : rates) {
                assertThat(rate.path("currency_code").asText()).isNotEqualTo("XXX");
            }
        }
    }

    @Nested
    @DisplayName("Req2: 入力バリデーション")
    class Req2_InputValidation {

        @Test
        @DisplayName("AC1: 不正な形式の日付で入力不備として拒否する")
        void ac1_shouldRejectInvalidDateFormat() throws Exception {
            mockMvc.perform(get(BASE_ENDPOINT + "/{date}", "not-a-date")
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isBadRequest());
        }
    }
}
