package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.constants.BusinessConstants;
import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.dto.response.internal.ExchangeRateResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("TransactionController Web Tests")
class TransactionControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    private static final String BASE_ENDPOINT = "/groups/{groupId}/transactions";

    // ========================================
    // createTransaction Tests
    // ========================================

    @Nested
    @DisplayName("POST /groups/{groupId}/transactions - Create Transaction")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should return 201 CREATED when valid LOAN transaction is created")
        void shouldReturn201WhenValidLoanTransactionCreated() throws Exception {
            // Given: Valid LOAN request
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower1 = UUID.randomUUID();
            UUID borrower2 = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation1 =
                    new CreateTransactionRequestDTO.Loan.Obligation(2500, borrower1);
            CreateTransactionRequestDTO.Loan.Obligation obligation2 =
                    new CreateTransactionRequestDTO.Loan.Obligation(2500, borrower2);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation1, obligation2));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner at restaurant",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 201
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(transactionService, times(1)).createTransaction(eq(groupId), any(CreateTransactionRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 201 CREATED when valid REPAYMENT transaction is created")
        void shouldReturn201WhenValidRepaymentTransactionCreated() throws Exception {
            // Given: Valid REPAYMENT request
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();

            CreateTransactionRequestDTO.Repayment repayment =
                    new CreateTransactionRequestDTO.Repayment(recipientId);

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.REPAYMENT,
                    "Repayment for dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    null,
                    repayment
            );

            // When & Then: Should return 201
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(transactionService, times(1)).createTransaction(eq(groupId), any(CreateTransactionRequestDTO.class));
        }

        // ========================================
        // Basic Field Validation Tests
        // ========================================

        @Test
        @DisplayName("Should return 400 when transaction type is null")
        void shouldReturn400WhenTransactionTypeIsNull() throws Exception {
            // Given: Request with null transaction type
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":null,\"title\":\"Dinner\",\"amount\":5000,\"currency_code\":\"JPY\",\"date_str\":\"2024-01-15\",\"payer_id\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleIsBlank() throws Exception {
            // Given: Request with blank title
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":\"LOAN\",\"title\":\"\",\"amount\":5000,\"currency_code\":\"JPY\",\"date_str\":\"2024-01-15\",\"payer_id\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when title exceeds 50 characters")
        void shouldReturn400WhenTitleExceedsMaxLength() throws Exception {
            // Given: Title with 51 characters
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "A".repeat(51),  // 51 characters
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when amount is null")
        void shouldReturn400WhenAmountIsNull() throws Exception {
            // Given: Request with null amount
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":\"LOAN\",\"title\":\"Dinner\",\"amount\":null,\"currency_code\":\"JPY\",\"date_str\":\"2024-01-15\",\"payer_id\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            // Given: Request with zero amount
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    0,  // Invalid: must be positive
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when amount is negative")
        void shouldReturn400WhenAmountIsNegative() throws Exception {
            // Given: Request with negative amount
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    -1000,  // Invalid: must be positive
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when currency code is blank")
        void shouldReturn400WhenCurrencyCodeIsBlank() throws Exception {
            // Given: Request with blank currency code
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":\"LOAN\",\"title\":\"Dinner\",\"amount\":5000,\"currency_code\":\"\",\"date_str\":\"2024-01-15\",\"payer_id\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when currency code is lowercase")
        void shouldReturn400WhenCurrencyCodeIsLowercase() throws Exception {
            // Given: Request with lowercase currency code
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "jpy",  // Invalid: must be uppercase
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when currency code is not 3 characters")
        void shouldReturn400WhenCurrencyCodeIsNotThreeCharacters() throws Exception {
            // Given: Request with 2-character currency code
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JP",  // Invalid: must be 3 characters
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when date format is invalid (MM/DD/YYYY)")
        void shouldReturn400WhenDateFormatIsInvalid() throws Exception {
            // Given: Request with invalid date format
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "01/15/2024",  // Invalid: must be ISO 8601 with timezone
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when date has no timezone")
        void shouldReturn400WhenDateHasNoTimezone() throws Exception {
            // Given: Request with date missing timezone
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00",  // Invalid: missing timezone
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when date is date-only format")
        void shouldReturn400WhenDateIsDateOnly() throws Exception {
            // Given: Request with date-only format (no time)
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15",  // Invalid: must include time and timezone
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when payer ID is null")
        void shouldReturn400WhenPayerIdIsNull() throws Exception {
            // Given: Request with null payer ID
            UUID groupId = UUID.randomUUID();
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":\"LOAN\",\"title\":\"Dinner\",\"amount\":5000,\"currency_code\":\"JPY\",\"date_str\":\"2024-01-15\",\"payer_id\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        // ========================================
        // Custom Validation Tests (@ValidTransactionDetails)
        // ========================================

        @Test
        @DisplayName("Should return 400 when LOAN transaction has no loan details")
        void shouldReturn400WhenLoanTransactionHasNoLoanDetails() throws Exception {
            // Given: LOAN transaction without loan details
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    null,  // Invalid: loan details required for LOAN
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when LOAN transaction has repayment details")
        void shouldReturn400WhenLoanTransactionHasRepaymentDetails() throws Exception {
            // Given: LOAN transaction with both loan and repayment details
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));
            CreateTransactionRequestDTO.Repayment repayment =
                    new CreateTransactionRequestDTO.Repayment(recipientId);

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    repayment  // Invalid: repayment should not be provided for LOAN
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when REPAYMENT transaction has no repayment details")
        void shouldReturn400WhenRepaymentTransactionHasNoRepaymentDetails() throws Exception {
            // Given: REPAYMENT transaction without repayment details
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.REPAYMENT,
                    "Repayment",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    null,
                    null  // Invalid: repayment details required for REPAYMENT
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when REPAYMENT transaction has loan details")
        void shouldReturn400WhenRepaymentTransactionHasLoanDetails() throws Exception {
            // Given: REPAYMENT transaction with both loan and repayment details
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));
            CreateTransactionRequestDTO.Repayment repayment =
                    new CreateTransactionRequestDTO.Repayment(recipientId);

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.REPAYMENT,
                    "Repayment",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,  // Invalid: loan should not be provided for REPAYMENT
                    repayment
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        // ========================================
        // Nested Object Validation Tests
        // ========================================

        @Test
        @DisplayName("Should return 400 when obligations list is empty")
        void shouldReturn400WhenObligationsListIsEmpty() throws Exception {
            // Given: LOAN transaction with empty obligations list
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of());  // Invalid: empty obligations

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when obligations list exceeds maximum size")
        void shouldReturn400WhenObligationsListExceedsMaxSize() throws Exception {
            // Given: LOAN transaction with 10 obligations (exceeds max of 9)
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();

            List<CreateTransactionRequestDTO.Loan.Obligation> obligations = new ArrayList<>();
            for (int i = 0; i < BusinessConstants.MAX_TRANSACTION_OBLIGATIONS + 1; i++) {
                obligations.add(new CreateTransactionRequestDTO.Loan.Obligation(1000, UUID.randomUUID()));
            }

            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(obligations);

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    9000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when obligation amount is null")
        void shouldReturn400WhenObligationAmountIsNull() throws Exception {
            // Given: LOAN transaction with null obligation amount
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":\"LOAN\",\"title\":\"Dinner\",\"amount\":5000,\"currency_code\":\"JPY\",\"date_str\":\"2024-01-15\",\"payer_id\":\"" + payerId + "\",\"loan\":{\"obligations\":[{\"amount\":null,\"user_uuid\":\"" + borrower + "\"}]}}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when obligation amount is zero")
        void shouldReturn400WhenObligationAmountIsZero() throws Exception {
            // Given: LOAN transaction with zero obligation amount
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(0, borrower);  // Invalid: must be positive
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 400
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when obligation user UUID is null")
        void shouldReturn400WhenObligationUserUuidIsNull() throws Exception {
            // Given: LOAN transaction with null obligation user UUID
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();

            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":\"LOAN\",\"title\":\"Dinner\",\"amount\":5000,\"currency_code\":\"JPY\",\"date_str\":\"2024-01-15\",\"payer_id\":\"" + payerId + "\",\"loan\":{\"obligations\":[{\"amount\":2500,\"user_uuid\":null}]}}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when repayment recipient ID is null")
        void shouldReturn400WhenRepaymentRecipientIdIsNull() throws Exception {
            // Given: REPAYMENT transaction with null recipient ID
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();

            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transaction_type\":\"REPAYMENT\",\"title\":\"Repayment\",\"amount\":5000,\"currency_code\":\"JPY\",\"date_str\":\"2024-01-15\",\"payer_id\":\"" + payerId + "\",\"repayment\":{\"recipient_id\":null}}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(transactionService, never()).createTransaction(any(), any());
        }

        // ========================================
        // Media Type Validation Tests
        // ========================================

        @Test
        @DisplayName("Should return 415 when Content-Type is missing")
        void shouldReturn415WhenContentTypeIsMissing() throws Exception {
            // Given: Valid LOAN transaction JSON but no Content-Type header
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is not JSON")
        void shouldReturn415WhenContentTypeIsNotJson() throws Exception {
            // Given: Valid LOAN transaction JSON but wrong Content-Type
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(transactionService, never()).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type is XML")
        void shouldReturn415WhenContentTypeIsXml() throws Exception {
            // Given: Valid LOAN transaction JSON but XML Content-Type
            UUID groupId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();
            UUID borrower = UUID.randomUUID();

            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, borrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Dinner",
                    5000,
                    "JPY",
                    "2024-01-15T18:30:00+09:00",
                    payerId,
                    loan,
                    null
            );

            // When & Then: Should return 415 UNSUPPORTED_MEDIA_TYPE
            mockMvc.perform(post(BASE_ENDPOINT, groupId)
                            .contentType(MediaType.APPLICATION_XML)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(transactionService, never()).createTransaction(any(), any());
        }
    }

    // ========================================
    // getTransactionHistory Tests
    // ========================================

    @Nested
    @DisplayName("GET /groups/{groupId}/transactions/history - Get Transaction History")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("Should return 200 OK with transaction history")
        void shouldReturn200WithTransactionHistory() throws Exception {
            // Given: Group has transactions
            UUID groupId = UUID.randomUUID();
            int count = 10;

            TransactionHistoryResponseDTO expectedResponse =
                    new TransactionHistoryResponseDTO(List.of());

            when(transactionService.getTransactionHistory(anyInt(), eq(groupId)))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200
            mockMvc.perform(get(BASE_ENDPOINT + "/history", groupId)
                            .param("count", String.valueOf(count)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.transactions_history").isArray());

            verify(transactionService, times(1)).getTransactionHistory(anyInt(), eq(groupId));
        }

        @Test
        @DisplayName("Should return 200 OK with default count when count parameter is not provided")
        void shouldReturn200WithDefaultCount() throws Exception {
            // Given: No count parameter provided
            UUID groupId = UUID.randomUUID();

            TransactionHistoryResponseDTO expectedResponse =
                    new TransactionHistoryResponseDTO(List.of());

            when(transactionService.getTransactionHistory(anyInt(), eq(groupId)))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200 with default count (5)
            mockMvc.perform(get(BASE_ENDPOINT + "/history", groupId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.transactions_history").isArray());

            verify(transactionService, times(1)).getTransactionHistory(anyInt(), eq(groupId));
        }

        @Test
        @DisplayName("Should return 400 when groupId is invalid UUID")
        void shouldReturn400WhenInvalidUUID() throws Exception {
            // Given: Invalid UUID
            String invalidUUID = "not-a-uuid";

            // When & Then: Should return 400
            mockMvc.perform(get(BASE_ENDPOINT + "/history", invalidUUID)
                            .param("count", "10"))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).getTransactionHistory(anyInt(), any());
        }
    }

    // ========================================
    // getTransactionSettlement Tests
    // ========================================

    @Nested
    @DisplayName("GET /groups/{groupId}/transactions/settlement - Get Transaction Settlement")
    class GetTransactionSettlementTests {

        @Test
        @DisplayName("Should return 200 OK with settlement information")
        void shouldReturn200WithSettlementInfo() throws Exception {
            // Given: Group has settlements
            UUID groupId = UUID.randomUUID();

            TransactionSettlementResponseDTO expectedResponse =
                    new TransactionSettlementResponseDTO(List.of());

            when(transactionService.getSettlements(eq(groupId)))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200
            mockMvc.perform(get(BASE_ENDPOINT + "/settlement", groupId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.transactions_settlement").isArray());

            verify(transactionService, times(1)).getSettlements(eq(groupId));
        }

        @Test
        @DisplayName("Should return 404 when group not found")
        void shouldReturn404WhenGroupNotFound() throws Exception {
            // Given: Group does not exist
            UUID groupId = UUID.randomUUID();

            when(transactionService.getSettlements(eq(groupId)))
                    .thenThrow(new EntityNotFoundException("Group not found"));

            // When & Then: Should return 404
            mockMvc.perform(get(BASE_ENDPOINT + "/settlement", groupId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(transactionService, times(1)).getSettlements(eq(groupId));
        }

        @Test
        @DisplayName("Should return 400 when groupId is invalid UUID")
        void shouldReturn400WhenInvalidUUID() throws Exception {
            // Given: Invalid UUID
            String invalidUUID = "not-a-uuid";

            // When & Then: Should return 400
            mockMvc.perform(get(BASE_ENDPOINT + "/settlement", invalidUUID))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).getSettlements(any());
        }
    }

    // ========================================
    // getTransactionDetail Tests
    // ========================================

    @Nested
    @DisplayName("GET /groups/{groupId}/transactions/{transactionId} - Get Transaction Detail")
    class GetTransactionDetailTests {

        @Test
        @DisplayName("Should return 200 OK with transaction detail")
        void shouldReturn200WithTransactionDetail() throws Exception {
            // Given: Transaction exists
            UUID groupId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID payerId = UUID.randomUUID();

            UserResponseDTO payer = new UserResponseDTO(
                    payerId.toString(),
                    "Test Payer",
                    null,
                    "2024-01-01T09:00:00+09:00",
                    "2024-01-01T09:00:00+09:00"
            );

            ExchangeRateResponse exchangeRate = new ExchangeRateResponse(
                    "JPY",
                    "日本円",
                    "Japanese Yen",
                    "日本",
                    "Japan",
                    "¥",
                    SymbolPosition.PREFIX,
                    "1.00"
            );

            CreateTransactionResponseDTO expectedResponse =
                    new CreateTransactionResponseDTO(
                            transactionId.toString(),
                            TransactionType.LOAN,
                            "Test Transaction",
                            5000L,
                            payer,
                            exchangeRate,
                            "2024-01-15T18:00:00+09:00",
                            null,
                            null
                    );

            when(transactionService.getTransactionDetail(eq(transactionId)))
                    .thenReturn(expectedResponse);

            // When & Then: Should return 200
            mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", groupId, transactionId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.transaction_id").value(transactionId.toString()));

            verify(transactionService, times(1)).getTransactionDetail(eq(transactionId));
        }

        @Test
        @DisplayName("Should return 404 when transaction not found")
        void shouldReturn404WhenTransactionNotFound() throws Exception {
            // Given: Transaction does not exist
            UUID groupId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            when(transactionService.getTransactionDetail(eq(transactionId)))
                    .thenThrow(new EntityNotFoundException("Transaction not found"));

            // When & Then: Should return 404
            mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", groupId, transactionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(transactionService, times(1)).getTransactionDetail(eq(transactionId));
        }

        @Test
        @DisplayName("Should return 400 when groupId is invalid UUID")
        void shouldReturn400WhenInvalidGroupIdUUID() throws Exception {
            // Given: Invalid groupId UUID
            String invalidGroupId = "not-a-uuid";
            UUID transactionId = UUID.randomUUID();

            // When & Then: Should return 400
            mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", invalidGroupId, transactionId))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).getTransactionDetail(any());
        }

        @Test
        @DisplayName("Should return 400 when transactionId is invalid UUID")
        void shouldReturn400WhenInvalidTransactionIdUUID() throws Exception {
            // Given: Invalid transactionId UUID
            UUID groupId = UUID.randomUUID();
            String invalidTransactionId = "not-a-uuid";

            // When & Then: Should return 400
            mockMvc.perform(get(BASE_ENDPOINT + "/{transactionId}", groupId, invalidTransactionId))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).getTransactionDetail(any());
        }
    }

    // ========================================
    // deleteTransaction Tests
    // ========================================

    @Nested
    @DisplayName("DELETE /groups/{groupId}/transactions/{transactionId} - Delete Transaction")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should return 204 NO CONTENT when transaction is deleted")
        void shouldReturn204WhenTransactionDeleted() throws Exception {
            // Given: Transaction exists
            UUID groupId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            doNothing().when(transactionService).deleteTransaction(eq(transactionId));

            // When & Then: Should return 204
            mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", groupId, transactionId))
                    .andExpect(status().isNoContent());

            verify(transactionService, times(1)).deleteTransaction(eq(transactionId));
        }

        @Test
        @DisplayName("Should return 404 when transaction not found")
        void shouldReturn404WhenTransactionNotFound() throws Exception {
            // Given: Transaction does not exist
            UUID groupId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            doThrow(new EntityNotFoundException("Transaction not found"))
                    .when(transactionService).deleteTransaction(eq(transactionId));

            // When & Then: Should return 404
            mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", groupId, transactionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(transactionService, times(1)).deleteTransaction(eq(transactionId));
        }

        @Test
        @DisplayName("Should return 400 when groupId is invalid UUID")
        void shouldReturn400WhenInvalidGroupIdUUID() throws Exception {
            // Given: Invalid groupId UUID
            String invalidGroupId = "not-a-uuid";
            UUID transactionId = UUID.randomUUID();

            // When & Then: Should return 400
            mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", invalidGroupId, transactionId))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).deleteTransaction(any());
        }

        @Test
        @DisplayName("Should return 400 when transactionId is invalid UUID")
        void shouldReturn400WhenInvalidTransactionIdUUID() throws Exception {
            // Given: Invalid transactionId UUID
            UUID groupId = UUID.randomUUID();
            String invalidTransactionId = "not-a-uuid";

            // When & Then: Should return 400
            mockMvc.perform(delete(BASE_ENDPOINT + "/{transactionId}", groupId, invalidTransactionId))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).deleteTransaction(any());
        }
    }
}
