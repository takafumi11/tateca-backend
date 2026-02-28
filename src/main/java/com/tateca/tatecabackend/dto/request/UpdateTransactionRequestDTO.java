package com.tateca.tatecabackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tateca.tatecabackend.constants.BusinessConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateTransactionRequestDTO(
        @NotBlank(message = "Title is required and cannot be blank")
        @Size(max = 50, message = "Title must not exceed 50 characters")
        @JsonProperty("title")
        String title,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than 0")
        @JsonProperty("amount")
        Integer amount,

        @NotBlank(message = "Currency code is required and cannot be blank")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be exactly 3 uppercase letters")
        @JsonProperty("currency_code")
        String currencyCode,

        @NotBlank(message = "Date is required and cannot be blank")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([+-]\\d{2}:\\d{2}|Z)$",
                message = "Date must be in ISO 8601 format with timezone (e.g., 2024-01-15T18:30:00+09:00 or 2024-01-15T09:30:00Z)")
        @JsonProperty("date_str")
        String dateStr,

        @NotNull(message = "Payer ID is required")
        @JsonProperty("payer_id")
        UUID payerId,

        @NotNull(message = "Loan details are required")
        @Valid
        @JsonProperty("loan")
        Loan loan
) {
    public record Loan(
            @NotNull(message = "Obligations are required for loan transactions")
            @NotEmpty(message = "Obligations list cannot be empty")
            @Size(max = BusinessConstants.MAX_TRANSACTION_OBLIGATIONS, message = "Obligations list must not exceed " + BusinessConstants.MAX_TRANSACTION_OBLIGATIONS + " items")
            @Valid
            @JsonProperty("obligations")
            List<Obligation> obligations
    ) {
        public record Obligation(
                @NotNull(message = "Obligation amount is required")
                @Positive(message = "Obligation amount must be greater than 0")
                @JsonProperty("amount")
                Integer amount,

                @NotNull(message = "User UUID is required for obligation")
                @JsonProperty("user_uuid")
                UUID userUuid
        ) {}
    }
}
