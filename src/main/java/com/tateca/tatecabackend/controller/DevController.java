package com.tateca.tatecabackend.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.tateca.tatecabackend.constants.ApiConstants;

import com.tateca.tatecabackend.dto.response.TokenResponseDTO;
import com.tateca.tatecabackend.service.FirebaseService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Development", description = "Development and testing utilities (dev profile only)")
@Profile("dev")
public class DevController {

    private final FirebaseService firebaseService;

    @GetMapping("/firebase-token/{uid}")
    @Operation(
        summary = "Generate Firebase Custom Token",
        description = "Generates a Firebase custom token for the given UID. " +
                     "Note: In dev mode, use x-uid header instead of Bearer token for easier testing. " +
                     "Example: curl -H 'x-uid: test-user-001' http://localhost:8080/auth/users/test-user-001"
    )
    public ResponseEntity<TokenResponseDTO> generateFirebaseToken(
            @PathVariable("uid") String uid) {
        try {
            String customToken = firebaseService.generateCustomToken(uid);
            TokenResponseDTO response = new TokenResponseDTO(customToken);
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Failed to generate Firebase custom token", e);
        }
    }
}