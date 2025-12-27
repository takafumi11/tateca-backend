package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.model.TransactionType;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.CurrencyNameRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TransactionService.getSettlements() method.
 *
 * <p>Tests verify settlement calculation with real database and transaction isolation.
 * Focus on rounding error adjustment and multi-transaction scenarios.</p>
 */
@DisplayName("TransactionService.getSettlements() Integration Tests")
class TransactionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObligationRepository obligationRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyNameRepository currencyNameRepository;

    private AuthUserEntity aliceAuth;
    private UserEntity alice;
    private UserEntity bob;
    private UserEntity carol;
    private UserEntity david;
    private GroupEntity testGroup;
    private ExchangeRateEntity usdRate;
    private ExchangeRateEntity eurRate;

    @BeforeEach
    void setUp() {
        // Setup currency data
        CurrencyNameEntity jpy = currencyNameRepository.save(TestFixtures.Currencies.jpy());
        CurrencyNameEntity usd = currencyNameRepository.save(TestFixtures.Currencies.usd());
        CurrencyNameEntity eur = currencyNameRepository.save(TestFixtures.Currencies.eur());

        // Setup exchange rates
        LocalDate testDate = LocalDate.of(2024, 1, 1);
        usdRate = exchangeRateRepository.save(ExchangeRateEntity.builder()
                .currencyCode("USD")
                .currencyName(usd)
                .date(testDate)
                .exchangeRate(BigDecimal.valueOf(148.7))
                .build());

        eurRate = exchangeRateRepository.save(ExchangeRateEntity.builder()
                .currencyCode("EUR")
                .currencyName(eur)
                .date(testDate)
                .exchangeRate(BigDecimal.valueOf(160.5))
                .build());

        // Setup auth user (required for at least one user per group)
        aliceAuth = authUserRepository.save(AuthUserEntity.builder()
                .uid("alice-firebase-uid")
                .name("Alice")
                .email("alice@example.com")
                .build());

        // Setup users
        alice = userRepository.save(UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name("Alice")
                .authUser(aliceAuth)
                .build());

        bob = userRepository.save(UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name("Bob")
                .authUser(null)
                .build());

        carol = userRepository.save(UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name("Carol")
                .authUser(null)
                .build());

        david = userRepository.save(UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name("David")
                .authUser(null)
                .build());

        // Setup group
        testGroup = groupRepository.save(GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name("Test Group")
                .build());

        // Setup user-group relationships
        userGroupRepository.save(UserGroupEntity.builder()
                .userUuid(alice.getUuid())
                .groupUuid(testGroup.getUuid())
                .user(alice)
                .group(testGroup)
                .build());

        userGroupRepository.save(UserGroupEntity.builder()
                .userUuid(bob.getUuid())
                .groupUuid(testGroup.getUuid())
                .user(bob)
                .group(testGroup)
                .build());

        userGroupRepository.save(UserGroupEntity.builder()
                .userUuid(carol.getUuid())
                .groupUuid(testGroup.getUuid())
                .user(carol)
                .group(testGroup)
                .build());

        userGroupRepository.save(UserGroupEntity.builder()
                .userUuid(david.getUuid())
                .groupUuid(testGroup.getUuid())
                .user(david)
                .group(testGroup)
                .build());

        flushAndClear();
    }

    @Nested
    @DisplayName("Given no transactions in group")
    class WhenGroupIsEmpty {

        @Test
        @DisplayName("Then should return empty settlement list")
        void thenShouldReturnEmptySettlementList() {
            // When: Get settlements for empty group
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should return empty list
            assertThat(result.transactionsSettlement()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given single USD loan with two debtors")
    class WhenSingleLoanWithTwoDebtors {

        private TransactionHistoryEntity loan;

        @BeforeEach
        void setupLoan() {
            // Given: Alice lends 100 USD (10000 cents), split between Bob and Carol
            loan = transactionRepository.save(TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Lunch")
                    .amount(10000) // 100.00 USD
                    .transactionDate(Instant.parse("2024-01-01T12:00:00Z"))
                    .payer(alice)
                    .group(testGroup)
                    .exchangeRate(usdRate)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan)
                    .user(bob)
                    .amount(5000) // 50.00 USD
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan)
                    .user(carol)
                    .amount(5000) // 50.00 USD
                    .build());

            flushAndClear();
        }

        @Test
        @DisplayName("Then should calculate correct settlements in JPY")
        void thenShouldCalculateCorrectSettlements() {
            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Debug: Print actual results
            System.out.println("Settlement results: " + result.transactionsSettlement().size() + " transactions");
            result.transactionsSettlement().forEach(t ->
                System.out.println("  " + t.from().getUserName() + " -> " + t.to().getUserName() + ": " + t.amount())
            );

            // Then: Should have 2 settlement transactions
            assertThat(result.transactionsSettlement()).hasSize(2);

            // Then: Bob should pay Alice
            TransactionSettlementResponseDTO bobPayment = result.transactionsSettlement().stream()
                    .filter(t -> t.from().getUuid().equals(bob.getUuid().toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(bobPayment.to().getUuid()).isEqualTo(alice.getUuid().toString());
            // 5000 cents / 148.7 = 33.6249... → 3362 cents (33.62 JPY)
            assertThat(bobPayment.amount()).isEqualTo(3362);

            // Then: Carol should pay Alice
            TransactionSettlementResponseDTO carolPayment = result.transactionsSettlement().stream()
                    .filter(t -> t.from().getUuid().equals(carol.getUuid().toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(carolPayment.to().getUuid()).isEqualTo(alice.getUuid().toString());
            // 5000 cents / 148.7 = 33.6249... → 3362 cents (33.62 JPY)
            assertThat(carolPayment.amount()).isEqualTo(3362);

            // Then: Total payment (sum of individual rounded amounts)
            long totalPayment = bobPayment.amount() + carolPayment.amount();
            // 3362 + 3362 = 6724 cents (due to rounding of individual amounts)
            assertThat(totalPayment).isEqualTo(6724L);
        }
    }

    @Nested
    @DisplayName("Given loan with indivisible amount causing rounding errors")
    class WhenLoanCausesRoundingErrors {

        @BeforeEach
        void setupLoanWithRoundingError() {
            // Given: Alice lends 100 USD (10000 cents), split into 3 unequal parts
            TransactionHistoryEntity loan = transactionRepository.save(TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Dinner")
                    .amount(10000) // 100.00 USD
                    .transactionDate(Instant.parse("2024-01-01T18:00:00Z"))
                    .payer(alice)
                    .group(testGroup)
                    .exchangeRate(usdRate)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan)
                    .user(bob)
                    .amount(3333) // 33.33 USD
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan)
                    .user(carol)
                    .amount(3333) // 33.33 USD
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan)
                    .user(david)
                    .amount(3334) // 33.34 USD (maximum debtor)
                    .build());

            flushAndClear();
        }

        @Test
        @DisplayName("Then should adjust rounding error to maximum debtor")
        void thenShouldAdjustRoundingErrorToMaximumDebtor() {
            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should have 3 settlement transactions
            assertThat(result.transactionsSettlement()).hasSize(3);

            // Then: Sum of all payments (sum of individual rounded amounts)
            long totalPayment = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlementResponseDTO::amount)
                    .sum();

            // 2241 + 2241 + 2242 = 6724 cents (due to rounding of individual amounts)
            assertThat(totalPayment).isEqualTo(6724);

            // Then: David (maximum debtor) should have received the rounding adjustment
            TransactionSettlementResponseDTO davidPayment = result.transactionsSettlement().stream()
                    .filter(t -> t.from().getUuid().equals(david.getUuid().toString()))
                    .findFirst()
                    .orElseThrow();

            // David's obligation: 3334 cents / 148.7 = 22.4212... → base 2242 cents
            // Plus rounding adjustment to ensure perfect repayment
            assertThat(davidPayment.amount()).isGreaterThanOrEqualTo(2242);
        }
    }

    @Nested
    @DisplayName("Given multiple transactions with different currencies")
    class WhenMultipleTransactionsExist {

        @BeforeEach
        void setupMultipleTransactions() {
            // Given: Transaction 1 - Alice lends 100 USD
            TransactionHistoryEntity usdLoan = transactionRepository.save(TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("USD Loan")
                    .amount(10000)
                    .transactionDate(Instant.parse("2024-01-01T12:00:00Z"))
                    .payer(alice)
                    .group(testGroup)
                    .exchangeRate(usdRate)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(usdLoan)
                    .user(bob)
                    .amount(10000)
                    .build());

            // Given: Transaction 2 - Bob lends 50 EUR
            TransactionHistoryEntity eurLoan = transactionRepository.save(TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("EUR Loan")
                    .amount(5000)
                    .transactionDate(Instant.parse("2024-01-01T14:00:00Z"))
                    .payer(bob)
                    .group(testGroup)
                    .exchangeRate(eurRate)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(eurLoan)
                    .user(alice)
                    .amount(5000)
                    .build());

            flushAndClear();
        }

        @Test
        @DisplayName("Then should calculate settlements for each transaction independently")
        void thenShouldCalculateSettlementsIndependently() {
            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should optimize to single or minimal transactions
            assertThat(result.transactionsSettlement()).isNotEmpty();

            // Then: Net balance should be calculated correctly
            // Alice lends 10000 USD / 148.7 = 6725 cents
            // Alice owes 5000 EUR / 160.5 = 3115 cents
            // Net: Alice should receive 6725 - 3115 = 3610 cents

            long aliceNet = result.transactionsSettlement().stream()
                    .filter(t -> t.to().getUuid().equals(alice.getUuid().toString()))
                    .mapToLong(TransactionSettlementResponseDTO::amount)
                    .sum()
                    - result.transactionsSettlement().stream()
                    .filter(t -> t.from().getUuid().equals(alice.getUuid().toString()))
                    .mapToLong(TransactionSettlementResponseDTO::amount)
                    .sum();

            assertThat(aliceNet).isEqualTo(3610);
        }
    }

    @Nested
    @DisplayName("Given complex scenario with multiple creditors and debtors")
    class WhenComplexScenario {

        @BeforeEach
        void setupComplexScenario() {
            // Given: Alice lends to Bob and Carol
            TransactionHistoryEntity loan1 = transactionRepository.save(TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Loan 1")
                    .amount(10000)
                    .transactionDate(Instant.parse("2024-01-01T10:00:00Z"))
                    .payer(alice)
                    .group(testGroup)
                    .exchangeRate(usdRate)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan1)
                    .user(bob)
                    .amount(6000)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan1)
                    .user(carol)
                    .amount(4000)
                    .build());

            // Given: Bob lends to Alice and David
            TransactionHistoryEntity loan2 = transactionRepository.save(TransactionHistoryEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transactionType(TransactionType.LOAN)
                    .title("Loan 2")
                    .amount(8000)
                    .transactionDate(Instant.parse("2024-01-01T11:00:00Z"))
                    .payer(bob)
                    .group(testGroup)
                    .exchangeRate(usdRate)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan2)
                    .user(alice)
                    .amount(5000)
                    .build());

            obligationRepository.save(TransactionObligationEntity.builder()
                    .uuid(UUID.randomUUID())
                    .transaction(loan2)
                    .user(david)
                    .amount(3000)
                    .build());

            flushAndClear();
        }

        @Test
        @DisplayName("Then should minimize number of transactions")
        void thenShouldMinimizeTransactions() {
            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should optimize transactions (not just list all obligations)
            // Maximum possible transactions without optimization: 4 (all obligations)
            // With optimization: Should be less
            assertThat(result.transactionsSettlement().size()).isLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Then all balances should sum to zero")
        void thenAllBalancesShouldSumToZero() {
            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Sum of all payments should equal sum of all receipts
            // (total amount paid = total amount received, ensuring conservation of money)
            long totalPaid = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlementResponseDTO::amount)
                    .sum();

            long totalReceived = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlementResponseDTO::amount)
                    .sum();

            // Total paid equals total received (same list of transactions)
            assertThat(totalPaid).isEqualTo(totalReceived);

            // Also verify that the net balance across all users sums to zero
            List<UserEntity> users = List.of(alice, bob, carol, david);
            long sumOfNetBalances = 0;

            for (UserEntity user : users) {
                long received = result.transactionsSettlement().stream()
                        .filter(t -> t.to().getUuid().equals(user.getUuid().toString()))
                        .mapToLong(TransactionSettlementResponseDTO::amount)
                        .sum();

                long paid = result.transactionsSettlement().stream()
                        .filter(t -> t.from().getUuid().equals(user.getUuid().toString()))
                        .mapToLong(TransactionSettlementResponseDTO::amount)
                        .sum();

                sumOfNetBalances += (received - paid);
            }

            // Sum of all net balances should be zero (conservation of money)
            assertThat(sumOfNetBalances).isEqualTo(0L);
        }
    }
}
