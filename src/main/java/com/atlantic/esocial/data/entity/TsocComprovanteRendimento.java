package com.atlantic.esocial.data.entity;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "TSOC_COMPROVANTE_RENDIMENTO")
public class TsocComprovanteRendimento {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID_SEQ_REGISTRO")
	private Long idSeqRegistro;
	private Long idCtrComprovante;
	private Integer anoRef;
	private Integer mesRef;
	private String numCnpjCpfDeclarante;
	private String codTipPessoa;
	private String numCpfCnpjPessoa;
	private String codRetencao;
	private String codAgrupacao;
	private String descAgrupacao;
	private String numItemAgrupacao;
	private String descAgrupacaoItem;
	private BigDecimal valorItem;
	private BigDecimal numQtdItem;
	private String descComplementoItem;
	private Calendar	datIng;
	private Calendar	datUltAtu;
	private String	nomUsuUltAtu;
	private String	nomProUltAtu;
}
