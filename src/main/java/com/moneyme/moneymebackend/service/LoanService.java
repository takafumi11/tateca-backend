package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.model.LoanObligationInfo;
import com.moneyme.moneymebackend.dto.request.CreateLoanRequest;
import com.moneyme.moneymebackend.dto.response.CreateLoanResponse;
import com.moneyme.moneymebackend.dto.response.LoanResponse;
import com.moneyme.moneymebackend.dto.response.ObligationResponse;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.LoanObligationEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.LoanObligationRepository;
import com.moneyme.moneymebackend.repository.LoanRepository;
import com.moneyme.moneymebackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository repository;
    private final LoanObligationRepository loanObligationRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateLoanResponse createLoan(CreateLoanRequest request) {
        UUID userUuid = UUID.fromString(request.getLoanInfo().getPayerId());
        UserEntity user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        LoanEntity loan = LoanEntity.builder()
                .uuid(UUID.randomUUID())
                .title(request.getLoanInfo().getTitle())
                .amount(request.getLoanInfo().getAmount())
                .date(convertToTokyoTime(request.getLoanInfo().getDate()))
                .payer(user)
                .detail(request.getLoanInfo().getDetail())
                .build();

        LoanEntity savedLoan = repository.save(loan);

        List<LoanObligationEntity> obligationList = request.getObligationInfoList().stream()
            .map(obligationInfo -> buildObligationEntity(savedLoan, obligationInfo))
                .toList();

        List<LoanObligationEntity> savedObligations = loanObligationRepository.saveAll(obligationList);

        LoanResponse loanResponse = LoanResponse.from(savedLoan);
        List<ObligationResponse> obligationResponses = savedObligations.stream().map(ObligationResponse::from).toList();

        return CreateLoanResponse.builder()
                .loan(loanResponse)
                .obligations(obligationResponses)
                .build();
    }

    public LoanObligationEntity buildObligationEntity(LoanEntity loan, LoanObligationInfo obligationInfo) {
        UUID userUuid = UUID.fromString(obligationInfo.getUserUuid());
        UserEntity user = userRepository.findById(userUuid).orElseThrow(() -> new IllegalArgumentException("user not found"));

        return LoanObligationEntity.builder()
                .uuid(UUID.randomUUID())
                .loan(loan)
                .user(user)
                .amount(obligationInfo.getAmount())
                .build();
    }
}
