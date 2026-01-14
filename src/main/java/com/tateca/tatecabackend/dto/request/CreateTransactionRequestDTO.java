package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.constants.BusinessConstants;
import com.tateca.tatecabackend.model.TransactionType;
import com.tateca.tatecabackend.validation.ValidTransactionDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new transaction")
@ValidTransactionDetails
public record CreateTransactionRequestDTO(
        @NotNull(message = "{validation.transaction.type.required}")
        @JsonProperty("transaction_type")
        @Schema(description = "Type of transaction (LOAN, REPAYMENT)", example = "LOAN", requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionType transactionType,

        @NotBlank(message = "{validation.transaction.title.required}")
        @Size(max = 50, message = "{validation.transaction.title.size}")
        @JsonProperty("title")
        @Schema(description = "Transaction title or description", example = "Dinner at restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotNull(message = "{validation.transaction.amount.required}")
        @Positive(message = "{validation.transaction.amount.positive}")
        @JsonProperty("amount")
        @Schema(description = "Transaction amount in cents", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer amount,

        @NotBlank(message = "{validation.transaction.currencyCode.required}")
        @Pattern(regexp = "^[A-Z]{3}$", message = "{validation.transaction.currencyCode.pattern}")
        @JsonProperty("currency_code")
        @Schema(description = "Currency code (ISO 4217)", example = "JPY", requiredMode = Schema.RequiredMode.REQUIRED)
        String currencyCode,

        @NotBlank(message = "{validation.transaction.date.required}")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([+-]\\d{2}:\\d{2}|Z)$",
                message = "{validation.transaction.date.pattern}")
        @JsonProperty("date_str")
        @Schema(description = "Transaction date in ISO 8601 format with timezone",
                example = "2024-01-15T18:30:00+09:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String dateStr,

        @NotNull(message = "{validation.transaction.payerId.required}")
        @JsonProperty("payer_id")
        @Schema(description = "UUID of the user who paid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID payerId,

        @Valid
        @JsonProperty("loan")
        @Schema(description = "Loan details (required if transaction_type is LOAN)")
        Loan loan,

        @Valid
        @JsonProperty("repayment")
        @Schema(description = "Repayment details (required if transaction_type is REPAYMENT)")
        Repayment repayment
) {
    @Schema(description = "Loan details for transaction creation")
    public record Loan(
            @NotNull(message = "{validation.transaction.loan.obligations.required}")
            @NotEmpty(message = "{validation.transaction.loan.obligations.notEmpty}")
            @Size(max = BusinessConstants.MAX_TRANSACTION_OBLIGATIONS, message = "{validation.transaction.loan.obligations.size}")
            @Valid
            @JsonProperty("obligations")
            @Schema(description = "List of loan obligations specifying who owes what amount (max " + BusinessConstants.MAX_TRANSACTION_OBLIGATIONS + ")", requiredMode = Schema.RequiredMode.REQUIRED)
            List<Obligation> obligations
    ) {
        @Schema(description = "Loan obligation specifying user and amount")
        public record Obligation(
                @NotNull(message = "{validation.transaction.loan.obligation.amount.required}")
                @Positive(message = "{validation.transaction.loan.obligation.amount.positive}")
                @JsonProperty("amount")
                @Schema(description = "Amount owed in cents", example = "2500", requiredMode = Schema.RequiredMode.REQUIRED)
                Integer amount,

                @NotNull(message = "{validation.transaction.loan.obligation.userUuid.required}")
                @JsonProperty("user_uuid")
                @Schema(description = "UUID of the user who owes this amount", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
                UUID userUuid
        ) {}
    }

    @Schema(description = "Repayment details for transaction creation")
    public record Repayment(
            @NotNull(message = "{validation.transaction.repayment.recipientId.required}")
            @JsonProperty("recipient_id")
            @Schema(description = "UUID of the user receiving the repayment", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
            UUID recipientId
    ) {}
}
