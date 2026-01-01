package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CurrencyAccessor {
    private final CurrencyRepository repository;

    public CurrencyEntity findById(String currencyCode) {
        try {
            return repository.findById(currencyCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found with code: " + currencyCode));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<CurrencyEntity> findAllById(List<String> currencyCodes) {
        try {
            return repository.findAllById(currencyCodes);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
