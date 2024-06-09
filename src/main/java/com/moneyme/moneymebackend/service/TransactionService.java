package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.ObligationAccessor;
import com.moneyme.moneymebackend.accessor.RepaymentAccessor;
import com.moneyme.moneymebackend.dto.response.TransactionSettlementResponseDTO;
import com.moneyme.moneymebackend.dto.response.TransactionHistoryResponseDTO;
import com.moneyme.moneymebackend.dto.response.TransactionsSettlementResponse;
import com.moneyme.moneymebackend.dto.response.TransactionsHistoryResponse;
import com.moneyme.moneymebackend.dto.response.UserResponseDTO;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.entity.UserGroupEntity;
import com.moneyme.moneymebackend.model.ParticipantModel;
import com.moneyme.moneymebackend.repository.LoanRepository;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import com.moneyme.moneymebackend.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final RedisService service;

    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final UserGroupRepository userGroupRepository;

    private final ObligationAccessor obligationAccessor;
    private final RepaymentAccessor repaymentAccessor;

    public TransactionsHistoryResponse getTransactions(int count, UUID groupId) {
        List<LoanEntity> loans = loanRepository.getLoansByGroup(groupId, PageRequest.of(0, count));
        List<RepaymentEntity> repayments = repaymentRepository.getRepaymentsByGroup(groupId, PageRequest.of(0, count));

        List<TransactionHistoryResponseDTO> transactionsResponses = loans.stream().map(TransactionHistoryResponseDTO::from).toList();
        List<TransactionHistoryResponseDTO> transactionsResponses2 = repayments.stream().map(TransactionHistoryResponseDTO::from).toList();

        List<TransactionHistoryResponseDTO> combinedResponses = Stream.concat(transactionsResponses.stream(), transactionsResponses2.stream())
                .toList()
                .stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(count)
                .toList();

        return TransactionsHistoryResponse.builder()
                .transactionsHistory(combinedResponses)
                .build();
    }

    public TransactionsSettlementResponse getSettlements(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuid(groupId);
        List<String> userIds = userGroups.stream()
                .map(UserGroupEntity::getUserUuid)
                .map(UUID::toString)
                .toList();

        List<ObligationEntity> obligationEntityList = obligationAccessor.findByGroupId(groupId);
        List<RepaymentEntity> repaymentEntityList = repaymentAccessor.findByIdGroupId(groupId);

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

            for (RepaymentEntity repayment : repaymentEntityList) {
                UUID payerId = repayment.getPayer().getUuid();
                UUID recipientId = repayment.getRecipientUser().getUuid();
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
