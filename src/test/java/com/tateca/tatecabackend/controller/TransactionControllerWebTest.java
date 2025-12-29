package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO;
import com.tateca.tatecabackend.dto.response.TransactionsSettlementResponseDTO.TransactionSettlement;
import com.tateca.tatecabackend.dto.response.UserResponseDTO;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for TransactionController.
 *
 * <p>Tests verify API contract, response format, status codes, and error handling.
 * Uses MockMvc with mocked service layer to test controller logic in isolation.</p>
 */
@WebMvcTest(TransactionController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("TransactionController Web Tests")
class TransactionControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    private static final String BASE_PATH = "/groups";
    private static final String SETTLEMENT_PATH = "/transactions/settlement";

    @Nested
    @DisplayName("GET /groups/{groupId}/transactions/settlement")
    class GetSettlement {

        @Nested
        @DisplayName("Given valid group with transactions")
        class GivenValidGroupWithTransactions {

            @Test
            @DisplayName("Then should return 200 OK with settlement transactions")
            void thenShouldReturn200WithSettlements() throws Exception {
                // Given: Valid group ID and service returns settlements
                UUID groupId = UUID.randomUUID();
                UserResponseDTO alice = createUser(UUID.randomUUID(), "Alice");
                UserResponseDTO bob = createUser(UUID.randomUUID(), "Bob");

                TransactionSettlement settlement = new TransactionSettlement(bob, alice, 5000);
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of(settlement)
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should return 200 with proper JSON structure
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.transactions_settlement").isArray())
                        .andExpect(jsonPath("$.transactions_settlement[0].from").exists())
                        .andExpect(jsonPath("$.transactions_settlement[0].from.uuid").value(bob.uuid()))
                        .andExpect(jsonPath("$.transactions_settlement[0].to").exists())
                        .andExpect(jsonPath("$.transactions_settlement[0].to.uuid").value(alice.uuid()))
                        .andExpect(jsonPath("$.transactions_settlement[0].amount").value(5000));

                verify(transactionService, times(1)).getSettlements(groupId);
            }

            @Test
            @DisplayName("Then should include all user details in response")
            void thenShouldIncludeAllUserDetails() throws Exception {
                // Given: Settlement with full user information
                UUID groupId = UUID.randomUUID();
                UserResponseDTO alice = createUser(UUID.randomUUID(), "Alice Johnson");
                UserResponseDTO bob = createUser(UUID.randomUUID(), "Bob Smith");

                TransactionSettlement settlement = new TransactionSettlement(bob, alice, 10000);
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of(settlement)
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should include user names
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactions_settlement[0].from.name").value("Bob Smith"))
                        .andExpect(jsonPath("$.transactions_settlement[0].to.name").value("Alice Johnson"));

                verify(transactionService, times(1)).getSettlements(groupId);
            }

            @Test
            @DisplayName("Then should handle multiple settlement transactions")
            void thenShouldHandleMultipleSettlements() throws Exception {
                // Given: Multiple settlements
                UUID groupId = UUID.randomUUID();
                UserResponseDTO alice = createUser(UUID.randomUUID(), "Alice");
                UserResponseDTO bob = createUser(UUID.randomUUID(), "Bob");
                UserResponseDTO carol = createUser(UUID.randomUUID(), "Carol");

                List<TransactionSettlement> settlements = List.of(
                        new TransactionSettlement(bob, alice, 5000),
                        new TransactionSettlement(carol, alice, 3000)
                );
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(settlements);

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should return all settlements in correct format
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactions_settlement").isArray())
                        .andExpect(jsonPath("$.transactions_settlement.length()").value(2))
                        .andExpect(jsonPath("$.transactions_settlement[0].amount").value(5000))
                        .andExpect(jsonPath("$.transactions_settlement[1].amount").value(3000));

                verify(transactionService, times(1)).getSettlements(groupId);
            }
        }

        @Nested
        @DisplayName("Given group with no transactions")
        class GivenGroupWithNoTransactions {

            @Test
            @DisplayName("Then should return 200 OK with empty settlement list")
            void thenShouldReturn200WithEmptyList() throws Exception {
                // Given: Group with no transactions
                UUID groupId = UUID.randomUUID();
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        new ArrayList<>()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should return empty array
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactions_settlement").isArray())
                        .andExpect(jsonPath("$.transactions_settlement").isEmpty());

                verify(transactionService, times(1)).getSettlements(groupId);
            }
        }

        @Nested
        @DisplayName("Given group with balanced accounts")
        class GivenGroupWithBalancedAccounts {

            @Test
            @DisplayName("Then should return 200 OK with empty settlement list")
            void thenShouldReturn200WithEmptyList() throws Exception {
                // Given: All accounts are balanced (no settlements needed)
                UUID groupId = UUID.randomUUID();
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should return empty settlements
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactions_settlement").isEmpty());

                verify(transactionService, times(1)).getSettlements(groupId);
            }
        }

        @Nested
        @DisplayName("Error Handling")
        class ErrorHandling {

            @Test
            @DisplayName("Should return 404 when group not found")
            void shouldReturn404WhenGroupNotFound() throws Exception {
                // Given: Service throws NOT_FOUND exception
                UUID groupId = UUID.randomUUID();

                when(transactionService.getSettlements(groupId))
                        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

                // When & Then: Should return 404
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404))
                        .andExpect(jsonPath("$.message").exists());

                verify(transactionService, times(1)).getSettlements(groupId);
            }

            @Test
            @DisplayName("Should return 500 when internal server error occurs")
            void shouldReturn500WhenInternalServerError() throws Exception {
                // Given: Service throws internal error
                UUID groupId = UUID.randomUUID();

                when(transactionService.getSettlements(groupId))
                        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database connection failed"));

                // When & Then: Should return 500
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.message").exists());

                verify(transactionService, times(1)).getSettlements(groupId);
            }

            @Test
            @DisplayName("Should return 400 when invalid UUID format")
            void shouldReturn400WhenInvalidUUIDFormat() throws Exception {
                // Given: Invalid UUID format
                String invalidUUID = "not-a-valid-uuid";

                // When & Then: Should return 400 without calling service
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, invalidUUID))
                        .andExpect(status().isBadRequest());

                verify(transactionService, never()).getSettlements(any());
            }
        }

        @Nested
        @DisplayName("API Contract Validation")
        class ApiContractValidation {

            @Test
            @DisplayName("Should return application/json content type")
            void shouldReturnJsonContentType() throws Exception {
                // Given: Valid request
                UUID groupId = UUID.randomUUID();
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should return JSON content type
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
            }

            @Test
            @DisplayName("Should follow OpenAPI response schema")
            void shouldFollowOpenApiSchema() throws Exception {
                // Given: Settlement response
                UUID groupId = UUID.randomUUID();
                UserResponseDTO alice = createUser(UUID.randomUUID(), "Alice");
                UserResponseDTO bob = createUser(UUID.randomUUID(), "Bob");

                TransactionSettlement settlement = new TransactionSettlement(bob, alice, 1234);
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of(settlement)
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should match expected schema structure
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        // Response root should have transactions_settlement array
                        .andExpect(jsonPath("$.transactions_settlement").isArray())
                        // Each transaction should have from, to, amount
                        .andExpect(jsonPath("$.transactions_settlement[0].from").isMap())
                        .andExpect(jsonPath("$.transactions_settlement[0].from.uuid").isString())
                        .andExpect(jsonPath("$.transactions_settlement[0].from.name").isString())
                        .andExpect(jsonPath("$.transactions_settlement[0].to").isMap())
                        .andExpect(jsonPath("$.transactions_settlement[0].to.uuid").isString())
                        .andExpect(jsonPath("$.transactions_settlement[0].to.name").isString())
                        .andExpect(jsonPath("$.transactions_settlement[0].amount").isNumber());
            }

            @Test
            @DisplayName("Should use snake_case for JSON property names")
            void shouldUseSnakeCaseForPropertyNames() throws Exception {
                // Given: Response with data
                UUID groupId = UUID.randomUUID();
                UserResponseDTO alice = createUser(UUID.randomUUID(), "Alice");
                UserResponseDTO bob = createUser(UUID.randomUUID(), "Bob");

                TransactionSettlement settlement = new TransactionSettlement(bob, alice, 5000);
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of(settlement)
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should use snake_case (transactions_settlement not transactionsSettlement)
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactions_settlement").exists())
                        .andExpect(jsonPath("$.transactionsSettlement").doesNotExist());
            }

            @Test
            @DisplayName("Should accept UUID in path parameter")
            void shouldAcceptUuidInPathParameter() throws Exception {
                // Given: Specific UUID format
                UUID groupId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should accept and parse UUID
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk());

                verify(transactionService, times(1)).getSettlements(groupId);
            }
        }

        @Nested
        @DisplayName("Service Integration")
        class ServiceIntegration {

            @Test
            @DisplayName("Should call service with correct group ID")
            void shouldCallServiceWithCorrectGroupId() throws Exception {
                // Given: Specific group ID
                UUID groupId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When: Calling endpoint
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk());

                // Then: Service should receive exact group ID
                verify(transactionService, times(1)).getSettlements(eq(groupId));
            }

            @Test
            @DisplayName("Should call service exactly once per request")
            void shouldCallServiceOncePerRequest() throws Exception {
                // Given: Valid group
                UUID groupId = UUID.randomUUID();
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When: Making request
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk());

                // Then: Service should be called exactly once
                verify(transactionService, times(1)).getSettlements(any());
            }
        }
    }

    // Helper methods

    private UserResponseDTO createUser(UUID uuid, String name) {
        return new UserResponseDTO(
                uuid.toString(),
                name,
                null,
                "2024-01-01T09:00:00+09:00",
                "2024-01-01T09:00:00+09:00"
        );
    }
}
