package com.tateca.tatecabackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.dto.response.internal.ExchangeRateResponse;
import com.tateca.tatecabackend.dto.response.internal.LoanResponse;
import com.tateca.tatecabackend.dto.response.internal.RepaymentResponse;
import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;

import java.util.List;

import static com.tateca.tatecabackend.util.TimeHelper.convertToTokyoTime;

public record CreateTransactionResponseDTO(
        @JsonProperty("transaction_id")
        String id,

        @JsonProperty("transaction_type")
        TransactionType transactionType,

        String title,

        long amount,

        UserResponseDTO payer,

        @JsonProperty("exchange_rate")
        ExchangeRateResponse exchangeRateResponse,

        @JsonProperty("date_str")
        String dateStr,

        @JsonProperty("loan")
        LoanResponse loan,

        @JsonProperty("repayment")
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
