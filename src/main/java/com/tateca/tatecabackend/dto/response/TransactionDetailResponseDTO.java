package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static com.tateca.tatecabackend.util.TimeHelper.convertToTokyoTime;

@Schema(description = "Detailed transaction information")
public record TransactionDetailResponseDTO(
        @JsonProperty("transaction_id")
        @Schema(description = "Transaction unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @JsonProperty("transaction_type")
        @Schema(description = "Type of transaction", example = "LOAN")
        TransactionType transactionType,

        @Schema(description = "Transaction title", example = "Dinner at restaurant")
        String title,

        @Schema(description = "Transaction amount in cents", example = "5000")
        long amount,

        @Schema(description = "User who made the payment")
        UserResponseDTO payer,

        @Schema(description = "Exchange rate at the time of transaction")
        ExchangeRateResponseDTO exchangeRate,

        @JsonProperty("date_str")
        @Schema(description = "Transaction date (Tokyo time)", example = "2024-01-15T18:30:00+09:00")
        String dateStr,

        @JsonProperty("loan")
        @Schema(description = "Loan details (present if transaction_type is LOAN)")
        LoanDetailResponseDTO loan,

        @JsonProperty("repayment")
        @Schema(description = "Repayment details (present if transaction_type is REPAYMENT)")
        RepaymentDetailResponseDTO repayment
) {
    public static TransactionDetailResponseDTO from(TransactionHistoryEntity transaction,
                                                     List<TransactionObligationEntity> transactionObligationEntityList) {
        return createFromTransaction(
                transaction,
                LoanDetailResponseDTO.from(transactionObligationEntityList),
                null
        );
    }

    public static TransactionDetailResponseDTO from(TransactionHistoryEntity transaction,
                                                     TransactionObligationEntity obligation) {
        return createFromTransaction(
                transaction,
                null,
                RepaymentDetailResponseDTO.from(obligation.getUser())
        );
    }

    private static TransactionDetailResponseDTO createFromTransaction(TransactionHistoryEntity transaction,
                                                                       LoanDetailResponseDTO loan,
                                                                       RepaymentDetailResponseDTO repayment) {
        return new TransactionDetailResponseDTO(
                transaction.getUuid().toString(),
                transaction.getTransactionType(),
                transaction.getTitle(),
                transaction.getAmount(),
                UserResponseDTO.from(transaction.getPayer()),
                ExchangeRateResponseDTO.from(transaction.getExchangeRate()),
                convertToTokyoTime(transaction.getTransactionDate()),
                loan,
                repayment
        );
    }
}
