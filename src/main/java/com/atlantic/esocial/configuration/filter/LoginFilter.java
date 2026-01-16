package com.atlantic.esocial.configuration.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.atlantic.esocial.configuration.security.UtilsJwt;
import com.atlantic.esocial.etc.exception.FilterException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Classe Filtro Centralizadora para verificar se o usuário tem acesso às funcionalidades via token JWT.
 *
 * @author Tony Caravana / Renan Watanabe
 * @since 21/10/2022.
 */
//TODO essa funcionalidade deverá ser migrada futuramente utilizando Spring Security.
@Component
@Order(1)
public class LoginFilter extends OncePerRequestFilter {

    @Value("${atlantic.servicos.modo-desenvolvedor-localhost}")
    private boolean isModoDesenvolvedorLocalhost;

    @Value("${atlantic.servicos.modo-desenvolvedor-localhost.jwt-token}")
    private String jwtTokenModoDesenvolvedorLocalhost;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return
            // 🔓 recursos estáticos
            path.startsWith("/esocialInformeApi/css/")
         || path.startsWith("/esocialInformeApi/js/")
         || path.startsWith("/esocialInformeApi/assets/")
         || path.startsWith("/esocialInformeApi/static/")
         || path.startsWith("/esocialInformeApi/reports/")
         || path.endsWith(".jfif")
         || path.endsWith(".jasper")
         || path.endsWith(".jrxml")
         || path.endsWith(".bak")
         || path.endsWith(".css")
         || path.endsWith(".js")
         || path.endsWith(".map")
         || path.endsWith(".png")
         || path.endsWith(".jpg")
         || path.endsWith(".ico")

            // 🔓 outros já existentes
         || path.contains("/swagger")
         || path.contains("/v3/api-docs")
         || path.contains("/favicon.ico")
         || path.contains("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            String authorization = requestWrapper.getHeader("Authorization");
            if (authorization == null || authorization.isEmpty()) {
                if(isModoDesenvolvedorLocalhost || request.getRequestURI().startsWith("/api/v1/alertas")){
                    authorization = jwtTokenModoDesenvolvedorLocalhost;
                }
            }
            Map<String, Object> mapaJwtToken = transformarJwtTokenEmMapa(authorization);
            requestWrapper.setAttribute("jwtToken", authorization);
            requestWrapper.setAttribute("jwtCodIns", mapaJwtToken.get("jwtCodIns"));
            requestWrapper.setAttribute("jwtLoginUsuario", mapaJwtToken.get("jwtLoginUsuario"));
            filterChain.doFilter(requestWrapper, responseWrapper);
        
        } catch (Exception e) {
            responseWrapper.setStatus(HttpStatus.FORBIDDEN.value());
        }
        responseWrapper.copyBodyToResponse();
    }

    // TODO mudar as mensagens preenchidas aqui depois, provavelmente não poderá colocar mensagens muito específicas
    //      para não ser possível uma engenharia reversa para hacking.
    private Map<String, Object> transformarJwtTokenEmMapa(String authorization) throws Exception {
        Map<String, Object> mapaJwtToken = null;
        if (authorization == null || authorization.isEmpty()) {
            throw new FilterException("O header 'Authorization' não está preenchido com o token JWT.");
        }

        if (authorization.startsWith("Bearer ")) {
            authorization = authorization.substring("Bearer ".length());
        }

         String jsonData = UtilsJwt.getDataFromDecodeJWTToken(authorization);
        if (jsonData == null || jsonData.equals("") || jsonData.isEmpty() || jsonData.equals("{}") || jsonData.equals("[]")) {
            throw new FilterException("O header 'Authorization' está com um Token JWT inválido.");
        } else {
            mapaJwtToken = new LinkedHashMap<>();
            Map<String, Object> mapaJson = new ObjectMapper().readValue(jsonData, HashMap.class);
            mapaJwtToken.put("jwtAmbiente", mapaJson.get("ambiente"));
            mapaJwtToken.put("jwtCodIns", mapaJson.get("codIns"));
            mapaJwtToken.put("jwtLoginUsuario", mapaJson.get("login"));
            mapaJwtToken.put("jwtCpfUsuario", mapaJson.get("numCpfLogin"));
            mapaJwtToken.put("jwtOrigem", mapaJson.get("origem"));
        }
        return mapaJwtToken;
    }

}