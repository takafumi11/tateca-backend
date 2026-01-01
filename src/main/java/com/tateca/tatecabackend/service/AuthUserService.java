package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;

public interface AuthUserService {
    AuthUserResponseDTO getAuthUserInfo(String uid);

    AuthUserResponseDTO createAuthUser(String uid, CreateAuthUserRequestDTO request);

    void deleteAuthUser(String uid);

    AuthUserResponseDTO updateAppReview(String uid, UpdateAppReviewRequestDTO request);
}
