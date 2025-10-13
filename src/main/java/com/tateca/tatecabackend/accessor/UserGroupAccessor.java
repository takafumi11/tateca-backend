package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserGroupAccessor {
    private final UserGroupRepository repository;

    public List<UserGroupEntity> findByGroupUuid(UUID groupId) {
        try {
            return repository.findByGroupUuid(groupId);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<UserGroupEntity> findByUserUuidList(List<UUID> userUuidList) {
        try {
            return repository.findByUserUuidList(userUuidList);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<UserGroupEntity> saveAll(List<UserGroupEntity> userGroupEntityList) {
        try {
            return repository.saveAll(userGroupEntityList);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public UserGroupEntity findByUserUuidAndGroupUuid(UUID userUuid, UUID groupUuid) {
        try {
            return repository.findByUserUuidAndGroupUuid(userUuid, groupUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not in this group"));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}
