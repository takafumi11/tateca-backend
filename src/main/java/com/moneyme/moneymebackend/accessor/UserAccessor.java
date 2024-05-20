package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class UserAccessor {
    private final UserRepository repository;

    public UserEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }


}
