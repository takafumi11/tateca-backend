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
}
