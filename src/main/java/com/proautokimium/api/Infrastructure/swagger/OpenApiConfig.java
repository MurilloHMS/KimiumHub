package com.proautokimium.api.Infrastructure.swagger;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI() {
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("FlexPro API")
                        .version("1.0.0")
                        .description("API para ferramentas internas da Proauto Kimium")
                        .contact(new Contact()
                                .name("MurilloHMS")
                                .url("https://murillohms.vercel.app/")
                                .email("murillo.henrique@prautokimium.com.br")
                        )
                )
                .addSecurityItem(securityRequirement);
    }
}
