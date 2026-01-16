package com.atlantic.esocial.etc.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.LinkedHashMap;

/**
 * MapDTO responsável por:
 *
 * Deixar o mapa com os registros na ordem que foram inseridos: LinkedHashMap
 * Transformar o padrão da colunas UPPER_CASE_UNDERSCORE para LOWER_CAMEL_CASE (por exemplo: CODIGO_CARGO para codigoCargo)
 */
//@JsonInclude(JsonInclude.Include.NON_NULL) - deve ignorar null como padrao, testar.
@JsonSerialize(keyUsing = CamelCaseSerializer.class)
public class MapDTO extends LinkedHashMap<String, Object> { //LinkedHashMap não funciona para o JpaRepository pela razão do Spring Jpa
                                                            //internamente já trazer desordenado pelas queries dinânicamicas.
                                                            //Verificar outra maneira, talvez json property order.
}