package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.repository.TransactionRepository;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.repository.GroupRepository;
import com.tateca.tatecabackend.repository.ObligationRepository;
import com.tateca.tatecabackend.repository.UserGroupRepository;
import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO.TransactionSettlement;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.model.ParticipantModel;
import com.tateca.tatecabackend.model.TransactionType;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.TransactionService;
import com.tateca.tatecabackend.util.LogFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import static com.tateca.tatecabackend.util.TimeHelper.convertToLocalDateInUtc;
import static com.tateca.tatecabackend.util.TimeHelper.dateStringToInstant;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private static final Logger logger = LogFactory.getLogger(TransactionServiceImpl.class);
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final TransactionRepository transactionRepository;
    private final ObligationRepository obligationRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    @Transactional(readOnly = true)
    public TransactionHistoryResponseDTO getTransactionHistory(int count, UUID groupId) {
        List<TransactionHistoryEntity> transactionHistoryEntityList = transactionRepository.findTransactionsByGroupWithLimit(groupId, count);

        return TransactionHistoryResponseDTO.buildResponse(transactionHistoryEntityList);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSettlementResponseDTO getSettlements(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);

        List<String> userIds = userGroups.stream()
                .map(UserGroupEntity::getUserUuid)
                .map(UUID::toString)
                .toList();

        List<TransactionObligationEntity> transactionObligationEntityList = obligationRepository.findByGroupId(groupId);

        Map<String, BigDecimal> balances = getUserBalances(userIds, transactionObligationEntityList);
        List<TransactionSettlement> transactions = optimizeTransactions(balances, userGroups);

        return new TransactionSettlementResponseDTO(transactions);
    }

    private Map<String, BigDecimal> getUserBalances(List<String> userIds, List<TransactionObligationEntity> transactionObligationEntityList) {
        Map<String, BigDecimal> balances = new HashMap<>();

        for (String userId : userIds) {
            BigDecimal balance = BigDecimal.ZERO;

            for (TransactionObligationEntity obligation : transactionObligationEntityList) {
                String obligationUserId = obligation.getUser().getUuid().toString();
                String payerId = obligation.getTransaction().getPayer().getUuid().toString();

                // Convert transaction currency to JPY
                BigDecimal transactionRate = obligation.getTransaction().getExchangeRate().getExchangeRate();
                BigDecimal amountInJpy = BigDecimal.valueOf(obligation.getAmount())
                        .divide(transactionRate, 7, RoundingMode.HALF_UP);

                if (obligationUserId.equals(userId)) {
                    balance = balance.add(amountInJpy);
                }

                if (payerId.equals(userId)) {
                    balance = balance.subtract(amountInJpy);
                }
            }

            balances.put(userId, balance);
        }

        // Apply final adjustment to ensure perfect balance
        applyFinalBalanceAdjustment(balances);

        return balances;
    }

    /**
     * Apply final balance adjustment to ensure perfect conservation of money.
     * Ensures that the sum of all user balances equals zero by adjusting the largest debtor.
     */
    private void applyFinalBalanceAdjustment(Map<String, BigDecimal> balances) {
        // Calculate total net balance (should be zero, may have rounding residual)
        BigDecimal totalBalance = balances.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // If there's a rounding residual, adjust the largest debtor
        if (totalBalance.compareTo(BigDecimal.ZERO) != 0) {
            // Find largest debtor (positive balance = owes money)
            String largestDebtor = balances.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .max(Comparator.comparing(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (largestDebtor != null) {
                // Adjust largest debtor's balance to ensure total = 0
                balances.merge(largestDebtor, totalBalance.negate(), BigDecimal::add);
                logger.debug("Applied final balance adjustment: {} JPY to largest debtor {}",
                        totalBalance.negate(), largestDebtor);
            }
        }
    }

    private List<TransactionSettlement> optimizeTransactions(Map<String, BigDecimal> balances, List<UserGroupEntity> userGroups) {
        PriorityQueue<ParticipantModel> creditors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));
        PriorityQueue<ParticipantModel> debtors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));

        classifyParticipants(balances, creditors, debtors, userGroups);

        List<TransactionSettlement> transactions = new ArrayList<>();

        processTransactions(creditors, debtors, transactions);

        return transactions;
    }

    private void classifyParticipants(Map<String, BigDecimal> balances, PriorityQueue<ParticipantModel> creditors, PriorityQueue<ParticipantModel> debtors, List<UserGroupEntity> userGroups) {
        Map<String, UserResponseDTO> userMap = userGroups.stream()
                .collect(Collectors.toMap(
                        u -> u.getUserUuid().toString(),
                        u -> UserResponseDTO.from(u.getUser())
                ));

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
            // Round to nearest integer using HALF_UP (JPY doesn't have fractional units)
            long roundedAmount = minAmount.setScale(0, RoundingMode.HALF_UP).longValue();
            if (roundedAmount != 0) {
                transactions.add(new TransactionSettlement(debtor.getUserId(), creditor.getUserId(), roundedAmount));
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
    public CreateTransactionResponseDTO createTransaction(UUID groupId, CreateTransactionRequestDTO request) {
        // Save into transaction_history
        LocalDate date = convertToLocalDateInUtc(request.dateStr());
        ExchangeRateEntity exchangeRate = exchangeRateRepository
                .findByCurrencyCodeAndDate(request.currencyCode(), date)
                .orElseGet(() -> {
                    /*
                    If the exchange rate for the specified date doesn't exist, use the latest
                    (most recent) available exchange rate for that currency. This provides a
                    more accurate fallback than LocalDate.now(), especially for historical dates
                    or when the current date's rate hasn't been updated yet.
                    */
                    ExchangeRateEntity latestRate = exchangeRateRepository
                            .findLatestByCurrencyCode(request.currencyCode())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "No exchange rate found for currency code: " + request.currencyCode()
                            ));

                    ExchangeRateEntity newExchangeRateEntity = ExchangeRateEntity.builder()
                            .currencyCode(latestRate.getCurrencyCode())
                            .date(date)
                            .exchangeRate(latestRate.getExchangeRate())
                            .currency(latestRate.getCurrency())
                            .build();
                    return exchangeRateRepository.save(newExchangeRateEntity);
                });

        UserEntity payer = userRepository.findById(request.payerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.payerId()));
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));
        TransactionHistoryEntity savedTransaction = transactionRepository.save(TransactionHistoryEntity.from(request.transactionType(), request.title(), request.amount(), dateStringToInstant(request.dateStr()), payer, group, exchangeRate));

        // Save into transaction_obligations
        if (request.transactionType() == TransactionType.LOAN) {
            List<TransactionObligationEntity> transactionObligationEntityList = request.loan().obligations().stream()
                    .map(obligation -> {
                        UserEntity obligationUser = userRepository.findById(obligation.userUuid())
                                .orElseThrow(() -> new EntityNotFoundException("User not found: " + obligation.userUuid()));

                        return TransactionObligationEntity.builder()
                                .uuid(UUID.randomUUID())
                                .transaction(savedTransaction)
                                .user(obligationUser)
                                .amount(obligation.amount())
                                .build();
                    })
                    .collect(Collectors.toList());

            List<TransactionObligationEntity> savedObligations = obligationRepository.saveAll(transactionObligationEntityList);

            return CreateTransactionResponseDTO.from(savedTransaction, savedObligations);
        } else {
            UserEntity recipient = userRepository.findById(request.repayment().recipientId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.repayment().recipientId()));

            TransactionObligationEntity savedObligation = obligationRepository.save(TransactionObligationEntity.from(savedTransaction, recipient));

            return CreateTransactionResponseDTO.from(savedTransaction, savedObligation);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CreateTransactionResponseDTO getTransactionDetail(UUID transactionId) {
        TransactionHistoryEntity transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
        TransactionType transactionType = transaction.getTransactionType();

        List<TransactionObligationEntity> transactionObligationEntityList = obligationRepository.findByTransactionId(transactionId);

        if (transactionType == TransactionType.LOAN) {
           return CreateTransactionResponseDTO.from(transaction, transactionObligationEntityList);
        } else {
            return CreateTransactionResponseDTO.from(transaction, transactionObligationEntityList.getFirst());
        }
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID transactionId) {
        // Delete Obligations first
        List<TransactionObligationEntity> transactionObligationEntityList = obligationRepository.findByTransactionId(transactionId);
        List<UUID> uuidList = transactionObligationEntityList.stream().map(TransactionObligationEntity::getUuid).toList();
        obligationRepository.deleteAllById(uuidList);

        transactionRepository.deleteById(transactionId);
    }
}
