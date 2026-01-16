package com.atlantic.esocial.data.entity;

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
@Table(name = "TSOC_CAD_COMPROVANTE_REND")
public class TsocCadComprovanteRend {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID_CAD_COMPROVANTE")
	private Long idCadComprovante;
	private String NumCnpjDeclarante;
	private String nomOrgao;
	private String nomUnidOrc;
	private String nomMunicipio;
	private String sgUf;
	private Date datIniVig;
	private Date datFimVig;
	private Calendar	datIng;
	private Calendar	datUltAtu;
	private String	nomUsuUltAtu;
	private String	nomProUltAtu;
}
