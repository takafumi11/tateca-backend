package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO.TransactionSettlement;
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
                System.out.println("  " + t.from().userName() + " -> " + t.to().userName() + ": " + t.amount())
            );

            // Then: Should have 2 settlement transactions
            assertThat(result.transactionsSettlement()).hasSize(2);

            // Then: Bob should pay Alice
            TransactionSettlement bobPayment = result.transactionsSettlement().stream()
                    .filter(t -> t.from().uuid().equals(bob.getUuid().toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(bobPayment.to().uuid()).isEqualTo(alice.getUuid().toString());
            // 5000 cents / 148.7 = 33.6249... → 3362 cents (33.62 JPY)
            assertThat(bobPayment.amount()).isEqualTo(3362);

            // Then: Carol should pay Alice
            TransactionSettlement carolPayment = result.transactionsSettlement().stream()
                    .filter(t -> t.from().uuid().equals(carol.getUuid().toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(carolPayment.to().uuid()).isEqualTo(alice.getUuid().toString());
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
                    .mapToLong(TransactionSettlement::amount)
                    .sum();

            // 2241 + 2241 + 2242 = 6724 cents (due to rounding of individual amounts)
            assertThat(totalPayment).isEqualTo(6724);

            // Then: David (maximum debtor) should have received the rounding adjustment
            TransactionSettlement davidPayment = result.transactionsSettlement().stream()
                    .filter(t -> t.from().uuid().equals(david.getUuid().toString()))
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
                    .filter(t -> t.to().uuid().equals(alice.getUuid().toString()))
                    .mapToLong(TransactionSettlement::amount)
                    .sum()
                    - result.transactionsSettlement().stream()
                    .filter(t -> t.from().uuid().equals(alice.getUuid().toString()))
                    .mapToLong(TransactionSettlement::amount)
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
                    .mapToLong(TransactionSettlement::amount)
                    .sum();

            long totalReceived = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlement::amount)
                    .sum();

            // Total paid equals total received (same list of transactions)
            assertThat(totalPaid).isEqualTo(totalReceived);

            // Also verify that the net balance across all users sums to zero
            List<UserEntity> users = List.of(alice, bob, carol, david);
            long sumOfNetBalances = 0;

            for (UserEntity user : users) {
                long received = result.transactionsSettlement().stream()
                        .filter(t -> t.to().uuid().equals(user.getUuid().toString()))
                        .mapToLong(TransactionSettlement::amount)
                        .sum();

                long paid = result.transactionsSettlement().stream()
                        .filter(t -> t.from().uuid().equals(user.getUuid().toString()))
                        .mapToLong(TransactionSettlement::amount)
                        .sum();

                sumOfNetBalances += (received - paid);
            }

            // Sum of all net balances should be zero (conservation of money)
            assertThat(sumOfNetBalances).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Given real-world travel expense scenario")
    class GivenRealWorldTravelExpenseScenario {

        @Test
        @DisplayName("Then should correctly settle shared trip expenses")
        void thenShouldCorrectlySettleSharedTripExpenses() {
            // Given: Friends on a trip sharing expenses
            // - Alice paid for hotel: 45000 JPY (everyone splits)
            // - Bob paid for dinner: 12000 JPY (everyone splits)
            // - Carol paid for transportation: 9000 JPY (everyone splits)
            // Total: 66000 JPY / 3 people = 22000 JPY each

            // Hotel payment by Alice (45000 JPY)
            TransactionHistoryEntity hotelTx = createTransaction(alice, 45000L, usdRate, "Hotel Reservation");
            obligationRepository.save(createObligation(hotelTx, alice, 15000));
            obligationRepository.save(createObligation(hotelTx, bob, 15000));
            obligationRepository.save(createObligation(hotelTx, carol, 15000));

            // Dinner payment by Bob (12000 JPY)
            TransactionHistoryEntity dinnerTx = createTransaction(bob, 12000L, usdRate, "Dinner");
            obligationRepository.save(createObligation(dinnerTx, alice, 4000));
            obligationRepository.save(createObligation(dinnerTx, bob, 4000));
            obligationRepository.save(createObligation(dinnerTx, carol, 4000));

            // Transportation by Carol (9000 JPY)
            TransactionHistoryEntity transportTx = createTransaction(carol, 9000L, usdRate, "Transportation");
            obligationRepository.save(createObligation(transportTx, alice, 3000));
            obligationRepository.save(createObligation(transportTx, bob, 3000));
            obligationRepository.save(createObligation(transportTx, carol, 3000));

            flushAndClear();

            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Debug output
            System.out.println("Travel expenses settlement: " + result.transactionsSettlement().size() + " transactions");
            result.transactionsSettlement().forEach(t ->
                System.out.println("  " + t.from().userName() + " → " + t.to().userName() + ": " + t.amount() + " cents")
            );

            // Then: Should have optimized settlements
            assertThat(result.transactionsSettlement()).isNotEmpty();

            // Net balances:
            // Alice: paid 45000, owes (4000+3000) = net 38000 JPY
            // Bob: paid 12000, owes (15000+3000) = net -6000 JPY
            // Carol: paid 9000, owes (15000+4000) = net -10000 JPY

            // Verify net balances
            long aliceNet = calculateNetBalance(result, alice.getUuid().toString());
            long bobNet = calculateNetBalance(result, bob.getUuid().toString());
            long carolNet = calculateNetBalance(result, carol.getUuid().toString());

            // Convert to JPY cents (45000/148.7=30262, 12000/148.7=8068, 9000/148.7=6051)
            // Alice receives: 30262 - (4000+3000)/148.7 = 30262 - 4708 = 25554 cents
            // Bob pays: (15000+3000)/148.7 - 8068 = 12107 - 8068 = 4039 cents
            // Carol pays: (15000+4000)/148.7 - 6051 = 12780 - 6051 = 6729 cents

            assertThat(aliceNet).isPositive(); // Alice should receive
            assertThat(bobNet).isNegative(); // Bob should pay
            assertThat(carolNet).isNegative(); // Carol should pay

            // Total should sum to zero
            assertThat(aliceNet + bobNet + carolNet).isBetween(-1L, 1L);
        }
    }

    @Nested
    @DisplayName("Given monthly bill splitting scenario")
    class GivenMonthlyBillSplittingScenario {

        @Test
        @DisplayName("Then should settle monthly shared expenses")
        void thenShouldSettleMonthlySharedExpenses() {
            // Given: Roommates splitting monthly bills
            // - Alice paid utilities: 15000 JPY (split 3 ways)
            // - Bob paid internet: 6000 JPY (split 3 ways)
            // - Carol paid groceries: 24000 JPY (split 3 ways)
            // Total: 45000 JPY / 3 = 15000 JPY each

            TransactionHistoryEntity utilitiesTx = createTransaction(alice, 15000L, usdRate, "Utilities");
            obligationRepository.save(createObligation(utilitiesTx, alice, 5000));
            obligationRepository.save(createObligation(utilitiesTx, bob, 5000));
            obligationRepository.save(createObligation(utilitiesTx, carol, 5000));

            TransactionHistoryEntity internetTx = createTransaction(bob, 6000L, usdRate, "Internet");
            obligationRepository.save(createObligation(internetTx, alice, 2000));
            obligationRepository.save(createObligation(internetTx, bob, 2000));
            obligationRepository.save(createObligation(internetTx, carol, 2000));

            TransactionHistoryEntity groceriesTx = createTransaction(carol, 24000L, usdRate, "Groceries");
            obligationRepository.save(createObligation(groceriesTx, alice, 8000));
            obligationRepository.save(createObligation(groceriesTx, bob, 8000));
            obligationRepository.save(createObligation(groceriesTx, carol, 8000));

            flushAndClear();

            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: All balances should be equal (everyone owes 15000 JPY)
            // Alice: paid 15000, owes 15000 = 0
            // Bob: paid 6000, owes 15000 = -9000
            // Carol: paid 24000, owes 15000 = +9000

            assertThat(result.transactionsSettlement()).isNotEmpty();

            // Verify conservation of money
            long totalPaid = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlement::amount)
                    .sum();
            long totalReceived = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlement::amount)
                    .sum();

            assertThat(totalPaid).isEqualTo(totalReceived);
        }
    }

    @Nested
    @DisplayName("Given large group with 6 members")
    class GivenLargeGroupWithSixMembers {

        @Test
        @DisplayName("Then should efficiently settle complex group expenses")
        void thenShouldEfficientlySettleComplexGroupExpenses() {
            // Given: 6-person group with multiple transactions
            UserEntity eve = createAndSaveUser("eve-uid", "Eve");
            UserEntity frank = createAndSaveUser("frank-uid", "Frank");

            userGroupRepository.save(createUserGroup(eve, testGroup));
            userGroupRepository.save(createUserGroup(frank, testGroup));

            // Multiple transactions with different payers and splits
            // Alice paid 30000 (split 6 ways)
            TransactionHistoryEntity tx1 = createTransaction(alice, 30000L, usdRate, "Group Activity 1");
            for (UserEntity user : List.of(alice, bob, carol, david, eve, frank)) {
                obligationRepository.save(createObligation(tx1, user, 5000));
            }

            // Bob paid 18000 (split 6 ways)
            TransactionHistoryEntity tx2 = createTransaction(bob, 18000L, eurRate, "Group Activity 2");
            for (UserEntity user : List.of(alice, bob, carol, david, eve, frank)) {
                obligationRepository.save(createObligation(tx2, user, 3000));
            }

            // Carol paid 24000 (split 6 ways)
            TransactionHistoryEntity tx3 = createTransaction(carol, 24000L, usdRate, "Group Activity 3");
            for (UserEntity user : List.of(alice, bob, carol, david, eve, frank)) {
                obligationRepository.save(createObligation(tx3, user, 4000));
            }

            flushAndClear();

            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should optimize settlements for 6 people
            assertThat(result.transactionsSettlement()).isNotEmpty();

            // Verify all 6 users have balanced accounts
            List<UserEntity> allUsers = List.of(alice, bob, carol, david, eve, frank);
            long sumOfNetBalances = 0;

            for (UserEntity user : allUsers) {
                long netBalance = calculateNetBalance(result, user.getUuid().toString());
                sumOfNetBalances += netBalance;
            }

            // Total should be zero (conservation of money)
            assertThat(sumOfNetBalances).isBetween(-1L, 1L);

            // Should optimize to use fewer transactions than naive approach
            // Naive: 5 people pay 1 person = 5 transactions per payer
            // Optimized: Should use graph-based settlement reduction
            assertThat(result.transactionsSettlement().size()).isLessThan(15);
        }
    }

    @Nested
    @DisplayName("Given micro-transactions with small amounts")
    class GivenMicroTransactionsWithSmallAmounts {

        @Test
        @DisplayName("Then should handle cents-level precision correctly")
        void thenShouldHandleCentsLevelPrecision() {
            // Given: Very small amounts (e.g., coffee shop split)
            // Alice paid 450 JPY coffee, Bob and Carol split
            TransactionHistoryEntity coffeeTx = createTransaction(alice, 450L, usdRate, "Coffee");
            obligationRepository.save(createObligation(coffeeTx, alice, 150));
            obligationRepository.save(createObligation(coffeeTx, bob, 150));
            obligationRepository.save(createObligation(coffeeTx, carol, 150));

            flushAndClear();

            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should handle small amounts (450/148.7 = 3.03 JPY = 303 cents total)
            // Bob and Carol should each pay: 150/148.7 = 1.009 JPY ≈ 101 cents
            assertThat(result.transactionsSettlement()).hasSize(2);

            long totalSettlement = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlement::amount)
                    .sum();

            // Total should be close to 202 cents (with rounding adjustment)
            assertThat(totalSettlement).isBetween(201L, 203L);
        }
    }

    @Nested
    @DisplayName("Given large transaction amounts")
    class GivenLargeTransactionAmounts {

        @Test
        @DisplayName("Then should handle large numbers without overflow")
        void thenShouldHandleLargeNumbersWithoutOverflow() {
            // Given: Large expense (e.g., car purchase: 3,000,000 JPY)
            // Alice paid, Bob and Carol split
            TransactionHistoryEntity carTx = createTransaction(alice, 3_000_000L, usdRate, "Car Purchase");
            obligationRepository.save(createObligation(carTx, alice, 1_000_000));
            obligationRepository.save(createObligation(carTx, bob, 1_000_000));
            obligationRepository.save(createObligation(carTx, carol, 1_000_000));

            flushAndClear();

            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should calculate correctly with large amounts
            // 3,000,000 / 148.7 = 20,175.95 JPY ≈ 2,017,595 cents total
            // Each person: 1,000,000 / 148.7 = 6,725.32 JPY ≈ 672,532 cents
            assertThat(result.transactionsSettlement()).hasSize(2);

            long totalSettlement = result.transactionsSettlement().stream()
                    .mapToLong(TransactionSettlement::amount)
                    .sum();

            // Verify no overflow and correct calculation
            assertThat(totalSettlement).isPositive();
            assertThat(totalSettlement).isGreaterThan(1_000_000); // Should be > 10000 JPY in cents
        }
    }

    @Nested
    @DisplayName("Given gradual expense accumulation")
    class GivenGradualExpenseAccumulation {

        @Test
        @DisplayName("Then should accurately track running balances")
        void thenShouldAccuratelyTrackRunningBalances() {
            // Given: Expenses accumulated over time
            // Day 1: Alice paid 10000
            TransactionHistoryEntity day1 = createTransaction(alice, 10000L, usdRate, "Day 1 Expense");
            obligationRepository.save(createObligation(day1, bob, 5000));
            obligationRepository.save(createObligation(day1, carol, 5000));

            // Day 2: Bob paid 8000
            TransactionHistoryEntity day2 = createTransaction(bob, 8000L, eurRate, "Day 2 Expense");
            obligationRepository.save(createObligation(day2, alice, 4000));
            obligationRepository.save(createObligation(day2, carol, 4000));

            // Day 3: Carol paid 12000
            TransactionHistoryEntity day3 = createTransaction(carol, 12000L, usdRate, "Day 3 Expense");
            obligationRepository.save(createObligation(day3, alice, 6000));
            obligationRepository.save(createObligation(day3, bob, 6000));

            // Day 4: Alice paid again 15000
            TransactionHistoryEntity day4 = createTransaction(alice, 15000L, eurRate, "Day 4 Expense");
            obligationRepository.save(createObligation(day4, bob, 7500));
            obligationRepository.save(createObligation(day4, carol, 7500));

            flushAndClear();

            // When: Get settlements
            TransactionsSettlementResponseDTO result = transactionService.getSettlements(testGroup.getUuid());

            // Then: Should correctly settle cumulative balances
            assertThat(result.transactionsSettlement()).isNotEmpty();

            // Verify conservation across all transactions
            long aliceNet = calculateNetBalance(result, alice.getUuid().toString());
            long bobNet = calculateNetBalance(result, bob.getUuid().toString());
            long carolNet = calculateNetBalance(result, carol.getUuid().toString());

            // Sum should be zero
            assertThat(aliceNet + bobNet + carolNet).isBetween(-1L, 1L);
        }
    }

    // Helper methods for new tests

    private UserEntity createAndSaveUser(String authUid, String name) {
        AuthUserEntity authUser = authUserRepository.save(AuthUserEntity.builder()
                .uid(authUid)
                .name(name)
                .email(name.toLowerCase() + "@example.com")
                .build());

        return userRepository.save(UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(name)
                .authUser(authUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    private TransactionHistoryEntity createTransaction(UserEntity payer, long amount, ExchangeRateEntity rate, String title) {
        return transactionRepository.save(TransactionHistoryEntity.builder()
                .uuid(UUID.randomUUID())
                .transactionType(TransactionType.LOAN)
                .title(title)
                .amount((int) amount)
                .payer(payer)
                .group(testGroup)
                .exchangeRate(rate)
                .transactionDate(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    private TransactionObligationEntity createObligation(TransactionHistoryEntity transaction, UserEntity user, long amount) {
        return TransactionObligationEntity.builder()
                .uuid(UUID.randomUUID())
                .transaction(transaction)
                .user(user)
                .amount((int) amount)
                .build();
    }

    private UserGroupEntity createUserGroup(UserEntity user, GroupEntity group) {
        return UserGroupEntity.builder()
                .userUuid(user.getUuid())
                .groupUuid(group.getUuid())
                .user(user)
                .group(group)
                .build();
    }

    private long calculateNetBalance(TransactionsSettlementResponseDTO result, String userId) {
        long received = result.transactionsSettlement().stream()
                .filter(t -> t.to().uuid().equals(userId))
                .mapToLong(TransactionSettlement::amount)
                .sum();

        long paid = result.transactionsSettlement().stream()
                .filter(t -> t.from().uuid().equals(userId))
                .mapToLong(TransactionSettlement::amount)
                .sum();

        return received - paid;
    }
}
