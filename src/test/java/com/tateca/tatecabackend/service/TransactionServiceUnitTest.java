package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
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
import java.util.ArrayList;
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
@DisplayName("TransactionService Unit Tests")
class TransactionServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ObligationRepository obligationRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private GroupEntity testGroup;
    private UserEntity testPayer;
    private UserEntity testBorrower;
    private CurrencyEntity jpyCurrency;
    private ExchangeRateEntity jpyExchangeRate;

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

    // ========================================
    // getTransactionHistory Tests
    // ========================================

    @Nested
    @DisplayName("getTransactionHistory Tests")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("Should call repository with correct parameters")
        void shouldCallRepositoryWithCorrectParameters() {
            // Given: Repository returns empty list
            UUID groupId = UUID.randomUUID();
            int count = 10;
            when(transactionRepository.findTransactionsByGroupWithLimit(groupId, count))
                    .thenReturn(new ArrayList<>());

            // When: Getting transaction history
            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(count, groupId);

            // Then: Should call repository with correct parameters
            verify(transactionRepository, times(1)).findTransactionsByGroupWithLimit(groupId, count);
            assertThat(result).isNotNull();
            assertThat(result.transactionsHistory()).isEmpty();
        }

        @Test
        @DisplayName("Should return mapped response from repository")
        void shouldReturnMappedResponseFromRepository() {
            // Given: Repository returns transactions
            UUID groupId = UUID.randomUUID();
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test Transaction")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            when(transactionRepository.findTransactionsByGroupWithLimit(groupId, 10))
                    .thenReturn(List.of(transaction));

            // When: Getting transaction history
            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(10, groupId);

            // Then: Should map response correctly
            assertThat(result.transactionsHistory()).hasSize(1);
        }
    }

    // ========================================
    // getSettlements Tests
    // ========================================

    @Nested
    @DisplayName("getSettlements Tests")
    class GetSettlementsTests {

        @Test
        @DisplayName("Should calculate settlements with correct balance")
        void shouldCalculateSettlementsWithCorrectBalance() {
            // Given: Users and obligations setup
            UUID groupId = UUID.randomUUID();
            UserGroupEntity ug1 = TestFixtures.UserGroups.create(testPayer, testGroup);
            UserGroupEntity ug2 = TestFixtures.UserGroups.create(testBorrower, testGroup);

            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test Transaction")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            TransactionObligationEntity obligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower)
                    .amount(5000)
                    .build();

            when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                    .thenReturn(List.of(ug1, ug2));
            when(obligationRepository.findByGroupId(groupId))
                    .thenReturn(List.of(obligation));

            // When: Getting settlements
            TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

            // Then: Should have settlement transaction
            assertThat(result.transactionsSettlement()).hasSize(1);
            assertThat(result.transactionsSettlement().get(0).amount()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should return empty when no obligations")
        void shouldReturnEmptyWhenNoObligations() {
            // Given: Users but no obligations
            UUID groupId = UUID.randomUUID();
            UserGroupEntity ug = TestFixtures.UserGroups.create(testPayer, testGroup);

            when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                    .thenReturn(List.of(ug));
            when(obligationRepository.findByGroupId(groupId))
                    .thenReturn(new ArrayList<>());

            // When: Getting settlements
            TransactionSettlementResponseDTO result = transactionService.getSettlements(groupId);

            // Then: Should return empty transactions
            assertThat(result.transactionsSettlement()).isEmpty();
        }

        @Test
        @DisplayName("Should call repositories with correct parameters")
        void shouldCallRepositoriesWithCorrectParameters() {
            // Given: Setup with mocks
            UUID groupId = UUID.randomUUID();
            when(userGroupRepository.findByGroupUuidWithUserDetails(groupId))
                    .thenReturn(new ArrayList<>());
            when(obligationRepository.findByGroupId(groupId))
                    .thenReturn(new ArrayList<>());

            // When: Getting settlements
            transactionService.getSettlements(groupId);

            // Then: Should call both repositories
            verify(userGroupRepository, times(1)).findByGroupUuidWithUserDetails(groupId);
            verify(obligationRepository, times(1)).findByGroupId(groupId);
        }
    }

    // ========================================
    // createTransaction Tests
    // ========================================

    @Nested
    @DisplayName("createTransaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create LOAN transaction with obligations")
        void shouldCreateLoanTransactionWithObligations() {
            // Given: Valid LOAN request
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Test Loan",
                    5000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            UUID groupId = testGroup.getUuid();
            TransactionHistoryEntity savedTransaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test Loan")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            TransactionObligationEntity savedObligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(savedTransaction)
                    .user(testBorrower)
                    .amount(5000)
                    .build();

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(jpyExchangeRate));
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(transactionRepository.save(any(TransactionHistoryEntity.class))).thenReturn(savedTransaction);
            when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
            when(obligationRepository.saveAll(anyList())).thenReturn(List.of(savedObligation));

            // When: Creating transaction
            CreateTransactionResponseDTO result = transactionService.createTransaction(groupId, request);

            // Then: Should save transaction and obligations
            verify(transactionRepository, times(1)).save(any(TransactionHistoryEntity.class));
            verify(obligationRepository, times(1)).saveAll(anyList());
            assertThat(result).isNotNull();
            assertThat(result.loan()).isNotNull();
        }

        @Test
        @DisplayName("Should create REPAYMENT transaction with single obligation")
        void shouldCreateRepaymentTransactionWithSingleObligation() {
            // Given: Valid REPAYMENT request
            CreateTransactionRequestDTO.Repayment repayment =
                    new CreateTransactionRequestDTO.Repayment(testBorrower.getUuid());

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.REPAYMENT,
                    "Test Repayment",
                    3000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    null,
                    repayment
            );

            UUID groupId = testGroup.getUuid();
            TransactionHistoryEntity savedTransaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.REPAYMENT)
                    .title("Test Repayment")
                    .amount(3000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            TransactionObligationEntity savedObligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(savedTransaction)
                    .user(testBorrower)
                    .amount(3000)
                    .build();

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(jpyExchangeRate));
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(transactionRepository.save(any(TransactionHistoryEntity.class))).thenReturn(savedTransaction);
            when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
            when(obligationRepository.save(any(TransactionObligationEntity.class))).thenReturn(savedObligation);

            // When: Creating transaction
            CreateTransactionResponseDTO result = transactionService.createTransaction(groupId, request);

            // Then: Should save transaction and single obligation
            verify(transactionRepository, times(1)).save(any(TransactionHistoryEntity.class));
            verify(obligationRepository, times(1)).save(any(TransactionObligationEntity.class));
            assertThat(result).isNotNull();
            assertThat(result.repayment()).isNotNull();
        }

        @Test
        @DisplayName("Should use existing exchange rate when available")
        void shouldUseExistingExchangeRateWhenAvailable() {
            // Given: Exchange rate exists for date
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Test",
                    5000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            UUID groupId = testGroup.getUuid();
            TransactionHistoryEntity savedTransaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(jpyExchangeRate));
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(transactionRepository.save(any(TransactionHistoryEntity.class))).thenReturn(savedTransaction);
            when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
            when(obligationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            // When: Creating transaction
            transactionService.createTransaction(groupId, request);

            // Then: Should use existing exchange rate (not create new)
            verify(exchangeRateRepository, never()).save(any(ExchangeRateEntity.class));
        }

        @Test
        @DisplayName("Should create new exchange rate when date not found")
        void shouldCreateNewExchangeRateWhenDateNotFound() {
            // Given: Exchange rate doesn't exist for date, but latest exists
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            LocalDate futureDate = LocalDate.now().plusDays(10);
            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Future Transaction",
                    5000,
                    "JPY",
                    futureDate.atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            UUID groupId = testGroup.getUuid();
            ExchangeRateEntity latestRate = ExchangeRateEntity.builder()
                    .currencyCode("JPY")
                    .date(LocalDate.now())
                    .exchangeRate(BigDecimal.ONE)
                    .currency(jpyCurrency)
                    .build();

            ExchangeRateEntity newRate = ExchangeRateEntity.builder()
                    .currencyCode("JPY")
                    .date(futureDate)
                    .exchangeRate(BigDecimal.ONE)
                    .currency(jpyCurrency)
                    .build();

            TransactionHistoryEntity savedTransaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Future Transaction")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(newRate)
                    .build();

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), eq(futureDate)))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.findLatestByCurrencyCode("JPY"))
                    .thenReturn(Optional.of(latestRate));
            when(exchangeRateRepository.save(any(ExchangeRateEntity.class))).thenReturn(newRate);
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(transactionRepository.save(any(TransactionHistoryEntity.class))).thenReturn(savedTransaction);
            when(userRepository.findById(testBorrower.getUuid())).thenReturn(Optional.of(testBorrower));
            when(obligationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            // When: Creating transaction
            transactionService.createTransaction(groupId, request);

            // Then: Should create new exchange rate
            verify(exchangeRateRepository, times(1)).save(any(ExchangeRateEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when payer not found")
        void shouldThrowExceptionWhenPayerNotFound() {
            // Given: Non-existent payer
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Test",
                    5000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(jpyExchangeRate));
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.empty());

            // When & Then: Should throw exception
            assertThatThrownBy(() -> transactionService.createTransaction(testGroup.getUuid(), request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when group not found")
        void shouldThrowExceptionWhenGroupNotFound() {
            // Given: Non-existent group
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Test",
                    5000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            UUID groupId = testGroup.getUuid();

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(jpyExchangeRate));
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
            when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

            // When & Then: Should throw exception
            assertThatThrownBy(() -> transactionService.createTransaction(groupId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Group not found");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when no exchange rate found")
        void shouldThrowExceptionWhenNoExchangeRateFound() {
            // Given: No exchange rate exists at all
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Test",
                    5000,
                    "USD",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("USD"), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.findLatestByCurrencyCode("USD"))
                    .thenReturn(Optional.empty());

            // When & Then: Should throw exception
            assertThatThrownBy(() -> transactionService.createTransaction(testGroup.getUuid(), request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No exchange rate found");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when borrower not found")
        void shouldThrowExceptionWhenBorrowerNotFound() {
            // Given: LOAN transaction with non-existent borrower
            UUID nonExistentBorrower = UUID.randomUUID();
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, nonExistentBorrower);
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Test",
                    5000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            UUID groupId = testGroup.getUuid();
            TransactionHistoryEntity savedTransaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test")
                    .amount(5000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(jpyExchangeRate));
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(transactionRepository.save(any(TransactionHistoryEntity.class))).thenReturn(savedTransaction);
            when(userRepository.findById(nonExistentBorrower)).thenReturn(Optional.empty());

            // When & Then: Should throw exception
            assertThatThrownBy(() -> transactionService.createTransaction(groupId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(obligationRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception when recipient not found")
        void shouldThrowExceptionWhenRecipientNotFound() {
            // Given: REPAYMENT transaction with non-existent recipient
            UUID nonExistentRecipient = UUID.randomUUID();
            CreateTransactionRequestDTO.Repayment repayment =
                    new CreateTransactionRequestDTO.Repayment(nonExistentRecipient);

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.REPAYMENT,
                    "Test Repayment",
                    3000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    null,
                    repayment
            );

            UUID groupId = testGroup.getUuid();
            TransactionHistoryEntity savedTransaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.REPAYMENT)
                    .title("Test Repayment")
                    .amount(3000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            when(exchangeRateRepository.findByCurrencyCodeAndDate(eq("JPY"), any(LocalDate.class)))
                    .thenReturn(Optional.of(jpyExchangeRate));
            when(userRepository.findById(testPayer.getUuid())).thenReturn(Optional.of(testPayer));
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup));
            when(transactionRepository.save(any(TransactionHistoryEntity.class))).thenReturn(savedTransaction);
            when(userRepository.findById(nonExistentRecipient)).thenReturn(Optional.empty());

            // When & Then: Should throw exception
            assertThatThrownBy(() -> transactionService.createTransaction(groupId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(obligationRepository, never()).save(any(TransactionObligationEntity.class));
        }
    }

    // ========================================
    // getTransactionDetail Tests
    // ========================================

    @Nested
    @DisplayName("getTransactionDetail Tests")
    class GetTransactionDetailTests {

        @Test
        @DisplayName("Should return detail for LOAN transaction")
        void shouldReturnDetailForLoanTransaction() {
            // Given: LOAN transaction exists
            UUID transactionId = UUID.randomUUID();
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(transactionId)
                    .transactionType(TransactionType.LOAN)
                    .title("Test Loan")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            TransactionObligationEntity obligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower)
                    .amount(5000)
                    .build();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(obligationRepository.findByTransactionId(transactionId)).thenReturn(List.of(obligation));

            // When: Getting transaction detail
            CreateTransactionResponseDTO result = transactionService.getTransactionDetail(transactionId);

            // Then: Should return LOAN detail
            assertThat(result).isNotNull();
            assertThat(result.loan()).isNotNull();
            assertThat(result.repayment()).isNull();
        }

        @Test
        @DisplayName("Should return detail for REPAYMENT transaction")
        void shouldReturnDetailForRepaymentTransaction() {
            // Given: REPAYMENT transaction exists
            UUID transactionId = UUID.randomUUID();
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(transactionId)
                    .transactionType(TransactionType.REPAYMENT)
                    .title("Test Repayment")
                    .amount(3000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            TransactionObligationEntity obligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower)
                    .amount(3000)
                    .build();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(obligationRepository.findByTransactionId(transactionId)).thenReturn(List.of(obligation));

            // When: Getting transaction detail
            CreateTransactionResponseDTO result = transactionService.getTransactionDetail(transactionId);

            // Then: Should return REPAYMENT detail
            assertThat(result).isNotNull();
            assertThat(result.repayment()).isNotNull();
            assertThat(result.loan()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            // Given: Transaction doesn't exist
            UUID transactionId = UUID.randomUUID();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

            // When & Then: Should throw exception
            assertThatThrownBy(() -> transactionService.getTransactionDetail(transactionId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Transaction not found");

            verify(obligationRepository, never()).findByTransactionId(any());
        }

        @Test
        @DisplayName("Should call repositories with correct parameters")
        void shouldCallRepositoriesWithCorrectParameters() {
            // Given: Transaction exists
            UUID transactionId = UUID.randomUUID();
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(transactionId)
                    .transactionType(TransactionType.LOAN)
                    .title("Test")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(obligationRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());

            // When: Getting transaction detail
            transactionService.getTransactionDetail(transactionId);

            // Then: Should call both repositories
            verify(transactionRepository, times(1)).findById(transactionId);
            verify(obligationRepository, times(1)).findByTransactionId(transactionId);
        }
    }

    // ========================================
    // deleteTransaction Tests
    // ========================================

    @Nested
    @DisplayName("deleteTransaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should delete obligations before transaction")
        void shouldDeleteObligationsBeforeTransaction() {
            // Given: Transaction with obligations
            UUID transactionId = UUID.randomUUID();
            TransactionObligationEntity obligation1 = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .build();
            TransactionObligationEntity obligation2 = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .build();

            when(obligationRepository.findByTransactionId(transactionId))
                    .thenReturn(List.of(obligation1, obligation2));

            // When: Deleting transaction
            transactionService.deleteTransaction(transactionId);

            // Then: Should delete obligations first, then transaction
            ArgumentCaptor<List<UUID>> obligationIdsCaptor = ArgumentCaptor.forClass(List.class);
            verify(obligationRepository).deleteAllById(obligationIdsCaptor.capture());
            verify(transactionRepository).deleteById(transactionId);

            List<UUID> deletedObligationIds = obligationIdsCaptor.getValue();
            assertThat(deletedObligationIds).containsExactlyInAnyOrder(
                    obligation1.getUuid(),
                    obligation2.getUuid()
            );
        }

        @Test
        @DisplayName("Should handle single obligation deletion")
        void shouldHandleSingleObligationDeletion() {
            // Given: Transaction with single obligation
            UUID transactionId = UUID.randomUUID();
            TransactionObligationEntity obligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .build();

            when(obligationRepository.findByTransactionId(transactionId))
                    .thenReturn(List.of(obligation));

            // When: Deleting transaction
            transactionService.deleteTransaction(transactionId);

            // Then: Should delete single obligation and transaction
            verify(obligationRepository).deleteAllById(List.of(obligation.getUuid()));
            verify(transactionRepository).deleteById(transactionId);
        }

        @Test
        @DisplayName("Should handle transaction with no obligations")
        void shouldHandleTransactionWithNoObligations() {
            // Given: Transaction with no obligations
            UUID transactionId = UUID.randomUUID();
            when(obligationRepository.findByTransactionId(transactionId))
                    .thenReturn(new ArrayList<>());

            // When: Deleting transaction
            transactionService.deleteTransaction(transactionId);

            // Then: Should still delete transaction
            verify(obligationRepository).deleteAllById(new ArrayList<>());
            verify(transactionRepository).deleteById(transactionId);
        }

        @Test
        @DisplayName("Should call repositories with correct parameters")
        void shouldCallRepositoriesWithCorrectParameters() {
            // Given: Transaction with obligations
            UUID transactionId = UUID.randomUUID();
            when(obligationRepository.findByTransactionId(transactionId))
                    .thenReturn(new ArrayList<>());

            // When: Deleting transaction
            transactionService.deleteTransaction(transactionId);

            // Then: Should call repositories with correct IDs
            verify(obligationRepository, times(1)).findByTransactionId(transactionId);
            verify(transactionRepository, times(1)).deleteById(transactionId);
        }
    }
}
