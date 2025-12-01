package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.constants.ApiConstants;
import com.tateca.tatecabackend.dto.request.UpdateUserRequestDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.PATH_USERS)
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @PatchMapping("/{userId}")
    public ResponseEntity<UserInfoDTO> updateUserName(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserRequestDTO nameRequestDTO
    ) {
        UserInfoDTO responseDTO = service.updateUserName(userId, nameRequestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
