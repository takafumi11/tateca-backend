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
        return RepaymentCreationResponse.from(savedRepayment);
    }

    @Transactional
    public void deleteRepayment(UUID repaymentId) {
        accessor.deleteById(repaymentId);
    }

}
