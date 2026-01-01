package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.accessor.AuthUserAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.AuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserInfoDTO;
import com.tateca.tatecabackend.entity.AppReviewStatus;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.AuthUserService;
import com.tateca.tatecabackend.util.LogFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthUserServiceImpl implements AuthUserService {
    private static final Logger logger = LogFactory.getLogger(AuthUserServiceImpl.class);

    private final AuthUserAccessor accessor;
    private final UserAccessor userAccessor;

    @Override
    @Transactional
    public AuthUserInfoDTO getAuthUserInfo(String uid) {
        long totalStartTime = System.currentTimeMillis();

        // Measure findByUid query time
        long findStartTime = System.currentTimeMillis();
        AuthUserEntity authUser = accessor.findByUid(uid);
        long findEndTime = System.currentTimeMillis();

        long findTime = findEndTime - findStartTime;

        logger.info("getAuthUserInfo findByUid query time for uid {}: {} ms", uid, (findEndTime - findStartTime));


        authUser.setLastLoginTime(Instant.now());
        authUser.setTotalLoginCount(authUser.getTotalLoginCount() + 1);

        // Measure save query time
        long saveStartTime = System.currentTimeMillis();
        AuthUserEntity updatedAuthUser = accessor.save(authUser);
        long saveEndTime = System.currentTimeMillis();
        long saveTime = saveEndTime - saveStartTime;

        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;

        LogFactory.logQueryTime(logger, "getAuthUserInfo",
            "findByUid", findTime,
            "save", saveTime,
            "total", totalTime);

        return AuthUserInfoDTO.from(updatedAuthUser);
    }

    @Override
    @Transactional
    public AuthUserInfoDTO createAuthUser(String uid, AuthUserRequestDTO request) {
        accessor.validateEmail(request.email());

        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(uid)
                .name(request.name())
                .email(request.email())
                .lastLoginTime(Instant.now())
                .totalLoginCount(1)
                .appReviewStatus(AppReviewStatus.PENDING)
                .build();

        AuthUserEntity createdAuthUser = accessor.save(authUser);

        return AuthUserInfoDTO.from(createdAuthUser);
    }

    @Override
    @Transactional
    public void deleteAuthUser(String uid) {
        List<UserEntity> userEntityList = userAccessor.findByAuthUserUid(uid);
        userEntityList.forEach(user -> {
            user.setAuthUser(null);
        });

        userAccessor.saveAll(userEntityList);
        accessor.deleteById(uid);
    }

    @Override
    @Transactional
    public AuthUserInfoDTO updateAppReview(String uid, UpdateAppReviewRequestDTO request) {
        AuthUserEntity authUser = accessor.findByUid(uid);

        if (request.showDialog()) {
            authUser.setLastAppReviewDialogShownAt(Instant.now());
        }

        if (request.appReviewStatus() != null) {
            authUser.setAppReviewStatus(request.appReviewStatus());
        }

        AuthUserEntity updatedAuthUser = accessor.save(authUser);
        return AuthUserInfoDTO.from(updatedAuthUser);
    }
}
