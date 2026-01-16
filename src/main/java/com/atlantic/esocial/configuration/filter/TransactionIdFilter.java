package com.atlantic.esocial.configuration.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TransactionIdFilter extends OncePerRequestFilter{

    private static final Logger logger = LoggerFactory.getLogger(TransactionIdFilter.class);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String transactionId = request.getHeader("Custom-X-Transaction");
        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = generateTransactionId();
            response.setHeader("Custom-X-Transaction", transactionId);
        } else {
            logger.info("Using existing transaction ID");
        }
        TransactionIdHolder.setTransactionId(transactionId);
        filterChain.doFilter(request, response);
    }
 
    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

}
