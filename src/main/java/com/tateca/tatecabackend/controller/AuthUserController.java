package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.AuthUserRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateAppReviewRequestDTO;
import com.tateca.tatecabackend.dto.response.AuthUserInfoDTO;
import com.tateca.tatecabackend.service.AuthUserService;
import com.tateca.tatecabackend.constants.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping(ApiConstants.PATH_AUTH_USERS)
@RequiredArgsConstructor
public class AuthUserController {
    private final AuthUserService service;

    @GetMapping("/{uid}")
    public ResponseEntity<AuthUserInfoDTO> getAuthUser(
            @PathVariable("uid") String uid
    ) {
        AuthUserInfoDTO response = service.getAuthUserInfo(uid);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AuthUserInfoDTO> createAuthUser(
            @UId String uid,
            @RequestBody AuthUserRequestDTO request) {
        AuthUserInfoDTO response = service.createAuthUser(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUserInfo(
            @PathVariable("uid") String uid
    ) {
        service.deleteAuthUser(uid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/review-preferences")
    public ResponseEntity<AuthUserInfoDTO> updateReviewPreferences(
            @UId String uid,
            @Valid @RequestBody UpdateAppReviewRequestDTO request
    ) {
        AuthUserInfoDTO response = service.updateAppReview(uid, request);
        return ResponseEntity.ok(response);
    }
}
