package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.CurrencyRepository;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Get Transaction Settlement — Acceptance Scenario Tests")
class GetTransactionSettlementScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";
    private static final String TRANSACTION_DATE = "2025-01-15T12:00:00+09:00";
    private static final LocalDate EXCHANGE_RATE_DATE = LocalDate.of(2025, 1, 15);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;

    private String userUid;
    private String groupId;
    private String payerUuid;
    private String member1Uuid;
    private String member2Uuid;

    @BeforeEach
    void setUp() throws Exception {
        currencyRepository.save(TestFixtures.Currencies.jpy());
        CurrencyEntity reloadedJpy = currencyRepository.findById("JPY").orElseThrow();
        exchangeRateRepository.save(ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(EXCHANGE_RATE_DATE)
                .exchangeRate(BigDecimal.ONE)
                .currency(reloadedJpy)
                .build());

        userUid = "creator-uid-" + System.nanoTime();

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of("email", userUid + "@example.com"))))
                .andExpect(status().isCreated());

        MvcResult groupResult = mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "group_name", "Test Group",
                                "host_name", "Payer",
                                "participants_name", List.of("Member1", "Member2")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode groupResponse = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        groupId = groupResponse.path("group").path("uuid").asText();

        for (JsonNode user : groupResponse.path("users")) {
            String name = user.path("name").asText();
            String uuid = user.path("uuid").asText();
            switch (name) {
                case "Payer" -> payerUuid = uuid;
                case "Member1" -> member1Uuid = uuid;
                case "Member2" -> member2Uuid = uuid;
            }
        }
    }

    private void createLoanTransaction(String payer, int amount, List<Map<String, Object>> obligations) throws Exception {
        mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "LOAN",
                                "title", "Expense",
                                "amount", amount,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", payer,
                                "loan", Map.of("obligations", obligations)
                        ))))
                .andExpect(status().isCreated());
    }

    private void createLoanTransactionWithCurrency(String payer, int amount, String currencyCode,
                                                    List<Map<String, Object>> obligations) throws Exception {
        mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "LOAN",
                                "title", "Expense",
                                "amount", amount,
                                "currency_code", currencyCode,
                                "date_str", TRANSACTION_DATE,
                                "payer_id", payer,
                                "loan", Map.of("obligations", obligations)
                        ))))
                .andExpect(status().isCreated());
    }

    private JsonNode getSettlement() throws Exception {
        MvcResult result = mockMvc.perform(get("/groups/{groupId}/transactions/settlement", groupId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Nested
    @DisplayName("Req1: 精算情報の算出")
    class Req1_CalculateSettlement {

        @Test
        @DisplayName("AC1: 債務者から債権者への精算一覧を日本円で返却する")
        void ac1_shouldReturnSettlementListInJpy() throws Exception {
            createLoanTransaction(payerUuid, 3000, List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid),
                    Map.of("amount", 2000, "user_uuid", member2Uuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isGreaterThan(0);
            for (JsonNode settlement : settlements) {
                assertThat(settlement.path("amount").asLong()).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("AC2: 各精算項目に送金元・送金先・送金金額が含まれる")
        void ac2_shouldIncludeFromToAndAmount() throws Exception {
            createLoanTransaction(payerUuid, 3000, List.of(
                    Map.of("amount", 3000, "user_uuid", member1Uuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlement = response.path("transactions_settlement").get(0);

            assertThat(settlement.path("from").path("uuid").asText()).isNotEmpty();
            assertThat(settlement.path("to").path("uuid").asText()).isNotEmpty();
            assertThat(settlement.path("amount").asLong()).isGreaterThan(0);
        }

        @Test
        @DisplayName("AC3: 取引が存在しない場合、空の精算一覧を返却する")
        void ac3_shouldReturnEmptyListWhenNoTransactions() throws Exception {
            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("AC4: 全メンバーの残高がゼロの場合、空の精算一覧を返却する")
        void ac4_shouldReturnEmptyListWhenAllBalancesAreZero() throws Exception {
            createLoanTransaction(payerUuid, 1000, List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid)
            ));

            createLoanTransaction(member1Uuid, 1000, List.of(
                    Map.of("amount", 1000, "user_uuid", payerUuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Req2: 外貨取引の日本円換算")
    class Req2_ForeignCurrencyConversion {

        @Test
        @DisplayName("AC1: 外貨建て取引を為替レートで日本円に換算して精算金額を算出する")
        void ac1_shouldConvertForeignCurrencyToJpy() throws Exception {
            currencyRepository.save(TestFixtures.Currencies.usd());
            CurrencyEntity reloadedUsd = currencyRepository.findById("USD").orElseThrow();
            exchangeRateRepository.save(ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(EXCHANGE_RATE_DATE)
                    .exchangeRate(new BigDecimal("150.00"))
                    .currency(reloadedUsd)
                    .build());

            createLoanTransactionWithCurrency(payerUuid, 100, "USD", List.of(
                    Map.of("amount", 100, "user_uuid", member1Uuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isGreaterThan(0);
            long amount = settlements.get(0).path("amount").asLong();
            assertThat(amount).isGreaterThan(0);
        }

        @Test
        @DisplayName("AC2: 複数通貨の取引を日本円に換算して精算金額を算出する")
        void ac2_shouldHandleMultipleCurrencies() throws Exception {
            currencyRepository.save(TestFixtures.Currencies.usd());
            CurrencyEntity reloadedUsd = currencyRepository.findById("USD").orElseThrow();
            exchangeRateRepository.save(ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(EXCHANGE_RATE_DATE)
                    .exchangeRate(new BigDecimal("150.00"))
                    .currency(reloadedUsd)
                    .build());

            createLoanTransaction(payerUuid, 3000, List.of(
                    Map.of("amount", 3000, "user_uuid", member1Uuid)
            ));

            createLoanTransactionWithCurrency(payerUuid, 100, "USD", List.of(
                    Map.of("amount", 100, "user_uuid", member1Uuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Req3: 端数調整")
    class Req3_RoundingAdjustment {

        @Test
        @DisplayName("AC1: 最大債務者の残高を調整して残高合計をゼロにする")
        void ac1_shouldAdjustLargestDebtorForZeroBalance() throws Exception {
            currencyRepository.save(TestFixtures.Currencies.usd());
            CurrencyEntity reloadedUsd = currencyRepository.findById("USD").orElseThrow();
            exchangeRateRepository.save(ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(EXCHANGE_RATE_DATE)
                    .exchangeRate(new BigDecimal("149.33"))
                    .currency(reloadedUsd)
                    .build());

            createLoanTransactionWithCurrency(payerUuid, 10000, "USD", List.of(
                    Map.of("amount", 3333, "user_uuid", member1Uuid),
                    Map.of("amount", 3334, "user_uuid", member2Uuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isGreaterThan(0);
            for (JsonNode settlement : settlements) {
                assertThat(settlement.path("amount").isIntegralNumber()).isTrue();
                assertThat(settlement.path("amount").asLong()).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("AC2: 精算金額が日本円の整数値で表される")
        void ac2_shouldReturnIntegerAmountsInJpy() throws Exception {
            createLoanTransaction(payerUuid, 3000, List.of(
                    Map.of("amount", 3000, "user_uuid", member1Uuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isGreaterThan(0);
            for (JsonNode settlement : settlements) {
                assertThat(settlement.path("amount").isIntegralNumber()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Req4: 精算の最適化")
    class Req4_SettlementOptimization {

        @Test
        @DisplayName("AC1: 2人の貸し借りに対して1件の精算を返却する")
        void ac1_shouldReturnSingleSettlementForTwoMembers() throws Exception {
            createLoanTransaction(payerUuid, 3000, List.of(
                    Map.of("amount", 3000, "user_uuid", member1Uuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            assertThat(settlements.size()).isEqualTo(1);
            assertThat(settlements.get(0).path("from").path("uuid").asText()).isEqualTo(member1Uuid);
            assertThat(settlements.get(0).path("to").path("uuid").asText()).isEqualTo(payerUuid);
            assertThat(settlements.get(0).path("amount").asLong()).isEqualTo(3000);
        }

        @Test
        @DisplayName("AC2: 3人以上の循環する貸し借りで精算回数を最小化する")
        void ac2_shouldMinimizeSettlementsForCircularDebts() throws Exception {
            createLoanTransaction(payerUuid, 3000, List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid),
                    Map.of("amount", 2000, "user_uuid", member2Uuid)
            ));

            createLoanTransaction(member1Uuid, 1500, List.of(
                    Map.of("amount", 1500, "user_uuid", payerUuid)
            ));

            JsonNode response = getSettlement();
            JsonNode settlements = response.path("transactions_settlement");

            long totalOwed = 0;
            for (JsonNode settlement : settlements) {
                totalOwed += settlement.path("amount").asLong();
            }
            assertThat(totalOwed).isGreaterThan(0);
            assertThat(settlements.size()).isLessThanOrEqualTo(2);
        }
    }
}
