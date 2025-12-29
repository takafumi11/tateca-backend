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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for TransactionController.
 *
 * <p>Tests verify OpenAPI contract compliance: response schema, field naming conventions,
 * HTTP status codes, and error response formats. Uses mocked service layer to test
 * controller and serialization logic in isolation.</p>
 *
 * <p>Business logic and realistic scenarios are tested in TransactionServiceIntegrationTest.</p>
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
        @DisplayName("OpenAPI Contract Validation")
        class OpenApiContractValidation {

            @Test
            @DisplayName("Should follow OpenAPI response schema with all required fields")
            void shouldFollowOpenApiSchema() throws Exception {
                // Given: Settlement response
                UUID groupId = UUID.randomUUID();
                UserResponseDTO user1 = createUser(UUID.randomUUID(), "User1");
                UserResponseDTO user2 = createUser(UUID.randomUUID(), "User2");

                TransactionSettlement settlement = new TransactionSettlement(user1, user2, 1234);
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of(settlement)
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should match OpenAPI schema structure
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        // Root level: transactions_settlement array
                        .andExpect(jsonPath("$.transactions_settlement").isArray())
                        // Transaction settlement object structure
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
                UserResponseDTO user1 = createUser(UUID.randomUUID(), "User1");
                UserResponseDTO user2 = createUser(UUID.randomUUID(), "User2");

                TransactionSettlement settlement = new TransactionSettlement(user1, user2, 5000);
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
            @DisplayName("Should return empty array when no settlements")
            void shouldReturnEmptyArrayWhenNoSettlements() throws Exception {
                // Given: Empty settlements
                UUID groupId = UUID.randomUUID();
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should return empty array (not null)
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactions_settlement").isArray())
                        .andExpect(jsonPath("$.transactions_settlement").isEmpty());
            }

            @Test
            @DisplayName("Should accept UUID in path parameter")
            void shouldAcceptUuidInPathParameter() throws Exception {
                // Given: Standard UUID format
                UUID groupId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
                TransactionsSettlementResponseDTO response = new TransactionsSettlementResponseDTO(
                        List.of()
                );

                when(transactionService.getSettlements(groupId)).thenReturn(response);

                // When & Then: Should accept and parse UUID correctly
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("Error Response Format")
        class ErrorResponseFormat {

            @Test
            @DisplayName("Should return 404 with error message when group not found")
            void shouldReturn404WhenGroupNotFound() throws Exception {
                // Given: Service throws NOT_FOUND exception
                UUID groupId = UUID.randomUUID();

                when(transactionService.getSettlements(groupId))
                        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

                // When & Then: Should return 404 with error response format
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value(404))
                        .andExpect(jsonPath("$.message").exists());
            }

            @Test
            @DisplayName("Should return 500 with error message when internal server error")
            void shouldReturn500WhenInternalServerError() throws Exception {
                // Given: Service throws internal error
                UUID groupId = UUID.randomUUID();

                when(transactionService.getSettlements(groupId))
                        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database connection failed"));

                // When & Then: Should return 500 with error response format
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, groupId))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.message").exists());
            }

            @Test
            @DisplayName("Should return 400 when UUID format is invalid")
            void shouldReturn400WhenInvalidUUIDFormat() throws Exception {
                // Given: Invalid UUID format
                String invalidUUID = "not-a-valid-uuid";

                // When & Then: Should return 400 without calling service
                mockMvc.perform(get(BASE_PATH + "/{groupId}" + SETTLEMENT_PATH, invalidUUID))
                        .andExpect(status().isBadRequest());

                verify(transactionService, never()).getSettlements(any());
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
