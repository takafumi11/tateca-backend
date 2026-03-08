package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO.TransactionSettlement;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.model.TransactionType;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.repository.GroupRepository;
import com.tateca.tatecabackend.repository.ObligationRepository;
import com.tateca.tatecabackend.repository.TransactionRepository;
import com.tateca.tatecabackend.repository.UserGroupRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl Unit Tests")
class TransactionServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ObligationRepository obligationRepository;
    @Mock private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private GroupEntity testGroup;
    private UserEntity testPayer;
    private UserEntity testBorrower;
    private CurrencyEntity jpyCurrency;
    private ExchangeRateEntity jpyExchangeRate;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");

    @BeforeEach
    void setUp() {
        testGroup = TestFixtures.Groups.defaultGroup();
        testPayer = TestFixtures.Users.userWithoutAuthUser("Test Payer");
        testBorrower = TestFixtures.Users.userWithoutAuthUser("Test Borrower");
        jpyCurrency = TestFixtures.Currencies.jpy();
        jpyExchangeRate = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(LocalDate.now())
                .exchangeRate(BigDecimal.ONE)
                .currency(jpyCurrency)
                .build();
    }

    private String todayDateStr() {
        return LocalDate.now().atStartOfDay().atZone(ZoneOffset.UTC).format(DATE_FORMAT);
    }

    // =========================================================================
    // createTransaction
    // =========================================================================

    @Nested
    @DisplayName("createTransaction")
    class CreateTransactionMethod {

        @Nested
        @DisplayName("Given valid LOAN request")
        class GivenValidLoanRequest {

            @Test
            @DisplayName("Should save transaction and obligations")
            void shouldSaveTransactionAndObligations() {
                var obligation = new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
                var loan = new CreateTransactionRequestDTO.Loan(List.of(obligation));
                var request = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Test Loan", 5000, "JPY",
                        todayDateStr(), testPayer.getUuid(), loan, null);

                var savedTransaction = buildTransaction(TransactionType.LOAN, "Test Loan", 5000);
                var savedObligation = buildObligation(savedTransaction, testBorrower, 5000);

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(groupRepository.findById(testGroup.getUuid())).thenReturn(Optional.of(testGroup));
                when(transactionRepository.save(any())).thenReturn(savedTransaction);
                when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
                when(obligationRepository.saveAll(anyList())).thenReturn(List.of(savedObligation));

                CreateTransactionResponseDTO result = transactionService.createTransaction(testGroup.getUuid(), request);

                verify(transactionRepository).save(any(TransactionHistoryEntity.class));
                verify(obligationRepository).saveAll(anyList());
                assertThat(result).isNotNull();
                assertThat(result.loan()).isNotNull();
            }
        }

        @Nested
        @DisplayName("Given valid REPAYMENT request")
        class GivenValidRepaymentRequest {

            @Test
            @DisplayName("Should save transaction and single obligation")
            void shouldSaveTransactionAndSingleObligation() {
                var repayment = new CreateTransactionRequestDTO.Repayment(testBorrower.getUuid());
                var request = new CreateTransactionRequestDTO(
                        TransactionType.REPAYMENT, "Test Repay", 3000, "JPY",
                        todayDateStr(), testPayer.getUuid(), null, repayment);

                var savedTransaction = buildTransaction(TransactionType.REPAYMENT, "Test Repay", 3000);
                var savedObligation = buildObligation(savedTransaction, testBorrower, 3000);

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(groupRepository.findById(testGroup.getUuid())).thenReturn(Optional.of(testGroup));
                when(transactionRepository.save(any())).thenReturn(savedTransaction);
                when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
                when(obligationRepository.save(any(TransactionObligationEntity.class))).thenReturn(savedObligation);

                CreateTransactionResponseDTO result = transactionService.createTransaction(testGroup.getUuid(), request);

                verify(transactionRepository).save(any(TransactionHistoryEntity.class));
                verify(obligationRepository).save(any(TransactionObligationEntity.class));
                assertThat(result).isNotNull();
                assertThat(result.repayment()).isNotNull();
            }
        }

        @Nested
        @DisplayName("Given exchange rate exists for specified date")
        class GivenExchangeRateExistsForDate {

            @Test
            @DisplayName("Should use existing exchange rate without creating new one")
            void shouldUseExistingExchangeRate() {
                var request = buildLoanRequest("JPY", todayDateStr());

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(groupRepository.findById(testGroup.getUuid())).thenReturn(Optional.of(testGroup));
                when(transactionRepository.save(any())).thenReturn(buildTransaction(TransactionType.LOAN, "Test", 5000));
                when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
                when(obligationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

                transactionService.createTransaction(testGroup.getUuid(), request);

                verify(exchangeRateRepository, never()).save(any(ExchangeRateEntity.class));
            }
        }

        @Nested
        @DisplayName("Given exchange rate does not exist for specified date")
        class GivenExchangeRateNotExistsForDate {

            @Test
            @DisplayName("Should fall back to latest rate and save new rate for the date")
            void shouldFallbackToLatestRateAndSave() {
                LocalDate futureDate = LocalDate.now().plusDays(10);
                String futureDateStr = futureDate.atStartOfDay().atZone(ZoneOffset.UTC).format(DATE_FORMAT);
                var request = buildLoanRequest("JPY", futureDateStr);

                var latestRate = ExchangeRateEntity.builder()
                        .currencyCode("JPY").date(LocalDate.now())
                        .exchangeRate(BigDecimal.ONE).currency(jpyCurrency).build();
                var newRate = ExchangeRateEntity.builder()
                        .currencyCode("JPY").date(futureDate)
                        .exchangeRate(BigDecimal.ONE).currency(jpyCurrency).build();

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), eq(futureDate)))
                        .thenReturn(Optional.empty());
                when(exchangeRateRepository.findLatestByCurrencyCode("JPY"))
                        .thenReturn(Optional.of(latestRate));
                when(exchangeRateRepository.save(any(ExchangeRateEntity.class))).thenReturn(newRate);
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(groupRepository.findById(testGroup.getUuid())).thenReturn(Optional.of(testGroup));
                when(transactionRepository.save(any())).thenReturn(
                        buildTransactionWithRate(TransactionType.LOAN, "Test", 5000, newRate));
                when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
                when(obligationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

                transactionService.createTransaction(testGroup.getUuid(), request);

                verify(exchangeRateRepository).save(any(ExchangeRateEntity.class));
            }
        }

        @Nested
        @DisplayName("Given no exchange rate exists for currency at all")
        class GivenNoExchangeRateExists {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                var request = buildLoanRequest("USD", todayDateStr());

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("USD"), any(LocalDate.class)))
                        .thenReturn(Optional.empty());
                when(exchangeRateRepository.findLatestByCurrencyCode("USD"))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.createTransaction(testGroup.getUuid(), request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("No exchange rate found");

                verify(transactionRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("Given payer does not exist")
        class GivenPayerNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                var request = buildLoanRequest("JPY", todayDateStr());

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.createTransaction(testGroup.getUuid(), request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("User not found");

                verify(transactionRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("Given group does not exist")
        class GivenGroupNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                var request = buildLoanRequest("JPY", todayDateStr());

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(groupRepository.findById(testGroup.getUuid())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.createTransaction(testGroup.getUuid(), request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("Group not found");

                verify(transactionRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("Given LOAN obligor does not exist")
        class GivenLoanObligorNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                UUID nonExistentUser = UUID.randomUUID();
                var obligation = new CreateTransactionRequestDTO.Loan.Obligation(5000, nonExistentUser);
                var loan = new CreateTransactionRequestDTO.Loan(List.of(obligation));
                var request = new CreateTransactionRequestDTO(
                        TransactionType.LOAN, "Test", 5000, "JPY",
                        todayDateStr(), testPayer.getUuid(), loan, null);

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(groupRepository.findById(testGroup.getUuid())).thenReturn(Optional.of(testGroup));
                when(transactionRepository.save(any())).thenReturn(buildTransaction(TransactionType.LOAN, "Test", 5000));
                when(userRepository.findById(nonExistentUser)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.createTransaction(testGroup.getUuid(), request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("User not found");

                verify(obligationRepository, never()).saveAll(anyList());
            }
        }

        @Nested
        @DisplayName("Given REPAYMENT recipient does not exist")
        class GivenRepaymentRecipientNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                UUID nonExistentRecipient = UUID.randomUUID();
                var repayment = new CreateTransactionRequestDTO.Repayment(nonExistentRecipient);
                var request = new CreateTransactionRequestDTO(
                        TransactionType.REPAYMENT, "Repay", 3000, "JPY",
                        todayDateStr(), testPayer.getUuid(), null, repayment);

                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(groupRepository.findById(testGroup.getUuid())).thenReturn(Optional.of(testGroup));
                when(transactionRepository.save(any())).thenReturn(
                        buildTransaction(TransactionType.REPAYMENT, "Repay", 3000));
                when(userRepository.findById(nonExistentRecipient)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.createTransaction(testGroup.getUuid(), request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("User not found");

                verify(obligationRepository, never()).save(any(TransactionObligationEntity.class));
            }
        }
    }

    // =========================================================================
    // getTransactionDetail
    // =========================================================================

    @Nested
    @DisplayName("getTransactionDetail")
    class GetTransactionDetailMethod {

        @Nested
        @DisplayName("Given LOAN transaction exists")
        class GivenLoanTransactionExists {

            @Test
            @DisplayName("Should return detail with loan obligations list")
            void shouldReturnDetailWithLoanObligationsList() {
                UUID transactionId = UUID.randomUUID();
                var transaction = buildTransaction(TransactionType.LOAN, "Test Loan", 5000);
                transaction.setUuid(transactionId);
                var obligation = buildObligation(transaction, testBorrower, 5000);

                when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
                when(obligationRepository.findByTransactionId(transactionId)).thenReturn(List.of(obligation));

                CreateTransactionResponseDTO result = transactionService.getTransactionDetail(transactionId);

                assertThat(result.loan()).isNotNull();
                assertThat(result.repayment()).isNull();
                verify(transactionRepository).findById(transactionId);
                verify(obligationRepository).findByTransactionId(transactionId);
            }
        }

        @Nested
        @DisplayName("Given REPAYMENT transaction exists")
        class GivenRepaymentTransactionExists {

            @Test
            @DisplayName("Should return detail with repayment recipient")
            void shouldReturnDetailWithRepaymentRecipient() {
                UUID transactionId = UUID.randomUUID();
                var transaction = buildTransaction(TransactionType.REPAYMENT, "Test Repay", 3000);
                transaction.setUuid(transactionId);
                var obligation = buildObligation(transaction, testBorrower, 3000);

                when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
                when(obligationRepository.findByTransactionId(transactionId)).thenReturn(List.of(obligation));

                CreateTransactionResponseDTO result = transactionService.getTransactionDetail(transactionId);

                assertThat(result.repayment()).isNotNull();
                assertThat(result.loan()).isNull();
            }
        }

        @Nested
        @DisplayName("Given transaction does not exist")
        class GivenTransactionNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                UUID transactionId = UUID.randomUUID();
                when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> transactionService.getTransactionDetail(transactionId))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("Transaction not found");

                verify(obligationRepository, never()).findByTransactionId(any());
            }
        }
    }

    // =========================================================================
    // getTransactionHistory
    // =========================================================================

    @Nested
    @DisplayName("getTransactionHistory")
    class GetTransactionHistoryMethod {

        @Test
        @DisplayName("Should delegate to repository with correct parameters")
        void shouldDelegateToRepositoryWithCorrectParameters() {
            UUID groupId = UUID.randomUUID();
            int count = 10;
            when(transactionRepository.findTransactionsByGroupWithLimit(groupId, count))
                    .thenReturn(new ArrayList<>());

            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(count, groupId);

            verify(transactionRepository).findTransactionsByGroupWithLimit(groupId, count);
            assertThat(result).isNotNull();
            assertThat(result.transactionsHistory()).isEmpty();
        }

        @Test
        @DisplayName("Should map repository results to response DTO")
        void shouldMapRepositoryResultsToResponseDto() {
            UUID groupId = UUID.randomUUID();
            var transaction = buildTransaction(TransactionType.LOAN, "Test", 5000);

            when(transactionRepository.findTransactionsByGroupWithLimit(groupId, 10))
                    .thenReturn(List.of(transaction));

            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(10, groupId);

            assertThat(result.transactionsHistory()).hasSize(1);
        }
    }

    // =========================================================================
    // deleteTransaction
    // =========================================================================

    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransactionMethod {

        @Test
        @DisplayName("Should delete obligations before transaction")
        void shouldDeleteObligationsBeforeTransaction() {
            UUID transactionId = UUID.randomUUID();
            var ob1 = TransactionObligationEntity.builder().uuid(UUID.randomUUID()).build();
            var ob2 = TransactionObligationEntity.builder().uuid(UUID.randomUUID()).build();

            when(obligationRepository.findByTransactionId(transactionId))
                    .thenReturn(List.of(ob1, ob2));

            transactionService.deleteTransaction(transactionId);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UUID>> idsCaptor = ArgumentCaptor.forClass(List.class);
            verify(obligationRepository).deleteAllById(idsCaptor.capture());
            verify(transactionRepository).deleteById(transactionId);

            assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(ob1.getUuid(), ob2.getUuid());
        }

        @Test
        @DisplayName("Should handle transaction with no obligations")
        void shouldHandleTransactionWithNoObligations() {
            UUID transactionId = UUID.randomUUID();
            when(obligationRepository.findByTransactionId(transactionId))
                    .thenReturn(new ArrayList<>());

            transactionService.deleteTransaction(transactionId);

            verify(obligationRepository).deleteAllById(new ArrayList<>());
            verify(transactionRepository).deleteById(transactionId);
        }
    }

    // =========================================================================
    // updateTransaction
    // =========================================================================

    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransactionMethod {

        private UUID transactionId;
        private TransactionHistoryEntity existingLoanTransaction;

        @BeforeEach
        void setUpUpdate() {
            transactionId = UUID.randomUUID();
            existingLoanTransaction = TransactionHistoryEntity.builder()
                    .uuid(transactionId)
                    .transactionType(TransactionType.LOAN)
                    .title("Original")
                    .amount(5000)
                    .transactionDate(Instant.now().minusSeconds(86400))
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .createdAt(Instant.now().minusSeconds(86400))
                    .updatedAt(Instant.now().minusSeconds(86400))
                    .build();
        }

        @Nested
        @DisplayName("Given valid LOAN update request")
        class GivenValidLoanUpdate {

            @Test
            @DisplayName("Should update all fields and replace obligations")
            void shouldUpdateAllFieldsAndReplaceObligations() {
                UserEntity newPayer = TestFixtures.Users.userWithoutAuthUser("New Payer");
                UserEntity obligUser = TestFixtures.Users.userWithoutAuthUser("Oblig User");
                CurrencyEntity usdCurrency = TestFixtures.Currencies.usd();
                ExchangeRateEntity usdRate = ExchangeRateEntity.builder()
                        .currencyCode("USD").date(LocalDate.now())
                        .exchangeRate(new BigDecimal("150.0")).currency(usdCurrency).build();

                when(transactionRepository.findById(transactionId))
                        .thenReturn(Optional.of(existingLoanTransaction));
                when(userRepository.findById(newPayer.getUuid())).thenReturn(Optional.of(newPayer));
                when(userRepository.findById(obligUser.getUuid())).thenReturn(Optional.of(obligUser));
                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("USD"), any(LocalDate.class)))
                        .thenReturn(Optional.of(usdRate));
                when(transactionRepository.save(any())).thenReturn(existingLoanTransaction);
                when(obligationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

                var request = new UpdateTransactionRequestDTO(
                        "Updated Title", 10000, "USD", "2024-02-20T12:00:00+09:00",
                        newPayer.getUuid(),
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(10000, obligUser.getUuid()))));

                transactionService.updateTransaction(transactionId, request);

                verify(obligationRepository).deleteAllByTransactionId(transactionId);
                verify(obligationRepository).saveAll(anyList());

                ArgumentCaptor<TransactionHistoryEntity> captor =
                        ArgumentCaptor.forClass(TransactionHistoryEntity.class);
                verify(transactionRepository).save(captor.capture());

                var saved = captor.getValue();
                assertThat(saved.getTitle()).isEqualTo("Updated Title");
                assertThat(saved.getAmount()).isEqualTo(10000);
                assertThat(saved.getPayer().getUuid()).isEqualTo(newPayer.getUuid());
                assertThat(saved.getExchangeRate().getCurrencyCode()).isEqualTo("USD");
            }

            @Test
            @DisplayName("Should preserve created_at timestamp")
            void shouldPreserveCreatedAtTimestamp() {
                Instant originalCreatedAt = Instant.parse("2024-01-01T00:00:00Z");
                existingLoanTransaction.setCreatedAt(originalCreatedAt);
                UserEntity obligUser = TestFixtures.Users.userWithoutAuthUser("OUser");

                when(transactionRepository.findById(transactionId))
                        .thenReturn(Optional.of(existingLoanTransaction));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(userRepository.findById(obligUser.getUuid())).thenReturn(Optional.of(obligUser));
                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));
                when(transactionRepository.save(any())).thenReturn(existingLoanTransaction);
                when(obligationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

                var request = new UpdateTransactionRequestDTO(
                        "Updated", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        testPayer.getUuid(),
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(5000, obligUser.getUuid()))));

                transactionService.updateTransaction(transactionId, request);

                ArgumentCaptor<TransactionHistoryEntity> captor =
                        ArgumentCaptor.forClass(TransactionHistoryEntity.class);
                verify(transactionRepository).save(captor.capture());
                assertThat(captor.getValue().getCreatedAt()).isEqualTo(originalCreatedAt);
            }
        }

        @Nested
        @DisplayName("Given REPAYMENT transaction")
        class GivenRepaymentTransaction {

            @Test
            @DisplayName("Should throw IllegalArgumentException")
            void shouldThrowIllegalArgumentException() {
                var repaymentTransaction = TransactionHistoryEntity.builder()
                        .uuid(transactionId)
                        .transactionType(TransactionType.REPAYMENT)
                        .title("Repay").amount(5000)
                        .transactionDate(Instant.now())
                        .payer(testPayer).group(testGroup).exchangeRate(jpyExchangeRate)
                        .build();

                when(transactionRepository.findById(transactionId))
                        .thenReturn(Optional.of(repaymentTransaction));

                var request = new UpdateTransactionRequestDTO(
                        "Title", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        testPayer.getUuid(),
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid()))));

                assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Only LOAN transactions can be updated");

                verify(transactionRepository, never()).save(any());
                verify(obligationRepository, never()).deleteAllByTransactionId(any());
            }
        }

        @Nested
        @DisplayName("Given transaction does not exist")
        class GivenTransactionNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

                var request = new UpdateTransactionRequestDTO(
                        "Title", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        testPayer.getUuid(),
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid()))));

                assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("Transaction not found");
            }
        }

        @Nested
        @DisplayName("Given payer does not exist")
        class GivenPayerNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                UUID nonExistentPayer = UUID.randomUUID();
                when(transactionRepository.findById(transactionId))
                        .thenReturn(Optional.of(existingLoanTransaction));
                when(userRepository.findById(nonExistentPayer)).thenReturn(Optional.empty());

                var request = new UpdateTransactionRequestDTO(
                        "Title", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        nonExistentPayer,
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid()))));

                assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("not found");
            }
        }

        @Nested
        @DisplayName("Given obligation user does not exist")
        class GivenObligationUserNotFound {

            @Test
            @DisplayName("Should throw EntityNotFoundException")
            void shouldThrowEntityNotFoundException() {
                UUID nonExistentUser = UUID.randomUUID();
                when(transactionRepository.findById(transactionId))
                        .thenReturn(Optional.of(existingLoanTransaction));
                when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
                when(userRepository.findById(nonExistentUser)).thenReturn(Optional.empty());
                when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                        .thenReturn(Optional.of(jpyExchangeRate));

                var request = new UpdateTransactionRequestDTO(
                        "Title", 5000, "JPY", "2024-01-15T18:30:00+09:00",
                        testPayer.getUuid(),
                        new UpdateTransactionRequestDTO.Loan(List.of(
                                new UpdateTransactionRequestDTO.Loan.Obligation(5000, nonExistentUser))));

                assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("not found");
            }
        }
    }

    // =========================================================================
    // getSettlements
    // =========================================================================

    @Nested
    @DisplayName("getSettlements")
    class GetSettlementsMethod {

        @Nested
        @DisplayName("Given no obligations in group")
        class GivenNoObligations {

            @Test
            @DisplayName("Should return empty settlement list")
            void shouldReturnEmptySettlementList() {
                UUID groupId = UUID.randomUUID();
                when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(new ArrayList<>());
                when(obligationRepository.findByGroupId(groupId))
                        .thenReturn(new ArrayList<>());

                TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

                assertThat(result.transactionsSettlement()).isEmpty();
                verify(userGroupRepository).findByGroupUuidWithUserDetails(groupId);
                verify(obligationRepository).findByGroupId(groupId);
            }
        }

        @Nested
        @DisplayName("Given simple two-person loan")
        class GivenSimpleTwoPersonLoan {

            @Test
            @DisplayName("Should calculate single settlement from debtor to creditor")
            void shouldCalculateSingleSettlement() {
                UUID groupId = UUID.randomUUID();
                UserEntity alice = createUser("Alice", "s1");
                UserEntity bob = createUser("Bob", "s1");
                ExchangeRateEntity jpyRate = createExchangeRate("JPY", BigDecimal.ONE);

                var tx = createTransaction(alice, 5000, jpyRate);
                var ob = createObligation(tx, bob, 5000);

                when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(List.of(ob)));
                when(obligationRepository.findByGroupId(groupId)).thenReturn(List.of(ob));

                TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

                assertThat(result.transactionsSettlement()).hasSize(1);
                var settlement = result.transactionsSettlement().getFirst();
                assertThat(settlement.from().uuid()).isEqualTo(bob.getUuid().toString());
                assertThat(settlement.to().uuid()).isEqualTo(alice.getUuid().toString());
                assertThat(settlement.amount()).isEqualTo(5000);
            }
        }

        @Nested
        @DisplayName("Given all users have balanced accounts (self-payment)")
        class GivenAllUsersBalanced {

            @Test
            @DisplayName("Should return empty settlement list")
            void shouldReturnEmptySettlementList() {
                UUID groupId = UUID.randomUUID();
                UserEntity alice = createUser("Alice", "s2");
                ExchangeRateEntity jpyRate = createExchangeRate("JPY", BigDecimal.ONE);

                var tx = createTransaction(alice, 10000, jpyRate);
                var ob = createObligation(tx, alice, 10000);

                when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(List.of(ob)));
                when(obligationRepository.findByGroupId(groupId)).thenReturn(List.of(ob));

                TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

                assertThat(result.transactionsSettlement()).isEmpty();
            }
        }

        @Nested
        @DisplayName("Given three-person group with circular debts")
        class GivenThreePersonCircularDebts {

            @Test
            @DisplayName("Should optimize settlements and conserve money")
            void shouldOptimizeSettlementsAndConserveMoney() {
                UUID groupId = UUID.randomUUID();
                UserEntity alice = createUser("Alice", "s3");
                UserEntity bob = createUser("Bob", "s3");
                UserEntity carol = createUser("Carol", "s3");
                ExchangeRateEntity jpyRate = createExchangeRate("JPY", BigDecimal.ONE);

                var tx1 = createTransaction(alice, 15000, jpyRate);
                var ob1a = createObligation(tx1, bob, 7500);
                var ob1b = createObligation(tx1, carol, 7500);

                var tx2 = createTransaction(bob, 9000, jpyRate);
                var ob2a = createObligation(tx2, alice, 4500);
                var ob2b = createObligation(tx2, carol, 4500);

                var obligations = List.of(ob1a, ob1b, ob2a, ob2b);
                when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationRepository.findByGroupId(groupId)).thenReturn(obligations);

                TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

                assertThat(result.transactionsSettlement()).isNotEmpty();

                long totalNet = calculateNetBalance(result, alice.getUuid().toString())
                        + calculateNetBalance(result, bob.getUuid().toString())
                        + calculateNetBalance(result, carol.getUuid().toString());
                assertThat(totalNet).isBetween(-1L, 1L);
            }
        }

        @Nested
        @DisplayName("Given multi-currency obligations")
        class GivenMultiCurrencyObligations {

            @Test
            @DisplayName("Should convert to JPY using exchange rates")
            void shouldConvertToJpyUsingExchangeRates() {
                UUID groupId = UUID.randomUUID();
                UserEntity alice = createUser("Alice", "s4");
                UserEntity bob = createUser("Bob", "s4");

                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));
                ExchangeRateEntity eurRate = createExchangeRate("EUR", new BigDecimal("160.00"));

                var tx1 = createTransaction(alice, 15000, usdRate);
                var ob1 = createObligation(tx1, bob, 15000);
                var tx2 = createTransaction(bob, 24000, eurRate);
                var ob2 = createObligation(tx2, alice, 24000);

                var obligations = List.of(ob1, ob2);
                when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationRepository.findByGroupId(groupId)).thenReturn(obligations);

                TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

                assertThat(result.transactionsSettlement()).isNotEmpty();
                long totalNet = calculateNetBalance(result, alice.getUuid().toString())
                        + calculateNetBalance(result, bob.getUuid().toString());
                assertThat(totalNet).isBetween(-1L, 1L);
            }
        }

        @Nested
        @DisplayName("Given rounding-prone exchange rate")
        class GivenRoundingProneExchangeRate {

            @Test
            @DisplayName("Should adjust rounding residuals to largest debtor")
            void shouldAdjustRoundingResiduals() {
                UUID groupId = UUID.randomUUID();
                UserEntity alice = createUser("Alice", "s5");
                UserEntity bob = createUser("Bob", "s5");
                UserEntity carol = createUser("Carol", "s5");
                UserEntity david = createUser("David", "s5");

                ExchangeRateEntity problemRate = createExchangeRate("USD", new BigDecimal("148.7"));

                var tx = createTransaction(alice, 10000, problemRate);
                var ob1 = createObligation(tx, bob, 3333);
                var ob2 = createObligation(tx, carol, 3333);
                var ob3 = createObligation(tx, david, 3334);

                var obligations = List.of(ob1, ob2, ob3);
                when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationRepository.findByGroupId(groupId)).thenReturn(obligations);

                TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

                long totalNet = calculateNetBalance(result, alice.getUuid().toString())
                        + calculateNetBalance(result, bob.getUuid().toString())
                        + calculateNetBalance(result, carol.getUuid().toString())
                        + calculateNetBalance(result, david.getUuid().toString());
                assertThat(totalNet).isBetween(-1L, 1L);
            }
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private CreateTransactionRequestDTO buildLoanRequest(String currencyCode, String dateStr) {
        var obligation = new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
        var loan = new CreateTransactionRequestDTO.Loan(List.of(obligation));
        return new CreateTransactionRequestDTO(
                TransactionType.LOAN, "Test", 5000, currencyCode,
                dateStr, testPayer.getUuid(), loan, null);
    }

    private TransactionHistoryEntity buildTransaction(TransactionType type, String title, int amount) {
        return buildTransactionWithRate(type, title, amount, jpyExchangeRate);
    }

    private TransactionHistoryEntity buildTransactionWithRate(
            TransactionType type, String title, int amount, ExchangeRateEntity rate) {
        return TransactionHistoryEntity.builder()
                .uuid(UUID.randomUUID())
                .transactionType(type)
                .title(title)
                .amount(amount)
                .transactionDate(Instant.now())
                .payer(testPayer)
                .group(testGroup)
                .exchangeRate(rate)
                .build();
    }

    private TransactionObligationEntity buildObligation(
            TransactionHistoryEntity transaction, UserEntity user, int amount) {
        return TransactionObligationEntity.builder()
                .uuid(UUID.randomUUID())
                .transaction(transaction)
                .user(user)
                .amount(amount)
                .build();
    }

    private UserEntity createUser(String name, String uuidSeed) {
        UUID userUuid = UUID.nameUUIDFromBytes((name + uuidSeed).getBytes());
        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(name.toLowerCase() + "-auth-uid")
                .name(name)
                .email(name.toLowerCase() + "@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginTime(Instant.now())
                .build();
        return UserEntity.builder()
                .uuid(userUuid)
                .name(name)
                .authUser(authUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private ExchangeRateEntity createExchangeRate(String currencyCode, BigDecimal rate) {
        CurrencyEntity currency = CurrencyEntity.builder()
                .currencyCode(currencyCode)
                .jpCurrencyName(currencyCode)
                .engCurrencyName(currencyCode)
                .build();
        return ExchangeRateEntity.builder()
                .currencyCode(currencyCode)
                .date(LocalDate.now())
                .exchangeRate(rate)
                .currency(currency)
                .build();
    }

    private TransactionHistoryEntity createTransaction(UserEntity payer, int amount, ExchangeRateEntity rate) {
        return TransactionHistoryEntity.builder()
                .uuid(UUID.randomUUID())
                .transactionType(TransactionType.LOAN)
                .title("Test Transaction")
                .amount(amount)
                .payer(payer)
                .group(TestFixtures.Groups.defaultGroup())
                .exchangeRate(rate)
                .transactionDate(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TransactionObligationEntity createObligation(
            TransactionHistoryEntity transaction, UserEntity user, int amount) {
        return TransactionObligationEntity.builder()
                .uuid(UUID.randomUUID())
                .transaction(transaction)
                .user(user)
                .amount(amount)
                .build();
    }

    private long calculateNetBalance(TransactionSettlementResponseDTO result, String userId) {
        long received = result.transactionsSettlement().stream()
                .filter(s -> s.to().uuid().equals(userId))
                .mapToLong(TransactionSettlement::amount).sum();
        long paid = result.transactionsSettlement().stream()
                .filter(s -> s.from().uuid().equals(userId))
                .mapToLong(TransactionSettlement::amount).sum();
        return received - paid;
    }

    private List<UserGroupEntity> createUserGroupsFromObligations(List<TransactionObligationEntity> obligations) {
        var uniqueUsers = new HashSet<UserEntity>();
        for (var ob : obligations) {
            uniqueUsers.add(ob.getUser());
            uniqueUsers.add(ob.getTransaction().getPayer());
        }
        return uniqueUsers.stream()
                .map(user -> UserGroupEntity.builder()
                        .userUuid(user.getUuid())
                        .groupUuid(UUID.randomUUID())
                        .user(user)
                        .build())
                .toList();
    }
}
