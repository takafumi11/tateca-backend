package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.TransactionEntity;
import com.tateca.tatecabackend.repository.LoanRepository;
import com.tateca.tatecabackend.repository.RepaymentRepository;
import com.tateca.tatecabackend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionAccessor {
    private final TransactionRepository repository;

    public TransactionEntity save(TransactionEntity entity) {
        try {
            return repository.save(entity);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public TransactionEntity findById(UUID id) {
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
}
