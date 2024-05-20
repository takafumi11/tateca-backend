package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class GroupAccessor {
    private final GroupRepository groupRepository;

    public GroupEntity findById(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("group not found"));
    }
}
