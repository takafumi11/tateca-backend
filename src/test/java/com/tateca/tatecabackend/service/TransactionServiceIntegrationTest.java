package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
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
import com.tateca.tatecabackend.repository.CurrencyRepository;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.repository.GroupRepository;
import com.tateca.tatecabackend.repository.ObligationRepository;
import com.tateca.tatecabackend.repository.TransactionRepository;
import com.tateca.tatecabackend.repository.UserGroupRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransactionService Integration Tests")
class TransactionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObligationRepository obligationRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private GroupEntity testGroup;
    private UserEntity testPayer;
    private UserEntity testBorrower1;
    private UserEntity testBorrower2;
    private CurrencyEntity jpyCurrency;
    private ExchangeRateEntity jpyExchangeRate;

    @BeforeEach
    void setUp() {
        // Create test group
        testGroup = TestFixtures.Groups.defaultGroup();
        groupRepository.save(testGroup);

        // Create test users
        testPayer = TestFixtures.Users.userWithoutAuthUser("Test Payer");
        testBorrower1 = TestFixtures.Users.userWithoutAuthUser("Test Borrower 1");
        testBorrower2 = TestFixtures.Users.userWithoutAuthUser("Test Borrower 2");
        userRepository.save(testPayer);
        userRepository.save(testBorrower1);
        userRepository.save(testBorrower2);

        // Create user-group relationships
        userGroupRepository.save(TestFixtures.UserGroups.create(testPayer, testGroup));
        userGroupRepository.save(TestFixtures.UserGroups.create(testBorrower1, testGroup));
        userGroupRepository.save(TestFixtures.UserGroups.create(testBorrower2, testGroup));

        // Create JPY currency
        jpyCurrency = TestFixtures.Currencies.jpy();
        currencyRepository.save(jpyCurrency);

        flushAndClear();

        // Reload currency from database and create exchange rate
        CurrencyEntity reloadedCurrency = currencyRepository.findById("JPY").orElseThrow();
        jpyExchangeRate = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(LocalDate.now())
                .exchangeRate(BigDecimal.ONE)
                .currency(reloadedCurrency)
                .build();
        exchangeRateRepository.save(jpyExchangeRate);

        flushAndClear();
    }

    // ========================================
    // createTransaction Tests
    // ========================================

    @Nested
    @DisplayName("createTransaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create LOAN transaction and save to database")
        void shouldCreateLoanTransactionAndSaveToDatabase() {
            // Given: Valid LOAN request
            CreateTransactionRequestDTO.Loan.Obligation obligation1 =
                    new CreateTransactionRequestDTO.Loan.Obligation(2500, testBorrower1.getUuid());
            CreateTransactionRequestDTO.Loan.Obligation obligation2 =
                    new CreateTransactionRequestDTO.Loan.Obligation(2500, testBorrower2.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation1, obligation2));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Team Dinner",
                    5000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC)
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            // When: Creating transaction
            CreateTransactionResponseDTO result = transactionService.createTransaction(testGroup.getUuid(), request);

            flushAndClear();

            // Then: Transaction should be saved in database
            assertThat(result).isNotNull();
            assertThat(result.transactionType()).isEqualTo(TransactionType.LOAN);
            assertThat(result.title()).isEqualTo("Team Dinner");
            assertThat(result.amount()).isEqualTo(5000);
            assertThat(result.loan()).isNotNull();
            assertThat(result.loan().obligationResponses()).hasSize(2);

            // Verify database state
            TransactionHistoryEntity savedTransaction = transactionRepository.findById(UUID.fromString(result.id())).orElseThrow();
            assertThat(savedTransaction.getTitle()).isEqualTo("Team Dinner");
            assertThat(savedTransaction.getAmount()).isEqualTo(5000);
            assertThat(savedTransaction.getTransactionType()).isEqualTo(TransactionType.LOAN);

            List<TransactionObligationEntity> obligations = obligationRepository.findByTransactionId(savedTransaction.getUuid());
            assertThat(obligations).hasSize(2);
        }

        @Test
        @DisplayName("Should create REPAYMENT transaction and save to database")
        void shouldCreateRepaymentTransactionAndSaveToDatabase() {
            // Given: Valid REPAYMENT request
            CreateTransactionRequestDTO.Repayment repayment =
                    new CreateTransactionRequestDTO.Repayment(testBorrower1.getUuid());

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.REPAYMENT,
                    "Repayment for dinner",
                    3000,
                    "JPY",
                    LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC)
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    null,
                    repayment
            );

            // When: Creating transaction
            CreateTransactionResponseDTO result = transactionService.createTransaction(testGroup.getUuid(), request);

            flushAndClear();

            // Then: Transaction should be saved in database
            assertThat(result).isNotNull();
            assertThat(result.transactionType()).isEqualTo(TransactionType.REPAYMENT);
            assertThat(result.title()).isEqualTo("Repayment for dinner");
            assertThat(result.amount()).isEqualTo(3000);
            assertThat(result.repayment()).isNotNull();

            // Verify database state
            TransactionHistoryEntity savedTransaction = transactionRepository.findById(UUID.fromString(result.id())).orElseThrow();
            assertThat(savedTransaction.getTitle()).isEqualTo("Repayment for dinner");
            assertThat(savedTransaction.getAmount()).isEqualTo(3000);
            assertThat(savedTransaction.getTransactionType()).isEqualTo(TransactionType.REPAYMENT);

            List<TransactionObligationEntity> obligations = obligationRepository.findByTransactionId(savedTransaction.getUuid());
            assertThat(obligations).hasSize(1);
        }

        @Test
        @DisplayName("Should create new exchange rate when date not found")
        void shouldCreateNewExchangeRateWhenDateNotFound() {
            // Given: Future date without exchange rate
            LocalDate futureDate = LocalDate.now().plusDays(10);
            CreateTransactionRequestDTO.Loan.Obligation obligation =
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower1.getUuid());
            CreateTransactionRequestDTO.Loan loan =
                    new CreateTransactionRequestDTO.Loan(List.of(obligation));

            CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN,
                    "Future Transaction",
                    5000,
                    "JPY",
                    futureDate.atStartOfDay().atZone(java.time.ZoneOffset.UTC)
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX")),
                    testPayer.getUuid(),
                    loan,
                    null
            );

            // When: Creating transaction
            transactionService.createTransaction(testGroup.getUuid(), request);

            flushAndClear();

            // Then: New exchange rate should be created for the future date
            ExchangeRateEntity newRate = exchangeRateRepository.findByCurrencyCodeAndDate("JPY", futureDate).orElseThrow();
            assertThat(newRate.getDate()).isEqualTo(futureDate);
            assertThat(newRate.getExchangeRate()).isEqualByComparingTo(BigDecimal.ONE);
        }
    }

    // ========================================
    // getTransactionHistory Tests
    // ========================================

    @Nested
    @DisplayName("getTransactionHistory Tests")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("Should retrieve transaction history from database")
        void shouldRetrieveTransactionHistoryFromDatabase() {
            // Given: Transaction exists in database
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test Transaction")
                    .amount(5000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            flushAndClear();

            // When: Getting transaction history
            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(10, testGroup.getUuid());

            // Then: Should retrieve transaction from database
            assertThat(result).isNotNull();
            assertThat(result.transactionsHistory()).hasSize(1);
            assertThat(result.transactionsHistory().get(0).title()).isEqualTo("Test Transaction");
        }

        @Test
        @DisplayName("Should limit results by count parameter")
        void shouldLimitResultsByCountParameter() {
            // Given: Multiple transactions in database
            for (int i = 0; i < 10; i++) {
                TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                        .uuid(UUID.randomUUID())
                        .transactionType(TransactionType.LOAN)
                        .title("Transaction " + i)
                        .amount(1000)
                        .transactionDate(java.time.Instant.now().minusSeconds(i))
                        .payer(testPayer)
                        .group(testGroup)
                        .exchangeRate(jpyExchangeRate)
                        .build();
                transactionRepository.save(transaction);
            }

            flushAndClear();

            // When: Getting transaction history with limit
            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(5, testGroup.getUuid());

            // Then: Should return only requested number
            assertThat(result.transactionsHistory()).hasSize(5);
        }

        @Test
        @DisplayName("Should return empty list when no transactions exist")
        void shouldReturnEmptyListWhenNoTransactionsExist() {
            // Given: No transactions in database

            // When: Getting transaction history
            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(10, testGroup.getUuid());

            // Then: Should return empty list
            assertThat(result.transactionsHistory()).isEmpty();
        }
    }

    // ========================================
    // getSettlements Tests
    // ========================================

    @Nested
    @DisplayName("getSettlements Tests")
    class GetSettlementsTests {

        @Test
        @DisplayName("Should calculate settlements correctly from database")
        void shouldCalculateSettlementsCorrectlyFromDatabase() {
            // Given: Transaction with obligations in database
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Dinner")
                    .amount(6000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            TransactionObligationEntity obligation1 = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower1)
                    .amount(3000)
                    .build();
            TransactionObligationEntity obligation2 = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower2)
                    .amount(3000)
                    .build();
            obligationRepository.save(obligation1);
            obligationRepository.save(obligation2);

            flushAndClear();

            // When: Getting settlements
            TransactionSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should calculate settlements correctly
            assertThat(result).isNotNull();
            assertThat(result.transactionsSettlement()).hasSize(2);

            // Verify total settlement amount
            long totalAmount = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlementResponseDTO.TransactionSettlement::amount)
                    .sum();
            assertThat(totalAmount).isEqualTo(6000);
        }

        @Test
        @DisplayName("Should return empty settlements when no obligations exist")
        void shouldReturnEmptySettlementsWhenNoObligationsExist() {
            // Given: No obligations in database

            // When: Getting settlements
            TransactionSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should return empty settlements
            assertThat(result.transactionsSettlement()).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple transactions with complex settlement")
        void shouldHandleMultipleTransactionsWithComplexSettlement() {
            // Given: Multiple transactions creating complex debts
            // Transaction 1: Payer pays 9000, Borrower1 and Borrower2 each owe 3000
            TransactionHistoryEntity tx1 = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Transaction 1")
                    .amount(6000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(tx1);

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(tx1)
                    .user(testBorrower1)
                    .amount(3000)
                    .build());
            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(tx1)
                    .user(testBorrower2)
                    .amount(3000)
                    .build());

            // Transaction 2: Borrower1 pays 6000, Payer owes 3000
            TransactionHistoryEntity tx2 = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Transaction 2")
                    .amount(3000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testBorrower1)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(tx2);

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(tx2)
                    .user(testPayer)
                    .amount(3000)
                    .build());

            flushAndClear();

            // When: Getting settlements
            TransactionSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should optimize and calculate settlements correctly
            assertThat(result).isNotNull();
            assertThat(result.transactionsSettlement()).isNotEmpty();
        }
    }

    // ========================================
    // getTransactionDetail Tests
    // ========================================

    @Nested
    @DisplayName("getTransactionDetail Tests")
    class GetTransactionDetailTests {

        @Test
        @DisplayName("Should retrieve LOAN transaction detail from database")
        void shouldRetrieveLoanTransactionDetailFromDatabase() {
            // Given: LOAN transaction in database
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test Loan")
                    .amount(5000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            TransactionObligationEntity obligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower1)
                    .amount(5000)
                    .build();
            obligationRepository.save(obligation);

            flushAndClear();

            // When: Getting transaction detail
            CreateTransactionResponseDTO result = transactionService.getTransactionDetail(transaction.getUuid());

            // Then: Should retrieve complete detail from database
            assertThat(result).isNotNull();
            assertThat(result.transactionType()).isEqualTo(TransactionType.LOAN);
            assertThat(result.title()).isEqualTo("Test Loan");
            assertThat(result.amount()).isEqualTo(5000);
            assertThat(result.loan()).isNotNull();
            assertThat(result.loan().obligationResponses()).hasSize(1);
        }

        @Test
        @DisplayName("Should retrieve REPAYMENT transaction detail from database")
        void shouldRetrieveRepaymentTransactionDetailFromDatabase() {
            // Given: REPAYMENT transaction in database
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.REPAYMENT)
                    .title("Test Repayment")
                    .amount(3000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            TransactionObligationEntity obligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower1)
                    .amount(3000)
                    .build();
            obligationRepository.save(obligation);

            flushAndClear();

            // When: Getting transaction detail
            CreateTransactionResponseDTO result = transactionService.getTransactionDetail(transaction.getUuid());

            // Then: Should retrieve complete detail from database
            assertThat(result).isNotNull();
            assertThat(result.transactionType()).isEqualTo(TransactionType.REPAYMENT);
            assertThat(result.title()).isEqualTo("Test Repayment");
            assertThat(result.amount()).isEqualTo(3000);
            assertThat(result.repayment()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            // Given: Non-existent transaction ID
            UUID nonExistentId = UUID.randomUUID();

            // When & Then: Should throw exception
            assertThatThrownBy(() -> transactionService.getTransactionDetail(nonExistentId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }
    }

    // ========================================
    // deleteTransaction Tests
    // ========================================

    @Nested
    @DisplayName("deleteTransaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should delete transaction and obligations from database")
        void shouldDeleteTransactionAndObligationsFromDatabase() {
            // Given: Transaction with obligations in database
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("To Delete")
                    .amount(5000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            TransactionObligationEntity obligation = TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower1)
                    .amount(5000)
                    .build();
            obligationRepository.save(obligation);

            flushAndClear();

            UUID transactionId = transaction.getUuid();

            // When: Deleting transaction
            transactionService.deleteTransaction(transactionId);

            flushAndClear();

            // Then: Transaction and obligations should be deleted from database
            assertThat(transactionRepository.findById(transactionId)).isEmpty();
            assertThat(obligationRepository.findByTransactionId(transactionId)).isEmpty();
        }

        @Test
        @DisplayName("Should delete transaction with multiple obligations")
        void shouldDeleteTransactionWithMultipleObligations() {
            // Given: Transaction with multiple obligations
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("To Delete")
                    .amount(6000)
                    .transactionDate(java.time.Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower1)
                    .amount(3000)
                    .build());
            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower2)
                    .amount(3000)
                    .build());

            flushAndClear();

            UUID transactionId = transaction.getUuid();

            // When: Deleting transaction
            transactionService.deleteTransaction(transactionId);

            flushAndClear();

            // Then: All obligations should be deleted
            assertThat(transactionRepository.findById(transactionId)).isEmpty();
            assertThat(obligationRepository.findByTransactionId(transactionId)).isEmpty();
        }
    }
}
