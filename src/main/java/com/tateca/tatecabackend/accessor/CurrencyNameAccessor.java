package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.repository.CurrencyNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CurrencyNameAccessor {
    private final CurrencyNameRepository repository;

    public CurrencyNameEntity findById(String currencyCode) {
        try {
            return repository.findById(currencyCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency Name not found with code: " + currencyCode));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public CurrencyNameEntity findForJPY() {
        try {
            return repository.findById("JPY")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "JPY Not Found in currency_name"));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<CurrencyNameEntity> findAllById(List<String> currencyCodes) {
        try {
            return repository.findAllById(currencyCodes);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<CurrencyNameEntity> findAll() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
    
    public CurrencyNameEntity findFirstCurrencyCode() {
        try {
            List<CurrencyNameEntity> currencies = repository.findFirstCurrencyCode(PageRequest.of(0, 1));
            if (currencies.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No currency codes found");
            }
            return currencies.get(0);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
