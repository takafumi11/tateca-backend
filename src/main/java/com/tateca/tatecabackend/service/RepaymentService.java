package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.RepaymentAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.RepaymentCreationRequest;
import com.tateca.tatecabackend.dto.request.RepaymentRequestDTO;
import com.tateca.tatecabackend.dto.response.RepaymentCreationResponse;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.RepaymentEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.util.AmountHelper;
import com.tateca.tatecabackend.service.util.TimeHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

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
        String payerId = repaymentRequestDTO.getPayerId();
        String recipientId = repaymentRequestDTO.getRecipientId();

        UserEntity payer = userAccessor.findById(UUID.fromString(payerId));
        UserEntity recipient = userAccessor.findById(UUID.fromString(recipientId));
        GroupEntity group = groupAccessor.findById(groupId);

        RepaymentEntity repaymentEntity = RepaymentEntity.builder()
                .uuid(UUID.randomUUID())
                .group(group)
                .title(repaymentRequestDTO.getTitle())
                .amount(repaymentRequestDTO.getAmount())
                .currencyCode(repaymentRequestDTO.getCurrencyCode())
                .currencyRate(repaymentRequestDTO.getCurrencyRate())
                .date(TimeHelper.convertToTokyoTime(repaymentRequestDTO.getDate()))
                .payer(payer)
                .recipientUser(recipient)
                .build();

        RepaymentEntity savedRepayment = accessor.save(repaymentEntity);
//        updateBalancesInRedis(groupId.toString(), payerId, recipientId, savedRepayment.getAmount(), savedRepayment.getCurrencyRate(), 0, BigDecimal.ZERO);

        return RepaymentCreationResponse.from(savedRepayment);
    }

    @Transactional
    public RepaymentCreationResponse updateRepayment(UUID groupId, UUID repaymentId, RepaymentCreationRequest request) {
        RepaymentEntity repayment = accessor.findById(repaymentId);

        int prevAmount = repayment.getAmount();
        BigDecimal prevCurrencyRate = repayment.getCurrencyRate();

        repayment.setTitle(request.getRepaymentRequestDTO().getTitle());
        repayment.setAmount(request.getRepaymentRequestDTO().getAmount());
        repayment.setDate(TimeHelper.convertToTokyoTime(request.getRepaymentRequestDTO().getDate()));

        RepaymentEntity savedRepayment = accessor.save(repayment);

//        updateBalancesInRedis(groupId.toString(), savedRepayment.getPayer().getUuid().toString(), savedRepayment.getRecipientUser().getUuid().toString(), savedRepayment.getAmount(), savedRepayment.getCurrencyRate(), prevAmount, prevCurrencyRate);

        return RepaymentCreationResponse.from(repayment);
    }

    @Transactional
    public void deleteRepayment(UUID repaymentId) {
        accessor.deleteById(repaymentId);
    }

    private void updateBalancesInRedis(String groupId, String payerId, String recipientId, int newAmountInt, BigDecimal newCurrencyRate, int oldAmountInt, BigDecimal oldCurrencyRate) {
        Map<String, BigDecimal> balanceUpdates = new HashMap<>();

        BigDecimal oldAmount = AmountHelper.calculateAmount(oldAmountInt, oldCurrencyRate);
        BigDecimal newAmount = AmountHelper.calculateAmount(newAmountInt, newCurrencyRate);

        balanceUpdates.put(payerId, oldAmount.subtract(newAmount));
        balanceUpdates.put(recipientId, newAmount.subtract(oldAmount));
//        redisService.updateBalances(groupId, balanceUpdates);
    }

}
