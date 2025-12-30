package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.accessor.UserGroupAccessor;
import com.tateca.tatecabackend.dto.request.TransactionCreationRequestDTO;
import com.tateca.tatecabackend.dto.response.TransactionDetailResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO.TransactionSettlement;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.model.ParticipantModel;
import com.tateca.tatecabackend.model.TransactionType;
import com.tateca.tatecabackend.util.LogFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tateca.tatecabackend.service.util.TimeHelper.UTC_ZONE_ID;
import static com.tateca.tatecabackend.service.util.TimeHelper.convertToLocalDateInUtc;
import static com.tateca.tatecabackend.service.util.TimeHelper.dateStringToInstant;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private static final Logger logger = LogFactory.getLogger(TransactionServiceImpl.class);
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;
    private final UserGroupAccessor userGroupAccessor;
    private final TransactionAccessor accessor;
    private final ObligationAccessor obligationAccessor;
    private final ExchangeRateAccessor exchangeRateAccessor;

    @Override
    @Transactional(readOnly = true)
    public TransactionsHistoryResponseDTO getTransactionHistory(int count, UUID groupId) {
        List<TransactionHistoryEntity> transactionHistoryEntityList = accessor.findTransactionsByGroupWithLimit(groupId, count);

        return TransactionsHistoryResponseDTO.buildResponse(transactionHistoryEntityList);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionsSettlementResponseDTO getSettlements(UUID groupId) {
        // Fetch all users in group (not just those with obligations)
        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuidWithUserDetails(groupId);

        List<String> userIds = userGroups.stream()
                .map(UserGroupEntity::getUserUuid)
                .map(UUID::toString)
                .toList();

        List<TransactionObligationEntity> obligations = obligationAccessor.findByGroupId(groupId);

        Map<String, BigDecimal> balances = getUserBalances(userIds, obligations);
        Map<String, UserResponseDTO> userMap = extractUserMapFromGroups(userGroups);

        List<TransactionSettlement> transactions = optimizeTransactions(balances, userMap);

        return new TransactionsSettlementResponseDTO(transactions);
    }

    /**
     * Extract UserResponseDTO map from UserGroupEntity list.
     * This includes ALL users in the group, not just those with obligations.
     */
    private Map<String, UserResponseDTO> extractUserMapFromGroups(List<UserGroupEntity> userGroups) {
        return userGroups.stream()
                .collect(Collectors.toMap(
                        ug -> ug.getUserUuid().toString(),
                        ug -> UserResponseDTO.from(ug.getUser())
                ));
    }

    /**
     * Calculate user balances in JPY with two-phase approach:
     * Phase 1: User-based loop (handles LOAN and REPAYMENT correctly)
     * Phase 2: Transaction-based rounding adjustment
     */
    private Map<String, BigDecimal> getUserBalances(List<String> userIds, List<TransactionObligationEntity> obligations) {
        // Phase 1: Calculate base balances per user
        Map<String, BigDecimal> balances = new HashMap<>();

        // Initialize all users with zero balance
        for (String userId : userIds) {
            balances.put(userId, BigDecimal.ZERO);
        }

        // Calculate balances by iterating through users and their obligations
        for (String userId : userIds) {
            BigDecimal balance = BigDecimal.ZERO;

            for (TransactionObligationEntity obligation : obligations) {
                String obligationUserId = obligation.getUser().getUuid().toString();
                String payerId = obligation.getTransaction().getPayer().getUuid().toString();

                // Convert to JPY
                BigDecimal transactionRate = obligation.getTransaction().getExchangeRate().getExchangeRate();
                BigDecimal amountInJpy = BigDecimal.valueOf(obligation.getAmount())
                        .divide(transactionRate, 7, RoundingMode.HALF_UP);

                // If user is the debtor (obligation.user), they owe money → positive balance
                if (obligationUserId.equals(userId)) {
                    balance = balance.add(amountInJpy);
                }

                // If user is the payer (creditor), they are owed money → negative balance
                if (payerId.equals(userId)) {
                    balance = balance.subtract(amountInJpy);
                }
            }

            balances.put(userId, balance);
        }

        // Phase 2: Apply rounding error adjustments per transaction
        applyRoundingAdjustments(balances, obligations);

        return balances;
    }

    /**
     * Apply rounding error adjustments per transaction.
     * Ensures that the sum of individual obligation amounts equals the total transaction amount.
     */
    private void applyRoundingAdjustments(Map<String, BigDecimal> balances, List<TransactionObligationEntity> obligations) {
        // Group obligations by transaction UUID
        Map<UUID, List<TransactionObligationEntity>> obligationsByTransaction =
                obligations.stream()
                        .collect(Collectors.groupingBy(o -> o.getTransaction().getUuid()));

        // Process each transaction to adjust rounding errors
        for (List<TransactionObligationEntity> transactionObligations : obligationsByTransaction.values()) {
            if (transactionObligations.isEmpty()) continue;

            TransactionHistoryEntity transaction = transactionObligations.getFirst().getTransaction();
            BigDecimal transactionRate = transaction.getExchangeRate().getExchangeRate();

            // Calculate total amount in JPY
            BigDecimal totalAmountInJpy = BigDecimal.valueOf(transaction.getAmount())
                    .divide(transactionRate, 7, RoundingMode.HALF_UP);

            // Calculate sum of individual obligation amounts in JPY
            BigDecimal individualSum = BigDecimal.ZERO;
            String maxDebtorId = null;
            BigDecimal maxDebtorAmount = BigDecimal.ZERO;

            for (TransactionObligationEntity obligation : transactionObligations) {
                String debtorId = obligation.getUser().getUuid().toString();
                BigDecimal amountInJpy = BigDecimal.valueOf(obligation.getAmount())
                        .divide(transactionRate, 7, RoundingMode.HALF_UP);

                individualSum = individualSum.add(amountInJpy);

                // Track maximum debtor for this transaction
                if (amountInJpy.compareTo(maxDebtorAmount) > 0) {
                    maxDebtorId = debtorId;
                    maxDebtorAmount = amountInJpy;
                }
            }

            // Adjust rounding error to maximum debtor
            BigDecimal difference = totalAmountInJpy.subtract(individualSum);
            if (difference.compareTo(BigDecimal.ZERO) != 0 && maxDebtorId != null) {
                balances.merge(maxDebtorId, difference, BigDecimal::add);
                logger.debug("Adjusted rounding error {} JPY for transaction {} to user {}",
                        difference, transaction.getUuid(), maxDebtorId);
            }
        }
    }

    private List<TransactionSettlement> optimizeTransactions(Map<String, BigDecimal> balances, Map<String, UserResponseDTO> userMap) {
        PriorityQueue<ParticipantModel> creditors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));
        PriorityQueue<ParticipantModel> debtors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));

        classifyParticipants(balances, creditors, debtors, userMap);

        List<TransactionSettlement> transactions = new ArrayList<>();

        processTransactions(creditors, debtors, transactions);

        return transactions;
    }

    private void classifyParticipants(Map<String, BigDecimal> balances, PriorityQueue<ParticipantModel> creditors, PriorityQueue<ParticipantModel> debtors, Map<String, UserResponseDTO> userMap) {
        balances.forEach((userId, amount) -> {
            UserResponseDTO user = userMap.get(userId);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                creditors.add(new ParticipantModel(user, amount.negate()));
            } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new ParticipantModel(user, amount));
            }
        });
    }

    private void processTransactions(PriorityQueue<ParticipantModel> creditors, PriorityQueue<ParticipantModel> debtors, List<TransactionSettlement> transactions) {
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            ParticipantModel debtor = debtors.poll();
            ParticipantModel creditor = creditors.poll();

            assert creditor != null;
            BigDecimal minAmount = debtor.getAmount().min(creditor.getAmount());
            // Convert JPY to cents (multiply by 100 and round)
            long amountInCents = minAmount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();
            if (amountInCents != 0) {
                transactions.add(new TransactionSettlement(debtor.getUserId(), creditor.getUserId(), amountInCents));
            }

            updateBalances(debtor, creditor, minAmount, debtors, creditors);
        }
    }

    private void updateBalances(ParticipantModel debtor, ParticipantModel creditor, BigDecimal minAmount, PriorityQueue<ParticipantModel> debtors, PriorityQueue<ParticipantModel> creditors) {
        debtor.setAmount(debtor.getAmount().subtract(minAmount));
        creditor.setAmount(creditor.getAmount().subtract(minAmount));

        if (debtor.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            debtors.add(debtor);
        }
        if (creditor.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            creditors.add(creditor);
        }
    }

    @Override
    @Transactional
    public TransactionDetailResponseDTO createTransaction(UUID groupId, TransactionCreationRequestDTO request) {
        // Save into transaction_history
        ExchangeRateEntity exchangeRate = null;
        LocalDate date = convertToLocalDateInUtc(request.getDateStr());

        try {
            exchangeRate = exchangeRateAccessor.findByCurrencyCodeAndDate(request.getCurrencyCode(), date);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                /*
                If the client sends data with a future date that isn't present in the database, we temporarily substitute it with the most recent available value.
                However, since the scheduler updates the value the day before, if a repayment occurs before that update, the value may become inaccurate.
                */
                ExchangeRateEntity existing = exchangeRateAccessor.findByCurrencyCodeAndDate(request.getCurrencyCode(), LocalDate.now(UTC_ZONE_ID));
                ExchangeRateEntity newExchangeRateEntity = ExchangeRateEntity.builder()
                        .currencyCode(existing.getCurrencyCode())
                        .date(date)
                        .exchangeRate(existing.getExchangeRate())
                        .currencyName(existing.getCurrencyName())
                        .build();
                exchangeRate = exchangeRateAccessor.save(newExchangeRateEntity);
            }
        }

        UserEntity payer = userAccessor.findById(request.getPayerId());
        GroupEntity group = groupAccessor.findById(groupId);
        TransactionHistoryEntity savedTransaction = accessor.save(TransactionHistoryEntity.from(request.getTransactionType(), request.getTitle(), request.getAmount(), dateStringToInstant(request.getDateStr()), payer, group, exchangeRate));

        // Save into transaction_obligations
        if (request.getTransactionType() == TransactionType.LOAN) {
            List<TransactionObligationEntity> transactionObligationEntityList = request.getLoanRequest().getObligationRequestDTOs().stream()
                    .map(obligation -> {
                        UserEntity obligationUser = userAccessor.findById(obligation.getUserUuid());

                        return TransactionObligationEntity.builder()
                                .uuid(UUID.randomUUID())
                                .transaction(savedTransaction)
                                .user(obligationUser)
                                .amount(obligation.getAmount())
                                .build();
                    })
                    .collect(Collectors.toList());

            List<TransactionObligationEntity> savedObligations = obligationAccessor.saveAll(transactionObligationEntityList);

            return TransactionDetailResponseDTO.from(savedTransaction, savedObligations);
        } else {
            UserEntity recipient = userAccessor.findById(request.getRepaymentRequest().getRecipientId());

            TransactionObligationEntity savedObligation = obligationAccessor.save(TransactionObligationEntity.from(savedTransaction, recipient));

            return TransactionDetailResponseDTO.from(savedTransaction, savedObligation);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponseDTO getTransactionDetail(UUID transactionId) {
        TransactionHistoryEntity transaction = accessor.findById(transactionId);
        TransactionType transactionType = transaction.getTransactionType();

        List<TransactionObligationEntity> transactionObligationEntityList = obligationAccessor.findByTransactionId(transactionId);

        if (transactionType == TransactionType.LOAN) {
           return TransactionDetailResponseDTO.from(transaction, transactionObligationEntityList);
        } else {
            return TransactionDetailResponseDTO.from(transaction, transactionObligationEntityList.get(0));
        }
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID transactionId) {
        // Delete Obligations first
        List<TransactionObligationEntity> transactionObligationEntityList = obligationAccessor.findByTransactionId(transactionId);
        List<UUID> uuidList = transactionObligationEntityList.stream().map(TransactionObligationEntity::getUuid).toList();
        obligationAccessor.deleteAllById(uuidList);

        accessor.deleteById(transactionId);
    }
}
