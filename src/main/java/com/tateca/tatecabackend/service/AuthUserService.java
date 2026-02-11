package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.exception.domain.DuplicateResourceException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;

public interface AuthUserService {
    AuthUserResponseDTO getAuthUserInfo(String uid) throws EntityNotFoundException;

    AuthUserResponseDTO createAuthUser(String uid, CreateAuthUserRequestDTO request) throws DuplicateResourceException;

    void deleteAuthUser(String uid) throws EntityNotFoundException;

    AuthUserResponseDTO updateAppReview(String uid, UpdateAppReviewRequestDTO request) throws EntityNotFoundException;
}
