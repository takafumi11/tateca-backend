package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.exception.domain.ForbiddenException;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.UserService;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository repository;

    @Override
    @Transactional
    public UserResponseDTO updateUserName(String authUid, UUID userId, UpdateUserNameRequestDTO request) {
        logger.info("Updating user name: userId={}", PiiMaskingUtil.maskUuid(userId));

        UserEntity user = repository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        String resourceOwnerUid = user.getAuthUser() != null ? user.getAuthUser().getUid() : null;
        if (!authUid.equals(resourceOwnerUid)) {
            throw new ForbiddenException(ErrorCode.USER_FORBIDDEN);
        }

        String newName = request.name();
        if (newName.equals(user.getName())) {
            logger.info("Same value update skipped: userId={}", PiiMaskingUtil.maskUuid(userId));
            return UserResponseDTO.from(user);
        }

        user.setName(newName);
        UserResponseDTO response = UserResponseDTO.from(repository.save(user));
        logger.info("User name updated successfully: userId={}", PiiMaskingUtil.maskUuid(userId));
        return response;
    }
}
