package com.atlantic.esocial.feature.informeRendimentos.dto;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class ComprovanteItemDto {
	private Long idCtrComprovante;
    private Integer anoRef;
    private String numCnpjCpfDeclarante;
    private String codTipPessoa;
    private String numCpfCnpjPessoa;
    private String codRetencao;
    private String codAgrupacao;
    private String descAgrupacao;
    private String numItemAgrupacao;
    private String descAgrupacaoItem;
    private String descrComplemento;
    private BigDecimal valorItem;
    private BigDecimal numQtdItem;
    private Date datUltAtu;
    
    private String dataPagamentoRRA;
    private String vlDataPagamento;
    private String vlQuantidadeMeses;
    
    private String txtMensagem;
}
