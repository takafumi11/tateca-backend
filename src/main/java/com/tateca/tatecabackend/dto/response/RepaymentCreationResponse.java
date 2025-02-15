package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.ObligationEntity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Data
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

    public static RepaymentCreationResponse from(ObligationEntity repayment) {
        return RepaymentCreationResponse.builder()
                .uuid(repayment.getUuid().toString())
                .group(GroupResponseDTO.from(repayment.getTransaction().getGroup()))
                .title(repayment.getTransaction().getTitle())
                .amount(repayment.getAmount())
                // TODO: It is wront info. Can be updated
                .currencyCode("JPY")
                // TODO: It it wrong info. Can be updated.
                .currencyRate(BigDecimal.ONE)
                // TODO: It it wrong info. Can be updated.
                .date(convertToTokyoTime(repayment.getTransaction().getCreatedAt()))
                .payer(UserResponseDTO.from(repayment.getTransaction().getPayer()))
                .recipient(UserResponseDTO.from(repayment.getUser()))
                .createdAt(convertToTokyoTime(repayment.getCreatedAt()))
                .updatedAt(convertToTokyoTime(repayment.getUpdatedAt()))
                .build();
    }

}
