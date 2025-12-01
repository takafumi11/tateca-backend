package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.UpdateUserRequestDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserAccessor accessor;
    private final CurrencyNameAccessor currencyNameAccessor;

    @Override
    public UserInfoDTO updateUserName(UUID userId, UpdateUserRequestDTO request) {
        // Validation: At least one field must be provided
        if (request.name() == null && request.currencyCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either name or currency_code must be provided");
        }

        UserEntity user = accessor.findById(userId);

        // Update name if provided
        if (request.name() != null) {
            user.setName(request.name());
        }

        // Update currency code if provided
        if (request.currencyCode() != null) {
            CurrencyNameEntity currencyName = currencyNameAccessor.findById(request.currencyCode());
            user.setCurrencyName(currencyName);
        }

        return UserInfoDTO.from(accessor.save(user));
    }
}
