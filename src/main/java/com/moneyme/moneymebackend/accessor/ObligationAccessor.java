package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.repository.ObligationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        return repository.findByLoanId(id);
    }

    public List<ObligationEntity> findByGroupId(UUID id) {
        return repository.findByGroupId(id);
    }

    public void deleteAll(List<ObligationEntity> obligationEntityList) {
        repository.deleteAll(obligationEntityList);
    }
}
