package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserAccessor {
    private final UserRepository repository;

    public UserEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

    }

    public UserEntity save(UserEntity userEntity) throws ResponseStatusException {
        try {
            return repository.save(userEntity);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
