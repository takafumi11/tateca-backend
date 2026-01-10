package com.tateca.tatecabackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tatecaOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Tateca API")
                .description("Group expense management API with multi-currency support. " +
                        "Track expenses, loans, and settlements across multiple users and currencies.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Tateca Team")
                    .url("https://tateca.net")
                    .email("support@tateca.net"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://tateca.net/terms")))
            .externalDocs(new ExternalDocumentation()
                .description("Full Documentation")
                .url("https://docs.tateca.net"))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://staging-api.tateca.net").description("Staging"),
                new Server().url("https://api.tateca.net").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Firebase JWT token for user authentication"))
                .addSecuritySchemes("ApiKeyAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("API Key for internal system operations (EventBridge + Lambda)")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("tateca-api")
            .pathsToMatch("/**")
            .build();
    }
}
