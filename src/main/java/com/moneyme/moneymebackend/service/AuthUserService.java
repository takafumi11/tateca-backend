package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.AuthUserAccessor;
import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.dto.request.AuthUserRequestDTO;
import com.moneyme.moneymebackend.dto.response.AuthUserResponseDTO;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

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
