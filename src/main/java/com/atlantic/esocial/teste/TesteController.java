package com.atlantic.esocial.teste;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/teste")
@SecurityRequirement(name = "SecurityScheme")
@Tag(name = "E0 - Teste", description = "Endpoint simples")
public class TesteController {

    @Autowired
    TesteService testeService;

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    @Operation(summary = "S1 - Teste do endpoint")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Lista",
                    value = "{ \"status\": \"ok\",\"message\": \"\"}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Desautorizado",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "JWT Bearer Token Expirado",
                    value = "{ \"data\": null, \"status\": \"error\", \"message\": \"JWT expired 1865633 milliseconds ago at 2024-09-05T19:48:46.000Z. Current time: 2024-09-05T20:19:51.633Z. Allowed clock skew: 0 milliseconds.\" }"
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Erro interno no Servidor",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "Erro genérico de negócios",
                        value = "{ \"data\": null, \"status\": \"error\", \"message\": \"Erro na tentativa de recuperar dados de status.\" }"
                    ),
                    @ExampleObject(
                        name = "Erro geral",
                        value = "Stacktrace"
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "204", description = "Sem conteúdo",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Sem conteúdo",
                    value = ""
                )
            )
        )
    })
    public Map<String, Object> teste() {
        return testeService.gerar();
    }
}