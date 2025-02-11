package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.RepaymentCreationRequest;
import com.tateca.tatecabackend.dto.request.RepaymentRequestDTO;
import com.tateca.tatecabackend.dto.response.RepaymentCreationResponse;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.TransactionEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepaymentService {
    private final TransactionAccessor accessor;
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;

    @Transactional
    public RepaymentCreationResponse getRepayment(UUID repaymentId) {
        TransactionEntity repayment = accessor.findById(repaymentId);
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

        TransactionEntity savedRepayment = accessor.save(TransactionEntity.from(request, payer, recipient, group));
        return RepaymentCreationResponse.from(savedRepayment);
    }

    @Transactional
    public void deleteRepayment(UUID repaymentId) {
        accessor.deleteById(repaymentId);
    }

}
