package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.ObligationEntity;
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

    public List<ObligationEntity> saveAll(List<ObligationEntity> obligationEntityList) {
        return repository.saveAll(obligationEntityList);
    }

    public ObligationEntity save(ObligationEntity obligationEntity) {
        return repository.save(obligationEntity);
    }

    public List<ObligationEntity> findByLoanId(UUID id) {
        try {
            return repository.findByLoanId(id);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<ObligationEntity> findByGroupId(UUID id) {
        return repository.findByGroupId(id);
    }

    public void deleteAll(List<ObligationEntity> obligationEntityList) {
        repository.deleteAll(obligationEntityList);
    }

    public void deleteAllById(List<UUID> loanIdList) {
        try {
            repository.deleteAllById(loanIdList);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
