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

    public UserGroupEntity findByIds(UUID useruuid, UUID groupUuid) {
        try {
            return repository.findByUserUuidAndGroupUuid(useruuid, groupUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User Group Not Found"));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<UserGroupEntity> findByGroupUuid(UUID groupId) {
        try {
            return repository.findByGroupUuid(groupId);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<UserGroupEntity> findByUserUuid(UUID userId) {
        try {
            return repository.findByUserUuid(userId);
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

    public UserGroupEntity save(UserGroupEntity userGroupEntity) {
        try {
            return repository.save(userGroupEntity);
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

    public void delete(UUID userUuid, UUID groupUuid) {
        try {
            repository.deleteByUserUuidAndGroupUuid(userUuid, groupUuid);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

}
