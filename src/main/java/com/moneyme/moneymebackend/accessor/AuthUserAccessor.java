package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.AuthUserEntity;
import com.moneyme.moneymebackend.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@Component
public class AuthUserAccessor {
    private final AuthUserRepository repository;

    public AuthUserEntity findByUid(String uid) {
        try {
            List<AuthUserEntity> authUserEntityList = repository.findByUid(uid);
            if (authUserEntityList.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auth user not found with uid: " + uid);
            } else {
                return authUserEntityList.get(0);
            }
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public AuthUserEntity save(AuthUserEntity entity) {
        try {
            return repository.save(entity);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
