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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserGroupAccessor {
    private final UserGroupRepository repository;

    public List<UserGroupEntity> findByGroupUuid(UUID groupId) {
        try {
            List<UserGroupEntity> userGroupEntityList = repository.findByGroupUuid(groupId);

            if (userGroupEntityList.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UserGroups not found with group id: " + groupId);
            } else {
                return userGroupEntityList;
            }
        } catch(DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    public List<UserGroupEntity> findByUserUuid(UUID userId) {
        try {
            List<UserGroupEntity> userGroupEntityList = repository.findByUserUuid(userId);

            if (userGroupEntityList.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UserGroups not found with user id: " + userId);
            } else {
                return userGroupEntityList;
            }
        } catch(DataAccessException e) {
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
}
