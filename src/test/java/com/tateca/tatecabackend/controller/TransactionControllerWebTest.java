package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.constants.BusinessConstants;
import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO.TransactionSettlement;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.dto.response.internal.ExchangeRateResponse;
import com.tateca.tatecabackend.dto.response.internal.LoanResponse;
import com.tateca.tatecabackend.dto.response.internal.ObligationResponse;
import com.tateca.tatecabackend.dto.response.internal.RepaymentResponse;
import com.tateca.tatecabackend.dto.response.internal.TransactionHistoryResponse;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.model.SymbolPosition;
import com.tateca.tatecabackend.model.TransactionType;
import com.tateca.tatecabackend.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("TransactionController Web Tests")
class TransactionControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TransactionService transactionService;

    private static final String BASE_ENDPOINT = "/groups/{groupId}/transactions";
    private static final UUID STUB_GROUP_ID = UUID.randomUUID();
    private static final UUID STUB_TRANSACTION_ID = UUID.randomUUID();
    private static final UUID STUB_PAYER_ID = UUID.randomUUID();
    private static final UUID STUB_OBLIGOR_ID = UUID.randomUUID();
    private static final UUID STUB_RECIPIENT_ID = UUID.randomUUID();

    private static final UserResponseDTO STUB_PAYER = new UserResponseDTO(
            STUB_PAYER_ID.toString(), "Payer", null, "2024-01-01T09:00:00+09:00", "2024-01-01T09:00:00+09:00");
    private static final UserResponseDTO STUB_OBLIGOR = new UserResponseDTO(
            STUB_OBLIGOR_ID.toString(), "Obligor", null, "2024-01-01T09:00:00+09:00", "2024-01-01T09:00:00+09:00");
    private static final UserResponseDTO STUB_RECIPIENT = new UserResponseDTO(
            STUB_RECIPIENT_ID.toString(), "Recipient", null, "2024-01-01T09:00:00+09:00", "2024-01-01T09:00:00+09:00");
    private static final ExchangeRateResponse STUB_EXCHANGE_RATE = new ExchangeRateResponse(
            "JPY", "日本円", "Japanese Yen", "日本", "Japan", "¥", SymbolPosition.PREFIX, "1");

    private static final CreateTransactionResponseDTO STUB_LOAN_RESPONSE = new CreateTransactionResponseDTO(
            STUB_TRANSACTION_ID.toString(), TransactionType.LOAN, "Dinner", 5000L, STUB_PAYER,
            STUB_EXCHANGE_RATE, "2024-01-15T18:30:00+09:00",
            new LoanResponse(List.of(new ObligationResponse(STUB_OBLIGOR, 5000L))), null);

    private static final CreateTransactionResponseDTO STUB_REPAYMENT_RESPONSE = new CreateTransactionResponseDTO(
            STUB_TRANSACTION_ID.toString(), TransactionType.REPAYMENT, "Repay", 3000L, STUB_PAYER,
            STUB_EXCHANGE_RATE, "2024-01-15T18:30:00+09:00",
            null, new RepaymentResponse(STUB_RECIPIENT));

    private CreateTransactionRequestDTO validLoanRequest() {
        return new CreateTransactionRequestDTO(
                TransactionType.LOAN, "Dinner", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                STUB_PAYER_ID,
                new CreateTransactionRequestDTO.Loan(List.of(
                        new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
    }

    private CreateTransactionRequestDTO validRepaymentRequest() {
        return new CreateTransactionRequestDTO(
                TransactionType.REPAYMENT, "Repay", 3000, "JPY", "2024-01-15T18:30:00+09:00",
                STUB_PAYER_ID, null,
                new CreateTransactionRequestDTO.Repayment(STUB_RECIPIENT_ID));
    }

    private UpdateTransactionRequestDTO validUpdateRequest() {
        return new UpdateTransactionRequestDTO(
                "Updated", 8000, "JPY", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                new UpdateTransactionRequestDTO.Loan(List.of(
                        new UpdateTransactionRequestDTO.Loan.Obligation(8000, STUB_OBLIGOR_ID))));
    }

    // =========================================================================
    // POST /groups/{groupId}/transactions — createTransaction
    // =========================================================================

    @Nested
    @DisplayName("POST /groups/{groupId}/transactions — createTransaction")
    class CreateTransaction {

        @Nested
        @DisplayName("201 Created")
        class Status201 {

            @Test
            @DisplayName("Should return 201 with LOAN TransactionResponse schema")
            void shouldReturn201WithLoanResponseSchema() throws Exception {
                when(transactionService.createTransaction(eq(STUB_GROUP_ID), any())).thenReturn(STUB_LOAN_RESPONSE);

                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validLoanRequest())))
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.transaction_id").value(STUB_TRANSACTION_ID.toString()))
                        .andExpect(jsonPath("$.transaction_type").value("LOAN"))
                        .andExpect(jsonPath("$.title").value("Dinner"))
                        .andExpect(jsonPath("$.amount").value(5000))
                        .andExpect(jsonPath("$.payer.uuid").value(STUB_PAYER_ID.toString()))
                        .andExpect(jsonPath("$.exchange_rate.currency_code").value("JPY"))
                        .andExpect(jsonPath("$.date_str").value("2024-01-15T18:30:00+09:00"))
                        .andExpect(jsonPath("$.loan.obligations").isArray())
                        .andExpect(jsonPath("$.loan.obligations[0].user.uuid").value(STUB_OBLIGOR_ID.toString()))
                        .andExpect(jsonPath("$.loan.obligations[0].amount").value(5000));

                verify(transactionService, times(1)).createTransaction(eq(STUB_GROUP_ID), any());
            }

            @Test
            @DisplayName("Should return 201 with REPAYMENT TransactionResponse schema")
            void shouldReturn201WithRepaymentResponseSchema() throws Exception {
                when(transactionService.createTransaction(eq(STUB_GROUP_ID), any())).thenReturn(STUB_REPAYMENT_RESPONSE);

                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRepaymentRequest())))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.transaction_type").value("REPAYMENT"))
                        .andExpect(jsonPath("$.repayment.recipient.uuid").value(STUB_RECIPIENT_ID.toString()));

                verify(transactionService, times(1)).createTransaction(eq(STUB_GROUP_ID), any());
            }
        }

        @Nested
        @DisplayName("400 Bad Request — Validation errors")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when transaction_type is null")
            void shouldReturn400WhenTransactionTypeIsNull() throws Exception {
                String json = """
                        {"transaction_type":null,"title":"X","amount":1,"currency_code":"JPY",
                         "date_str":"2024-01-15T18:30:00+09:00","payer_id":"%s"}""".formatted(STUB_PAYER_ID);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when title is blank")
            void shouldReturn400WhenTitleIsBlank() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "", 5000, "JPY", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when title exceeds 50 characters")
            void shouldReturn400WhenTitleExceedsMaxLength() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "A".repeat(51), 5000, "JPY", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when amount is null")
            void shouldReturn400WhenAmountIsNull() throws Exception {
                String json = """
                        {"transaction_type":"LOAN","title":"X","amount":null,"currency_code":"JPY",
                         "date_str":"2024-01-15T18:30:00+09:00","payer_id":"%s"}""".formatted(STUB_PAYER_ID);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when amount is zero")
            void shouldReturn400WhenAmountIsZero() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 0, "JPY", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when amount is negative")
            void shouldReturn400WhenAmountIsNegative() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", -1, "JPY", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when currency_code is blank")
            void shouldReturn400WhenCurrencyCodeIsBlank() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when currency_code pattern is invalid")
            void shouldReturn400WhenCurrencyCodePatternInvalid() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "jpy", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when date_str is blank")
            void shouldReturn400WhenDateIsBlank() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "JPY", "", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when date_str has no timezone")
            void shouldReturn400WhenDateHasNoTimezone() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "JPY", "2024-01-15T18:30:00", STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when payer_id is null")
            void shouldReturn400WhenPayerIdIsNull() throws Exception {
                String json = """
                        {"transaction_type":"LOAN","title":"X","amount":1,"currency_code":"JPY",
                         "date_str":"2024-01-15T18:30:00+09:00","payer_id":null}""";
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when LOAN has no loan details")
            void shouldReturn400WhenLoanHasNoLoanDetails() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID, null, null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when LOAN has repayment details")
            void shouldReturn400WhenLoanHasRepaymentDetails() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))),
                        new CreateTransactionRequestDTO.Repayment(STUB_RECIPIENT_ID));
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when REPAYMENT has no repayment details")
            void shouldReturn400WhenRepaymentHasNoRepaymentDetails() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.REPAYMENT, "Repay", 3000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID, null, null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when REPAYMENT has loan details")
            void shouldReturn400WhenRepaymentHasLoanDetails() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.REPAYMENT, "Repay", 3000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(3000, STUB_OBLIGOR_ID))),
                        new CreateTransactionRequestDTO.Repayment(STUB_RECIPIENT_ID));
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when obligations list is empty")
            void shouldReturn400WhenObligationsIsEmpty() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID, new CreateTransactionRequestDTO.Loan(List.of()), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when obligations exceeds max size")
            void shouldReturn400WhenObligationsExceedsMax() throws Exception {
                List<CreateTransactionRequestDTO.Loan.Obligation> obligations = new ArrayList<>();
                for (int i = 0; i < BusinessConstants.MAX_TRANSACTION_OBLIGATIONS + 1; i++) {
                    obligations.add(new CreateTransactionRequestDTO.Loan.Obligation(100, UUID.randomUUID()));
                }
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID, new CreateTransactionRequestDTO.Loan(obligations), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when obligation amount is zero")
            void shouldReturn400WhenObligationAmountIsZero() throws Exception {
                var req = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Dinner", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID,
                        new CreateTransactionRequestDTO.Loan(List.of(
                                new CreateTransactionRequestDTO.Loan.Obligation(0, STUB_OBLIGOR_ID))), null);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when obligation user_uuid is null")
            void shouldReturn400WhenObligationUserUuidIsNull() throws Exception {
                String json = """
                        {"transaction_type":"LOAN","title":"X","amount":1,"currency_code":"JPY",
                         "date_str":"2024-01-15T18:30:00+09:00","payer_id":"%s",
                         "loan":{"obligations":[{"amount":1,"user_uuid":null}]}}""".formatted(STUB_PAYER_ID);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when repayment recipient_id is null")
            void shouldReturn400WhenRecipientIdIsNull() throws Exception {
                String json = """
                        {"transaction_type":"REPAYMENT","title":"X","amount":1,"currency_code":"JPY",
                         "date_str":"2024-01-15T18:30:00+09:00","payer_id":"%s",
                         "repayment":{"recipient_id":null}}""".formatted(STUB_PAYER_ID);
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when groupId is invalid UUID")
            void shouldReturn400WhenGroupIdIsInvalidUuid() throws Exception {
                mockMvc.perform(post(BASE_ENDPOINT, "not-a-uuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validLoanRequest())))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when JSON is malformed")
            void shouldReturn400WhenJsonIsMalformed() throws Exception {
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid json"))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).createTransaction(any(), any());
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Status404 {

            @Test
            @DisplayName("Should return 404 when Service throws EntityNotFoundException")
            void shouldReturn404WhenEntityNotFound() throws Exception {
                when(transactionService.createTransaction(any(), any()))
                        .thenThrow(new EntityNotFoundException("Not found"));

                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validLoanRequest())))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404));

                verify(transactionService, times(1)).createTransaction(any(), any());
            }
        }

        @Nested
        @DisplayName("415 Unsupported Media Type")
        class Status415 {

            @Test
            @DisplayName("Should return 415 when Content-Type is missing")
            void shouldReturn415WhenContentTypeMissing() throws Exception {
                mockMvc.perform(post(BASE_ENDPOINT, STUB_GROUP_ID)
                                .content(objectMapper.writeValueAsString(validLoanRequest())))
                        .andExpect(status().isUnsupportedMediaType());
                verify(transactionService, never()).createTransaction(any(), any());
            }
        }
    }

    // =========================================================================
    // GET /groups/{groupId}/transactions/{transactionId} — getTransactionDetail
    // =========================================================================

    @Nested
    @DisplayName("GET /groups/{groupId}/transactions/{transactionId} — getTransactionDetail")
    class GetTransactionDetail {

        @Nested
        @DisplayName("200 OK")
        class Status200 {

            @Test
            @DisplayName("Should return 200 with TransactionResponse schema")
            void shouldReturn200WithTransactionResponseSchema() throws Exception {
                when(transactionService.getTransactionDetail(eq(STUB_TRANSACTION_ID))).thenReturn(STUB_LOAN_RESPONSE);

                mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.transaction_id").value(STUB_TRANSACTION_ID.toString()))
                        .andExpect(jsonPath("$.transaction_type").value("LOAN"))
                        .andExpect(jsonPath("$.title").value("Dinner"))
                        .andExpect(jsonPath("$.amount").value(5000))
                        .andExpect(jsonPath("$.payer.uuid").exists())
                        .andExpect(jsonPath("$.exchange_rate.currency_code").value("JPY"))
                        .andExpect(jsonPath("$.date_str").exists());

                verify(transactionService, times(1)).getTransactionDetail(eq(STUB_TRANSACTION_ID));
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when groupId is invalid UUID")
            void shouldReturn400WhenGroupIdInvalid() throws Exception {
                mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", "not-a-uuid", STUB_TRANSACTION_ID))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).getTransactionDetail(any());
            }

            @Test
            @DisplayName("Should return 400 when transactionId is invalid UUID")
            void shouldReturn400WhenTransactionIdInvalid() throws Exception {
                mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, "not-a-uuid"))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).getTransactionDetail(any());
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Status404 {

            @Test
            @DisplayName("Should return 404 when transaction not found")
            void shouldReturn404WhenTransactionNotFound() throws Exception {
                when(transactionService.getTransactionDetail(eq(STUB_TRANSACTION_ID)))
                        .thenThrow(new EntityNotFoundException("Transaction not found"));

                mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404));

                verify(transactionService, times(1)).getTransactionDetail(eq(STUB_TRANSACTION_ID));
            }
        }
    }

    // =========================================================================
    // PUT /groups/{groupId}/transactions/{transactionId} — updateTransaction
    // =========================================================================

    @Nested
    @DisplayName("PUT /groups/{groupId}/transactions/{transactionId} — updateTransaction")
    class UpdateTransaction {

        @Nested
        @DisplayName("200 OK")
        class Status200 {

            @Test
            @DisplayName("Should return 200 with updated TransactionResponse schema")
            void shouldReturn200WithUpdatedResponseSchema() throws Exception {
                when(transactionService.updateTransaction(eq(STUB_TRANSACTION_ID), any())).thenReturn(STUB_LOAN_RESPONSE);

                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.transaction_id").exists())
                        .andExpect(jsonPath("$.transaction_type").value("LOAN"))
                        .andExpect(jsonPath("$.title").exists())
                        .andExpect(jsonPath("$.amount").exists())
                        .andExpect(jsonPath("$.payer.uuid").exists())
                        .andExpect(jsonPath("$.exchange_rate.currency_code").exists())
                        .andExpect(jsonPath("$.loan.obligations").isArray());

                verify(transactionService, times(1)).updateTransaction(eq(STUB_TRANSACTION_ID), any());
            }
        }

        @Nested
        @DisplayName("400 Bad Request — Validation errors")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when title is blank")
            void shouldReturn400WhenTitleIsBlank() throws Exception {
                var req = new UpdateTransactionRequestDTO("", 5000, "JPY", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))));
                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).updateTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when amount is zero")
            void shouldReturn400WhenAmountIsZero() throws Exception {
                var req = new UpdateTransactionRequestDTO("X", 0, "JPY", "2024-01-15T18:30:00+09:00", STUB_PAYER_ID,
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(5000, STUB_OBLIGOR_ID))));
                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).updateTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when loan is null")
            void shouldReturn400WhenLoanIsNull() throws Exception {
                var req = new UpdateTransactionRequestDTO("X", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID, null);
                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).updateTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when obligations list is empty")
            void shouldReturn400WhenObligationsIsEmpty() throws Exception {
                var req = new UpdateTransactionRequestDTO("X", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        STUB_PAYER_ID, new UpdateTransactionRequestDTO.Loan(List.of()));
                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).updateTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when groupId is invalid UUID")
            void shouldReturn400WhenGroupIdInvalid() throws Exception {
                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", "not-a-uuid", STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).updateTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when transactionId is invalid UUID")
            void shouldReturn400WhenTransactionIdInvalid() throws Exception {
                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, "not-a-uuid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).updateTransaction(any(), any());
            }

            @Test
            @DisplayName("Should return 400 when REPAYMENT update is attempted (IllegalArgumentException)")
            void shouldReturn400WhenRepaymentUpdateAttempted() throws Exception {
                when(transactionService.updateTransaction(any(), any()))
                        .thenThrow(new IllegalArgumentException("Only LOAN transactions can be updated"));

                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400));

                verify(transactionService, times(1)).updateTransaction(any(), any());
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Status404 {

            @Test
            @DisplayName("Should return 404 when transaction not found")
            void shouldReturn404WhenTransactionNotFound() throws Exception {
                when(transactionService.updateTransaction(any(), any()))
                        .thenThrow(new EntityNotFoundException("Transaction not found"));

                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404));

                verify(transactionService, times(1)).updateTransaction(any(), any());
            }
        }

        @Nested
        @DisplayName("415 Unsupported Media Type")
        class Status415 {

            @Test
            @DisplayName("Should return 415 when Content-Type is missing")
            void shouldReturn415WhenContentTypeMissing() throws Exception {
                mockMvc.perform(put(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID)
                                .content(objectMapper.writeValueAsString(validUpdateRequest())))
                        .andExpect(status().isUnsupportedMediaType());
                verify(transactionService, never()).updateTransaction(any(), any());
            }
        }
    }

    // =========================================================================
    // DELETE /groups/{groupId}/transactions/{transactionId} — deleteTransaction
    // =========================================================================

    @Nested
    @DisplayName("DELETE /groups/{groupId}/transactions/{transactionId} — deleteTransaction")
    class DeleteTransaction {

        @Nested
        @DisplayName("204 No Content")
        class Status204 {

            @Test
            @DisplayName("Should return 204 when transaction is deleted")
            void shouldReturn204WhenDeleted() throws Exception {
                doNothing().when(transactionService).deleteTransaction(eq(STUB_TRANSACTION_ID));

                mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID))
                        .andExpect(status().isNoContent());

                verify(transactionService, times(1)).deleteTransaction(eq(STUB_TRANSACTION_ID));
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when groupId is invalid UUID")
            void shouldReturn400WhenGroupIdInvalid() throws Exception {
                mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", "not-a-uuid", STUB_TRANSACTION_ID))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).deleteTransaction(any());
            }

            @Test
            @DisplayName("Should return 400 when transactionId is invalid UUID")
            void shouldReturn400WhenTransactionIdInvalid() throws Exception {
                mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, "not-a-uuid"))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).deleteTransaction(any());
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Status404 {

            @Test
            @DisplayName("Should return 404 when Service throws EntityNotFoundException")
            void shouldReturn404WhenEntityNotFound() throws Exception {
                doThrow(new EntityNotFoundException("Transaction not found"))
                        .when(transactionService).deleteTransaction(eq(STUB_TRANSACTION_ID));

                mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", STUB_GROUP_ID, STUB_TRANSACTION_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404));

                verify(transactionService, times(1)).deleteTransaction(eq(STUB_TRANSACTION_ID));
            }
        }
    }

    // =========================================================================
    // GET /groups/{groupId}/transactions/history — getTransactionHistory
    // =========================================================================

    @Nested
    @DisplayName("GET /groups/{groupId}/transactions/history — getTransactionHistory")
    class GetTransactionHistory {

        @Nested
        @DisplayName("200 OK")
        class Status200 {

            @Test
            @DisplayName("Should return 200 with TransactionHistoryResponse schema")
            void shouldReturn200WithHistorySchema() throws Exception {
                var historyEntry = new TransactionHistoryResponse(
                        STUB_TRANSACTION_ID.toString(), TransactionType.LOAN, "Dinner",
                        5000, STUB_EXCHANGE_RATE, "2024-01-15T18:30:00+09:00");
                var response = new TransactionHistoryResponseDTO(List.of(historyEntry));

                when(transactionService.getTransactionHistory(anyInt(), eq(STUB_GROUP_ID))).thenReturn(response);

                mockMvc.perform(get(BASE_ENDPOINT + "/history", STUB_GROUP_ID).param("count", "10"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.transactions_history").isArray())
                        .andExpect(jsonPath("$.transactions_history[0].transaction_id").value(STUB_TRANSACTION_ID.toString()))
                        .andExpect(jsonPath("$.transactions_history[0].transaction_type").value("LOAN"))
                        .andExpect(jsonPath("$.transactions_history[0].title").value("Dinner"))
                        .andExpect(jsonPath("$.transactions_history[0].amount").value(5000))
                        .andExpect(jsonPath("$.transactions_history[0].exchange_rate.currency_code").value("JPY"))
                        .andExpect(jsonPath("$.transactions_history[0].date").exists());

                verify(transactionService, times(1)).getTransactionHistory(anyInt(), eq(STUB_GROUP_ID));
            }

            @Test
            @DisplayName("Should use default count when not provided")
            void shouldUseDefaultCount() throws Exception {
                when(transactionService.getTransactionHistory(anyInt(), eq(STUB_GROUP_ID)))
                        .thenReturn(new TransactionHistoryResponseDTO(List.of()));

                mockMvc.perform(get(BASE_ENDPOINT + "/history", STUB_GROUP_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactions_history").isArray());

                verify(transactionService, times(1)).getTransactionHistory(eq(5), eq(STUB_GROUP_ID));
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when groupId is invalid UUID")
            void shouldReturn400WhenGroupIdInvalid() throws Exception {
                mockMvc.perform(get(BASE_ENDPOINT + "/history", "not-a-uuid"))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).getTransactionHistory(anyInt(), any());
            }
        }
    }

    // =========================================================================
    // GET /groups/{groupId}/transactions/settlement — getTransactionSettlement
    // =========================================================================

    @Nested
    @DisplayName("GET /groups/{groupId}/transactions/settlement — getTransactionSettlement")
    class GetTransactionSettlement {

        @Nested
        @DisplayName("200 OK")
        class Status200 {

            @Test
            @DisplayName("Should return 200 with TransactionSettlementResponse schema")
            void shouldReturn200WithSettlementSchema() throws Exception {
                var settlement = new TransactionSettlement(STUB_OBLIGOR, STUB_PAYER, 3000L);
                var response = new TransactionSettlementResponseDTO(List.of(settlement));

                when(transactionService.getSettlements(eq(STUB_GROUP_ID))).thenReturn(response);

                mockMvc.perform(get(BASE_ENDPOINT + "/settlement", STUB_GROUP_ID))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.transactions_settlement").isArray())
                        .andExpect(jsonPath("$.transactions_settlement[0].from.uuid").value(STUB_OBLIGOR_ID.toString()))
                        .andExpect(jsonPath("$.transactions_settlement[0].to.uuid").value(STUB_PAYER_ID.toString()))
                        .andExpect(jsonPath("$.transactions_settlement[0].amount").value(3000));

                verify(transactionService, times(1)).getSettlements(eq(STUB_GROUP_ID));
            }
        }

        @Nested
        @DisplayName("400 Bad Request")
        class Status400 {

            @Test
            @DisplayName("Should return 400 when groupId is invalid UUID")
            void shouldReturn400WhenGroupIdInvalid() throws Exception {
                mockMvc.perform(get(BASE_ENDPOINT + "/settlement", "not-a-uuid"))
                        .andExpect(status().isBadRequest());
                verify(transactionService, never()).getSettlements(any());
            }
        }
    }
}
