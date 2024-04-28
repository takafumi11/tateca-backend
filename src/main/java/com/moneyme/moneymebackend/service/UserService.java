package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.request.CreateUserRequest;
import com.moneyme.moneymebackend.dto.response.CreateUserResponse;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    public CreateUserResponse createUser(String uuid, CreateUserRequest request) {
        UserEntity user = UserEntity.builder()
                .uuid(UUID.fromString(uuid))
                .name(request.getUserName())
                .email(request.getEmail())
                .isTemporary(false)
                .build();

        UserEntity savedUser = repository.save(user);

        return CreateUserResponse.builder()
                .uuid(savedUser.getUuid().toString())
                .userName(savedUser.getName())
                .email(savedUser.getEmail())
                .isTemporary(savedUser.isTemporary())
                .createdAt(savedUser.getCreatedAt().toString())
                .updatedAt(savedUser.getUpdatedAt().toString())
                .build();
    }
}
