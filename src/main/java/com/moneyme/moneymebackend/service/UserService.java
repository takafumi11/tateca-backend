package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.dto.request.UserDeleteRequestDTO;
import com.moneyme.moneymebackend.dto.request.UserRequestDTO;
import com.moneyme.moneymebackend.dto.response.AuthUserResponseDTO;
import com.moneyme.moneymebackend.dto.response.UserResponseDTO;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserAccessor accessor;
}
