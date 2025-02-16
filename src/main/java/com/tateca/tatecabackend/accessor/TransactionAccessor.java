package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionAccessor {
    private final TransactionRepository repository;

    public TransactionHistoryEntity save(TransactionHistoryEntity entity) {
        try {
            return repository.save(entity);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public TransactionHistoryEntity findById(UUID id) {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found with ID: " + id));
        } catch(DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public void deleteById(UUID id) {
        try {
            repository.deleteById(id);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<TransactionHistoryEntity> findTransactionHistory(UUID groupId, int limit) {
        try {
            return repository.findTransactionsByGroupOrderByCreatedAtDescWithLimit(groupId, limit);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
