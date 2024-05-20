package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.request.LoanRequestDTO;
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
import java.util.stream.Collectors;

import static com.moneyme.moneymebackend.service.util.AmountHelper.calculateAmount;
import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

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
            BigDecimal amount = calculateAmount(obligation.getAmount(), request.getLoanRequestDTO().getCurrencyRate());
            balanceUpdates.put(obligation.getUser().getUuid().toString(), amount);
            totalAmount = totalAmount.add(amount);
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

    public LoanCreationResponse updateLoan(UUID groupId, UUID loanId, LoanCreationRequest request) {
        LoanEntity loan = repository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        LoanRequestDTO loanRequestDTO = request.getLoanRequestDTO();
        loan.setTitle(loanRequestDTO.getTitle());
        loan.setAmount(loanRequestDTO.getAmount());
        loan.setDate(convertToTokyoTime(loanRequestDTO.getDate()));

        LoanEntity savedLoan = repository.save(loan);

        // Fetch previous obligations to calculate balance updates correctly
        List<ObligationEntity> existingObligations = obligationRepository.findByLoanId(loanId);

        Map<UUID, BigDecimal> previousAmounts = existingObligations.stream()
                .collect(Collectors.toMap(obligation -> obligation.getUser().getUuid(), ObligationEntity::getAmount));

        List<ObligationEntity> updatedObligations = existingObligations.stream().map(existingObligation -> {
            ObligationRequestDTO matchingRequestDTO = request.getObligationRequestDTOS().stream()
                    .filter(reqDTO -> reqDTO.getUserUuid().equals(existingObligation.getUser().getUuid().toString()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Matching ObligationRequestDTO not found for existing obligation"));

            existingObligation.setAmount(matchingRequestDTO.getAmount());
            return existingObligation;
        }).collect(Collectors.toList());

        List<ObligationEntity> savedObligations = obligationRepository.saveAll(updatedObligations);

        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal prevTotalAmount = BigDecimal.ZERO;

        for (ObligationEntity obligation : savedObligations) {
            BigDecimal prevAmount = previousAmounts.getOrDefault(obligation.getUser().getUuid(), BigDecimal.ZERO);
            balanceUpdates.put(obligation.getUser().getUuid().toString(), prevAmount.negate().add(obligation.getAmount()));
            totalAmount = totalAmount.add(obligation.getAmount());
            prevTotalAmount = prevTotalAmount.add(prevAmount);
        }

        // Adjust the payer's balance considering the previous loan amount
        balanceUpdates.put(savedLoan.getPayer().getUuid().toString(), prevTotalAmount.add(totalAmount.negate()));

        redisService.updateBalances(groupId.toString(), balanceUpdates);

        LoanResponseDTO loanResponse = LoanResponseDTO.from(savedLoan);
        List<ObligationResponseDTO> obligationResponseDTOS = savedObligations.stream().map(ObligationResponseDTO::from).toList();

        return LoanCreationResponse.builder()
                .loanResponseDTO(loanResponse)
                .obligationResponseDTOS(obligationResponseDTOS)
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
            BigDecimal amount = calculateAmount(obligation.getAmount(), loan.getCurrencyRate());
            balanceUpdates.put(obligation.getUser().getUuid().toString(), amount.negate());
            totalAmount = totalAmount.add(amount);
        }

        // Subtract the total amount from the payer's balance
        balanceUpdates.put(loan.getPayer().getUuid().toString(), totalAmount);

        redisService.updateBalances(groupId.toString(), balanceUpdates);
    }
}
