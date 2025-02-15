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

    public static LoanResponseDTO from(TransactionEntity transaction) {
        return LoanResponseDTO.builder()
                .uuid(transaction.getUuid().toString())
                .group(GroupResponseDTO.from(transaction.getGroup()))
                .title(transaction.getTitle())
                .amount(transaction.getAmount())
                // TODO: It is not correct. Needs to be updated.
                .currencyCode("JPY")
                // TODO: It is not correct. Needs to be updated.
                .currencyRate(BigDecimal.ONE)
                // TODO: It is not correct. Needs to be updated.
                .date(convertToTokyoTime(transaction.getCreatedAt()))
                .payer(UserResponseDTO.from(transaction.getPayer()))
                .createdAt(convertToTokyoTime(transaction.getCreatedAt()))
                .updatedAt(convertToTokyoTime(transaction.getUpdatedAt()))
                .build();
    }
}

