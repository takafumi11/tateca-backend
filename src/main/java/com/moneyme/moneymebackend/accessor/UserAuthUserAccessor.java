package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.UserAuthUserEntity;
import com.moneyme.moneymebackend.repository.UserAuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserAuthUserAccessor {
    private final UserAuthUserRepository repository;

    public List<UserAuthUserEntity> findByAuthUserUuid(UUID uuid) {
        try {
            return repository.findByAuthUserUuid(uuid);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public UserAuthUserEntity save(UserAuthUserEntity entity) {
        try {
            return repository.save(entity);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
