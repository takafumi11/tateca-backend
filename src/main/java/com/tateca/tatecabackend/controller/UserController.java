package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.constants.ApiConstants;
import com.tateca.tatecabackend.dto.request.UpdateUserRequestDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "User management operations")
public class UserController {
    private final UserService service;

    @PatchMapping("/{userId}")
    @Operation(summary = "Update user information")
    public ResponseEntity<UserInfoDTO> updateUserName(
            @PathVariable("userId") UUID userId,
            @RequestBody UpdateUserRequestDTO nameRequestDTO
            ) {
        UserInfoDTO responseDTO = service.updateUserName(userId, nameRequestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
