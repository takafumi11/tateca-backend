package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.model.LoanResponseModel;
import com.moneyme.moneymebackend.dto.model.ObligationRequestModel;
import com.moneyme.moneymebackend.dto.model.ObligationResponseModel;
import com.moneyme.moneymebackend.dto.request.CreateLoanRequest;
import com.moneyme.moneymebackend.dto.response.CreateLoanResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.GroupRepository;
import com.moneyme.moneymebackend.repository.ObligationRepository;
import com.moneyme.moneymebackend.repository.LoanRepository;
import com.moneyme.moneymebackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository repository;
    private final ObligationRepository obligationRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RedisService redisService;

    @Transactional
    public CreateLoanResponse createLoan(CreateLoanRequest request) {
        UUID userUuid = UUID.fromString(request.getLoanRequestModel().getPayerId());
        UserEntity user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        UUID groupUuid = UUID.fromString(request.getGroupId());
        GroupEntity group = groupRepository.findById(groupUuid)
                .orElseThrow(() -> new IllegalArgumentException("group not found"));

        LoanEntity savedLoan = repository.save(LoanEntity.from(request, user, group));

        List<ObligationEntity> obligationList = request.getObligationRequestModels().stream().map(obligationRequestModel ->
                buildObligationEntity(savedLoan, obligationRequestModel)
        ).toList();

        List<ObligationEntity> savedObligations = obligationRepository.saveAll(obligationList);

        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ObligationEntity obligation : savedObligations) {
            balanceUpdates.put(obligation.getUser().getUuid().toString(), obligation.getAmount());
            totalAmount = totalAmount.add(obligation.getAmount());
        }

        // Subtract the total amount from the payer's balance
        balanceUpdates.put(user.getUuid().toString(), totalAmount.negate());

        redisService.updateBalances(request.getGroupId(), balanceUpdates);

        LoanResponseModel loanResponse = LoanResponseModel.from(savedLoan);
        List<ObligationResponseModel> obligationResponseModels = savedObligations.stream().map(ObligationResponseModel::from).toList();

        return CreateLoanResponse.builder()
                .loanResponseModel(loanResponse)
                .obligationResponseModels(obligationResponseModels)
                .build();
    }

    public ObligationEntity buildObligationEntity(LoanEntity loan, ObligationRequestModel obligationRequestModel) {
        UUID userUuid = UUID.fromString(obligationRequestModel.getUserUuid());
        UserEntity user = userRepository.findById(userUuid).orElseThrow(() -> new IllegalArgumentException("user not found"));

        return ObligationEntity.builder()
                .uuid(UUID.randomUUID())
                .loan(loan)
                .user(user)
                .amount(obligationRequestModel.getAmount())
                .build();
    }
}
