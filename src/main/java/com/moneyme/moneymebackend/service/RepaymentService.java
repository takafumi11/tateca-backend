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
        UserEntity payer = userRepository.findById(UUID.fromString(request.getRepaymentRequestDTO().getPayerId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserEntity recipient = userRepository.findById(UUID.fromString(request.getRepaymentRequestDTO().getRecipientId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        RepaymentEntity savedRepayment = repository.save(RepaymentEntity.from(request, payer, recipient, group));

        // Update balances in Redis
        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        balanceUpdates.put(payer.getUuid().toString(), savedRepayment.getAmount().negate());
        balanceUpdates.put(recipient.getUuid().toString(), savedRepayment.getAmount());

        redisService.updateBalances(groupId.toString(), balanceUpdates);

        return RepaymentCreationResponse.from(savedRepayment);
    }

    public RepaymentCreationResponse getRepayment(UUID groupId, UUID repaymentId) {
        RepaymentEntity repayment = repository.findById(repaymentId).orElseThrow(() -> new IllegalArgumentException("repayment not found"));

        return RepaymentCreationResponse.from(repayment);
    }

    @Transactional
    public RepaymentCreationResponse updateRepayment(UUID groupId, UUID repaymentId, RepaymentCreationRequest request) {
        RepaymentEntity repayment = repository.findById(repaymentId).orElseThrow(() -> new IllegalArgumentException("repayment not found"));
        RepaymentRequestDTO requestDTO = request.getRepaymentRequestDTO();

        BigDecimal prevAmount = repayment.getAmount();

        repayment.setTitle(requestDTO.getTitle());
        repayment.setAmount(requestDTO.getAmount());
        repayment.setDate(convertToTokyoTime(requestDTO.getDate()));
        repayment.setDetail(requestDTO.getDetail());
        RepaymentEntity savedRepayment = repository.save(repayment);

        // Update balances in Redis
        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        balanceUpdates.put(savedRepayment.getPayer().getUuid().toString(), prevAmount.add(savedRepayment.getAmount().negate()));
        balanceUpdates.put(savedRepayment.getRecipientUser().getUuid().toString(), prevAmount.negate().add(savedRepayment.getAmount()));
        redisService.updateBalances(groupId.toString(), balanceUpdates);

        return RepaymentCreationResponse.from(savedRepayment);
    }

    @Transactional
    public void deleteRepayment(UUID groupId, UUID repaymentId) {
        RepaymentEntity repayment = repository.findById(repaymentId).orElseThrow(() -> new IllegalArgumentException("repayment not found"));
        repository.delete(repayment);

        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        balanceUpdates.put(repayment.getPayer().getUuid().toString(), repayment.getAmount());
        balanceUpdates.put(repayment.getRecipientUser().getUuid().toString(), repayment.getAmount().negate());
        redisService.updateBalances(groupId.toString(), balanceUpdates);
    }

}
