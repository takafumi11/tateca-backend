package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.accessor.UserGroupAccessor;
import com.tateca.tatecabackend.dto.request.TransactionCreationRequestDTO;
import com.tateca.tatecabackend.dto.response.TransactionDetailResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponse;
import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponse;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.model.ParticipantModel;
import com.tateca.tatecabackend.model.TransactionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
public class TransactionService {
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;
    private final UserGroupAccessor userGroupAccessor;
    private final TransactionAccessor accessor;
    private final ObligationAccessor obligationAccessor;
    private final ExchangeRateAccessor exchangeRateAccessor;

    public TransactionsHistoryResponse getTransactionHistory(int count, UUID groupId) {
        List<TransactionHistoryEntity> transactionHistoryEntityList = accessor.findTransactionHistory(groupId, count);

        return TransactionsHistoryResponse.buildResponse(transactionHistoryEntityList);
    }

    public TransactionsSettlementResponse getSettlements(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuid(groupId);
        List<String> userIds = userGroups.stream()
                .map(UserGroupEntity::getUserUuid)
                .map(UUID::toString)
                .toList();

        List<TransactionObligationEntity> transactionObligationEntityList = obligationAccessor.findByGroupId(groupId);

        Map<String, BigDecimal> balances = getUserBalances(userIds, transactionObligationEntityList);

        List<TransactionSettlementResponseDTO> transactions = optimizeTransactions(balances, userGroups);

        return TransactionsSettlementResponse.builder()
                .transactionsSettlement(transactions)
                .build();
    }

    private Map<String, BigDecimal> getUserBalances(List<String> userIds, List<TransactionObligationEntity> transactionObligationEntityList) {
        Map<String, BigDecimal> balances = new HashMap<>();

        for (String userId : userIds) {
            BigDecimal balance = BigDecimal.ZERO;

            for (TransactionObligationEntity obligation : transactionObligationEntityList) {
                String obligationUserId = obligation.getUser().getUuid().toString();
                String payerId = obligation.getTransaction().getPayer().getUuid().toString();
                BigDecimal obligationAmount = BigDecimal.valueOf(obligation.getAmount())
                        .divide(obligation.getTransaction().getExchangeRate().getExchangeRate(), 7, RoundingMode.HALF_UP);

                if (obligationUserId.equals(userId)) {
                    balance = balance.add(obligationAmount);
                }

                if (payerId.equals(userId)) {
                    balance = balance.subtract(obligationAmount);
                }
            }

            balances.put(userId, balance);
        }

        return balances;
    }

    private List<TransactionSettlementResponseDTO> optimizeTransactions(Map<String, BigDecimal> balances, List<UserGroupEntity> userGroups) {
        PriorityQueue<ParticipantModel> creditors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));
        PriorityQueue<ParticipantModel> debtors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));

        classifyParticipants(balances, creditors, debtors, userGroups);

        List<TransactionSettlementResponseDTO> transactions = new ArrayList<>();

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

    private void processTransactions(PriorityQueue<ParticipantModel> creditors, PriorityQueue<ParticipantModel> debtors, List<TransactionSettlementResponseDTO> transactions) {
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            ParticipantModel debtor = debtors.poll();
            ParticipantModel creditor = creditors.poll();

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
                If the client sends data with a future date that isn’t present in the database, we temporarily substitute it with the most recent available value.
                However, since the scheduler updates the value the day before, if a repayment occurs before that update, the value may become inaccurate.
                */
                ExchangeRateEntity exchangeRateEntity = exchangeRateAccessor.findByCurrencyCodeAndDate(request.getCurrencyCode(), LocalDate.now(UTC_ZONE_ID));
                exchangeRateEntity.setDate(date);
                exchangeRate = exchangeRateAccessor.save(exchangeRateEntity);
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

    @Transactional
    public void deleteTransaction(UUID transactionId) {
        // Delete Obligations first
        List<TransactionObligationEntity> transactionObligationEntityList = obligationAccessor.findByTransactionId(transactionId);
        List<UUID> uuidList = transactionObligationEntityList.stream().map(TransactionObligationEntity::getUuid).toList();
        obligationAccessor.deleteAllById(uuidList);

        accessor.deleteById(transactionId);
    }
}
