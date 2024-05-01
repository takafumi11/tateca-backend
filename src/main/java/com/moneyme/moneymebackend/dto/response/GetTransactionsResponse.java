package com.moneyme.moneymebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moneyme.moneymebackend.dto.model.GroupResponseModel;
import com.moneyme.moneymebackend.dto.model.TransactionType;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Data
@Builder
public class GetTransactionsResponse {
    @JsonProperty("transaction_id") String id;
    @JsonProperty("transaction_type")
    TransactionType transactionType;
    @JsonProperty("title") String title;
    @JsonProperty("amount")
    BigDecimal amount;
    @JsonProperty("date") String date;
    @JsonProperty("payer") UserResponse payer;
    @JsonProperty("target_user") UserResponse targetUser;
    @JsonProperty("created_at") String createdAt;

    public static GetTransactionsResponse from(LoanEntity loan) {
        return GetTransactionsResponse.builder()
                .id(loan.getUuid().toString())
                .transactionType(TransactionType.LOAN)
                .title(loan.getTitle())
                .amount(loan.getAmount())
                .date(convertToTokyoTime(loan.getDate()))
                .payer(UserResponse.from(loan.getPayer()))
                .targetUser(null)
                .createdAt(convertToTokyoTime(loan.getCreatedAt()))
                .build();
    }

    public static GetTransactionsResponse from(RepaymentEntity repayment) {
        return GetTransactionsResponse.builder()
                .id(repayment.getUuid().toString())
                .transactionType(TransactionType.REPAYMENT)
                .title(repayment.getTitle())
                .amount(repayment.getAmount())
                .date(convertToTokyoTime(repayment.getDate()))
                .payer(UserResponse.from(repayment.getPayer()))
                .targetUser(UserResponse.from(repayment.getRecipientUser()))
                .createdAt(convertToTokyoTime(repayment.getCreatedAt()))
                .build();
    }

}