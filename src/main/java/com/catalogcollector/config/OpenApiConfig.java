package com.catalogcollector.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Catalog API",
                version = "0.1.0",
                description = "Barcode-scanning collectibles catalog API. "
                        + "Scan barcodes, resolve product metadata, and manage collections.",
                contact = @Contact(
                        name = "FigureCollecting",
                        url = "https://github.com/FigureCollecting/catalog-api"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT authentication token"
)
public class OpenApiConfig {
}
