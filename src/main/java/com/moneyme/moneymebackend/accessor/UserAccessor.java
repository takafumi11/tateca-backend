package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserAccessor {
    private final UserRepository repository;

    public UserEntity findById(UUID id) {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("DB error occurred");
        }
    }
}
