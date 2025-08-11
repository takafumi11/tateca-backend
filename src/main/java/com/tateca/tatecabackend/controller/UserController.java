package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.constants.ApiConstants;
import com.tateca.tatecabackend.dto.request.UpdateUserNameRequestDTO;
import com.tateca.tatecabackend.dto.response.UserInfoDTO;
import com.tateca.tatecabackend.service.UserService;
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
public class UserController {
    private final UserService service;

    @PatchMapping("/{userId}")
    public ResponseEntity<UserInfoDTO> updateUserName(
            @PathVariable("userId") UUID userId,
            @RequestBody UpdateUserNameRequestDTO nameRequestDTO
            ) {
        UserInfoDTO responseDTO = service.updateUserName(userId, nameRequestDTO.getName());
        return ResponseEntity.ok(responseDTO);
    }
}
