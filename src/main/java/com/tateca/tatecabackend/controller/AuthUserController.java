package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.CreateAuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserResponseDTO;
import com.tateca.tatecabackend.service.AuthUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth Users", description = "Authenticated user management operations")
public class AuthUserController {
    private final AuthUserService service;

    @GetMapping("/{uid}")
    public ResponseEntity<AuthUserResponseDTO> getAuthUser(
            @PathVariable("uid")
            @NotBlank(message = "UID is required and cannot be blank")
            @Size(max = 128, message = "UID must not exceed 128 characters")
            String uid
    ) {
        AuthUserResponseDTO response = service.getAuthUserInfo(uid);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AuthUserResponseDTO> createAuthUser(
            @UId String uid,
            @Valid @RequestBody CreateAuthUserRequestDTO request) {
        AuthUserResponseDTO response = service.createAuthUser(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUserInfo(
            @PathVariable("uid")
            @NotBlank(message = "UID is required and cannot be blank")
            @Size(max = 128, message = "UID must not exceed 128 characters")
            String uid
    ) {
        service.deleteAuthUser(uid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/review-preferences")
    public ResponseEntity<AuthUserResponseDTO> updateReviewPreferences(
            @UId String uid,
            @Valid @RequestBody UpdateAppReviewRequestDTO request
    ) {
        AuthUserResponseDTO response = service.updateAppReview(uid, request);
        return ResponseEntity.ok(response);
    }
}
