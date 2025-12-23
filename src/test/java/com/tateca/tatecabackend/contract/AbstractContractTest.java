package com.tateca.tatecabackend.contract;

import com.tateca.tatecabackend.controller.AbstractControllerIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * Base class for Contract Tests.
 *
 * <p>Contract tests verify that API responses match the OpenAPI specification.
 * They ensure consistency between documentation and implementation.</p>
 *
 * <h3>Test Strategy:</h3>
 * <ul>
 *   <li>Use REST Assured with MockMvc for fast in-memory testing</li>
 *   <li>Validate response structure against OpenAPI schemas</li>
 *   <li>Test both success and error scenarios</li>
 *   <li>Verify HTTP status codes match specification</li>
 * </ul>
 */
public abstract class AbstractContractTest extends AbstractControllerIntegrationTest {

    @LocalServerPort
    protected int port;

    /**
     * Base path for API endpoints.
     */
    protected static final String BASE_PATH = "";

    /**
     * Test authentication header value.
     */
    protected static final String TEST_AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUpRestAssured() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        RestAssured.port = port;
        RestAssured.basePath = BASE_PATH;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Creates a MockMvc request specification with authentication.
     *
     * @return MockMvcRequestSpecification with auth headers
     */
    protected MockMvcRequestSpecification givenAuthenticatedRequest() {
        return RestAssuredMockMvc.given()
                .header("Authorization", TEST_AUTH_HEADER)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /**
     * Creates a MockMvc request specification without authentication.
     *
     * @return MockMvcRequestSpecification without auth headers
     */
    protected MockMvcRequestSpecification givenUnauthenticatedRequest() {
        return RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
