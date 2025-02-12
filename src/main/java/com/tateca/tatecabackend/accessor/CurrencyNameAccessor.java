package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.repository.CurrencyNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CurrencyNameAccessor {
    private final CurrencyNameRepository repository;

    public List<CurrencyNameEntity> findAllById(List<String> currencyCodes) {
        try {
            return repository.findAllById(currencyCodes);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
