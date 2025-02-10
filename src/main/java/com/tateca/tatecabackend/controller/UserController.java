package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tateca.tatecabackend.constants.ApiConstants.PATH_USERS;

@RequiredArgsConstructor
@RequestMapping(PATH_USERS)
@RestController
public class UserController {
    private final UserService service;
}

