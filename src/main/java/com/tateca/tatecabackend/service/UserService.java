package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.UpdateUserNameDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserAccessor accessor;

    @Transactional
    public UserInfoDTO updateUserName(UUID userId, UpdateUserNameDTO request) {
        UserEntity user = accessor.findById(userId);

        // Update name (validated as required by controller)
        user.setName(request.getName());

        return UserInfoDTO.from(accessor.save(user));
    }
}
