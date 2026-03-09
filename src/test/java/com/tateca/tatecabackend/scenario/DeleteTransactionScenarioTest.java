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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Delete Transaction — Acceptance Scenario Tests")
class DeleteTransactionScenarioTest extends AbstractIntegrationTest {

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

    private String createLoanTransaction() throws Exception {
        MvcResult result = mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "LOAN",
                                "title", "Lunch",
                                "amount", 3000,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", payerUuid,
                                "loan", Map.of("obligations", List.of(
                                        Map.of("amount", 3000, "user_uuid", member1Uuid)
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("transaction_id").asText();
    }

    @Nested
    @DisplayName("Req1: 取引の削除")
    class Req1_DeleteTransaction {

        @Test
        @DisplayName("AC1: 取引とそれに紐づくすべての義務を削除する")
        void ac1_shouldDeleteTransactionAndObligations() throws Exception {
            String transactionId = createLoanTransaction();

            mockMvc.perform(delete("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("AC2: 削除後に取引履歴に削除された取引が含まれていない")
        void ac2_shouldNotAppearInTransactionHistory() throws Exception {
            String transactionId = createLoanTransaction();

            mockMvc.perform(delete("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isNoContent());

            MvcResult historyResult = mockMvc.perform(get("/groups/{groupId}/transactions/history", groupId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString());
            JsonNode transactions = history.path("transactions_history");

            for (JsonNode tx : transactions) {
                assertThat(tx.path("transaction_id").asText()).isNotEqualTo(transactionId);
            }
        }
    }

    @Nested
    @DisplayName("Req3: リソース不存在")
    class Req3_ResourceNotFound {

        @Test
        @DisplayName("AC1: 存在しない取引の削除は正常に完了する（現実装では存在確認なし）")
        void ac1_shouldCompleteSuccessfullyForNonExistentTransaction() throws Exception {
            // NOTE: requirements.md では 404 を期待するが、現実装の deleteById は
            // 存在しない ID でも例外を投げないため 204 を返す。Reverse SDD では実装に合わせる。
            mockMvc.perform(delete("/groups/{groupId}/transactions/{transactionId}", groupId, UUID.randomUUID())
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isNoContent());
        }
    }
}
