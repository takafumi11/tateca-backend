package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.LoanObligationEntity;
import lombok.Builder;

import java.math.BigDecimal;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class ObligationResponse {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("user") UserResponse user;
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static ObligationResponse from(LoanObligationEntity obligation) {
        return ObligationResponse.builder()
                .uuid(obligation.getUuid().toString())
                .user(UserResponse.from(obligation.getUser()))
                .amount(obligation.getAmount())
                .createdAt(convertToTokyoTime(obligation.getCreatedAt()))
                .updatedAt(convertToTokyoTime(obligation.getUpdatedAt()))
                .build();
    }
}
