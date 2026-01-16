//package com.atlantic.esocial.etc.exception;
//
//import com.atlantic.esocial.feature.informeRendimentos.dto.ApiResponseDto;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.dao.DataAccessException;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//
//import java.sql.SQLException;
//import java.util.NoSuchElementException;
//
//@ControllerAdvice
//public class GlobalExceptionHandler {
//    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
//
//    @ExceptionHandler(NoSuchElementException.class)
//    public ResponseEntity<ApiResponseDto<?>> handleNotFound(NoSuchElementException ex) {
//        log.warn("Recurso não encontrado: {}", ex.getMessage());
//        ApiResponseDto<?> body = ApiResponseDto.error(ex.getMessage());
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
//    }
//
//    @ExceptionHandler(DataAccessException.class)
//    public ResponseEntity<ApiResponseDto<?>> handleDataAccess(DataAccessException ex) {
//        log.error("Erro acesso a dados: {}", ex.getMessage(), ex);
//        // Mensagem amigável + orientação (ex.: checar conexão/schema/tabela)
//        String msg = "Erro de acesso a dados. Verifique conexão com banco, schema e existência das tabelas. Detalhe: " + ex.getMostSpecificCause().getMessage();
//        ApiResponseDto<?> body = ApiResponseDto.error(msg);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
//    }
//
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public ResponseEntity<ApiResponseDto<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
//        String msg = "Parâmetro inválido: " + ex.getName() + " valor recebido: " + ex.getValue();
//        log.warn(msg, ex);
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDto.error(msg));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponseDto<?>> handleGeneric(Exception ex) {
//        log.error("Erro inesperado: {}", ex.getMessage(), ex);
//        String msg = "Erro interno. Contate suporte. Mensagem: " + ex.getMessage();
//        ApiResponseDto<?> body = ApiResponseDto.error(msg);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
//    }
//}
