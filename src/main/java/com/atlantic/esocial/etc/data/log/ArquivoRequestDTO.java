package com.atlantic.esocial.etc.data.log;

import lombok.Data;

@Data
public class ArquivoRequestDTO {

    private String nomeArquivo;
    private Long   tamanhoArquivo;
    private String tipoArquivo;

}
