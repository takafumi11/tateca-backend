package com.tateca.tatecabackend.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Get Transaction Detail — Acceptance Scenario Tests")
class GetTransactionDetailScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";
    private static final String TRANSACTION_DATE = "2025-01-15T12:00:00+09:00";
    private static final LocalDate EXCHANGE_RATE_DATE = LocalDate.of(2025, 1, 15);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DatabaseCleaner databaseCleaner;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;

    private String userUid;
    private String groupId;
    private String payerUuid;
    private String member1Uuid;
    private String member2Uuid;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleaner.clean();

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

    private String createLoanTransaction() throws Exception {
        MvcResult result = mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "LOAN",
                                "title", "Dinner",
                                "amount", 5000,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", payerUuid,
                                "loan", Map.of("obligations", List.of(
                                        Map.of("amount", 2000, "user_uuid", member1Uuid),
                                        Map.of("amount", 3000, "user_uuid", member2Uuid)
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("transaction_id").asText();
    }

    private String createRepaymentTransaction() throws Exception {
        MvcResult result = mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "REPAYMENT",
                                "title", "Repay dinner",
                                "amount", 2000,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", member1Uuid,
                                "repayment", Map.of("recipient_id", payerUuid)
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("transaction_id").asText();
    }

    @Nested
    @DisplayName("Req1: 立替取引の詳細取得")
    class Req1_GetLoanTransactionDetail {

        @Test
        @DisplayName("AC1: 立替取引の詳細に取引種別・タイトル・金額・支払者・通貨情報・日付・義務リストが含まれる")
        void ac1_shouldReturnLoanTransactionDetail() throws Exception {
            String transactionId = createLoanTransaction();

            MvcResult result = mockMvc.perform(get("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("transaction_type").asText()).isEqualTo("LOAN");
            assertThat(response.path("title").asText()).isEqualTo("Dinner");
            assertThat(response.path("amount").asLong()).isEqualTo(5000);
            assertThat(response.path("payer").path("uuid").asText()).isEqualTo(payerUuid);
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("JPY");
            assertThat(response.path("date_str").asText()).isNotEmpty();
            assertThat(response.path("loan").path("obligations").size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Req2: 返済取引の詳細取得")
    class Req2_GetRepaymentTransactionDetail {

        @Test
        @DisplayName("AC1: 返済取引の詳細に取引種別・タイトル・金額・支払者・通貨情報・日付・受取人が含まれる")
        void ac1_shouldReturnRepaymentTransactionDetail() throws Exception {
            String transactionId = createRepaymentTransaction();

            MvcResult result = mockMvc.perform(get("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("transaction_type").asText()).isEqualTo("REPAYMENT");
            assertThat(response.path("title").asText()).isEqualTo("Repay dinner");
            assertThat(response.path("amount").asLong()).isEqualTo(2000);
            assertThat(response.path("payer").path("uuid").asText()).isEqualTo(member1Uuid);
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("JPY");
            assertThat(response.path("date_str").asText()).isNotEmpty();
            assertThat(response.path("repayment").path("recipient").path("uuid").asText()).isEqualTo(payerUuid);
        }
    }

    @Nested
    @DisplayName("Req4: リソース不存在")
    class Req4_ResourceNotFound {

        @Test
        @DisplayName("AC1: 存在しない取引の詳細取得をリソース不在として拒否する")
        void ac1_shouldRejectWhenTransactionDoesNotExist() throws Exception {
            mockMvc.perform(get("/groups/{groupId}/transactions/{transactionId}", groupId, UUID.randomUUID())
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isNotFound());
        }
    }
}
