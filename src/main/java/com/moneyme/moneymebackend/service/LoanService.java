package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.response.LoanResponseDTO;
import com.moneyme.moneymebackend.dto.request.ObligationRequestDTO;
import com.moneyme.moneymebackend.dto.response.ObligationResponseDTO;
import com.moneyme.moneymebackend.dto.request.LoanCreationRequest;
import com.moneyme.moneymebackend.dto.response.LoanCreationResponse;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public LoanCreationResponse createLoan(LoanCreationRequest request, UUID groupId) {
        UUID userUuid = UUID.fromString(request.getLoanRequestDTO().getPayerId());
        UserEntity user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("group not found"));

        LoanEntity savedLoan = repository.save(LoanEntity.from(request, user, group));

        List<ObligationEntity> obligationList = request.getObligationRequestDTOS().stream().map(obligationRequestDTO ->
                buildObligationEntity(savedLoan, obligationRequestDTO)
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

        redisService.updateBalances(groupId.toString(), balanceUpdates);

        LoanResponseDTO loanResponse = LoanResponseDTO.from(savedLoan);
        List<ObligationResponseDTO> obligationResponseDTOS = savedObligations.stream().map(ObligationResponseDTO::from).toList();

        return LoanCreationResponse.builder()
                .loanResponseDTO(loanResponse)
                .obligationResponseDTOS(obligationResponseDTOS)
                .build();
    }

    public ObligationEntity buildObligationEntity(LoanEntity loan, ObligationRequestDTO obligationRequestDTO) {
        UUID userUuid = UUID.fromString(obligationRequestDTO.getUserUuid());
        UserEntity user = userRepository.findById(userUuid).orElseThrow(() -> new IllegalArgumentException("user not found"));

        return ObligationEntity.builder()
                .uuid(UUID.randomUUID())
                .loan(loan)
                .user(user)
                .amount(obligationRequestDTO.getAmount())
                .build();
    }

    public LoanCreationResponse getLoan(UUID groupId, UUID loanId) {
        LoanEntity loan = repository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        List<ObligationEntity> obligations = obligationRepository.findByLoanId(loan.getUuid());

        List<ObligationResponseDTO> obligationResponseDTOList = obligations.stream().map(ObligationResponseDTO::from).toList();

        return LoanCreationResponse.builder()
                .loanResponseDTO(LoanResponseDTO.from(loan))
                .obligationResponseDTOS(obligationResponseDTOList)
                .build();
    }

    public void deleteLoan(UUID groupId, UUID loanId) {
        LoanEntity loan = repository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        List<ObligationEntity> obligations = obligationRepository.findByLoanId(loan.getUuid());

        obligationRepository.deleteAll(obligations);
        repository.delete(loan);

        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ObligationEntity obligation : obligations) {
            balanceUpdates.put(obligation.getUser().getUuid().toString(), obligation.getAmount().negate());
            totalAmount = totalAmount.add(obligation.getAmount());
        }

        // Subtract the total amount from the payer's balance
        balanceUpdates.put(loan.getPayer().getUuid().toString(), totalAmount);

        redisService.updateBalances(groupId.toString(), balanceUpdates);


    }
}
