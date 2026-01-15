package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.dto.response.CreateTransactionResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionHistoryResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionSettlementResponseDTO;
import com.tateca.tatecabackend.exception.ErrorResponse;
import com.tateca.tatecabackend.service.TransactionService;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/groups/{groupId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Transactions", description = "Transaction management operations")
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService service;

    @GetMapping("/history")
    @Operation(summary = "Get transaction history for a group", description = "Retrieve transaction history with optional count parameter (default: 5)")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transaction history retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid UUID format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Invalid UUID format",
                    description = "When groupId path parameter has invalid UUID format",
                    value = """
                        {
                          "status": 400,
                          "message": "Invalid format for parameter 'groupId': expected UUID"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group not found"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<TransactionHistoryResponseDTO> getTransactionHistory(
            @RequestParam(defaultValue = "5") int count,
            @PathVariable UUID groupId
    ) {
        TransactionHistoryResponseDTO response = service.getTransactionHistory(count, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settlement")
    @Operation(summary = "Get settlement information for a group in JPY")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Settlement information retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid UUID format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Invalid UUID format",
                    description = "When groupId path parameter has invalid UUID format",
                    value = """
                        {
                          "status": 400,
                          "message": "Invalid format for parameter 'groupId': expected UUID"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group not found"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<TransactionSettlementResponseDTO> getTransactionSettlement(
            @PathVariable UUID groupId
    ) {
        TransactionSettlementResponseDTO response = service.getSettlements(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new transaction")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Transaction created successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Transaction type is null",
                        description = "When transaction_type is null",
                        value = """
                            {
                              "status": 400,
                              "message": "transactionType: Transaction type is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Title is blank or null",
                        description = "When title is null, empty string, or only whitespace",
                        value = """
                            {
                              "status": 400,
                              "message": "title: Title is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Title exceeds maximum length",
                        description = "When title exceeds 50 characters",
                        value = """
                            {
                              "status": 400,
                              "message": "title: Title must not exceed 50 characters"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Amount is null, zero, or negative",
                        description = "When amount is null, zero, or negative number",
                        value = """
                            {
                              "status": 400,
                              "message": "amount: Amount must be a positive number"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Currency code is invalid",
                        description = "When currency_code is blank, not uppercase, or not 3 characters",
                        value = """
                            {
                              "status": 400,
                              "message": "currencyCode: Currency code must be 3 uppercase letters (ISO 4217)"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Date format is invalid",
                        description = "When date_str is not ISO 8601 format with timezone (e.g., 2024-01-15T18:30:00+09:00)",
                        value = """
                            {
                              "status": 400,
                              "message": "dateStr: Date must be in ISO 8601 format with timezone"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Payer ID is null",
                        description = "When payer_id is null",
                        value = """
                            {
                              "status": 400,
                              "message": "payerId: Payer ID is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "LOAN without loan details",
                        description = "When transaction_type is LOAN but loan field is null",
                        value = """
                            {
                              "status": 400,
                              "message": "LOAN transaction must have loan details"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "REPAYMENT without repayment details",
                        description = "When transaction_type is REPAYMENT but repayment field is null",
                        value = """
                            {
                              "status": 400,
                              "message": "REPAYMENT transaction must have repayment details"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Obligations list is empty or exceeds maximum",
                        description = "When obligations list is empty or has more than 8 items",
                        value = """
                            {
                              "status": 400,
                              "message": "obligations: Obligations must have between 1 and 8 items"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Obligation amount is invalid",
                        description = "When obligation amount is null, zero, or negative",
                        value = """
                            {
                              "status": 400,
                              "message": "obligations[].amount: Amount must be a positive number"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Obligation user UUID is null",
                        description = "When obligation user_uuid is null",
                        value = """
                            {
                              "status": 400,
                              "message": "obligations[].userUuid: User UUID is required"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group not found"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Unsupported Media Type - Content-Type header must be application/json",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 415,
                          "message": "Unsupported Media Type. Content-Type must be application/json"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<CreateTransactionResponseDTO> createTransaction(
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody CreateTransactionRequestDTO request
    ) {
        logger.info("Creating transaction: groupId={}, type={}, amount={}, currency={}",
                PiiMaskingUtil.maskUuid(groupId),
                request.transactionType(),
                request.amount(),
                request.currencyCode());

        CreateTransactionResponseDTO response = service.createTransaction(groupId, request);

        if (response != null && response.id() != null) {
            logger.info("Transaction created successfully: transactionId={}, groupId={}",
                    PiiMaskingUtil.maskUuid(UUID.fromString(response.id())),
                    PiiMaskingUtil.maskUuid(groupId));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details by ID")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transaction details retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid UUID format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid groupId UUID format",
                        description = "When groupId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'groupId': expected UUID"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid transactionId UUID format",
                        description = "When transactionId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'transactionId': expected UUID"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Transaction not found"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<CreateTransactionResponseDTO> getTransactionDetail(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId
    ) {
        CreateTransactionResponseDTO response = service.getTransactionDetail(transactionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Delete a transaction")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Transaction deleted successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid UUID format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid groupId UUID format",
                        description = "When groupId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'groupId': expected UUID"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid transactionId UUID format",
                        description = "When transactionId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'transactionId': expected UUID"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User doesn't have permission to delete this transaction",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 403,
                          "message": "Forbidden"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Transaction not found"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Void> deleteLoan(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("transactionId") UUID transactionId
    ) {
        logger.info("Deleting transaction: transactionId={}, groupId={}",
                PiiMaskingUtil.maskUuid(transactionId),
                PiiMaskingUtil.maskUuid(groupId));

        service.deleteTransaction(transactionId);

        logger.info("Transaction deleted successfully: transactionId={}, groupId={}",
                PiiMaskingUtil.maskUuid(transactionId),
                PiiMaskingUtil.maskUuid(groupId));

        return ResponseEntity.noContent().build();
    }
}
