package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.UpdateUserRequestDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;

import java.util.UUID;

public interface UserService {
    UserInfoDTO updateUserName(UUID userId, UpdateUserRequestDTO request);
}
