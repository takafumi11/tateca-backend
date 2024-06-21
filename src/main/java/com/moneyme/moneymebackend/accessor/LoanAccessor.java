package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class LoanAccessor {
    private final LoanRepository repository;

    public LoanEntity save(LoanEntity loanEntity) {
        return repository.save(loanEntity);
    }

    public LoanEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + id));
    }

    public List<LoanEntity> findByGroupId(UUID id) {
        return repository.findByGroupId(id);
    }

    public void delete(LoanEntity loanEntity) {
        repository.delete(loanEntity);
    }

}