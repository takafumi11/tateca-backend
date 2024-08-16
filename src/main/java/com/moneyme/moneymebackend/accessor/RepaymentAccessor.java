package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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

    public void deleteById(UUID repaymentId) {
        try {
            repository.deleteById(repaymentId);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
