package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.response.AuthUserResponseDTO;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import com.moneyme.moneymebackend.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_AUTH_USERS;

@RestController
@RequestMapping(PATH_AUTH_USERS)
@RequiredArgsConstructor
public class AuthUserController {
    private final AuthUserService service;

    @GetMapping
    public ResponseEntity<AuthUserResponseDTO> getAuthUser(
            @RequestParam("uid") String uid
    ) {
        AuthUserResponseDTO response = service.getAuthUserInfo(uid);
        return ResponseEntity.ok(response);
    }
}
