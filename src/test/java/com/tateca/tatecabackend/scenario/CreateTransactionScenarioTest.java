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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Create Transaction — Acceptance Scenario Tests")
class CreateTransactionScenarioTest extends AbstractIntegrationTest {

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

        JsonNode users = groupResponse.path("users");
        for (JsonNode user : users) {
            String name = user.path("name").asText();
            String uuid = user.path("uuid").asText();
            switch (name) {
                case "Payer" -> payerUuid = uuid;
                case "Member1" -> member1Uuid = uuid;
                case "Member2" -> member2Uuid = uuid;
            }
        }
    }

    private MvcResult createLoanTransaction(String payer, List<Map<String, Object>> obligations) throws Exception {
        return mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "LOAN",
                                "title", "Lunch",
                                "amount", 3000,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", payer,
                                "loan", Map.of("obligations", obligations)
                        ))))
                .andReturn();
    }

    private MvcResult createRepaymentTransaction(String payer, String recipient) throws Exception {
        return mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(X_UID_HEADER, userUid)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transaction_type", "REPAYMENT",
                                "title", "Repay lunch",
                                "amount", 1000,
                                "currency_code", "JPY",
                                "date_str", TRANSACTION_DATE,
                                "payer_id", payer,
                                "repayment", Map.of("recipient_id", recipient)
                        ))))
                .andReturn();
    }

    @Nested
    @DisplayName("Req1: 立替取引の作成")
    class Req1_CreateLoanTransaction {

        @Test
        @DisplayName("AC1: 立替取引を作成し一意の識別子が割り当てられる")
        void ac1_shouldCreateLoanTransactionWithUniqueId() throws Exception {
            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid),
                    Map.of("amount", 2000, "user_uuid", member2Uuid)
            );

            MvcResult result = createLoanTransaction(payerUuid, obligations);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("transaction_id").asText()).isNotEmpty();
            assertThat(response.path("transaction_type").asText()).isEqualTo("LOAN");
        }

        @Test
        @DisplayName("AC2: 作成結果にすべての義務者と負担金額が含まれる")
        void ac2_shouldIncludeAllObligationsInResponse() throws Exception {
            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid),
                    Map.of("amount", 2000, "user_uuid", member2Uuid)
            );

            MvcResult result = createLoanTransaction(payerUuid, obligations);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            JsonNode loanObligations = response.path("loan").path("obligations");
            assertThat(loanObligations.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("AC3: 作成結果に支払者・タイトル・金額・通貨情報・日付が含まれる")
        void ac3_shouldIncludePayerTitleAmountCurrencyDate() throws Exception {
            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid)
            );

            MvcResult result = createLoanTransaction(payerUuid, obligations);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("payer").path("uuid").asText()).isEqualTo(payerUuid);
            assertThat(response.path("title").asText()).isEqualTo("Lunch");
            assertThat(response.path("amount").asLong()).isEqualTo(3000);
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("JPY");
            assertThat(response.path("date_str").asText()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Req2: 返済取引の作成")
    class Req2_CreateRepaymentTransaction {

        @Test
        @DisplayName("AC1: 返済取引を作成し一意の識別子が割り当てられる")
        void ac1_shouldCreateRepaymentTransactionWithUniqueId() throws Exception {
            MvcResult result = createRepaymentTransaction(payerUuid, member1Uuid);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("transaction_id").asText()).isNotEmpty();
            assertThat(response.path("transaction_type").asText()).isEqualTo("REPAYMENT");
        }

        @Test
        @DisplayName("AC2: 作成結果に受取人が含まれる")
        void ac2_shouldIncludeRecipientInResponse() throws Exception {
            MvcResult result = createRepaymentTransaction(payerUuid, member1Uuid);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("repayment").path("recipient").path("uuid").asText())
                    .isEqualTo(member1Uuid);
        }
    }

    @Nested
    @DisplayName("Req3: 為替レートの解決")
    class Req3_ExchangeRateResolution {

        @Test
        @DisplayName("AC1: 指定日の為替レートが存在する場合、そのレートで取引を作成する")
        void ac1_shouldUseExactDateExchangeRate() throws Exception {
            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid)
            );

            MvcResult result = createLoanTransaction(payerUuid, obligations);

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("JPY");
            assertThat(response.path("exchange_rate").path("exchange_rate").asDouble()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("AC2: 指定日のレートが存在しない場合、最新レートを代替として使用する")
        void ac2_shouldFallbackToLatestExchangeRate() throws Exception {
            currencyRepository.save(TestFixtures.Currencies.usd());
            CurrencyEntity reloadedUsd = currencyRepository.findById("USD").orElseThrow();
            exchangeRateRepository.save(ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(LocalDate.of(2025, 1, 10))
                    .exchangeRate(new BigDecimal("150.00"))
                    .currency(reloadedUsd)
                    .build());

            MvcResult result = mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "transaction_type", "LOAN",
                                    "title", "USD Transaction",
                                    "amount", 100,
                                    "currency_code", "USD",
                                    "date_str", TRANSACTION_DATE,
                                    "payer_id", payerUuid,
                                    "loan", Map.of("obligations", List.of(
                                            Map.of("amount", 100, "user_uuid", member1Uuid)
                                    ))
                            ))))
                    .andReturn();

            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.path("exchange_rate").path("currency_code").asText()).isEqualTo("USD");
            assertThat(response.path("exchange_rate").path("exchange_rate").asDouble()).isEqualTo(150.00);
        }

        @Test
        @DisplayName("AC3: 通貨の為替レートが一切存在しない場合、リソース不在として拒否する")
        void ac3_shouldRejectWhenNoExchangeRateExists() throws Exception {
            currencyRepository.save(TestFixtures.Currencies.eur());

            mockMvc.perform(post("/groups/{groupId}/transactions", groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "transaction_type", "LOAN",
                                    "title", "EUR Transaction",
                                    "amount", 100,
                                    "currency_code", "EUR",
                                    "date_str", TRANSACTION_DATE,
                                    "payer_id", payerUuid,
                                    "loan", Map.of("obligations", List.of(
                                            Map.of("amount", 100, "user_uuid", member1Uuid)
                                    ))
                            ))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Req7: リソース不存在")
    class Req7_ResourceNotFound {

        @Test
        @DisplayName("AC1: 存在しない支払者を指定した場合、リソース不在として拒否する")
        void ac1_shouldRejectWhenPayerDoesNotExist() throws Exception {
            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", member1Uuid)
            );

            MvcResult result = createLoanTransaction(UUID.randomUUID().toString(), obligations);

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("AC2: 存在しないグループを指定した場合、リソース不在として拒否する")
        void ac2_shouldRejectWhenGroupDoesNotExist() throws Exception {
            String nonExistentGroupId = UUID.randomUUID().toString();

            mockMvc.perform(post("/groups/{groupId}/transactions", nonExistentGroupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(X_UID_HEADER, userUid)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "transaction_type", "LOAN",
                                    "title", "Lunch",
                                    "amount", 1000,
                                    "currency_code", "JPY",
                                    "date_str", TRANSACTION_DATE,
                                    "payer_id", payerUuid,
                                    "loan", Map.of("obligations", List.of(
                                            Map.of("amount", 1000, "user_uuid", member1Uuid)
                                    ))
                            ))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("AC3: 存在しない義務者を指定した場合、リソース不在として拒否する")
        void ac3_shouldRejectWhenObligorDoesNotExist() throws Exception {
            List<Map<String, Object>> obligations = List.of(
                    Map.of("amount", 1000, "user_uuid", UUID.randomUUID().toString())
            );

            MvcResult result = createLoanTransaction(payerUuid, obligations);

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("AC4: 存在しない受取人を指定した場合、リソース不在として拒否する")
        void ac4_shouldRejectWhenRecipientDoesNotExist() throws Exception {
            MvcResult result = createRepaymentTransaction(payerUuid, UUID.randomUUID().toString());

            assertThat(result.getResponse().getStatus()).isEqualTo(404);
        }
    }
}
