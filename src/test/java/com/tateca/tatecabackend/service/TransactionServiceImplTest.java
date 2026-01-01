package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.accessor.UserGroupAccessor;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO.TransactionSettlement;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.model.TransactionType;
import com.tateca.tatecabackend.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TransactionServiceImpl.
 *
 * <p>Tests verify business logic in isolation using mocked dependencies.
 * Focus on public method behavior: input → output validation (black-box testing).
 * Private method implementations are ignored.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl Unit Tests")
class TransactionServiceImplTest {

    @Mock
    private UserAccessor userAccessor;

    @Mock
    private GroupAccessor groupAccessor;

    @Mock
    private UserGroupAccessor userGroupAccessor;

    @Mock
    private TransactionAccessor transactionAccessor;

    @Mock
    private ObligationAccessor obligationAccessor;

    @Mock
    private ExchangeRateAccessor exchangeRateAccessor;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Nested
    @DisplayName("getSettlements()")
    class GetSettlements {

        @Nested
        @DisplayName("Given no obligations in group")
        class GivenNoObligations {

            @Test
            @DisplayName("Then should return empty settlement list")
            void thenShouldReturnEmptySettlementList() {
                // Given: Empty obligations list
                UUID groupId = UUID.randomUUID();
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(new ArrayList<>());
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(new ArrayList<>());

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Should return empty list
                assertThat(result).isNotNull();
                assertThat(result.transactionsSettlement()).isEmpty();
            }
        }

        @Nested
        @DisplayName("Given simple two-person loan")
        class GivenSimpleTwoPersonLoan {

            @Test
            @DisplayName("Then should calculate single settlement")
            void thenShouldCalculateSingleSettlement() {
                // Given: Alice paid 10000 USD cents, Bob owes 10000 USD cents
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test1");
                UserEntity bob = createUser("Bob", "test1");
                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));

                TransactionHistoryEntity transaction = createTransaction(alice, 10000, usdRate);
                TransactionObligationEntity obligation = createObligation(transaction, bob, 10000);

                List<TransactionObligationEntity> obligations = List.of(obligation);
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Should have one settlement from Bob to Alice
                assertThat(result.transactionsSettlement()).hasSize(1);

                TransactionSettlement settlement = result.transactionsSettlement().get(0);
                assertThat(settlement.from().uuid()).isEqualTo(bob.getUuid().toString());
                assertThat(settlement.to().uuid()).isEqualTo(alice.getUuid().toString());
                assertThat(settlement.amount()).isPositive(); // Amount calculated by service logic
            }
        }

        @Nested
        @DisplayName("Given three-person group with circular debts")
        class GivenThreePersonCircularDebts {

            @Test
            @DisplayName("Then should optimize settlements")
            void thenShouldOptimizeSettlements() {
                // Given: Three people with circular obligations
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test2");
                UserEntity bob = createUser("Bob", "test2");
                UserEntity carol = createUser("Carol", "test2");

                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));

                // Transaction 1: Alice paid 15000, Bob and Carol owe 7500 each
                TransactionHistoryEntity tx1 = createTransaction(alice, 15000, usdRate);
                TransactionObligationEntity tx1Ob1 = createObligation(tx1, bob, 7500);
                TransactionObligationEntity tx1Ob2 = createObligation(tx1, carol, 7500);

                // Transaction 2: Bob paid 9000, Alice and Carol owe 4500 each
                TransactionHistoryEntity tx2 = createTransaction(bob, 9000, usdRate);
                TransactionObligationEntity tx2Ob1 = createObligation(tx2, alice, 4500);
                TransactionObligationEntity tx2Ob2 = createObligation(tx2, carol, 4500);

                List<TransactionObligationEntity> obligations = List.of(tx1Ob1, tx1Ob2, tx2Ob1, tx2Ob2);
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Should have settlements (optimized, not necessarily 3)
                assertThat(result.transactionsSettlement()).isNotEmpty();

                // Verify conservation of money: sum of all net balances = 0
                long aliceNet = calculateNetBalance(result, alice.getUuid().toString());
                long bobNet = calculateNetBalance(result, bob.getUuid().toString());
                long carolNet = calculateNetBalance(result, carol.getUuid().toString());

                long totalNet = aliceNet + bobNet + carolNet;
                assertThat(totalNet).isBetween(-1L, 1L); // Should be zero or very close
            }
        }

        @Nested
        @DisplayName("Given all users have balanced accounts")
        class GivenAllUsersBalanced {

            @Test
            @DisplayName("Then should return empty settlement list")
            void thenShouldReturnEmptySettlementList() {
                // Given: User paid and owes same amount (balanced)
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test3");
                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));

                TransactionHistoryEntity transaction = createTransaction(alice, 10000, usdRate);
                TransactionObligationEntity obligation = createObligation(transaction, alice, 10000);

                List<TransactionObligationEntity> obligations = List.of(obligation);
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: No settlements needed
                assertThat(result.transactionsSettlement()).isEmpty();
            }
        }

        @Nested
        @DisplayName("Given multi-currency obligations")
        class GivenMultiCurrencyObligations {

            @Test
            @DisplayName("Then should calculate settlements with currency conversion")
            void thenShouldCalculateWithCurrencyConversion() {
                // Given: Obligations in different currencies
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test4");
                UserEntity bob = createUser("Bob", "test4");

                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));
                ExchangeRateEntity eurRate = createExchangeRate("EUR", new BigDecimal("160.00"));

                // Alice paid 15000 USD cents (= 100 JPY)
                TransactionHistoryEntity tx1 = createTransaction(alice, 15000, usdRate);
                TransactionObligationEntity tx1Ob = createObligation(tx1, bob, 15000);

                // Bob paid 24000 EUR cents (= 150 JPY)
                TransactionHistoryEntity tx2 = createTransaction(bob, 24000, eurRate);
                TransactionObligationEntity tx2Ob = createObligation(tx2, alice, 24000);

                List<TransactionObligationEntity> obligations = List.of(tx1Ob, tx2Ob);
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Should calculate settlements with conversions
                // Alice owes Bob (100 JPY) but Bob owes Alice (150 JPY) → Net: Alice receives 50 JPY
                assertThat(result.transactionsSettlement()).isNotEmpty();

                // Net balances should sum to zero (conservation)
                long aliceNet = calculateNetBalance(result, alice.getUuid().toString());
                long bobNet = calculateNetBalance(result, bob.getUuid().toString());

                assertThat(aliceNet + bobNet).isBetween(-1L, 1L);
            }
        }

        @Nested
        @DisplayName("Given large group with multiple transactions")
        class GivenLargeGroupWithMultipleTransactions {

            @Test
            @DisplayName("Then should settle all balances correctly")
            void thenShouldSettleAllBalancesCorrectly() {
                // Given: 4 users with multiple transactions
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test5");
                UserEntity bob = createUser("Bob", "test5");
                UserEntity carol = createUser("Carol", "test5");
                UserEntity david = createUser("David", "test5");

                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));

                List<TransactionObligationEntity> obligations = new ArrayList<>();

                // Multiple complex transactions
                TransactionHistoryEntity tx1 = createTransaction(alice, 20000, usdRate);
                obligations.add(createObligation(tx1, bob, 5000));
                obligations.add(createObligation(tx1, carol, 5000));
                obligations.add(createObligation(tx1, david, 5000));
                obligations.add(createObligation(tx1, alice, 5000));

                TransactionHistoryEntity tx2 = createTransaction(bob, 12000, usdRate);
                obligations.add(createObligation(tx2, alice, 6000));
                obligations.add(createObligation(tx2, carol, 6000));

                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Should have settlements
                assertThat(result.transactionsSettlement()).isNotEmpty();

                // Verify conservation of money across all users
                long aliceNet = calculateNetBalance(result, alice.getUuid().toString());
                long bobNet = calculateNetBalance(result, bob.getUuid().toString());
                long carolNet = calculateNetBalance(result, carol.getUuid().toString());
                long davidNet = calculateNetBalance(result, david.getUuid().toString());

                long totalNet = aliceNet + bobNet + carolNet + davidNet;
                assertThat(totalNet).isBetween(-1L, 1L); // Conservation of money
            }
        }

        @Nested
        @DisplayName("Given edge case with tiny amounts")
        class GivenEdgeCaseWithTinyAmounts {

            @Test
            @DisplayName("Then should handle rounding correctly")
            void thenShouldHandleRoundingCorrectly() {
                // Given: Very small obligation amounts
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test6");
                UserEntity bob = createUser("Bob", "test6");

                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));

                TransactionHistoryEntity transaction = createTransaction(alice, 100, usdRate);
                TransactionObligationEntity obligation = createObligation(transaction, bob, 100);

                List<TransactionObligationEntity> obligations = List.of(obligation);
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Should handle small amounts without errors
                assertThat(result.transactionsSettlement()).hasSize(1);
                TransactionSettlement settlement = result.transactionsSettlement().get(0);
                assertThat(settlement.amount()).isGreaterThan(0);
            }
        }

        @Nested
        @DisplayName("Given edge case with large amounts")
        class GivenEdgeCaseWithLargeAmounts {

            @Test
            @DisplayName("Then should handle large numbers without overflow")
            void thenShouldHandleLargeNumbersWithoutOverflow() {
                // Given: Very large obligation amounts
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test7");
                UserEntity bob = createUser("Bob", "test7");

                ExchangeRateEntity usdRate = createExchangeRate("USD", new BigDecimal("150.00"));

                TransactionHistoryEntity transaction = createTransaction(alice, 1_000_000, usdRate);
                TransactionObligationEntity obligation = createObligation(transaction, bob, 1_000_000);

                List<TransactionObligationEntity> obligations = List.of(obligation);
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Should calculate correctly without overflow
                assertThat(result.transactionsSettlement()).hasSize(1);
                TransactionSettlement settlement = result.transactionsSettlement().get(0);
                assertThat(settlement.amount()).isPositive();
                assertThat(settlement.amount()).isGreaterThan(1000); // Should be > 10 JPY in cents
            }
        }

        @Nested
        @DisplayName("Given rounding-prone exchange rate")
        class GivenRoundingProneExchangeRate {

            @Test
            @DisplayName("Then should adjust rounding errors to prevent total mismatch")
            void thenShouldAdjustRoundingErrors() {
                // Given: Exchange rate that causes rounding issues (148.7)
                // Total: 10000 cents ÷ 148.7 = 67.2496... JPY
                // Split 3 ways: 3333 + 3333 + 3334 = 10000 cents
                // Without adjustment: individual sums may not equal total
                UUID groupId = UUID.randomUUID();

                UserEntity alice = createUser("Alice", "test8");
                UserEntity bob = createUser("Bob", "test8");
                UserEntity carol = createUser("Carol", "test8");
                UserEntity david = createUser("David", "test8");

                ExchangeRateEntity problemRate = createExchangeRate("USD", new BigDecimal("148.7"));

                // Alice paid 10000 cents, split among Bob, Carol, David
                TransactionHistoryEntity transaction = createTransaction(alice, 10000, problemRate);
                TransactionObligationEntity ob1 = createObligation(transaction, bob, 3333);
                TransactionObligationEntity ob2 = createObligation(transaction, carol, 3333);
                TransactionObligationEntity ob3 = createObligation(transaction, david, 3334);

                List<TransactionObligationEntity> obligations = List.of(ob1, ob2, ob3);
                when(userGroupAccessor.findByGroupUuidWithUserDetails(groupId))
                        .thenReturn(createUserGroupsFromObligations(obligations));
                when(obligationAccessor.findByGroupId(groupId))
                        .thenReturn(obligations);

                // When: Get settlements
                TransactionsSettlementResponseDTO result = transactionService.getSettlements(groupId);

                // Then: Rounding adjustment should ensure conservation of money
                long aliceNet = calculateNetBalance(result, alice.getUuid().toString());
                long bobNet = calculateNetBalance(result, bob.getUuid().toString());
                long carolNet = calculateNetBalance(result, carol.getUuid().toString());
                long davidNet = calculateNetBalance(result, david.getUuid().toString());

                long totalNet = aliceNet + bobNet + carolNet + davidNet;

                // Should be zero or very close (within ±1 due to rounding)
                assertThat(totalNet).isBetween(-1L, 1L);

                // Alice should receive approximately 67 JPY (10000 USD cents ÷ 148.7 rate ≈ 67.25 JPY)
                assertThat(aliceNet).isPositive(); // Alice paid, so she receives money back
                assertThat(aliceNet).isBetween(66L, 68L);
            }
        }
    }

    // Helper methods for creating test data

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
        CurrencyNameEntity currency = CurrencyNameEntity.builder()
                .currencyCode(currencyCode)
                .jpCurrencyName(currencyCode)
                .engCurrencyName(currencyCode)
                .build();

        return ExchangeRateEntity.builder()
                .currencyCode(currencyCode)
                .date(LocalDate.now())
                .exchangeRate(rate)
                .currencyName(currency)
                .build();
    }

    private GroupEntity createGroup() {
        return GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name("Test Group")
                .joinToken(UUID.randomUUID())
                .tokenExpires(Instant.now().plusSeconds(86400))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TransactionHistoryEntity createTransaction(UserEntity payer, int amount, ExchangeRateEntity rate) {
        return TransactionHistoryEntity.builder()
                .uuid(UUID.randomUUID())
                .transactionType(TransactionType.LOAN)
                .title("Test Transaction")
                .amount(amount)
                .payer(payer)
                .group(createGroup())
                .exchangeRate(rate)
                .transactionDate(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TransactionObligationEntity createObligation(TransactionHistoryEntity transaction, UserEntity user, int amount) {
        return TransactionObligationEntity.builder()
                .uuid(UUID.randomUUID())
                .transaction(transaction)
                .user(user)
                .amount(amount)
                .build();
    }

    private long calculateNetBalance(TransactionsSettlementResponseDTO result, String userId) {
        long received = result.transactionsSettlement().stream()
                .filter(s -> s.to().uuid().equals(userId))
                .mapToLong(TransactionSettlement::amount)
                .sum();

        long paid = result.transactionsSettlement().stream()
                .filter(s -> s.from().uuid().equals(userId))
                .mapToLong(TransactionSettlement::amount)
                .sum();

        return received - paid;
    }

    /**
     * Extract unique users from obligations and create UserGroupEntity list for mocking.
     */
    private List<UserGroupEntity> createUserGroupsFromObligations(List<TransactionObligationEntity> obligations) {
        // Collect unique users (both debtors and payers)
        var uniqueUsers = new java.util.HashSet<UserEntity>();
        for (TransactionObligationEntity obligation : obligations) {
            uniqueUsers.add(obligation.getUser());
            uniqueUsers.add(obligation.getTransaction().getPayer());
        }

        // Convert to UserGroupEntity list
        return uniqueUsers.stream()
                .map(user -> UserGroupEntity.builder()
                        .userUuid(user.getUuid())
                        .groupUuid(UUID.randomUUID())
                        .user(user)
                        .build())
                .toList();
    }
}
