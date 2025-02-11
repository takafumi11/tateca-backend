package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionEntity;
import lombok.Builder;

import java.math.BigDecimal;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class RepaymentCreationResponse {
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
    @JsonProperty("recipient")
    UserResponseDTO recipient;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static RepaymentCreationResponse from(TransactionEntity repayment) {
        return RepaymentCreationResponse.builder()
                .uuid(repayment.getUuid().toString())
                .group(GroupResponseDTO.from(repayment.getGroup()))
                .title(repayment.getTitle())
                .amount(repayment.getAmount())
                .currencyCode(repayment.getCurrencyCode())
                .currencyRate(repayment.getCurrencyRate())
                .date(convertToTokyoTime(repayment.getDate()))
                .payer(UserResponseDTO.from(repayment.getPayer()))
                .recipient(UserResponseDTO.from(repayment.getRecipient()))
                .createdAt(convertToTokyoTime(repayment.getCreatedAt()))
                .updatedAt(convertToTokyoTime(repayment.getUpdatedAt()))
                .build();
    }

}
