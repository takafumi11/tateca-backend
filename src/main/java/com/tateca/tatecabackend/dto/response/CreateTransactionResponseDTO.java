package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.ExchangeRateResponse;
import com.tateca.tatecabackend.dto.response.internal.LoanResponse;
import com.tateca.tatecabackend.dto.response.internal.RepaymentResponse;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static com.tateca.tatecabackend.util.TimeHelper.convertToTokyoTime;

@Schema(description = "Detailed transaction information")
public record CreateTransactionResponseDTO(
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

        @JsonProperty("exchange_rate")
        @Schema(description = "Exchange rate at the time of transaction")
        ExchangeRateResponse exchangeRateResponse,

        @JsonProperty("date_str")
        @Schema(description = "Transaction date (Tokyo time)", example = "2024-01-15T18:30:00+09:00")
        String dateStr,

        @JsonProperty("loan")
        @Schema(description = "Loan details (present if transaction_type is LOAN)")
        LoanResponse loan,

        @JsonProperty("repayment")
        @Schema(description = "Repayment details (present if transaction_type is REPAYMENT)")
        RepaymentResponse repayment
) {
    public static CreateTransactionResponseDTO from(TransactionHistoryEntity transaction,
                                                    List<TransactionObligationEntity> transactionObligationEntityList) {
        return createFromTransaction(
                transaction,
                LoanResponse.from(transactionObligationEntityList),
                null
        );
    }

    public static CreateTransactionResponseDTO from(TransactionHistoryEntity transaction,
                                                    TransactionObligationEntity obligation) {
        return createFromTransaction(
                transaction,
                null,
                RepaymentResponse.from(obligation.getUser())
        );
    }

    private static CreateTransactionResponseDTO createFromTransaction(TransactionHistoryEntity transaction,
                                                                      LoanResponse loan,
                                                                      RepaymentResponse repayment) {
        return new CreateTransactionResponseDTO(
                transaction.getUuid().toString(),
                transaction.getTransactionType(),
                transaction.getTitle(),
                transaction.getAmount(),
                UserResponseDTO.from(transaction.getPayer()),
                ExchangeRateResponse.from(transaction.getExchangeRate()),
                convertToTokyoTime(transaction.getTransactionDate()),
                loan,
                repayment
        );
    }
}
