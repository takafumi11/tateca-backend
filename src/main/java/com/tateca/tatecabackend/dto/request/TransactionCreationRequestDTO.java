package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Request to create a new transaction")
public record TransactionCreationRequestDTO(
        @JsonProperty("transaction_type")
        @Schema(description = "Type of transaction (LOAN, REPAYMENT)", example = "LOAN")
        TransactionType transactionType,

        @JsonProperty("title")
        @Schema(description = "Transaction title or description", example = "Dinner at restaurant")
        String title,

        @JsonProperty("amount")
        @Schema(description = "Transaction amount in cents", example = "5000")
        int amount,

        @JsonProperty("currency_code")
        @Schema(description = "Currency code (ISO 4217)", example = "JPY")
        String currencyCode,

        @JsonProperty("date_str")
        @Schema(description = "Transaction date", example = "2024-01-15")
        String dateStr,

        @JsonProperty("payer_id")
        @Schema(description = "UUID of the user who paid", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID payerId,

        @JsonProperty("loan")
        @Schema(description = "Loan details (required if transaction_type is LOAN)")
        LoanCreationRequest loanRequest,

        @JsonProperty("repayment")
        @Schema(description = "Repayment details (required if transaction_type is REPAYMENT)")
        RepaymentCreationRequest repaymentRequest
) {
}
