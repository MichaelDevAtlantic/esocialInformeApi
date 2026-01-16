package com.atlantic.esocial.configuration.filter;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

/**
 * Classe Filtro Centralizadora de tratamento dos códigos http de retorno.
 *
 * @author Renan Watanabe
 * @since 20/10/2022.
 */
@Component
@Order(3)
public class HttpStatusFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper   requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(requestWrapper, responseWrapper);
        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding());
        if(!requestWrapper.getRequestURI().contains("/api/v1/workflow/")) { //TODO refatorar para maneira mais elegante depois (v1 pode mudar)
            tratarSolicitacao(requestWrapper, responseWrapper, responseBody);
        }
        responseWrapper.copyBodyToResponse();
    }

    private void tratarSolicitacao(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, String responseBody) {
        switch (requestWrapper.getMethod()) {
            case "GET":
                tratarSolicitacaoGet(requestWrapper, responseWrapper, responseBody);
                break;
            case "POST":
                tratarSolicitacaoPost(requestWrapper, responseWrapper, responseBody);
                break;
            case "PUT":
                tratarSolicitacaoPut(requestWrapper, responseWrapper, responseBody);
                break;
            case "DELETE":
                tratarSolicitacaoDelete(requestWrapper, responseWrapper, responseBody);
                break;
        }
    }

    private void tratarSolicitacaoGet(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, String responseBody) {
        if (responseBody == null || responseBody.isEmpty() || responseBody.equals("{}") || responseBody.equals("[]")) {
            responseWrapper.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }

    //Em vez de botar nos metodos criados @ResponseStatus(HttpStatus.CREATED), tratar aqui.
    //TODO conversar com o tony, retornar id, void ou o objeto carregado? //201
    private void tratarSolicitacaoPost(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, String responseBody) {
        if (responseBody == null || responseBody.isEmpty() || responseBody.equals("{}") || responseBody.equals("[]")) {
            responseWrapper.setStatus(HttpStatus.CREATED.value());
        }
    }

    private void tratarSolicitacaoPut(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, String responseBody) {
        //TODO conversar com o tony, retornar id, void ou o objeto carregado? //200
    }

    private void tratarSolicitacaoDelete(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, String responseBody) {
        //TODO conversar com o Tony, qnd for delete, o certo eh ser void e depender do 200 ou 400 do metodo HTTP ou devo retonar um booleano talvez? //204
    }

    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            return new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}