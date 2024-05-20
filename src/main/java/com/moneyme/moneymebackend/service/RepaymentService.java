package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.request.RepaymentCreationRequest;
import com.moneyme.moneymebackend.dto.request.RepaymentRequestDTO;
import com.moneyme.moneymebackend.dto.response.RepaymentCreationResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.GroupRepository;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import com.moneyme.moneymebackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Service
@RequiredArgsConstructor
public class RepaymentService {
    private final RepaymentRepository repository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RedisService redisService;

    @Transactional
    public RepaymentCreationResponse createRepayment(RepaymentCreationRequest request, UUID groupId) {
        UserEntity payer = findUserById(request.getRepaymentRequestDTO().getPayerId());
        UserEntity recipient = findUserById(request.getRepaymentRequestDTO().getRecipientId());
        GroupEntity group = findGroupById(groupId);

        RepaymentEntity savedRepayment = repository.save(RepaymentEntity.from(request, payer, recipient, group));
        updateBalancesInRedis(groupId, payer, recipient, savedRepayment.getAmount(), savedRepayment.getCurrencyRate(), 0, BigDecimal.ZERO);

        return RepaymentCreationResponse.from(savedRepayment);
    }

    public RepaymentCreationResponse getRepayment(UUID groupId, UUID repaymentId) {
        RepaymentEntity repayment = repository.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found with ID: " + repaymentId));
        return RepaymentCreationResponse.from(repayment);
    }

    @Transactional
    public RepaymentCreationResponse updateRepayment(UUID groupId, UUID repaymentId, RepaymentCreationRequest request) {
        RepaymentEntity repayment = repository.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found with ID: " + repaymentId));
        int prevAmount = repayment.getAmount();
        BigDecimal prevCurrencyRate = repayment.getCurrencyRate();

        updateRepaymentDetails(repayment, request.getRepaymentRequestDTO());
        RepaymentEntity savedRepayment = repository.save(repayment);

        updateBalancesInRedis(groupId, repayment.getPayer(), repayment.getRecipientUser(), savedRepayment.getAmount(), savedRepayment.getCurrencyRate(), prevAmount, prevCurrencyRate);

        return RepaymentCreationResponse.from(repayment);
    }

    @Transactional
    public void deleteRepayment(UUID groupId, UUID repaymentId) {
        RepaymentEntity repayment = repository.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found with ID: " + repaymentId));
        repository.delete(repayment);


        updateBalancesInRedis(groupId, repayment.getPayer(), repayment.getRecipientUser(), 0, BigDecimal.ZERO, repayment.getAmount(), repayment.getCurrencyRate());
    }

    private UserEntity findUserById(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    private GroupEntity findGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));
    }

    private void updateRepaymentDetails(RepaymentEntity repayment, RepaymentRequestDTO requestDTO) {
        repayment.setTitle(requestDTO.getTitle());
        repayment.setAmount(requestDTO.getAmount());
        repayment.setDate(convertToTokyoTime(requestDTO.getDate()));
    }

    private void updateBalancesInRedis(UUID groupId, UserEntity payer, UserEntity recipient, int newAmountInt, BigDecimal newCurrencyRate, int oldAmountInt, BigDecimal oldCurrencyRate) {
        Map<String, BigDecimal> balanceUpdates = new HashMap<>();

        BigDecimal oldAmount = calculateAmount(oldAmountInt, oldCurrencyRate);
        BigDecimal newAmount = calculateAmount(newAmountInt, newCurrencyRate);

        balanceUpdates.put(payer.getUuid().toString(), oldAmount.subtract(newAmount));
        balanceUpdates.put(recipient.getUuid().toString(), newAmount.subtract(oldAmount));
        redisService.updateBalances(groupId.toString(), balanceUpdates);
    }

    private BigDecimal calculateAmount(int amountInt, BigDecimal currencyRate) {
        if (amountInt == 0) {
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(amountInt).divide(currencyRate, 6, BigDecimal.ROUND_HALF_UP);
        }
    }

}
