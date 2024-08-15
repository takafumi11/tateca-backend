package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.UserGroupEntity;
import com.moneyme.moneymebackend.repository.UserGroupRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserGroupAccessor {
    private final UserGroupRepository repository;

    public Optional<UserGroupEntity> findByIds(UUID useruuid, UUID groupUuid) {
        try {
            return repository.findByUserUuidAndGroupUuid(useruuid, groupUuid);
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
