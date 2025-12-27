package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.TransactionCreationRequestDTO;
import com.tateca.tatecabackend.dto.response.TransactionDetailResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.UserEntity;
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
        List<TransactionObligationEntity> obligations = obligationAccessor.findByGroupId(groupId);

        Map<String, BigDecimal> balances = getUserBalances(obligations);
        Map<String, UserInfoDTO> userMap = extractUserMap(obligations);

        List<TransactionSettlementResponseDTO> transactions = optimizeTransactions(balances, userMap);

        return new TransactionsSettlementResponseDTO(transactions);
    }

    /**
     * Extract UserInfoDTO map from obligations (avoid additional DB query).
     */
    private Map<String, UserInfoDTO> extractUserMap(List<TransactionObligationEntity> obligations) {
        Map<String, UserInfoDTO> userMap = new HashMap<>();

        for (TransactionObligationEntity obligation : obligations) {
            String userId = obligation.getUser().getUuid().toString();
            if (!userMap.containsKey(userId)) {
                userMap.put(userId, UserInfoDTO.from(obligation.getUser()));
            }

            String payerId = obligation.getTransaction().getPayer().getUuid().toString();
            if (!userMap.containsKey(payerId)) {
                userMap.put(payerId, UserInfoDTO.from(obligation.getTransaction().getPayer()));
            }
        }

        return userMap;
    }

    private Map<String, BigDecimal> getUserBalances(List<TransactionObligationEntity> obligations) {
        Map<String, BigDecimal> balances = new HashMap<>();

        // Group obligations by transaction UUID
        Map<UUID, List<TransactionObligationEntity>> obligationsByTransaction =
                obligations.stream()
                        .collect(Collectors.groupingBy(o -> o.getTransaction().getUuid()));

        // Process each transaction (adjust rounding errors)
        for (List<TransactionObligationEntity> transactionObligations : obligationsByTransaction.values()) {
            if (transactionObligations.isEmpty()) continue;

            TransactionHistoryEntity transaction = transactionObligations.getFirst().getTransaction();
            BigDecimal transactionRate = transaction.getExchangeRate().getExchangeRate();

            // Calculate payer's total payment in JPY
            String payerId = transaction.getPayer().getUuid().toString();
            BigDecimal totalAmountInJpy = BigDecimal.valueOf(transaction.getAmount())
                    .divide(transactionRate, 7, RoundingMode.HALF_UP);

            balances.merge(payerId, totalAmountInJpy.negate(), BigDecimal::add);

            // Calculate individual debtor amounts and find maximum debtor
            BigDecimal individualSum = BigDecimal.ZERO;
            String maxDebtorId = null;
            BigDecimal maxDebtorAmount = BigDecimal.ZERO;

            for (TransactionObligationEntity obligation : transactionObligations) {
                String debtorId = obligation.getUser().getUuid().toString();
                BigDecimal amountInJpy = BigDecimal.valueOf(obligation.getAmount())
                        .divide(transactionRate, 7, RoundingMode.HALF_UP);

                balances.merge(debtorId, amountInJpy, BigDecimal::add);
                individualSum = individualSum.add(amountInJpy);

                // Track maximum debtor
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

        return balances;
    }

    private List<TransactionSettlementResponseDTO> optimizeTransactions(Map<String, BigDecimal> balances, Map<String, UserInfoDTO> userMap) {
        PriorityQueue<ParticipantModel> creditors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));
        PriorityQueue<ParticipantModel> debtors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));

        classifyParticipants(balances, creditors, debtors, userMap);

        List<TransactionSettlementResponseDTO> transactions = new ArrayList<>();

        processTransactions(creditors, debtors, transactions);

        return transactions;
    }

    private void classifyParticipants(Map<String, BigDecimal> balances, PriorityQueue<ParticipantModel> creditors, PriorityQueue<ParticipantModel> debtors, Map<String, UserInfoDTO> userMap) {
        balances.forEach((userId, amount) -> {
            UserInfoDTO user = userMap.get(userId);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                creditors.add(new ParticipantModel(user, amount.negate()));
            } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new ParticipantModel(user, amount));
            }
        });
    }

    private void processTransactions(PriorityQueue<ParticipantModel> creditors, PriorityQueue<ParticipantModel> debtors, List<TransactionSettlementResponseDTO> transactions) {
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            ParticipantModel debtor = debtors.poll();
            ParticipantModel creditor = creditors.poll();

            assert creditor != null;
            BigDecimal minAmount = debtor.getAmount().min(creditor.getAmount());
            if (minAmount.intValue() != 0) {
                transactions.add(new TransactionSettlementResponseDTO(debtor.getUserId(), creditor.getUserId(), minAmount.intValue()));
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
