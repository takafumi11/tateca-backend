package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found with ID: " + id));
        } catch(DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error at groupRepo.save", e);
        }
    }

    public List<LoanEntity> findByGroupId(UUID id) {
        return repository.findByGroupId(id);
    }

    public void deleteById(UUID loanId) {
        try {
            repository.deleteById(loanId);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

}