package com.atlantic.esocial.configuration.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.atlantic.esocial.etc.data.log.ArquivoRequestDTO;
import com.atlantic.esocial.etc.data.log.LogServico;
import com.atlantic.esocial.etc.data.log.LogServicoRespository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Classe Filtro de Log para printar logs de fácil entendimento para o desenvolvedor.
 * Essa classe entra em conjunto com o log das queries do hibernate habilitadas no application.properties:
 *    logging.level.org.hibernate.SQL=DEBUG
 *    logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
 *
 * @author Renan Watanabe
 * @since  20/10/2022
 */
@Component
@Order(2)
//public class LogFilter extends OncePerRequestFilter {
public class LogFilter extends GenericFilterBean {

    @Autowired private LogServicoRespository logServicoRespository;

    @Value("${atlantic.servicos.modo-desenvolvedor-localhost}")
    private boolean isModoDesenvolvedorLocalhost;

    @Autowired
    private HttpServletRequest request;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        ContentCachingRequestWrapper requestWrapper   = new ContentCachingRequestWrapper(httpServletRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);

        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());

        LogServico logServicoRequest = new LogServico();
        logServicoRequest.setRequestType("REQUEST");
        popularLogServicoRequest(logServicoRequest, httpServletRequest, requestBody, request.getCharacterEncoding());
        logServicoRequest = logServicoRespository.saveAndFlush(logServicoRequest);

        long tempoInicio = System.nanoTime();
        chain.doFilter(requestWrapper, responseWrapper);
        long tempoFim = System.nanoTime();

        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8.toString());

        if(isModoDesenvolvedorLocalhost)
            printarDetalhesServico(httpServletRequest, httpServletResponse, requestBody, responseBody, tempoInicio, tempoFim);

        //parece q so pega dps de ter rolado o chain
        requestBody = getStringValue(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());

        LogServico logServicoResponse = new LogServico();
        popularLogServicoResponse(logServicoResponse, logServicoRequest, httpServletResponse, responseBody,  requestBody,(tempoFim - tempoInicio) / 1000000);
        logServicoResponse.setRequestType("RESPONSE");
        logServicoResponse.setRequestId(logServicoRequest.getIdLogServico());
        logServicoResponse = logServicoRespository.saveAndFlush(logServicoResponse);

        System.out.println("============================================");
        System.out.println("Status code: " + logServicoResponse.getResponseStatus() + "\n");
        System.out.println("URL: " + logServicoResponse.getEndpoint() + "\n");
        System.out.println("Params URL: " + logServicoResponse.getRequestParamsUrl() + "\n");
        System.out.println("Body: " + logServicoResponse.getRequestParamsBody() + "\n");
        System.out.println("Mensagem: Request finalizada !");
        System.out.println("============================================");

        responseWrapper.copyBodyToResponse();
    }


    private void popularLogServicoRequest(LogServico logServico, HttpServletRequest request, String requestBody, String requestCharacterEncoding){
        logServico.setHost(request.getRequestURL().toString().replace(request.getRequestURI(), ""));
        logServico.setEndpoint(request.getRequestURI());
        logServico.setRequestMethod(request.getMethod());
        logServico.setRequestContentType(request.getContentType());

        StringBuilder sbParametros = new StringBuilder();
        request.getParameterMap().forEach((key, value)->{
            for(String valor : value){
                sbParametros.append(key).append("=").append(valor).append("&");
            }
        });
        if(sbParametros.length() != 0)
            sbParametros.deleteCharAt(sbParametros.length() - 1);

        logServico.setRequestParamsUrl(sbParametros.toString());
        logServico.setRequestParamsBody(requestBody);
        popularRequestFiles(logServico, request);
        logServico.setDatIng(Calendar.getInstance());
        logServico.setRequestEncoding(requestCharacterEncoding);
        logServico.setLogin( (String) request.getAttribute("jwtLoginUsuario"));
    }

    private void popularRequestFiles(LogServico logServico, HttpServletRequest request){
        if(request.getContentType() != null && request.getContentType().contains("multipart/form-data")){
            List<ArquivoRequestDTO> listaArquivos = new ArrayList<>();
            ArquivoRequestDTO arquivoRequestDTO;
            try {
                for (Part part : request.getParts()) {
                    long tamanhoArquivo = part.getSize();
                    String nomeArquivo = part.getSubmittedFileName();
                    String tipoArquivo  = part.getContentType();
                    if (nomeArquivo == null || nomeArquivo.isEmpty()) {
                        continue;
                    } else {
                        arquivoRequestDTO = new ArquivoRequestDTO();
                        arquivoRequestDTO.setNomeArquivo(nomeArquivo);
                        arquivoRequestDTO.setTamanhoArquivo(tamanhoArquivo);
                        arquivoRequestDTO.setTipoArquivo(tipoArquivo);
                        listaArquivos.add(arquivoRequestDTO);
                    }
                }
                if(!listaArquivos.isEmpty())
                    logServico.setRequestParamsFile(new ObjectMapper().writeValueAsString(listaArquivos));
            }catch(Exception e){//TODO
                logServico.setRequestParamsFile("Erro: Ocorreu um erro ao tentar tratar o request de Arquivos.");
            }
        }
    }

    private void popularLogServicoResponse(LogServico logServicoResponse, LogServico logServicoRequest, HttpServletResponse response, String responseBody, String requestBodyDeNovo, long delay){
        logServicoResponse.setResponseStatus(response.getStatus());
        logServicoResponse.setResponseContentType(response.getContentType());
        logServicoResponse.setResponseBody(responseBody);

        logServicoResponse.setHost(logServicoRequest.getHost());
        logServicoResponse.setEndpoint(logServicoRequest.getEndpoint());
        logServicoResponse.setRequestMethod(logServicoRequest.getRequestMethod());
        logServicoResponse.setRequestContentType(logServicoRequest.getRequestContentType());

        logServicoResponse.setRequestParamsUrl(logServicoRequest.getRequestParamsUrl());
        logServicoResponse.setRequestParamsBody(logServicoRequest.getRequestParamsBody());
        logServicoResponse.setDatIng(Calendar.getInstance());
        logServicoResponse.setRequestEncoding(logServicoRequest.getRequestEncoding());
        logServicoResponse.setLogin(logServicoRequest.getLogin());
        logServicoResponse.setRequestParamsBody(requestBodyDeNovo);

        popularResponseFiles(logServicoResponse, response);
        logServicoResponse.setDelay(delay);
    }


    private void popularResponseFiles(LogServico logServico, HttpServletResponse request){
      if(request.getContentType() != null && request.getContentType().contains("application/octet-stream")){
          logServico.setResponseFile("TO DO - Contém Arquivo");
      }
    }


    private void printarDetalhesServico(HttpServletRequest request, HttpServletResponse response, String requestBody, String responseBody, long tempoInicio, long tempoFim){
        StringBuilder sbParametros = new StringBuilder();
        request.getParameterMap().forEach((key, value)->{
            for(String valor : value){
                sbParametros.append(key).append("=").append(valor).append("&");
            }
        });
        if(sbParametros.length() != 0)
            sbParametros.deleteCharAt(sbParametros.length() - 1);

        StringBuilder sb = new StringBuilder();
        sb.append("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>><><><><><>><><><><><><><><><><><><><><><><><><><><><><>").append("\n");

//        Enumeration enumHeadersNames = request.getHeaderNames();
//        while (enumHeadersNames.hasMoreElements()) {
//            sb.append(enumHeadersNames.nextElement() + " ");
//        }
//        sb.append("\n");

        sb.append("Método/Serviço/Delay -> " + "[" + request.getMethod() + "] " + request.getRequestURI() + " (" + (tempoFim - tempoInicio) / 1000000 + "ms)").append("\n");
        sb.append("Request Parâmetros   -> " + sbParametros).append("\n");
        sb.append("Request Body         -> " + requestBody).append("\n");
        sb.append("Response Status      -> " + response.getStatus()).append("\n");
        sb.append("Response Body        -> " + responseBody).append("\n");
        sb.append("==========================================================================================================================================").append("\n");

        System.out.println(sb);
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