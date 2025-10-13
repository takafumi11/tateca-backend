package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.UpdateUserRequestDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserAccessor accessor;
    private final CurrencyNameAccessor currencyNameAccessor;

    public UserInfoDTO updateUserName(UUID userId, UpdateUserRequestDTO request) {
        UserEntity user = accessor.findById(userId);

        // Update name if provided
        if (request.getName() != null) {
            user.setName(request.getName());
        }

        // Update currency code if provided
        if (request.getCurrencyCode() != null) {
            CurrencyNameEntity currencyName = currencyNameAccessor.findById(request.getCurrencyCode());
            user.setCurrencyName(currencyName);
        }

        return UserInfoDTO.from(accessor.save(user));
    }
}
