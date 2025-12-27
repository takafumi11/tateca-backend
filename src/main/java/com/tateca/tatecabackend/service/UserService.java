package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.UpdateUserNameDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;

import java.util.UUID;

public interface UserService {
    UserInfoDTO updateUserName(UUID userId, UpdateUserNameDTO request);
}
