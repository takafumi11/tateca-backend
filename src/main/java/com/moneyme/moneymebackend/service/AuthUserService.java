package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.AuthUserAccessor;
import com.moneyme.moneymebackend.dto.response.AuthUserResponseDTO;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthUserService {
    private final AuthUserAccessor accessor;

    public AuthUserResponseDTO getAuthUserInfo(String uid) {
        AuthUserEntity authUser = accessor.findByUid(uid);
        return AuthUserResponseDTO.from(authUser);
    }
}
