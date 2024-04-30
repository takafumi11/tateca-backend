package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.model.LoanResponseModel;
import com.moneyme.moneymebackend.dto.model.ObligationRequestModel;
import com.moneyme.moneymebackend.dto.model.ObligationResponseModel;
import com.moneyme.moneymebackend.dto.request.CreateLoanRequest;
import com.moneyme.moneymebackend.dto.response.CreateLoanResponse;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.ObligationRepository;
import com.moneyme.moneymebackend.repository.LoanRepository;
import com.moneyme.moneymebackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository repository;
    private final ObligationRepository obligationRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    @Transactional
    public CreateLoanResponse createLoan(CreateLoanRequest request) {
        UUID userUuid = UUID.fromString(request.getLoanRequestModel().getPayerId());
        UserEntity user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        LoanEntity savedLoan = repository.save(LoanEntity.from(request, user));

        List<ObligationEntity> obligationList = request.getObligationRequestModels().stream().map(obligationRequestModel ->
                buildObligationEntity(savedLoan, obligationRequestModel)
        ).toList();

        List<ObligationEntity> savedObligations = obligationRepository.saveAll(obligationList);

        savedObligations.stream().forEach(obligation ->
                redisService.updateBalances(savedLoan.getPayer().getUuid().toString(), obligation.getUser().getUuid().toString(), obligation.getAmount(), request.getGroupId())
        );

        LoanResponseModel loanResponse = LoanResponseModel.from(savedLoan);
        List<ObligationResponseModel> obligationResponsModels = savedObligations.stream().map(ObligationResponseModel::from).toList();

        return CreateLoanResponse.builder()
                .loanResponseModel(loanResponse)
                .obligationResponseModels(obligationResponsModels)
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
