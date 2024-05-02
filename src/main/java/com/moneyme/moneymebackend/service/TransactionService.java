package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.model.TransactionResponseModel;
import com.moneyme.moneymebackend.dto.response.GetTransactionsResponse;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.repository.LoanRepository;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;

    public GetTransactionsResponse getTransactions(int count, String groupId) {
        UUID groupUuid = UUID.fromString(groupId);

        List<LoanEntity> loans = loanRepository.getLoansByGroup(groupUuid, PageRequest.of(0, count));
        List<RepaymentEntity> repayments = repaymentRepository.getRepaymentsByGroup(groupUuid, PageRequest.of(0, count));

        List<TransactionResponseModel> transactionsResponses = loans.stream().map(TransactionResponseModel::from).toList();
        List<TransactionResponseModel> transactionsResponses2 = repayments.stream().map(TransactionResponseModel::from).toList();

        List<TransactionResponseModel> combinedResponses = Stream.concat(transactionsResponses.stream(), transactionsResponses2.stream())
                .toList()
                .stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(count)
                .toList();

        return GetTransactionsResponse.builder()
                .transactions(combinedResponses)
                .build();
    }

}
