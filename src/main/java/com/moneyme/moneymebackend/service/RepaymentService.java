package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.request.CreateRepaymentRequest;
import com.moneyme.moneymebackend.dto.response.CreateRepaymentResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.GroupRepository;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import com.moneyme.moneymebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepaymentService {
    private final RepaymentRepository repository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RedisService redisService;

    public CreateRepaymentResponse createRepayment(CreateRepaymentRequest request, UUID groupId) {
        UserEntity payer = userRepository.findById(UUID.fromString(request.getRepaymentRequestModel().getPayerId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserEntity recipient = userRepository.findById(UUID.fromString(request.getRepaymentRequestModel().getRecipientId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        RepaymentEntity savedRepayment = repository.save(RepaymentEntity.from(request, payer, recipient, group));

        // Update balances in Redis
        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        balanceUpdates.put(payer.getUuid().toString(), savedRepayment.getAmount().negate());
        balanceUpdates.put(recipient.getUuid().toString(), savedRepayment.getAmount());

        redisService.updateBalances(groupId.toString(), balanceUpdates);

        return CreateRepaymentResponse.from(savedRepayment);
    }

}
