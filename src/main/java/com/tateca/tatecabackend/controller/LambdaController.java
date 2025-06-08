package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.response.LambdaResponseDTO;
import com.tateca.tatecabackend.service.LambdaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.tateca.tatecabackend.constants.ApiConstants.PATH_LAMBDA;

@RestController
@RequestMapping(PATH_LAMBDA)
@RequiredArgsConstructor
public class LambdaController {
    private final LambdaService service;

    @GetMapping
    public ResponseEntity<LambdaResponseDTO> getCurrencyCode(
            @UId String uid
    ) {
        return ResponseEntity.ok(service.getAuthUser(uid));
    }
} 