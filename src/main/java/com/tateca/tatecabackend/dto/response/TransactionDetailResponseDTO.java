package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;

@Data
@Builder
@Schema(description = "Detailed transaction information")
public class TransactionDetailResponseDTO {
    @JsonProperty("transaction_id")
    @Schema(description = "Transaction unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    String id;

    @JsonProperty("transaction_type")
    @Schema(description = "Type of transaction", example = "LOAN")
    TransactionType transactionType;

    @Schema(description = "Transaction title", example = "Dinner at restaurant")
    String title;

    @Schema(description = "Transaction amount in cents", example = "5000")
    long amount;

    @Schema(description = "User who made the payment")
    UserInfoDTO payer;

    @Schema(description = "Exchange rate at the time of transaction")
    ExchangeRateResponseDTO exchangeRate;

    @JsonProperty("date_str")
    @Schema(description = "Transaction date (Tokyo time)", example = "2024-01-15T18:30:00+09:00")
    String dateStr;

    @JsonProperty("loan")
    @Schema(description = "Loan details (present if transaction_type is LOAN)")
    LoanDetailResponseDTO loan;

    @JsonProperty("repayment")
    @Schema(description = "Repayment details (present if transaction_type is REPAYMENT)")
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