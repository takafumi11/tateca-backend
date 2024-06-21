package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class RepaymentAccessor {
    private final RepaymentRepository repository;

    public RepaymentEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found with ID: " + id));
    }

    public List<RepaymentEntity> findByGroupId(UUID id) {
        return repository.findByGroupId(id);
    }

    public RepaymentEntity save(RepaymentEntity repaymentEntity) {
        return repository.save(repaymentEntity);
    }

    public void delete(RepaymentEntity repaymentEntity) {
        repository.delete(repaymentEntity);
    }
}
