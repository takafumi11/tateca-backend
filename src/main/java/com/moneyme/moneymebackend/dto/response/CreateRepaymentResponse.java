package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.GroupResponseModel;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import lombok.Builder;

import java.math.BigDecimal;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Builder
public class CreateRepaymentResponse {
    @JsonProperty("uuid") String uuid;
    @JsonProperty("group") GroupResponseModel group;
    @JsonProperty("title") String title;
    @JsonProperty("amount") BigDecimal amount;
    @JsonProperty("payer")
    UserResponse payer;
    @JsonProperty("recipient")
    UserResponse recipient;
    @JsonProperty("detail") String detail;
    @JsonProperty("created_at") String createdAt;
    @JsonProperty("updated_at") String updatedAt;

    public static CreateRepaymentResponse from(RepaymentEntity repayment) {
        return CreateRepaymentResponse.builder()
                .uuid(repayment.getUuid().toString())
                .group(GroupResponseModel.from(repayment.getGroup()))
                .title(repayment.getTitle())
                .amount(repayment.getAmount())
                .payer(UserResponse.from(repayment.getPayer()))
                .recipient(UserResponse.from(repayment.getRecipientUser()))
                .detail(repayment.getDetail())
                .createdAt(convertToTokyoTime(repayment.getCreatedAt()))
                .updatedAt(convertToTokyoTime(repayment.getUpdatedAt()))
                .build();
    }

}
