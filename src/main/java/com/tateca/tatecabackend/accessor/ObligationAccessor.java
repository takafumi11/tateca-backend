package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.repository.ObligationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ObligationAccessor {
    private final ObligationRepository repository;

    public TransactionObligationEntity save(TransactionObligationEntity transactionObligationEntity) {
        try {
            return repository.save(transactionObligationEntity);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<TransactionObligationEntity> saveAll(List<TransactionObligationEntity> transactionObligationEntityList) {
        try {
            return repository.saveAll(transactionObligationEntityList);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<TransactionObligationEntity> findByTransactionId(UUID id) {
        try {
            return repository.findByTransactionId(id);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<TransactionObligationEntity> findByGroupId(UUID id) {
        return repository.findByGroupId(id);
    }

    public void deleteAllById(List<UUID> loanIdList) {
        try {
            repository.deleteAllById(loanIdList);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
