package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.moneyme.moneymebackend.service.util.AmountHelper.calculateAmount;

@RequiredArgsConstructor
public class LoanAccessor {
    private final LoanRepository repository;

    public LoanEntity save(LoanEntity loanEntity) {
        return repository.save(loanEntity);
    }

    public LoanEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + id));
    }

    public void delete(LoanEntity loanEntity) {
        repository.delete(loanEntity);
    }

}