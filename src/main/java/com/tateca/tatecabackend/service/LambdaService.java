package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.AuthUserAccessor;
import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.dto.response.LambdaResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LambdaService {
    private static final Logger logger = LoggerFactory.getLogger(LambdaService.class);
    private final AuthUserAccessor accessor;

    public LambdaResponseDTO getAuthUser(String uid) {
        try {
            AuthUserEntity authUser = accessor.findByUid(uid);
            return new LambdaResponseDTO(authUser.getUid());
        } catch (ResponseStatusException e) {
            // UIDがDBに存在しない場合、リクエストのUIDをそのまま返す
            logger.warn("AuthUser not found for uid: {}. Returning original uid.", uid);
            return new LambdaResponseDTO(uid);
        } catch (Exception e) {
            // その他のDB関連エラーの場合も、UIDをそのまま返す
            logger.error("Database error occurred while fetching AuthUser for uid: {}. Returning original uid. Error: {}", 
                        uid, e.getMessage(), e);
            return new LambdaResponseDTO(uid);
        }
    }
} 