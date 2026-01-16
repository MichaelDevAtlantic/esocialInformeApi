package com.atlantic.esocial.feature.informeRendimentos;

import com.atlantic.esocial.feature.informeRendimentos.dto.ApiResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/comprovante")
@Tag(name = "Comprovante Rendimento", description = "Geração de comprovante de rendimento")
public class InformeRendimentosController {

    @Autowired
    private InformeRendimentosService service;

    /**
     * Gera comprovante em PDF e retorna base64.
     * Agora recebe cpf + anoRef.
     * Ex.: GET /api/v1/comprovante/generateByCpf?cpf=04202602854&anoRef=2025&jrxml=Comprovante_Relatorio
     */
    @GetMapping(value = "/generateByCpf", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Gera comprovante (PDF base64) por CPF")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> gerarPorCpf(
            @RequestParam(value = "cpf") String cpf,
            @RequestParam(value = "anoRef") Integer anoRef,
            @RequestParam(value = "jrxml", defaultValue = "comprovanteRendimento") String jrxmlFileName) {

        // validações simples
        if (cpf == null || cpf.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Parâmetro 'cpf' obrigatório."));
        }
        if (anoRef == null) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Parâmetro 'anoRef' obrigatório."));
        }

        Map<String, Object> result = service.gerarComprovanteBase64PorCpf(cpf.trim(), anoRef, jrxmlFileName);
        return ResponseEntity.ok(ApiResponseDto.ok(result));
    }

    /**
     * Mantive também endpoint por idCtr caso seja necessário (compatibilidade).
     * Ex.: /generate?idCtr=1&anoRef=2025
     */
//    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<ApiResponseDto<Map<String, Object>>> gerar(
//            @RequestParam("idCtr") Long idCtr,
//            @RequestParam("anoRef") Integer anoRef,
//            @RequestParam(value = "jrxml", defaultValue = "Comprovante_Relatorio") String jrxmlFileName) {
//
//        if (idCtr == null) {
//            return ResponseEntity.badRequest().body(ApiResponseDto.error("Parâmetro 'idCtr' obrigatório."));
//        }
//        if (anoRef == null) {
//            return ResponseEntity.badRequest().body(ApiResponseDto.error("Parâmetro 'anoRef' obrigatório."));
//        }
//
//        Map<String, Object> result = service.gerarComprovanteBase64(idCtr, anoRef, jrxmlFileName);
//        return ResponseEntity.ok(ApiResponseDto.ok(result));
//    }
}
