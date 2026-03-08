package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.UserEntity;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionService Integration Tests — Persistence behavior")
@Transactional
class TransactionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TransactionService transactionService;
    @Autowired private GroupRepository groupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserGroupRepository userGroupRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private ObligationRepository obligationRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;
    @Autowired private CurrencyRepository currencyRepository;

    private GroupEntity testGroup;
    private UserEntity testPayer;
    private UserEntity testBorrower1;
    private UserEntity testBorrower2;
    private ExchangeRateEntity jpyExchangeRate;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");

    @BeforeEach
    void setUp() {
        testGroup = TestFixtures.Groups.defaultGroup();
        groupRepository.save(testGroup);

        testPayer = TestFixtures.Users.userWithoutAuthUser("Test Payer");
        testBorrower1 = TestFixtures.Users.userWithoutAuthUser("Test Borrower 1");
        testBorrower2 = TestFixtures.Users.userWithoutAuthUser("Test Borrower 2");
        userRepository.saveAll(List.of(testPayer, testBorrower1, testBorrower2));

        userGroupRepository.saveAll(List.of(
                TestFixtures.UserGroups.create(testPayer, testGroup),
                TestFixtures.UserGroups.create(testBorrower1, testGroup),
                TestFixtures.UserGroups.create(testBorrower2, testGroup)));

        CurrencyEntity jpyCurrency = TestFixtures.Currencies.jpy();
        currencyRepository.save(jpyCurrency);
        flushAndClear();

        CurrencyEntity reloadedJpy = currencyRepository.findById("JPY").orElseThrow();
        jpyExchangeRate = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(LocalDate.now())
                .exchangeRate(BigDecimal.ONE)
                .currency(reloadedJpy)
                .build();
        exchangeRateRepository.save(jpyExchangeRate);
        flushAndClear();
    }

    private String todayDateStr() {
        return LocalDate.now().atStartOfDay().atZone(ZoneOffset.UTC).format(DATE_FORMAT);
    }

    // =========================================================================
    // @PreUpdate behavior — updated_at timestamp
    // =========================================================================

    @Nested
    @DisplayName("@PreUpdate behavior — updated_at timestamp")
    class PreUpdateBehavior {

        @Test
        @DisplayName("Should update updated_at when transaction is modified (via @PreUpdate)")
        void shouldUpdateTimestampOnChange() throws InterruptedException {
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Original")
                    .amount(5000)
                    .transactionDate(Instant.now())
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

            var original = transactionRepository.findById(transaction.getUuid()).orElseThrow();
            Instant originalUpdatedAt = original.getUpdatedAt();

            Thread.sleep(1100);

            var updateRequest = new UpdateTransactionRequestDTO(
                    "Updated Title", 8000, "JPY", todayDateStr(),
                    testPayer.getUuid(),
                    new UpdateTransactionRequestDTO.Loan(List.of(
                            new UpdateTransactionRequestDTO.Loan.Obligation(8000, testBorrower1.getUuid()))));

            transactionService.updateTransaction(transaction.getUuid(), updateRequest);
            flushAndClear();

            var updated = transactionRepository.findById(transaction.getUuid()).orElseThrow();
            assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should preserve created_at when transaction is updated")
        void shouldPreserveCreatedAtOnUpdate() {
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Original")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);
            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(transaction)
                    .user(testBorrower1)
                    .amount(5000)
                    .build());
            flushAndClear();

            Instant originalCreatedAt = transactionRepository.findById(transaction.getUuid())
                    .orElseThrow().getCreatedAt();

            var updateRequest = new UpdateTransactionRequestDTO(
                    "Updated", 8000, "JPY", todayDateStr(),
                    testPayer.getUuid(),
                    new UpdateTransactionRequestDTO.Loan(List.of(
                            new UpdateTransactionRequestDTO.Loan.Obligation(8000, testBorrower1.getUuid()))));

            transactionService.updateTransaction(transaction.getUuid(), updateRequest);
            flushAndClear();

            var updated = transactionRepository.findById(transaction.getUuid()).orElseThrow();
            assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        }
    }

    // =========================================================================
    // Database encoding — special characters
    // =========================================================================

    @Nested
    @DisplayName("Database encoding — special characters")
    class DatabaseEncoding {

        @Test
        @DisplayName("Should persist and retrieve multibyte/emoji characters in title")
        void shouldPersistMultibyteCharactersInTitle() {
            var loan = new CreateTransactionRequestDTO.Loan(List.of(
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower1.getUuid())));
            var request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN, "晩ごはん🍕🍣", 5000, "JPY",
                    todayDateStr(), testPayer.getUuid(), loan, null);

            CreateTransactionResponseDTO result = transactionService.createTransaction(testGroup.getUuid(), request);
            flushAndClear();

            var saved = transactionRepository.findById(UUID.fromString(result.id())).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("晩ごはん🍕🍣");
        }
    }

    // =========================================================================
    // Entity relationship preservation
    // =========================================================================

    @Nested
    @DisplayName("Entity relationship preservation")
    class EntityRelationshipPreservation {

        @Test
        @DisplayName("Should maintain obligation-transaction FK after obligation replacement")
        void shouldMaintainFkAfterObligationReplacement() {
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Test")
                    .amount(5000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            obligationRepository.saveAll(List.of(
                    TransactionObligationEntity.builder()
                            .uuid(UUID.randomUUID())
                            .transaction(transaction)
                            .user(testBorrower1)
                            .amount(2500)
                            .build(),
                    TransactionObligationEntity.builder()
                            .uuid(UUID.randomUUID())
                            .transaction(transaction)
                            .user(testBorrower2)
                            .amount(2500)
                            .build()));
            flushAndClear();

            var updateRequest = new UpdateTransactionRequestDTO(
                    "Updated", 5000, "JPY", todayDateStr(),
                    testPayer.getUuid(),
                    new UpdateTransactionRequestDTO.Loan(List.of(
                            new UpdateTransactionRequestDTO.Loan.Obligation(5000, testBorrower1.getUuid()))));

            transactionService.updateTransaction(transaction.getUuid(), updateRequest);
            flushAndClear();

            List<TransactionObligationEntity> obligations =
                    obligationRepository.findByTransactionId(transaction.getUuid());
            assertThat(obligations).hasSize(1);
            assertThat(obligations).allMatch(o ->
                    o.getTransaction().getUuid().equals(transaction.getUuid()));
            assertThat(obligations.getFirst().getUser().getUuid()).isEqualTo(testBorrower1.getUuid());
        }

        @Test
        @DisplayName("Should cascade-delete obligations when transaction is deleted")
        void shouldCascadeDeleteObligations() {
            TransactionHistoryEntity transaction = TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("To Delete")
                    .amount(6000)
                    .transactionDate(Instant.now())
                    .payer(testPayer)
                    .group(testGroup)
                    .exchangeRate(jpyExchangeRate)
                    .build();
            transactionRepository.save(transaction);

            obligationRepository.saveAll(List.of(
                    TransactionObligationEntity.builder()
                            .uuid(UUID.randomUUID())
                            .transaction(transaction)
                            .user(testBorrower1)
                            .amount(3000)
                            .build(),
                    TransactionObligationEntity.builder()
                            .uuid(UUID.randomUUID())
                            .transaction(transaction)
                            .user(testBorrower2)
                            .amount(3000)
                            .build()));
            flushAndClear();

            UUID txId = transaction.getUuid();
            transactionService.deleteTransaction(txId);
            flushAndClear();

            assertThat(transactionRepository.findById(txId)).isEmpty();
            assertThat(obligationRepository.findByTransactionId(txId)).isEmpty();
        }
    }

    // =========================================================================
    // Custom query correctness
    // =========================================================================

    @Nested
    @DisplayName("Custom query correctness")
    class CustomQueryCorrectness {

        @Test
        @DisplayName("findTransactionsByGroupWithLimit should return ordered by createdAt DESC")
        void shouldReturnOrderedByCreatedAtDesc() throws InterruptedException {
            for (int i = 0; i < 3; i++) {
                TransactionHistoryEntity tx = TransactionHistoryEntity.builder()
                        .uuid(UUID.randomUUID())
                        .transactionType(TransactionType.LOAN)
                        .title("Tx " + i)
                        .amount(1000 * (i + 1))
                        .transactionDate(Instant.now())
                        .payer(testPayer)
                        .group(testGroup)
                        .exchangeRate(jpyExchangeRate)
                        .build();
                transactionRepository.save(tx);
                flushAndClear();
                Thread.sleep(1100);
            }

            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(10, testGroup.getUuid());

            assertThat(result.transactionsHistory()).hasSize(3);
            assertThat(result.transactionsHistory().get(0).title()).isEqualTo("Tx 2");
            assertThat(result.transactionsHistory().get(1).title()).isEqualTo("Tx 1");
            assertThat(result.transactionsHistory().get(2).title()).isEqualTo("Tx 0");
        }

        @Test
        @DisplayName("findTransactionsByGroupWithLimit should limit results by count")
        void shouldLimitResultsByCount() {
            for (int i = 0; i < 10; i++) {
                TransactionHistoryEntity tx = TransactionHistoryEntity.builder()
                        .uuid(UUID.randomUUID())
                        .transactionType(TransactionType.LOAN)
                        .title("Tx " + i)
                        .amount(1000)
                        .transactionDate(Instant.now().minusSeconds(i))
                        .payer(testPayer)
                        .group(testGroup)
                        .exchangeRate(jpyExchangeRate)
                        .build();
                transactionRepository.save(tx);
            }
            flushAndClear();

            TransactionHistoryResponseDTO result = transactionService.getTransactionHistory(5, testGroup.getUuid());

            assertThat(result.transactionsHistory()).hasSize(5);
        }
    }

    // =========================================================================
    // Exchange rate fallback persistence
    // =========================================================================

    @Nested
    @DisplayName("Exchange rate fallback persistence")
    class ExchangeRateFallbackPersistence {

        @Test
        @DisplayName("Should persist new exchange rate entry when falling back to latest rate")
        void shouldPersistNewExchangeRateForFallback() {
            LocalDate futureDate = LocalDate.now().plusDays(10);
            String futureDateStr = futureDate.atStartOfDay().atZone(ZoneOffset.UTC).format(DATE_FORMAT);

            var loan = new CreateTransactionRequestDTO.Loan(List.of(
                    new CreateTransactionRequestDTO.Loan.Obligation(5000, testBorrower1.getUuid())));
            var request = new CreateTransactionRequestDTO(
                    TransactionType.LOAN, "Future", 5000, "JPY",
                    futureDateStr, testPayer.getUuid(), loan, null);

            transactionService.createTransaction(testGroup.getUuid(), request);
            flushAndClear();

            var newRate = exchangeRateRepository.findByCurrencyCodeAndDate("JPY", futureDate);
            assertThat(newRate).isPresent();
            assertThat(newRate.get().getExchangeRate()).isEqualByComparingTo(BigDecimal.ONE);
        }
    }
}
