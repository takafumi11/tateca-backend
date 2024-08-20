package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.GroupAccessor;
import com.moneyme.moneymebackend.accessor.LoanAccessor;
import com.moneyme.moneymebackend.accessor.ObligationAccessor;
import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.dto.request.LoanRequestDTO;
import com.moneyme.moneymebackend.dto.request.ObligationRequestDTO;
import com.moneyme.moneymebackend.dto.request.LoanCreationRequest;
import com.moneyme.moneymebackend.dto.response.LoanCreationResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.moneyme.moneymebackend.service.util.AmountHelper.calculateAmount;
import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanAccessor accessor;
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;
    private final ObligationAccessor obligationAccessor;
    private final RedisService redisService;

    @Transactional
    public LoanCreationResponse getLoan(UUID loanId) {
        LoanEntity loan = accessor.findById(loanId);
        List<ObligationEntity> obligations = obligationAccessor.findByLoanId(loan.getUuid());

        return LoanCreationResponse.buildResponse(loan, obligations);
    }

    @Transactional
    public LoanCreationResponse createLoan(LoanCreationRequest request, UUID groupId) {
        UUID userId = UUID.fromString(request.getLoanRequestDTO().getPayerId());
        UserEntity user = userAccessor.findById(userId);
        GroupEntity group = groupAccessor.findById(groupId);

        LoanEntity savedLoan = accessor.save(LoanEntity.from(request, user, group));

        List<ObligationEntity> obligationEntityList = request.getObligationRequestDTOs().stream()
                .map(obligation -> {
                    UUID obligationUserId = UUID.fromString(obligation.getUserUuid());
                    UserEntity obligationUser = userAccessor.findById(obligationUserId);

                    return ObligationEntity.builder()
                            .uuid(UUID.randomUUID())
                            .loan(savedLoan)
                            .user(obligationUser)
                            .amount(obligation.getAmount())
                            .build();
                })
                .collect(Collectors.toList());

        List<ObligationEntity> savedObligations = obligationAccessor.saveAll(obligationEntityList);

        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ObligationEntity obligation : savedObligations) {
            BigDecimal amount = calculateAmount(obligation.getAmount(), request.getLoanRequestDTO().getCurrencyRate());
            balanceUpdates.put(obligation.getUser().getUuid().toString(), amount);
            totalAmount = totalAmount.add(amount);
        }

        // Subtract the total amount from the payer's balance
        balanceUpdates.put(user.getUuid().toString(), totalAmount.negate());

//        redisService.updateBalances(groupId.toString(), balanceUpdates);

        return LoanCreationResponse.buildResponse(savedLoan, savedObligations);
    }

    @Transactional
    public LoanCreationResponse updateLoan(UUID groupId, UUID loanId, LoanCreationRequest request) {
        LoanEntity loan = accessor.findById(loanId);

        LoanRequestDTO loanRequestDTO = request.getLoanRequestDTO();
        loan.setTitle(loanRequestDTO.getTitle());
        loan.setAmount(loanRequestDTO.getAmount());
        loan.setDate(convertToTokyoTime(loanRequestDTO.getDate()));

        LoanEntity savedLoan = accessor.save(loan);

        // Fetch previous obligations to calculate balance updates correctly
        List<ObligationEntity> existingObligations = obligationAccessor.findByLoanId(loanId);

        Map<UUID, Integer> previousAmounts = existingObligations.stream()
                .collect(Collectors.toMap(obligation -> obligation.getUser().getUuid(), ObligationEntity::getAmount));

        Map<UUID, BigDecimal> previousRates = existingObligations.stream()
                .collect(Collectors.toMap(
                        obligation -> obligation.getUser().getUuid(),
                        obligation -> obligation.getLoan().getCurrencyRate()
                ));

        List<ObligationEntity> updatedObligations = existingObligations.stream().map(existingObligation -> {
            ObligationRequestDTO matchingRequestDTO = request.getObligationRequestDTOs().stream()
                    .filter(reqDTO -> reqDTO.getUserUuid().equals(existingObligation.getUser().getUuid().toString()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Matching ObligationRequestDTO not found for existing obligation"));

            existingObligation.setAmount(matchingRequestDTO.getAmount());
            return existingObligation;
        }).collect(toList());

        List<ObligationEntity> savedObligations = obligationAccessor.saveAll(updatedObligations);

        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal prevTotalAmount = BigDecimal.ZERO;

        for (ObligationEntity obligation : savedObligations) {
            UUID uuid = obligation.getUser().getUuid();
            int prevAmountTmp = previousAmounts.getOrDefault(uuid, 0);
            BigDecimal prevRateTmp = previousRates.getOrDefault(uuid, BigDecimal.ZERO);

            BigDecimal prevAmount = calculateAmount(prevAmountTmp, prevRateTmp);
            BigDecimal amount = calculateAmount(obligation.getAmount(), request.getLoanRequestDTO().getCurrencyRate());

            balanceUpdates.put(uuid.toString(), prevAmount.negate().add(amount));
            totalAmount = totalAmount.add(amount);
            prevTotalAmount = prevTotalAmount.add(prevAmount);
        }

        // Adjust the payer's balance considering the previous loan amount
        balanceUpdates.put(savedLoan.getPayer().getUuid().toString(), prevTotalAmount.add(totalAmount.negate()));

//        redisService.updateBalances(groupId.toString(), balanceUpdates);

        return LoanCreationResponse.buildResponse(savedLoan, savedObligations);
    }

    @Transactional
    public void deleteLoan(UUID loanId) {
        // Delete Obligations
        List<ObligationEntity> obligationEntityList = obligationAccessor.findByLoanId(loanId);
        List<UUID> uuidList = obligationEntityList.stream().map(ObligationEntity::getUuid).toList();
        obligationAccessor.deleteAllById(uuidList);

        // Delete Loan
        accessor.deleteById(loanId);
    }

}
