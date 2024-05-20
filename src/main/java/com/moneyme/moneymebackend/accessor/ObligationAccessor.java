package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.repository.ObligationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ObligationAccessor {
    private final ObligationRepository repository;

    public List<ObligationEntity> saveAll(List<ObligationEntity> obligationEntityList) {
        return repository.saveAll(obligationEntityList);
    }

    public List<ObligationEntity> findByLoanId(UUID id) {
        return repository.findByLoanId(id);
    }

    public void deleteAll(List<ObligationEntity> obligationEntityList) {
        repository.deleteAll(obligationEntityList);
    }
}
