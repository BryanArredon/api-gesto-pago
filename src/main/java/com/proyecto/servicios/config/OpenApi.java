package com.proyecto.servicios.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Prueba Servicios API",
        version = "1.0.0",
        description = "API de catalogos y autenticacion JWT",
        contact = @io.swagger.v3.oas.annotations.info.Contact(name = "Prueba Servicios")))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Access token obtenido en /auth/login. Enviar como: Bearer <token>")
public class OpenApi {

    private final String baseUrl;

    public OpenApi(@Value("${api.base-url:http://localhost:${server.port}}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url(baseUrl));
    }
}