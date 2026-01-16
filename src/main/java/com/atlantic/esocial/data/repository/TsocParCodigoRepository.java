package com.atlantic.esocial.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.atlantic.esocial.data.entity.TsocParCodigo;


public interface TsocParCodigoRepository extends JpaRepository<TsocParCodigo, Long> {
	
	List<TsocParCodigo> findAllByCodNumOrderByNumOrdem(@Param("codNum") int codNum);
	
	List<TsocParCodigo> findAllByCodNumOrderByCodParAsc(Integer codNum);
	
	List<TsocParCodigo> findAllByIdDefinicao(Integer codNum);
	
	TsocParCodigo findByIdCodigo(Long idCodigo);
	
	Optional<TsocParCodigo> findByIdDefinicaoAndCodPar(Integer idDefinicao, String CodPar);
	
	Optional<TsocParCodigo> findByCodNumAndCodPar(Integer codNum, String codPar);	
	
	List<TsocParCodigo> findAllByCodNum(Integer codNum);
	
	@Query("SELECT t FROM TsocParCodigo t WHERE t.idCodigo = (SELECT MAX(t2.idCodigo) FROM TsocParCodigo t2)")
    TsocParCodigo findByMaxIdCodigo();
	
	Optional<TsocParCodigo> findByCodNum(Integer codNum);
	
	TsocParCodigo findFirstByCodNum(Integer codNum);
    
	Optional<TsocParCodigo> findFirstByCodNumAndCodPar(Integer codNum, String codPar);
	
	 @Query("SELECT uf FROM TsocParCodigo mun " +
	           "JOIN TsocParCodigo uf ON uf.codPar = mun.codParAssociado " +
	           "WHERE mun.codNum = 3 AND uf.codNum = 1061 AND mun.codPar = :codParMunicipio")
	    TsocParCodigo findUfByMunicipio(@Param("codParMunicipio") String codParMunicipio);

	 List<TsocParCodigo> findByCodPar(String codPar);

	 @Query(value = 
			 "SELECT CASE WHEN TO_NUMBER(DES_DESCRICAO_CURTA) < 30 THEN 30 ELSE TO_NUMBER(DES_DESCRICAO_CURTA)" +
			        "END AS DES_DESCRICAO_CURTA_NUM FROM TSOC_PAR_CODIGO WHERE COD_NUM = :codNum AND COD_PAR = :codPar", nativeQuery = true)
			Integer findQtePeriodoByCodNumNative(@Param("codNum") Integer codNum, @Param("codPar") String codPar);


}
