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
@DisplayName("Get Transaction History — Acceptance Scenario Tests")
class GetTransactionHistoryScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";
    private static final LocalDate EXCHANGE_RATE_DATE = LocalDate.of(2025, 1, 15);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;

    private String userUid;
    private String groupId;
    private String payerUuid;
    private String member1Uuid;

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
                                "participants_name", List.of("Member1")
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
            }
        }
    }

    private void createLoanTransaction(String title, int amount) throws Exception {
        mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "LOAN",
                                "title", title,
                                "amount", amount,
                                "currency_code", "JPY",
                                "date_str", "2025-01-15T12:00:00+09:00",
                                "payer_id", payerUuid,
                                "loan", Map.of("obligations", List.of(
                                        Map.of("amount", amount, "user_uuid", member1Uuid)
                                ))
                        ))))
                .andExpect(status().isCreated());
    }

    private JsonNode getTransactionHistory(Integer count) throws Exception {
        String url = (count != null)
                ? "/groups/" + groupId + "/transactions/history?count=" + count
                : "/groups/" + groupId + "/transactions/history";

        MvcResult result = mockMvc.perform(get(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Nested
    @DisplayName("Req1: 取引履歴の取得")
    class Req1_GetTransactionHistory {

        @Test
        @DisplayName("AC1: 作成日時の降順で取引概要の一覧を返却する")
        void ac1_shouldReturnTransactionsInDescendingOrder() throws Exception {
            createLoanTransaction("First", 1000);
            Thread.sleep(1100);
            createLoanTransaction("Second", 2000);
            Thread.sleep(1100);
            createLoanTransaction("Third", 3000);

            JsonNode response = getTransactionHistory(null);
            JsonNode transactions = response.path("transactions_history");

            assertThat(transactions.size()).isEqualTo(3);
            assertThat(transactions.get(0).path("title").asText()).isEqualTo("Third");
            assertThat(transactions.get(1).path("title").asText()).isEqualTo("Second");
            assertThat(transactions.get(2).path("title").asText()).isEqualTo("First");
        }

        @Test
        @DisplayName("AC2: 指定された件数を上限として取引概要の一覧を返却する")
        void ac2_shouldLimitResultsBySpecifiedCount() throws Exception {
            createLoanTransaction("Tx1", 1000);
            createLoanTransaction("Tx2", 2000);
            createLoanTransaction("Tx3", 3000);

            JsonNode response = getTransactionHistory(2);
            JsonNode transactions = response.path("transactions_history");

            assertThat(transactions.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("AC3: 取得件数を指定しない場合、既定取得件数（5件）を上限として返却する")
        void ac3_shouldReturnDefaultFiveTransactions() throws Exception {
            for (int i = 0; i < 7; i++) {
                createLoanTransaction("Tx" + i, 1000 + i);
            }

            JsonNode response = getTransactionHistory(null);
            JsonNode transactions = response.path("transactions_history");

            assertThat(transactions.size()).isEqualTo(5);
        }

        @Test
        @DisplayName("AC4: 取引が存在しない場合、空の一覧を返却する")
        void ac4_shouldReturnEmptyListWhenNoTransactions() throws Exception {
            JsonNode response = getTransactionHistory(null);
            JsonNode transactions = response.path("transactions_history");

            assertThat(transactions.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("AC5: 各取引概要に取引識別子・取引種別・タイトル・金額・通貨情報・日付が含まれる")
        void ac5_shouldIncludeRequiredFieldsInEachEntry() throws Exception {
            createLoanTransaction("Lunch", 3000);

            JsonNode response = getTransactionHistory(null);
            JsonNode entry = response.path("transactions_history").get(0);

            assertThat(entry.path("transaction_id").asText()).isNotEmpty();
            assertThat(entry.path("transaction_type").asText()).isEqualTo("LOAN");
            assertThat(entry.path("title").asText()).isEqualTo("Lunch");
            assertThat(entry.path("amount").asLong()).isEqualTo(3000);
            assertThat(entry.path("exchange_rate").path("currency_code").asText()).isEqualTo("JPY");
            assertThat(entry.path("date").asText()).isNotEmpty();
        }
    }
}
