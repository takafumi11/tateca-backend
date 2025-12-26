package com.tateca.tatecabackend.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.tateca.tatecabackend.constants.ApiConstants;

import com.tateca.tatecabackend.dto.response.TokenResponseDTO;
import com.tateca.tatecabackend.service.FirebaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.PATH_DEV)
@RequiredArgsConstructor
@Tag(name = "Development", description = "Development and testing utilities")
@Profile("dev")
public class DevController {
    
    private final FirebaseService firebaseService;
    
    @GetMapping("/firebase-token/{uid}")
    public ResponseEntity<TokenResponseDTO> generateFirebaseToken(
            @PathVariable("uid") String uid) {
        try {
            String customToken = firebaseService.generateCustomToken(uid);
            TokenResponseDTO response = TokenResponseDTO.builder()
                    .customToken(customToken)
                    .build();
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Failed to generate Firebase custom token", e);
        }
    }
}