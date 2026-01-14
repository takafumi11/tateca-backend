package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.domain.DuplicateResourceException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.AuthUserService;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthUserServiceImpl implements AuthUserService {
    private static final Logger logger = LoggerFactory.getLogger(AuthUserServiceImpl.class);
    private final AuthUserRepository repository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AuthUserResponseDTO getAuthUserInfo(String uid) {
        AuthUserEntity authUser = repository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND, uid));

        authUser.setLastLoginTime(Instant.now());
        authUser.setTotalLoginCount(authUser.getTotalLoginCount() + 1);

        AuthUserEntity updatedAuthUser = repository.save(authUser);

        logger.info("User login recorded: userId={}, email={}, loginCount={}",
                PiiMaskingUtil.maskUid(uid),
                PiiMaskingUtil.maskEmail(updatedAuthUser.getEmail()),
                updatedAuthUser.getTotalLoginCount());

        return AuthUserResponseDTO.from(updatedAuthUser);
    }

    @Override
    @Transactional
    public AuthUserResponseDTO createAuthUser(String uid, CreateAuthUserRequestDTO request) {
        logger.info("Creating new user account: userId={}, email={}",
                PiiMaskingUtil.maskUid(uid), PiiMaskingUtil.maskEmail(request.email()));

        if (repository.existsByEmail(request.email())) {
            logger.warn("User registration failed - email already exists: email={}",
                    PiiMaskingUtil.maskEmail(request.email()));
            throw new DuplicateResourceException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, request.email());
        }

        AuthUserEntity authUser = AuthUserEntity.builder()
                .uid(uid)
                .name("")
                .email(request.email())
                .lastLoginTime(Instant.now())
                .totalLoginCount(1)
                .appReviewStatus(AppReviewStatus.PENDING)
                .build();

        AuthUserEntity createdAuthUser = repository.save(authUser);

        logger.info("User account created successfully: userId={}, email={}",
                PiiMaskingUtil.maskUid(uid), PiiMaskingUtil.maskEmail(createdAuthUser.getEmail()));

        return AuthUserResponseDTO.from(createdAuthUser);
    }

    @Override
    @Transactional
    public void deleteAuthUser(String uid) {
        logger.info("Deleting user account: userId={}", PiiMaskingUtil.maskUid(uid));

        // Verify auth user exists
        AuthUserEntity authUser = repository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND, uid));

        List<UserEntity> userEntityList = userRepository.findByAuthUserUid(uid);
        int groupCount = userEntityList.size();
        userEntityList.forEach(user -> {
            user.setAuthUser(null);
        });

        userRepository.saveAll(userEntityList);
        repository.deleteById(uid);

        logger.info("User account deleted successfully: userId={}, email={}, groupCount={}",
                PiiMaskingUtil.maskUid(uid),
                PiiMaskingUtil.maskEmail(authUser.getEmail()),
                groupCount);
    }

    @Override
    @Transactional
    public AuthUserResponseDTO updateAppReview(String uid, UpdateAppReviewRequestDTO request) {
        AuthUserEntity authUser = repository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND, uid));

        authUser.setLastAppReviewDialogShownAt(Instant.now());
        authUser.setAppReviewStatus(request.appReviewStatus());

        AuthUserEntity updatedAuthUser = repository.save(authUser);
        return AuthUserResponseDTO.from(updatedAuthUser);
    }
}
