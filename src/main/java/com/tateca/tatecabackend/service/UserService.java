package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.UserAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserAccessor accessor;
}
