package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionEntity;
import lombok.Builder;

import java.math.BigDecimal;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class LoanResponseDTO {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("group")
    GroupResponseDTO group;
    @JsonProperty("title") String title;
    @JsonProperty("amount") Integer amount;
    @JsonProperty("currency_code") String currencyCode;
    @JsonProperty("currency_rate") BigDecimal currencyRate;
    @JsonProperty("date") String date;
    @JsonProperty("payer")
    UserResponseDTO payer;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static LoanResponseDTO from(TransactionEntity loan) {
        return LoanResponseDTO.builder()
                .uuid(loan.getUuid().toString())
                .group(GroupResponseDTO.from(loan.getGroup()))
                .title(loan.getTitle())
                .amount(loan.getAmount())
                .currencyCode(loan.getCurrencyCode())
                .currencyRate(loan.getCurrencyRate())
                .date(convertToTokyoTime(loan.getDate()))
                .payer(UserResponseDTO.from(loan.getPayer()))
                .createdAt(convertToTokyoTime(loan.getCreatedAt()))
                .updatedAt(convertToTokyoTime(loan.getUpdatedAt()))
                .build();
    }
}

