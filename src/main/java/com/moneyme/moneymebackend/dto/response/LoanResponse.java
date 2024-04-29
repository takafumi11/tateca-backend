package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.LoanEntity;
import lombok.Builder;

import java.math.BigDecimal;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class LoanResponse {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("title") String title;
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("payer") UserResponse payer;
    @JsonProperty("detail") String detail;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static LoanResponse from(LoanEntity loan) {
        return LoanResponse.builder()
                .uuid(loan.getUuid().toString())
                .title(loan.getTitle())
                .amount(loan.getAmount())
                .payer(UserResponse.from(loan.getPayer()))
                .detail(loan.getDetail())
                .createdAt(convertToTokyoTime(loan.getCreatedAt()))
                .updatedAt(convertToTokyoTime(loan.getUpdatedAt()))
                .build();
    }
}
