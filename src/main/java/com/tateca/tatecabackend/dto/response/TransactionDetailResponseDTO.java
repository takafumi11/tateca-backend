package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Data
@Builder
public class TransactionDetailResponseDTO {
    @JsonProperty("transaction_id") String id;
    @JsonProperty("transaction_type") TransactionType transactionType;
    String title;
    long amount;
    UserInfoDTO payer;
    ExchangeRateResponseDTO exchangeRate;
    @JsonProperty("date_str") String dateStr;
    @JsonProperty("loan")
    LoanDetailResponseDTO loan;
    @JsonProperty("repayment")
    RepaymentDetailResponseDTO repayment;

    public static TransactionDetailResponseDTO from(TransactionHistoryEntity transaction, List<TransactionObligationEntity> transactionObligationEntityList) {
        return getCommonTransactionBuilder(transaction)
                .loan(LoanDetailResponseDTO.from(transactionObligationEntityList))
                .repayment(null)
                .build();
    }

    public static TransactionDetailResponseDTO from(TransactionHistoryEntity transaction, TransactionObligationEntity obligation) {
        return getCommonTransactionBuilder(transaction)
                .loan(null)
                .repayment(RepaymentDetailResponseDTO.from(obligation.getUser()))
                .build();
    }

    private static TransactionDetailResponseDTOBuilder getCommonTransactionBuilder(TransactionHistoryEntity transaction) {
        return TransactionDetailResponseDTO.builder()
                .id(transaction.getUuid().toString())
                .transactionType(transaction.getTransactionType())
                .title(transaction.getTitle())
                .amount(transaction.getAmount())
                .payer(UserInfoDTO.from(transaction.getPayer()))
                .dateStr(convertToTokyoTime(transaction.getTransactionDate()))
                .exchangeRate(ExchangeRateResponseDTO.from(transaction.getExchangeRate()));
    }
}