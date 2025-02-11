package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserGroupAccessor;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponse;
import com.tateca.tatecabackend.dto.response.TransactionsHistoryResponse;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.ObligationEntity;
import com.tateca.tatecabackend.entity.TransactionEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.model.ParticipantModel;
import com.tateca.tatecabackend.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final UserGroupAccessor userGroupAccessor;
    private final TransactionAccessor accessor;
    private final ObligationAccessor obligationAccessor;

    public TransactionsHistoryResponse getTransactions(int count, UUID groupId) {
        List<TransactionEntity> transactionEntityList = accessor.findTransactionHistoryList(groupId, count);

        return TransactionsHistoryResponse.buildResponse(transactionEntityList);
    }

    public TransactionsSettlementResponse getSettlements(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuid(groupId);
        List<String> userIds = userGroups.stream()
                .map(UserGroupEntity::getUserUuid)
                .map(UUID::toString)
                .toList();

        List<ObligationEntity> obligationEntityList = obligationAccessor.findByGroupId(groupId);
        List<TransactionEntity> repaymentEntityList = accessor.findByGroup(groupId, TransactionType.REPAYMENT);

        Map<String, BigDecimal> balances = new HashMap<>();

        for (String userId : userIds) {
            BigDecimal balance = BigDecimal.ZERO;

            for (ObligationEntity obligation : obligationEntityList) {
                UUID obligationUserUuid = obligation.getUser().getUuid();
                UUID loanPayerUuid = obligation.getLoan().getPayer().getUuid();
                BigDecimal obligationAmount = BigDecimal.valueOf(obligation.getAmount()).multiply(obligation.getLoan().getCurrencyRate());

                if (obligationUserUuid.toString().equals(userId)) {
                    balance = balance.add(obligationAmount);
                }

                if (loanPayerUuid.toString().equals(userId)) {
                    balance = balance.subtract(obligationAmount);
                }
            }

            for (TransactionEntity repayment : repaymentEntityList) {
                UUID payerId = repayment.getPayer().getUuid();
                UUID recipientId = repayment.getRecipient().getUuid();
                BigDecimal repaymentAmount = BigDecimal.valueOf(repayment.getAmount()).multiply(repayment.getCurrencyRate());

                if (recipientId.toString().equals(userId)) {
                    balance = balance.add(repaymentAmount);
                }

                if (payerId.toString().equals(userId)) {
                    balance = balance.subtract(repaymentAmount);
                }
            }

            balances.put(userId, balance);
        }

        List<TransactionSettlementResponseDTO> transactions = optimizeTransactions(balances, userGroups);

        return TransactionsSettlementResponse.builder()
                .transactionsSettlement(transactions)
                .build();
    }
    private List<TransactionSettlementResponseDTO> optimizeTransactions(Map<String, BigDecimal> balances, List<UserGroupEntity> userGroups) {
        List<TransactionSettlementResponseDTO> transactions = new ArrayList<>();
        PriorityQueue<ParticipantModel> creditors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));
        PriorityQueue<ParticipantModel> debtors = new PriorityQueue<>(Comparator.comparing(ParticipantModel::getAmount));

        classifyParticipants(balances, creditors, debtors, userGroups);
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
            transactions.add(new TransactionSettlementResponseDTO(debtor.getUserId(), creditor.getUserId(), minAmount.intValue()));

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

}
