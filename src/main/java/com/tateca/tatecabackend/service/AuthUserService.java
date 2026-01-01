package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.AuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserInfoDTO;

public interface AuthUserService {
    AuthUserInfoDTO getAuthUserInfo(String uid);

    AuthUserInfoDTO createAuthUser(String uid, AuthUserRequestDTO request);

    void deleteAuthUser(String uid);

    AuthUserInfoDTO updateAppReview(String uid, UpdateAppReviewRequestDTO request);
}
