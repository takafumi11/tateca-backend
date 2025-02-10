package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.AuthUserAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.AuthUserRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthUserService {
    private final AuthUserAccessor accessor;
    private final UserAccessor userAccessor;

    public AuthUserResponseDTO getAuthUserInfo(String uid) {
        AuthUserEntity authUser = accessor.findByUid(uid);

        authUser.setLastLoginTime(ZonedDateTime.now());
        authUser.setTotalLoginCount(authUser.getTotalLoginCount() + 1);
        AuthUserEntity updatedAuthUser = accessor.save(authUser);

        return AuthUserResponseDTO.from(updatedAuthUser);
    }

    @Transactional
    public AuthUserResponseDTO createAuthUser(String uid, AuthUserRequestDTO request) {
        accessor.validateEmail(request.getEmail());

        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(uid)
                .name(request.getName())
                .email(request.getEmail())
                .lastLoginTime(ZonedDateTime.now())
                .totalLoginCount(1)
                .build();

        AuthUserEntity createdAuthUser = accessor.save(authUser);

        return AuthUserResponseDTO.from(createdAuthUser);
    }

    @Transactional
    public void deleteAuthUser(String uid) {
        List<UserEntity> userEntityList = userAccessor.findByAuthUserUid(uid);
        userEntityList.forEach(user -> {
            user.setAuthUser(null);
        });

        userAccessor.saveAll(userEntityList);
        accessor.deleteById(uid);
    }
}
