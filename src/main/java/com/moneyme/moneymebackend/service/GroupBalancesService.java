package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.model.GroupTransactionsResponseModel;
import com.moneyme.moneymebackend.dto.response.GetGroupTransactionsResponse;
import com.moneyme.moneymebackend.entity.UserGroupEntity;
import com.moneyme.moneymebackend.repository.UserGroupRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupBalancesService {
    private final RedisService service;
    private final UserGroupRepository userGroupRepository;

    public GetGroupTransactionsResponse getGroupBalances(String groupId) {
        List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuid(UUID.fromString(groupId));
        List<String> userIds = userGroups.stream()
                .map(UserGroupEntity::getUserUuid)
                .map(UUID::toString)
                .toList();

        Map<String, BigDecimal> balances = service.getBalances(groupId, userIds);
        List<GroupTransactionsResponseModel> transactions = optimizeTransactions(balances);

        return GetGroupTransactionsResponse.builder()
                .transactions(transactions)
                .build();
    }

    private List<GroupTransactionsResponseModel> optimizeTransactions(Map<String, BigDecimal> balances) {
        List<GroupTransactionsResponseModel> transactions = new ArrayList<>();
        PriorityQueue<Participant> creditors = new PriorityQueue<>(Comparator.comparing(Participant::getAmount));
        PriorityQueue<Participant> debtors = new PriorityQueue<>(Comparator.comparing(Participant::getAmount));

        classifyParticipants(balances, creditors, debtors);
        processTransactions(creditors, debtors, transactions);

        return transactions;
    }

    private void classifyParticipants(Map<String, BigDecimal> balances, PriorityQueue<Participant> creditors, PriorityQueue<Participant> debtors) {
        balances.forEach((userId, amount) -> {
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                creditors.add(new Participant(userId, amount.negate()));
            } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new Participant(userId, amount));
            }
        });
    }

    private void processTransactions(PriorityQueue<Participant> creditors, PriorityQueue<Participant> debtors, List<GroupTransactionsResponseModel> transactions) {
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Participant debtor = debtors.poll();
            Participant creditor = creditors.poll();

            BigDecimal minAmount = debtor.getAmount().min(creditor.getAmount());
            transactions.add(new GroupTransactionsResponseModel(debtor.getUserId(), creditor.getUserId(), minAmount));

            updateBalances(debtor, creditor, minAmount, debtors, creditors);
        }
    }

    private void updateBalances(Participant debtor, Participant creditor, BigDecimal minAmount, PriorityQueue<Participant> debtors, PriorityQueue<Participant> creditors) {
        debtor.setAmount(debtor.getAmount().subtract(minAmount));
        creditor.setAmount(creditor.getAmount().subtract(minAmount));

        if (debtor.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            debtors.add(debtor);
        }
        if (creditor.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            creditors.add(creditor);
        }
    }

    @AllArgsConstructor
    @Data
    private static class Participant implements Comparable<Participant> {
        private String userId;
        private BigDecimal amount;

        @Override
        public int compareTo(Participant other) {
            return this.amount.compareTo(other.amount);
        }
    }
}
