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
            List<UserAuthUserEntity> userAuthUserEntityList = repository.findByAuthUserUuid(uuid);

            if (userAuthUserEntityList.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UserAuthUsers not found with authUser uuid: " + uuid);
            } else {
                return userAuthUserEntityList;
            }
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
