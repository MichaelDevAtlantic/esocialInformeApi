package com.atlantic.esocial.etc.data.log;

import java.util.Calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TB_LOG_SERVICO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogServico {

    @Id
    @GeneratedValue(generator = "seqLogServico", strategy= GenerationType.SEQUENCE)
    @SequenceGenerator(name = "seqLogServico", sequenceName = "SEQ_LOG_SERVICO",  allocationSize = 1)
    @Column private Long idLogServico;

    @Column private String host;
    @Column private String endpoint;

    @Column private String requestMethod;
    @Column private String requestContentType;
    @Column private String requestParamsUrl;
    @Column private String requestParamsBody;
    @Column private String requestParamsFile;
    @Column private String requestEncoding;

    @Column private Integer responseStatus;
    @Column private String  responseContentType;
    @Column private String  responseEncoding;
    @Column private String  responseBody;
    @Column private String  responseFile;

    @Column private Long delay;

    @Column private String requestType;
    @Column private Long requestId; 

    @Column @DateTimeFormat(pattern="dd/MM/yyyy HH:mm:ss") private Calendar datIng;
    @Column private String login;

}
