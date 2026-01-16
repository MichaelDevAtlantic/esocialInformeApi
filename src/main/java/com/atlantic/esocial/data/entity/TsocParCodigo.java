package com.atlantic.esocial.data.entity;

import java.sql.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Entity
@Data
@Table(name = "TSOC_PAR_CODIGO")
public class TsocParCodigo {

	@Id
	private Long idCodigo;
	private Integer codIns;
	private Integer codNum;
	private String codPar;
	private String desDescricao;
	private String desDescricaoCurta;
	private Integer numOrdem;
	private String codNumAssociado;
	private String codParAssociado;
	private String flgVigente;
	private Date datIng;
	private Date datUltAtu;
	private String nomUsuUltAtu;
	private String nomProUltAtu;
	private Integer idDefinicao;
	
	// novo campo transient, não persistido no banco
    @Transient
    private String codNumAssociadoDescricao;

    @Transient
    private String codParAssociadoDescricao;

    // getters e setters do campo novo
    public String getCodNumAssociadoDescricao() {
        return codNumAssociadoDescricao;
    }

    public void setCodNumAssociadoDescricao(String codNumAssociadoDescricao) {
        this.codNumAssociadoDescricao = codNumAssociadoDescricao;
    }

    public String getCodParAssociadoDescricao() {
        return codParAssociadoDescricao;
    }

    public void setCodParAssociadoDescricao(String codParAssociadoDescricao) {
        this.codParAssociadoDescricao = codParAssociadoDescricao;
    }

}
