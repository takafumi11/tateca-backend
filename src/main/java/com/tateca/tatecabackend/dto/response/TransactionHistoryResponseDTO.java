package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.LoanEntity;
import com.tateca.tatecabackend.entity.RepaymentEntity;
import com.tateca.tatecabackend.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Data
@Builder
public class TransactionHistoryResponseDTO {
    @JsonProperty("transaction_id") String id;
    @JsonProperty("transaction_type")
    TransactionType transactionType;
    @JsonProperty("title") String title;
    @JsonProperty("amount") Integer amount;
    @JsonProperty("currency_code") String currencyCode;
    @JsonProperty("currency_rate") BigDecimal currencyRate;
    @JsonProperty("date") String date;
    @JsonProperty("payer")
    UserResponseDTO payer;
    @JsonProperty("target_user")
    UserResponseDTO targetUser;
    @JsonProperty("created_at") String createdAt;

    public static TransactionHistoryResponseDTO from(LoanEntity loan) {
        return TransactionHistoryResponseDTO.builder()
                .id(loan.getUuid().toString())
                .transactionType(TransactionType.LOAN)
                .title(loan.getTitle())
                .amount(loan.getAmount())
                .currencyCode(loan.getCurrencyCode())
                .currencyRate(loan.getCurrencyRate())
                .date(convertToTokyoTime(loan.getDate()))
                .payer(UserResponseDTO.from(loan.getPayer()))
                .targetUser(null)
                .createdAt(convertToTokyoTime(loan.getCreatedAt()))
                .build();
    }

    public static TransactionHistoryResponseDTO from(RepaymentEntity repayment) {
        return TransactionHistoryResponseDTO.builder()
                .id(repayment.getUuid().toString())
                .transactionType(TransactionType.REPAYMENT)
                .title(repayment.getTitle())
                .amount(repayment.getAmount())
                .currencyCode(repayment.getCurrencyCode())
                .currencyRate(repayment.getCurrencyRate())
                .date(convertToTokyoTime(repayment.getDate()))
                .payer(UserResponseDTO.from(repayment.getPayer()))
                .targetUser(UserResponseDTO.from(repayment.getRecipientUser()))
                .createdAt(convertToTokyoTime(repayment.getCreatedAt()))
                .build();
    }
}
