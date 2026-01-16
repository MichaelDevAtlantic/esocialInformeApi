package com.atlantic.esocial.etc.data.log;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.atlantic.esocial.etc.dto.MapDTO;

import feign.Param;

public interface LogServicoRespository extends JpaRepository<LogServico, Long> {

        @Query(nativeQuery = true, value = " SELECT * FROM (" +
                        "   SELECT ls.ID_LOG_SERVICO, ls.HOST, ls.ENDPOINT, ls.REQUEST_METHOD, ls.LOGIN, ls.DAT_ING, "+
                        " ROW_NUMBER() OVER (ORDER BY ls.id_log_servico desc) as row_num " +
                        "   FROM tb_log_servico ls WHERE ls.login like :login " +
                        " ) WHERE  row_num BETWEEN (:page - 1) * :limit + 1 AND :page * :limit ")
        List<MapDTO> findbyLoginAndHost(@Param("login") String login,
                        @Param("page") Long page,
                        @Param("limit") Long limit);

        @Query(nativeQuery = true, value = " SELECT * FROM (" +
                        "   SELECT ls.ID_LOG_SERVICO, ls.HOST, ls.ENDPOINT, ls.REQUEST_METHOD, ls.LOGIN, ls.DAT_ING, " +
                        " ROW_NUMBER() OVER (ORDER BY ls.id_log_servico) as row_num " +
                        "   FROM tb_log_servico ls WHERE 1 = 1 " +
                        "       AND ls.login = :login " +
                        "       AND ls.id_log_servico > :idLogServico " +
                        "   ORDER BY ls.dat_ing DESC " +
                        " ) WHERE  row_num BETWEEN (:page - 1) * :limit + 1 AND :page * :limit ")
        List<MapDTO> findAllbyLoginAndHostAndId(@Param("login") String login,
                        @Param("idLogServico") Long idLogServico,
                        @Param("page") Long page,
                        @Param("limit") Long limit);

        @Query(nativeQuery = true, value = " " +
                        "   SELECT ls.*, ROW_NUMBER() OVER (ORDER BY ls.id_log_servico) as row_num " +
                        "   FROM tb_log_servico ls WHERE 1 = 1 " +
                        "       AND ls.id_log_servico = :idLogServico " +
                        "   ORDER BY ls.dat_ing DESC " +
                        " ")
        List<LogServico> findbyLoginAndHostDetail(@Param("idLogServico") String idLogServico);
}
