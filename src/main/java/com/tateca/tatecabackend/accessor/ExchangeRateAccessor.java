package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeRateAccessor {
    private final ExchangeRateRepository repository;

    public void saveAll(List<ExchangeRateEntity> entityList) {
        try {
            repository.saveAll(entityList);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public ExchangeRateEntity findByCurrencyCodeAndDate(String currencyCode, LocalDate date) {
        try {
            return repository.findByCurrencyCodeAndDate(currencyCode, date)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exchange rate Not Found with code and date:" + currencyCode));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
