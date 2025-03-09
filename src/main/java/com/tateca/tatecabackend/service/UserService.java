package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserAccessor accessor;

    public UserResponseDTO updateUserName(UUID userId, String newName) {
        UserEntity user = accessor.findById(userId);
        user.setName(newName);

        return UserResponseDTO.from(accessor.save(user));

    }
}
