package com.spring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Customizes the Swagger UI header (title, version, description).
 * Swagger works without this class — springdoc auto-configures everything.
 * This bean only replaces the default "OpenAPI definition / v0" header.
 *
 * Swagger UI : http://localhost:8080/swagger-ui.html
 * API JSON   : http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Hub API")
                        .version("v1.0")
                        .description("REST APIs for User Hub: country/state/city lookups, "
                                + "user registration, login and password reset.")
                        .contact(new Contact()
                                .name("Deeps B")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")));
    }
}
