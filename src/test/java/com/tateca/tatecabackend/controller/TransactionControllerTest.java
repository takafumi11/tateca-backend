package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.constants.BusinessConstants;
import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("TransactionController Web Tests")
class TransactionControllerTest {

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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
        @DisplayName("Should return 400 when date format is invalid")
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
                    "01/15/2024",  // Invalid: must be yyyy-MM-dd
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
            // Given: LOAN transaction with 9 obligations (exceeds max of 8)
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
                    "2024-01-15",
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
}
