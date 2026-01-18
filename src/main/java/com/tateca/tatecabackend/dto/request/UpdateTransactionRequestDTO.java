package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.constants.BusinessConstants;
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

@Schema(description = "Request to update an existing LOAN transaction")
public record UpdateTransactionRequestDTO(
        @NotBlank(message = "Title is required and cannot be blank")
        @Size(max = 50, message = "Title must not exceed 50 characters")
        @JsonProperty("title")
        @Schema(description = "Transaction title or description", example = "Dinner at restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        @JsonProperty("amount")
        @Schema(description = "Transaction amount in cents", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer amount,

        @NotBlank(message = "Currency code is required and cannot be blank")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be exactly 3 uppercase letters")
        @JsonProperty("currency_code")
        @Schema(description = "Currency code (ISO 4217)", example = "JPY", requiredMode = Schema.RequiredMode.REQUIRED)
        String currencyCode,

        @NotBlank(message = "Date is required and cannot be blank")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([+-]\\d{2}:\\d{2}|Z)$",
                message = "Date must be in ISO 8601 format with timezone (e.g., 2024-01-15T18:30:00+09:00 or 2024-01-15T09:30:00Z)")
        @JsonProperty("date_str")
        @Schema(description = "Transaction date in ISO 8601 format with timezone",
                example = "2024-01-15T18:30:00+09:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String dateStr,

        @NotNull(message = "Payer ID is required")
        @JsonProperty("payer_id")
        @Schema(description = "UUID of the user who paid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID payerId,

        @NotNull(message = "Loan details are required")
        @Valid
        @JsonProperty("loan")
        @Schema(description = "Loan details specifying obligations", requiredMode = Schema.RequiredMode.REQUIRED)
        Loan loan
) {
    @Schema(description = "Loan details for transaction update")
    public record Loan(
            @NotNull(message = "Obligations are required for loan transactions")
            @NotEmpty(message = "Obligations list cannot be empty")
            @Size(max = BusinessConstants.MAX_TRANSACTION_OBLIGATIONS, message = "Obligations list must not exceed " + BusinessConstants.MAX_TRANSACTION_OBLIGATIONS + " items")
            @Valid
            @JsonProperty("obligations")
            @Schema(description = "List of loan obligations specifying who owes what amount (max " + BusinessConstants.MAX_TRANSACTION_OBLIGATIONS + ")", requiredMode = Schema.RequiredMode.REQUIRED)
            List<Obligation> obligations
    ) {
        @Schema(description = "Loan obligation specifying user and amount")
        public record Obligation(
                @NotNull(message = "Obligation amount is required")
                @Positive(message = "Obligation amount must be greater than 0")
                @JsonProperty("amount")
                @Schema(description = "Amount owed in cents", example = "2500", requiredMode = Schema.RequiredMode.REQUIRED)
                Integer amount,

                @NotNull(message = "User UUID is required for obligation")
                @JsonProperty("user_uuid")
                @Schema(description = "UUID of the user who owes this amount", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
                UUID userUuid
        ) {}
    }
}
