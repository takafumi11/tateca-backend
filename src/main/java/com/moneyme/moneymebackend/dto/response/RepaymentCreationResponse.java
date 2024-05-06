package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import lombok.Builder;

import java.math.BigDecimal;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class RepaymentCreationResponse {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("group")
    GroupResponseDTO group;
    @JsonProperty("title") String title;
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("payer")
    UserResponseDTO payer;
    @JsonProperty("recipient")
    UserResponseDTO recipient;
    @JsonProperty("detail") String detail;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static RepaymentCreationResponse from(RepaymentEntity repayment) {
        return RepaymentCreationResponse.builder()
                .uuid(repayment.getUuid().toString())
                .group(GroupResponseDTO.from(repayment.getGroup()))
                .title(repayment.getTitle())
                .amount(repayment.getAmount())
                .payer(UserResponseDTO.from(repayment.getPayer()))
                .recipient(UserResponseDTO.from(repayment.getRecipientUser()))
                .detail(repayment.getDetail())
                .createdAt(convertToTokyoTime(repayment.getCreatedAt()))
                .updatedAt(convertToTokyoTime(repayment.getUpdatedAt()))
                .build();
    }

}
