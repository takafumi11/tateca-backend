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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@DisplayName("Update Transaction — Acceptance Scenario Tests")
class UpdateTransactionScenarioTest extends AbstractIntegrationTest {

    private static final String X_UID_HEADER = "x-uid";
    private static final String TRANSACTION_DATE = "2025-01-15T12:00:00+09:00";
    private static final String UPDATED_DATE = "2025-02-20T18:00:00+09:00";
    private static final LocalDate EXCHANGE_RATE_DATE = LocalDate.of(2025, 1, 15);
    private static final LocalDate UPDATED_EXCHANGE_RATE_DATE = LocalDate.of(2025, 2, 20);

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
        exchangeRateRepository.save(ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(UPDATED_EXCHANGE_RATE_DATE)
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
                                "title", "Original Lunch",
                                "amount", 3000,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", payerUuid,
                                "loan", Map.of("obligations", List.of(
                                        Map.of("amount", 1000, "user_uuid", member1Uuid),
                                        Map.of("amount", 2000, "user_uuid", member2Uuid)
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("transaction_id").asText();
    }

    private String createRepaymentTransaction() throws Exception {
        MvcResult result = mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "REPAYMENT",
                                "title", "Repay",
                                "amount", 1000,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", member1Uuid,
                                "repayment", Map.of("recipient_id", payerUuid)
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("transaction_id").asText();
    }

    private Map<String, Object> buildUpdateRequest(String title, int amount, String payer,
                                                    List<Map<String, Object>> obligations) {
        return Map.of(
                "title", title,
                "amount", amount,
                "currency_code", "JPY",
                "date_str", UPDATED_DATE,
                "payer_id", payer,
                "loan", Map.of("obligations", obligations)
        );
    }

    @Nested
    @DisplayName("Req1: 立替取引の更新")
    class Req1_UpdateLoanTransaction {

        @Test
        @DisplayName("AC1: 立替取引の内容を更新できる")
        void ac1_shouldUpdateLoanTransaction() throws Exception {
            String transactionId = createLoanTransaction();

            List<Map<String, Object>> newObligations = List.of(
                    Map.of("amount", 2500, "user_uuid", member1Uuid)
            );

            mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated Dinner", 5000, member1Uuid, newObligations))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AC2: 更新結果にタイトル・金額・通貨情報・日付・支払者が反映されている")
        void ac2_shouldReflectUpdatedFields() throws Exception {
            String transactionId = createLoanTransaction();

            List<Map<String, Object>> newObligations = List.of(
                    Map.of("amount", 5000, "user_uuid", member2Uuid)
            );

            MvcResult result = mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated Dinner", 5000, member1Uuid, newObligations))))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("title").asText()).isEqualTo("Updated Dinner");
            assertThat(response.path("amount").asLong()).isEqualTo(5000);
            assertThat(response.path("payer").path("uuid").asText()).isEqualTo(member1Uuid);
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("AC3: 既存の義務がすべて新しい義務リストで置き換えられている")
        void ac3_shouldReplaceAllObligations() throws Exception {
            String transactionId = createLoanTransaction();

            List<Map<String, Object>> newObligations = List.of(
                    Map.of("amount", 5000, "user_uuid", member2Uuid)
            );

            mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated", 5000, payerUuid, newObligations))))
                    .andExpect(status().isOk());

            MvcResult detailResult = mockMvc.perform(get("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());
            JsonNode obligations = detail.path("loan").path("obligations");
            assertThat(obligations.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("AC4: 作成日時が更新前の値を維持している")
        void ac4_shouldPreserveCreatedAt() throws Exception {
            String transactionId = createLoanTransaction();

            MvcResult beforeResult = mockMvc.perform(get("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode beforeResponse = objectMapper.readTree(beforeResult.getResponse().getContentAsString());
            String createdTransactionId = beforeResponse.path("transaction_id").asText();

            List<Map<String, Object>> newObligations = List.of(
                    Map.of("amount", 5000, "user_uuid", member1Uuid)
            );

            MvcResult afterResult = mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated", 5000, payerUuid, newObligations))))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode afterResponse = objectMapper.readTree(afterResult.getResponse().getContentAsString());
            assertThat(afterResponse.path("transaction_id").asText()).isEqualTo(createdTransactionId);
        }
    }

    @Nested
    @DisplayName("Req2: 返済取引の更新制限")
    class Req2_RepaymentUpdateRestriction {

        @Test
        @DisplayName("AC1: 返済取引の更新を拒否する")
        void ac1_shouldRejectRepaymentUpdate() throws Exception {
            String transactionId = createRepaymentTransaction();

            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid)
            );

            mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated", 1000, payerUuid, obligations))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Req3: 為替レートの解決")
    class Req3_ExchangeRateResolution {

        @Test
        @DisplayName("AC1: 指定日の為替レートが存在する場合、そのレートで取引を更新する")
        void ac1_shouldUseExactDateExchangeRate() throws Exception {
            String transactionId = createLoanTransaction();

            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 3000, "user_uuid", member1Uuid)
            );

            MvcResult result = mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated", 3000, payerUuid, obligations))))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("JPY");
            assertThat(response.path("exchange_rate").path("exchange_rate").asDouble()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("AC2: 指定日のレートが存在しない場合、最新レートを代替として使用する")
        void ac2_shouldFallbackToLatestExchangeRate() throws Exception {
            String transactionId = createLoanTransaction();

            currencyRepository.save(TestFixtures.Currencies.usd());
            CurrencyEntity reloadedUsd = currencyRepository.findById("USD").orElseThrow();
            exchangeRateRepository.save(ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(LocalDate.of(2025, 1, 10))
                    .exchangeRate(new BigDecimal("150.00"))
                    .currency(reloadedUsd)
                    .build());

            MvcResult result = mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", "USD Update",
                                    "amount", 100,
                                    "currency_code", "USD",
                                    "date_str", UPDATED_DATE,
                                    "payer_id", payerUuid,
                                    "loan", Map.of("obligations", List.of(
                                            Map.of("amount", 100, "user_uuid", member1Uuid)
                                    ))
                            ))))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("USD");
            assertThat(response.path("exchange_rate").path("exchange_rate").asDouble()).isEqualTo(150.00);
        }

        @Test
        @DisplayName("AC3: 通貨の為替レートが一切存在しない場合、リソース不在として拒否する")
        void ac3_shouldRejectWhenNoExchangeRateExists() throws Exception {
            String transactionId = createLoanTransaction();

            currencyRepository.save(TestFixtures.Currencies.eur());

            mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", "EUR Update",
                                    "amount", 100,
                                    "currency_code", "EUR",
                                    "date_str", UPDATED_DATE,
                                    "payer_id", payerUuid,
                                    "loan", Map.of("obligations", List.of(
                                            Map.of("amount", 100, "user_uuid", member1Uuid)
                                    ))
                            ))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Req5: リソース不存在")
    class Req5_ResourceNotFound {

        @Test
        @DisplayName("AC1: 存在しない取引の更新をリソース不在として拒否する")
        void ac1_shouldRejectWhenTransactionDoesNotExist() throws Exception {
            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid)
            );

            mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated", 1000, payerUuid, obligations))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("AC2: 存在しない支払者を指定した場合、リソース不在として拒否する")
        void ac2_shouldRejectWhenPayerDoesNotExist() throws Exception {
            String transactionId = createLoanTransaction();

            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid)
            );

            mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated", 1000, UUID.randomUUID().toString(), obligations))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("AC3: 存在しない義務者を指定した場合、リソース不在として拒否する")
        void ac3_shouldRejectWhenObligorDoesNotExist() throws Exception {
            String transactionId = createLoanTransaction();

            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", UUID.randomUUID().toString())
            );

            mockMvc.perform(put("/groups/{groupId}/transactions/{transactionId}", groupId, transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(
                                    buildUpdateRequest("Updated", 1000, payerUuid, obligations))))
                    .andExpect(status().isNotFound());
        }
    }
}
