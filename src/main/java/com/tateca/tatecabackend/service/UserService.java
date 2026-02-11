package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.exception.domain.DatabaseOperationException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;

import java.util.UUID;

public interface UserService {
    UserResponseDTO updateUserName(UUID userId, UpdateUserNameRequestDTO request) throws EntityNotFoundException, DatabaseOperationException;
}
