package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;

import java.util.UUID;

public interface UserService {
    UserResponseDTO updateUserName(String authUid, UUID userId, UpdateUserNameRequestDTO request);
}
