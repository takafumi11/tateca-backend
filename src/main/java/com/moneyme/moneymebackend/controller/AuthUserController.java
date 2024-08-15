package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.annotation.UId;
import com.moneyme.moneymebackend.dto.request.AuthUserRequestDTO;
import com.moneyme.moneymebackend.dto.request.UserDeleteRequestDTO;
import com.moneyme.moneymebackend.dto.request.UserRequestDTO;
import com.moneyme.moneymebackend.dto.response.AuthUserResponseDTO;
import com.moneyme.moneymebackend.dto.response.UserResponseDTO;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import com.moneyme.moneymebackend.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_AUTH_USERS;

@RestController
@RequestMapping(PATH_AUTH_USERS)
@RequiredArgsConstructor
public class AuthUserController {
    private final AuthUserService service;

    @GetMapping("/{uid}")
    public ResponseEntity<AuthUserResponseDTO> getAuthUser(
            @PathVariable("uid") String uid
    ) {
        AuthUserResponseDTO response = service.getAuthUserInfo(uid);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AuthUserResponseDTO> createAuthUser(
            @UId String uid,
            @RequestBody AuthUserRequestDTO request) {
        AuthUserResponseDTO response = service.createAuthUser(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<UserResponseDTO> deleteUserInfo(
            @PathVariable("uid") String uid
    ) {
        service.deleteAuthUser(uid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}
