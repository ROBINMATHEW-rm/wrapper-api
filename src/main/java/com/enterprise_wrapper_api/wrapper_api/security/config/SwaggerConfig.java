package com.enterprise_wrapper_api.wrapper_api.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise RAG Wrapper API")
                        .version("1.0.0")
                        .description(
                            "Production-ready RAG (Retrieval-Augmented Generation) API with JWT authentication.\n\n" +
                            "**How to authenticate:**\n" +
                            "1. Call `POST /auth/register` to create an account\n" +
                            "2. Call `POST /auth/login` to get a JWT token\n" +
                            "3. Click the **Authorize** button and paste the token\n" +
                            "4. All RAG endpoints will now work"
                        )
                        .contact(new Contact()
                                .name("RAG API")
                                .email("support@enterprise-rag.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local Development")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here (without 'Bearer ' prefix)")));
    }
}
