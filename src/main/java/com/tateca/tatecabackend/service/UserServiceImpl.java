package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserAccessor accessor;

    @Override
    @Transactional
    public UserResponseDTO updateUserName(UUID userId, UpdateUserNameRequestDTO request) {
        UserEntity user = accessor.findById(userId);

        // Update name (validated as required by controller)
        user.setName(request.name());

        return UserResponseDTO.from(accessor.save(user));
    }
}
