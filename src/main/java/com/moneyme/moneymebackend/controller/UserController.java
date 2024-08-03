package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.annotation.UId;
import com.moneyme.moneymebackend.dto.request.UserDeleteRequestDTO;
import com.moneyme.moneymebackend.dto.response.UserResponseDTO;
import com.moneyme.moneymebackend.dto.request.UserRequestDTO;
import com.moneyme.moneymebackend.service.UserService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_USERS;

@RequiredArgsConstructor
@RequestMapping(PATH_USERS)
@RestController
public class UserController {
    private final UserService service;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @UId String uid,
            @RequestBody UserRequestDTO request) {
        UserResponseDTO response = service.createUser(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<UserResponseDTO> getUser(
            @RequestParam("uid") String uid
    ) {
        UserResponseDTO response = service.getUser(uid);
        return ResponseEntity.ok(response);
    }

    // TODO: Add ENUM or cretae new endpoint
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateUserInfo(
            @UId String uid,
            @PathVariable("userId") UUID uuid,
            @RequestBody UserRequestDTO request) {
        UserResponseDTO response = service.updateUserInfo(uuid, request, uid);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> deleteUserInfo(
            @PathVariable("userId") UUID uuid,
            @RequestBody UserDeleteRequestDTO request) {
        UserResponseDTO response = service.deleteUserInfo(uuid, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

//    @GetMapping("/{userId}")
//    public ResponseEntity<UserResponseDTO> getUserInfo(
//            @RequestHeader(HEADER_AUTHORIZATION) String idToken,
//            @PathVariable("userId") UUID userId
//    ) {
//        UserResponseDTO response = service.getUserInfo(idToken, userId);
//        return ResponseEntity.ok(response);
//    }
}

