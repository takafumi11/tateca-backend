package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.LoanEntity;
import lombok.Builder;

import java.math.BigDecimal;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class LoanResponseDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("group")
    GroupResponseDTO group;
    @JsonProperty("title") String title;
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("payer")
    UserResponseDTO payer;
    @JsonProperty("detail") String detail;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static LoanResponseDTO from(LoanEntity loan) {
        return LoanResponseDTO.builder()
                .uuid(loan.getUuid().toString())
                .group(GroupResponseDTO.from(loan.getGroup()))
                .title(loan.getTitle())
                .amount(loan.getAmount())
                .payer(UserResponseDTO.from(loan.getPayer()))
                .detail(loan.getDetail())
                .createdAt(convertToTokyoTime(loan.getCreatedAt()))
                .updatedAt(convertToTokyoTime(loan.getUpdatedAt()))
                .build();
    }
}

