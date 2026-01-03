package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.domain.DuplicateResourceException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.model.AppReviewStatus;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthUserServiceImpl implements AuthUserService {
    private final AuthUserRepository repository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AuthUserResponseDTO getAuthUserInfo(String uid) {
        AuthUserEntity authUser = repository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException("Auth user not found: " + uid));

        authUser.setLastLoginTime(Instant.now());
        authUser.setTotalLoginCount(authUser.getTotalLoginCount() + 1);

        AuthUserEntity updatedAuthUser = repository.save(authUser);
        return AuthUserResponseDTO.from(updatedAuthUser);
    }

    @Override
    @Transactional
    public AuthUserResponseDTO createAuthUser(String uid, CreateAuthUserRequestDTO request) {
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
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

        return AuthUserResponseDTO.from(createdAuthUser);
    }

    @Override
    @Transactional
    public void deleteAuthUser(String uid) {
        // Verify auth user exists
        repository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException("Auth user not found: " + uid));

        List<UserEntity> userEntityList = userRepository.findByAuthUserUid(uid);
        userEntityList.forEach(user -> {
            user.setAuthUser(null);
        });

        userRepository.saveAll(userEntityList);
        repository.deleteById(uid);
    }

    @Override
    @Transactional
    public AuthUserResponseDTO updateAppReview(String uid, UpdateAppReviewRequestDTO request) {
        AuthUserEntity authUser = repository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException("Auth user not found: " + uid));

        authUser.setLastAppReviewDialogShownAt(Instant.now());
        authUser.setAppReviewStatus(request.appReviewStatus());

        AuthUserEntity updatedAuthUser = repository.save(authUser);
        return AuthUserResponseDTO.from(updatedAuthUser);
    }
}
