package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserAccessor {
    private final UserRepository repository;

    public UserEntity findById(UUID id) {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + id));

        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<UserEntity> findByAuthUserUid(String uid) {
        try {
            return repository.findByAuthUserUid(uid);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public UserEntity save(UserEntity userEntity) throws ResponseStatusException {
        try {
            return repository.save(userEntity);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<UserEntity> saveAll(List<UserEntity> users) throws ResponseStatusException {
        try {
            return repository.saveAll(users);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
