package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class GroupAccessor {
    private final GroupRepository groupRepository;

    public GroupEntity findById(UUID id) {
        try {
            return groupRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "group not found: " + id));
        } catch(DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error at groupRepo.save", e);
        }
    }

    public GroupEntity save(GroupEntity groupEntity) {
        try {
            return groupRepository.save(groupEntity);
        } catch(DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error at groupRepo.save", e);
        }
    }
}
