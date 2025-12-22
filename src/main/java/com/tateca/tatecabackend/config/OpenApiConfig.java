package com.tateca.tatecabackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
                .description("グループ経費管理API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Tateca Team")
                    .url("https://tateca.net")))
            .servers(List.of(
                new Server().url("https://api.tateca.net").description("本番環境"),
                new Server().url("https://staging-api.tateca.net").description("ステージング環境"),
                new Server().url("http://localhost:8080").description("ローカル開発環境")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("tateca-api")
            .pathsToMatch("/**")
            .build();
    }
}
