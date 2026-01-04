package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.constants.ApiConstants;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management operations")
public class UserController {
    private final UserService service;

    @PatchMapping("/{userId}")
    @Operation(summary = "Update user name")
    public ResponseEntity<UserResponseDTO> updateUserName(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserNameRequestDTO nameRequestDTO
            ) {
        UserResponseDTO responseDTO = service.updateUserName(userId, nameRequestDTO);
        return ResponseEntity.ok(responseDTO);
    }
}
