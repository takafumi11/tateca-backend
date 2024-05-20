package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.GroupAccessor;
import com.moneyme.moneymebackend.accessor.RepaymentAccessor;
import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.dto.request.RepaymentCreationRequest;
import com.moneyme.moneymebackend.dto.request.RepaymentRequestDTO;
import com.moneyme.moneymebackend.dto.response.RepaymentCreationResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.moneyme.moneymebackend.service.util.AmountHelper.calculateAmount;
import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Service
@RequiredArgsConstructor
public class RepaymentService {
    private final RepaymentAccessor accessor;
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;
    private final RedisService redisService;

    @Transactional
    public RepaymentCreationResponse getRepayment(UUID repaymentId) {
        RepaymentEntity repayment = accessor.findById(repaymentId);
        return RepaymentCreationResponse.from(repayment);
    }

    @Transactional
    public RepaymentCreationResponse createRepayment(RepaymentCreationRequest request, UUID groupId) {
        RepaymentRequestDTO repaymentRequestDTO = request.getRepaymentRequestDTO();
        UserEntity payer = userAccessor.findById(UUID.fromString(repaymentRequestDTO.getPayerId()));
        UserEntity recipient = userAccessor.findById(UUID.fromString(request.getRepaymentRequestDTO().getRecipientId()));

        GroupEntity group = groupAccessor.findById(groupId);

        RepaymentEntity repaymentEntity = RepaymentEntity.builder()
                .uuid(UUID.randomUUID())
                .group(group)
                .title(repaymentRequestDTO.getTitle())
                .amount(repaymentRequestDTO.getAmount())
                .currencyCode(repaymentRequestDTO.getCurrencyCode())
                .currencyRate(repaymentRequestDTO.getCurrencyRate())
                .date(convertToTokyoTime(repaymentRequestDTO.getDate()))
                .payer(payer)
                .recipientUser(recipient)
                .build();

        RepaymentEntity savedRepayment = accessor.save(repaymentEntity);
        updateBalancesInRedis(groupId, payer, recipient, savedRepayment.getAmount(), savedRepayment.getCurrencyRate(), 0, BigDecimal.ZERO);

        return RepaymentCreationResponse.from(savedRepayment);
    }

    @Transactional
    public RepaymentCreationResponse updateRepayment(UUID groupId, UUID repaymentId, RepaymentCreationRequest request) {
        RepaymentEntity repayment = accessor.findById(repaymentId);

        int prevAmount = repayment.getAmount();
        BigDecimal prevCurrencyRate = repayment.getCurrencyRate();

        repayment.setTitle(request.getRepaymentRequestDTO().getTitle());
        repayment.setAmount(request.getRepaymentRequestDTO().getAmount());
        repayment.setDate(convertToTokyoTime(request.getRepaymentRequestDTO().getDate()));

        RepaymentEntity savedRepayment = accessor.save(repayment);

        updateBalancesInRedis(groupId, repayment.getPayer(), repayment.getRecipientUser(), savedRepayment.getAmount(), savedRepayment.getCurrencyRate(), prevAmount, prevCurrencyRate);

        return RepaymentCreationResponse.from(repayment);
    }

    @Transactional
    public void deleteRepayment(UUID groupId, UUID repaymentId) {
        RepaymentEntity repayment = accessor.findById(repaymentId);
        accessor.delete(repayment);

        updateBalancesInRedis(groupId, repayment.getPayer(), repayment.getRecipientUser(), 0, BigDecimal.ZERO, repayment.getAmount(), repayment.getCurrencyRate());
    }

    private void updateBalancesInRedis(UUID groupId, UserEntity payer, UserEntity recipient, int newAmountInt, BigDecimal newCurrencyRate, int oldAmountInt, BigDecimal oldCurrencyRate) {
        Map<String, BigDecimal> balanceUpdates = new HashMap<>();

        BigDecimal oldAmount = calculateAmount(oldAmountInt, oldCurrencyRate);
        BigDecimal newAmount = calculateAmount(newAmountInt, newCurrencyRate);

        balanceUpdates.put(payer.getUuid().toString(), oldAmount.subtract(newAmount));
        balanceUpdates.put(recipient.getUuid().toString(), newAmount.subtract(oldAmount));
        redisService.updateBalances(groupId.toString(), balanceUpdates);
    }

}
