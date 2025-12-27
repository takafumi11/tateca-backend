package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;

import java.util.UUID;

public interface UserService {
    UserResponseDTO updateUserName(UUID userId, UpdateUserNameRequestDTO request);
}
