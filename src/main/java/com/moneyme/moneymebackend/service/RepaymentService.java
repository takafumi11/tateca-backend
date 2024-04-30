package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.request.CreateRepaymentRequest;
import com.moneyme.moneymebackend.dto.response.CreateRepaymentResponse;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.RedisRepository;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import com.moneyme.moneymebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Service
@RequiredArgsConstructor
public class RepaymentService {
    private final RepaymentRepository repository;
    private final UserRepository userRepository;
    private final RedisService redisService;

    public CreateRepaymentResponse createRepayment(CreateRepaymentRequest request) {
        UserEntity payer = userRepository.findById(UUID.fromString(request.getRepaymentInfo().getPayerId())).orElseThrow(() -> new IllegalArgumentException("user not found"));
        UserEntity recipientUser = userRepository.findById(UUID.fromString(request.getRepaymentInfo().getRecipientId())).orElseThrow(() -> new IllegalArgumentException("user not found"));

        RepaymentEntity repayment = RepaymentEntity.builder()
                .uuid(UUID.randomUUID())
                .title(request.getRepaymentInfo().getTitle())
                .amount(request.getRepaymentInfo().getAmount())
                .date(convertToTokyoTime(request.getRepaymentInfo().getDate()))
                .payer(payer)
                .recipientUser(recipientUser)
                .detail(request.getRepaymentInfo().getDetail())
                .build();

        RepaymentEntity savedRepayment = repository.save(repayment);
        redisService.updateBalances(savedRepayment.getPayer().getUuid().toString(), savedRepayment.getRecipientUser().getUuid().toString(), savedRepayment.getAmount(), request.getGroupId());

        return CreateRepaymentResponse.from(savedRepayment);

    }
}
