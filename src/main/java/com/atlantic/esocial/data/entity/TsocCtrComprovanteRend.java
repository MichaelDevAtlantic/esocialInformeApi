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
@Table(name = "TSOC_CTR_COMPROVANTE_REND")
public class TsocCtrComprovanteRend {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID_CTR_COMPROVANTE")
	private Long idCtrComprovante;
	private Long idCadComprovante;
	private Integer anoRef;
	private Date datExecucao;
	private String flgStatus;
	private Integer qtdRegGerados;
	private String msgOcorrencia;
	private Calendar	datIng;
	private Calendar	datUltAtu;
	private String	nomUsuUltAtu;
	private String	nomProUltAtu;
}
