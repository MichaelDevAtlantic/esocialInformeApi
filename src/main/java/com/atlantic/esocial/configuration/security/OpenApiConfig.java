package com.atlantic.esocial.configuration.security;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API Estagiários")
                .version("v1")
                .description("Documentação da API Estagiários com autenticação JWT")
                .contact(new Contact()
                    .name("Equipe de Desenvolvimento")
                    .email("dev@atlantic.com.br")
                )
                .license(new License()
                    .name("Licença Padrão")
                    .url("https://atlantic.com.br/licenca")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("SecurityScheme"))
            .components(new Components()
                .addSecuritySchemes("SecurityScheme",
                    new SecurityScheme()
                        .name("SecurityScheme")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("Autenticação via JWT Bearer Token")
                )
            );
    }
}