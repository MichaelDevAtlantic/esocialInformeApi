package com.atlantic.esocial.feature.informeRendimentos;

import com.atlantic.esocial.data.entity.TsocCadComprovanteRend;
import com.atlantic.esocial.data.entity.TsocCtrComprovanteRend;
import com.atlantic.esocial.data.repository.TsocCadComprovanteRendRepository;
import com.atlantic.esocial.data.repository.TsocCtrComprovanteRendRepository;
import com.atlantic.esocial.feature.informeRendimentos.dto.ComprovanteItemDto;

import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.*;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.Date;

@Service
public class InformeRendimentosService {

    private final Logger log = LoggerFactory.getLogger(InformeRendimentosService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TsocCtrComprovanteRendRepository tsocCtrComprovanteRendRepository;

    @Autowired
    private TsocCadComprovanteRendRepository tsocCadComprovanteRendRepository;

    // ---------------------- público (existentes) ----------------------
    public Long findLatestIdCtrByCpfAndAno(String cpf, Integer anoRef) {
        if (cpf == null || cpf.trim().isEmpty() || anoRef == null) return null;

        String sql = ""
            + "SELECT ID_CTR_COMPROVANTE FROM ( "
            + "  SELECT ID_CTR_COMPROVANTE, DAT_ULT_ATU "
            + "  FROM TSOC_COMPROVANTE_RENDIMENTO "
            + "  WHERE NUM_CPF_CNPJ_PESSOA = ? AND ANO_REF = ? "
            + "  ORDER BY DAT_ULT_ATU DESC "
            + ") WHERE ROWNUM = 1";

        try {
            return jdbcTemplate.queryForObject(sql, Long.class, cpf, anoRef);
        } catch (org.springframework.dao.EmptyResultDataAccessException er) {
            return null;
        } catch (BadSqlGrammarException bsg) {
            String causeMsg = bsg.getMostSpecificCause() != null ? bsg.getMostSpecificCause().getMessage() : bsg.getMessage();
            log.error("Bad SQL grammar ao consultar TSOC_COMPROVANTE_RENDIMENTO. Detalhe: {}", causeMsg, bsg);
            throw new DataAccessResourceFailureException(
                "Erro de SQL ao consultar comprovantes: verifique se a tabela existe e permissões. Detalhe: " + causeMsg, bsg);
        } catch (DataAccessException dae) {
            log.error("Erro DataAccess durante consulta de comprovantes: {}", dae.getMessage(), dae);
            throw dae;
        }
    }

    public Map<String, Object> gerarComprovanteBase64PorCpf(String cpf, Integer anoRef, String jrxmlFileName) {
        byte[] pdf = gerarComprovantePdfPorCpf(cpf, anoRef, jrxmlFileName);
        String encodedString = Base64.getEncoder().withoutPadding().encodeToString(pdf);
        Map<String, Object> mapaRetorno = new HashMap<>();
        mapaRetorno.put("formato", "base64");
        mapaRetorno.put("PDF", encodedString);
        return mapaRetorno;
    }

    // ---------------------- método principal ----------------------

    public byte[] gerarComprovantePdfPorCpf(String cpf, Integer anoRef, String jrxmlFileName) {
        try {
            // debug fonts (opcional)
            for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                System.out.println(f);
            }

            if (cpf == null || cpf.trim().isEmpty()) {
                throw new IllegalArgumentException("CPF obrigatório para geração por CPF.");
            }

            Long latestCtrId = findLatestIdCtrByCpfAndAno(cpf, anoRef);
            TsocCtrComprovanteRend ctr = null;
            TsocCadComprovanteRend cad = null;
            if (latestCtrId != null) {
                Optional<TsocCtrComprovanteRend> ctrOpt = tsocCtrComprovanteRendRepository.findById(latestCtrId);
                ctr = ctrOpt.orElse(null);
                if (ctr != null && ctr.getIdCadComprovante() != null) {
                    cad = tsocCadComprovanteRendRepository.findById(ctr.getIdCadComprovante()).orElse(null);
                }
            }

            // 1) buscar dados das views (nomes exatos conforme você passou)
            List<ComprovanteItemDto> mainItems = queryItensAgregadosPorCpf(cpf, anoRef); // TSOC_VW_COMPROVANTE_RENDIMENTO
            List<RraItemDto> rraItems = queryRraPorCpf(cpf, anoRef); // TSOC_VW_COMPROVANTE_REND_RRA (cada linha = data + qtd meses)
            List<CompItemDto> compItems = queryComplementaresPorCpf(cpf, anoRef); // TSOC_VW_COMPROVANTE_REND_COMP

            // fallback simulado apenas para preview (mantido opcional)
            if (mainItems == null || mainItems.isEmpty()) {
                log.info("Nenhum item na view principal encontrado — usando dados simulados para visualização.");
            }

            // 2) montar mapa de parâmetros (separa responsabilidades)
            Map<String, Object> params = montarParametrosComprovante(cpf, anoRef, ctr, cad, mainItems, rraItems, compItems);

            // 3) carregar JRXMLs (main + subreport)
            //File fileJrxml = ResourceUtils.getFile("classpath:reports/" + jrxmlFileName + ".jrxml");
       //     File reportsFolder = ResourceUtils.getFile("classpath:reports/");
           // JasperReport mainReport = compileReportForceCompile(fileJrxml);
            
            InputStream mainStream =
                    getClass().getResourceAsStream("/reports/" + jrxmlFileName + ".jrxml");

            if (mainStream == null) {
                throw new IllegalStateException("JRXML principal não encontrado: " + jrxmlFileName);
            }

            JasperReport mainReport = JasperCompileManager.compileReport(mainStream);
            
            String reportsPath = getClass()
                    .getResource("/reports/")
                    .getPath();   

            if (!reportsPath.endsWith("/")) {
                reportsPath = reportsPath + "/";
            }


            // tenta compilar subreport e colocá-lo no params (caso exista)
            try {
//                File subJrxml = ResourceUtils.getFile("classpath:reports/Comprovante_Relatorio_Sub1.jrxml");
//                JasperReport subReport = compileReportForceCompile(subJrxml);
//                params.put("SUBREPORT", subReport);
            	
//            	InputStream subStream =
//            	        getClass().getResourceAsStream("/reports/Comprovante_Relatorio_Sub1.jrxml");
//
//            	if (subStream != null) {
//            	    JasperReport subReport = JasperCompileManager.compileReport(subStream);
//            	    params.put("SUBREPORT_RRA", subReport);
//            	} else {
//            	    log.warn("Subreport Comprovante_Relatorio_Sub1.jrxml não encontrado");
//            	}
//            	
//            	// Complementares
//            	InputStream compStream =
//            	        getClass().getResourceAsStream("/reports/ComprovanteRendimentoSubComp.jrxml");
//
//            	if (compStream != null) {
//            	    JasperReport compReport = JasperCompileManager.compileReport(compStream);
//            	    params.put("SUBREPORT_COMP", compReport);
//            	}
            	
            	params.put("SUBREPORT_RRA",
            	        reportsPath + "Comprovante_Relatorio_Sub1.jasper");

            	params.put("SUBREPORT_COMP",
            	        reportsPath + "comprovanteRendimentoSubComp.jasper");


            } catch (Exception fnf) {
                log.warn("Sub_RRA.jrxml não encontrado em classpath:reports. Certifique-se que existe se for usar o RRA.");
            }


            params.put("REPORT_BASE_PATH", reportsPath);
            params.put("SUBREPORT_DIR", reportsPath);


            // carregar logo (se disponível)
            try {
//                File imgFile = ResourceUtils.getFile("classpath:static/images/ministerioEconomia.jfif");
//                String logoImg = imgFile.getAbsolutePath();
//                params.put("LOGO", logoImg);
            	String logoPath =
            	        getClass().getResource("/static/images/ministerioEconomia.jfif").toString();

            	params.put("LOGO", logoPath);


            } catch (Exception ex) {
                log.warn("Logo não encontrada/erro ao carregar logo. Param LOGO será vazio.", ex);
                params.put("LOGO", "");
            }

            // 4) preencher report - main usa JREmptyDataSource porque o layout é parametrizado
            JasperPrint jasperPrint = JasperFillManager.fillReport(mainReport, params, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (DataAccessException dae) {
            log.error("Erro ao acessar dados durante geração do PDF por CPF: {}", dae.getMessage(), dae);
            throw dae;
        } catch (IllegalStateException ise) {
            log.error("Configuração do JRXML inválida: {}", ise.getMessage(), ise);
            throw new RuntimeException("Erro no JRXML: " + ise.getMessage(), ise);
        } catch (Exception e) {
            log.error("Erro durante geração do PDF por CPF: {}", e.getMessage(), e);
            throw new RuntimeException("Não foi possível gerar o PDF do comprovante por CPF.", e);
        }
    }

    // ---------------------- montagem de parâmetros ----------------------

    private Map<String, Object> montarParametrosComprovante(
            String cpf,
            Integer anoRef,
            TsocCtrComprovanteRend ctr,
            TsocCadComprovanteRend cad,
            List<ComprovanteItemDto> mainItems,
            List<RraItemDto> rraItems,
            List<CompItemDto> compItems) {

        Map<String, Object> params = new HashMap<>();

        // parâmetros básicos / identificadores
        params.put("CPF_BENEFICIARIO", cpf);
        String nomeBeneficiario = buscarNomeBeneficiarioPorCpf(cpf);
        params.put("NOME_BENEFICIARIO",
                (nomeBeneficiario != null && !nomeBeneficiario.isEmpty())
                        ? nomeBeneficiario
                        : "");

        params.put("NATUREZA_RENDIMENTO", "Vencimentos e Vantagens");
        params.put("observacao", extractObservacaoFromMainItems(mainItems));

        // dados do CAD (ou valores default)
        if (cad != null) {
            params.put("NUM_CNPJ_DECLARANTE", nullSafe(cad.getNumCnpjDeclarante()));
            params.put("NOM_ORGAO", nullSafe(cad.getNomOrgao()));
            params.put("NOM_UNID_ORC", nullSafe(cad.getNomUnidOrc()));
            params.put("NOM_MUNICIPIO", nullSafe(cad.getNomMunicipio()));
            params.put("SG_UF", nullSafe(cad.getSgUf()));
            params.put("DAT_INI_VIG", cad.getDatIniVig());
            params.put("DAT_FIM_VIG", cad.getDatFimVig());
        } else {
            params.put("NUM_CNPJ_DECLARANTE", "00.000.000/0000-00");
            params.put("NOM_ORGAO", "ÓRGÃO EXEMPLO");
            params.put("NOM_UNID_ORC", "");
            params.put("NOM_MUNICIPIO", "CIDADE EXEMPLO");
            params.put("SG_UF", "SP");
            params.put("DAT_INI_VIG", null);
            params.put("DAT_FIM_VIG", null);
        }

        // valores default (garantir que todo parâmetro esperado pelo JRXML exista)
        initializeDefaultFinancialParams(params);

        // preencher parâmetros a partir da view principal
        for (ComprovanteItemDto item : mainItems) {
            String cod = safe(item.getCodAgrupacao());
            String numItem = safe(item.getNumItemAgrupacao());
            String targetParam = ParamMapping.findParam(cod, numItem);
            if (targetParam != null) {
                params.put(targetParam, formatCurrency(item.getValorItem()));
            } else {
                log.debug("Sem mapeamento param para codAgr='{}' numItem='{}'. desc='{}'. valor={}", cod, numItem, item.getDescAgrupacaoItem(), item.getValorItem());
            }
            if (item.getTxtMensagem() != null && !item.getTxtMensagem().trim().isEmpty()) {
                params.put("observacao", item.getTxtMensagem());
            }
        }

        // Não tentamos somar ou reusar VALOR_ITEM vindo do RRA — a view principal já traz os valores necessários.
        // O RRA aqui é apenas fonte para o subreport: data pagamento + quantidade meses.
        // Montar datasource para subreport RRA:
        List<Map<String, Object>> rraForReport = new ArrayList<>();
        if (rraItems != null) {
            for (RraItemDto r : rraItems) {
                Map<String, Object> m = new HashMap<>();
                m.put("vlDataPagamento", r.getVlDataPagamento() != null ? r.getVlDataPagamento() : r.getDataPagamentoRRA());
                m.put("vlQuantidadeMeses", r.getVlQuantidadeMeses() != null ? r.getVlQuantidadeMeses() : (r.getNumQtdMeses() != null ? r.getNumQtdMeses().toString() : ""));
                // incluir campos extras caso subreport use
                m.put("dataPagamentoRRA", r.getDataPagamentoRRA());
                m.put("descAgrupacaoItem", r.getDescAgrupacaoItem());
                rraForReport.add(m);
            }
        }

        JRBeanCollectionDataSource rraDs = new JRBeanCollectionDataSource(rraForReport);
        // nome usado no seu subreport (você pode manter 'datasourceItems' se for o que o jrxml espera)
        params.put("datasourceItems", rraDs);
        params.put("RRA_DS", rraDs);

        // ------------ preencher datasourceComp (COMPLEMENTARES) ------------
        List<Map<String, Object>> compForReport = new ArrayList<>();
        if (compItems != null) {
            for (CompItemDto comp : compItems) {
                Map<String, Object> m = new HashMap<>();

                // descrição: "01. Descrição do item"
                m.put(
                    "itemDescricaoComp",
                    comp.getNumItemAgrupacao() + ". " + comp.getDescAgrupacaoItem()
                );

                // valor já formatado em pt-BR (String)
                m.put(
                    "itemValorComp",
                    formatCurrency(comp.getValorItem())
                );

                compForReport.add(m);
            }         
        }else {
        	Map<String, Object> m = new HashMap<>();
        	
        	m.put(
                    "itemDescricaoComp",
                    ""
                );

                // valor já formatado em pt-BR (String)
                m.put(
                    "itemValorComp",
                    ""
                );
             compForReport.add(m);
        }

        JRBeanCollectionDataSource compDs = new JRBeanCollectionDataSource(compForReport);
        params.put("datasourceComp", compDs);
        params.put("COMP_DS", compDs);
        params.put("mostrarComplementares",
                (compItems != null && !compItems.isEmpty()) ? "S" : "N");

        // complementar: preencher bloco 7 (informações complementares) — manter compatibilidade com mapeamentos antigos
        if (compItems != null) {
            for (CompItemDto comp : compItems) {
                String cod = safe(comp.getCodAgrupacao());
                String numItem = safe(comp.getNumItemAgrupacao());
                String targetParam = ParamMapping.findParam(cod, numItem);
                if (targetParam != null) {
                    params.put(targetParam, formatCurrency(comp.getValorItem()));
                } else {
                    if ("05".equals(cod)) {
                        if ("27".equals(normalizeNumber(numItem))) {
                            params.put("vlIasmpeAgregados", formatCurrency(comp.getValorItem()));
                        } else {
                            log.debug("Complementar bloco 7 sem mapeamento específico para numItem {}", numItem);
                        }
                    }
                }
            }
        }

        // datas / executor / responsavel
        params.put("dataResponsavelInformacoes", new SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()));
        params.put("nomeResponsavelInformacoes", "RESPONSÁVEL EXEMPLO");

        // ANO_REF como String
        if (ctr != null && ctr.getAnoRef() != null) {
            params.put("ANO_REF", String.valueOf(ctr.getAnoRef()));
        } else {
            params.put("ANO_REF", String.valueOf(anoRef));
        }

        // controle de impressão do subreport
        params.put("mostrarAlimentados", (rraItems != null && !rraItems.isEmpty()) ? "S" : "N");

        return params;
    }

    private String extractObservacaoFromMainItems(List<ComprovanteItemDto> items) {
        if (items == null) return "";
        for (ComprovanteItemDto it : items) {
            if (it.getTxtMensagem() != null && !it.getTxtMensagem().trim().isEmpty()) {
                return it.getTxtMensagem();
            }
        }
        return "";
    }

    private void initializeDefaultFinancialParams(Map<String, Object> params) {
        // Lista gerada a partir das planilhas: garante que todos os parâmetros que podem ser usados existam com 0,00
        String[] keys = {
            "vlTotalRendimentos","vlContribuicaoOficial","vlContribuicaoPrivada","vlPensaoAlimenti","vlImpostoRetidoFonte",
            "vlParteIsenta","vlAjudaCusto","vlPensao","vlAbonosSalariais","vlSalarioFamilia","vlIndenizacoes","vlOutrosIsentos",
            "vlDecimoTerceiro","vlDecimoTerceiroIR","vlOutrosTributaveis",
            "vlRendTribRra","vlAcaoJudRra","vlPrevRra","vlPaRra","vlIrRra","vlMolestiaRra",
            "vlPensionista","vlIamspeBeneficiarios","vlIasmpe","vlIasmpeAgregados",
            // parâmetros adicionais (mapeados a partir do Excel)
            "vlTotalDeRendimentos","vlContribuicaoPrevidenciariaOficial","vlImpostoDeRendaRetidoNaFonte",
            "vlComplPmspprevCod3533","vlPensaoJudicialPAViaRecibo","vlTotalDosRendimentosRra","vlContribuicaoPrevidenciariaOficialRra",
            "vlPensaoAlimenticiaRra","vlQuantidadeDeMesesRra","vlDataDeRecebimento","vlAuxilioSaudeRes8582008","vlAuxEducacaoLc140224",
            "vlAuxilioPreEscolar","vlAuxilioInclusaoLc140224","vlIndenizacoes","vlDiariasAjudaDeCusto","vlAssSaudeDeputAto0332024",
            "vlDecimoTerceiroSalario","vlImpostoDeRendaRetido13","vlPensionistas","vlIamspeBeneficiarios","vlIamspeAgregadosLei111252002",
            "vlIamspeOdonto","vlAssSaudeDeputAto172012","vlDependentes","vlIamspe","vlDeducoes13Salario","vlImpostoDeRendaRetido13Sal",
            "vlPrevidenciaComplemSpprevcom"
        };
        for (String k : keys) {
            if (!params.containsKey(k)) params.put(k, "0,00");
        }
    }

    // ---------------------- consultas às views (corrigidas) ----------------------

    /**
     * Consulta a view principal TSOC_VW_COMPROVANTE_RENDIMENTO
     * (a view já faz aggregation/group by — aqui apenas lemos as colunas expostas)
     *
     * Observação: removida a restrição que excluía agrupação '05' — agora todos os blocos são retornados.
     */
    private List<ComprovanteItemDto> queryItensAgregadosPorCpf(String cpf, Integer anoRef) {
        String sql = ""
            + "SELECT COD_AGRUPACAO, DESC_AGRUPACAO, NUM_ITEM_AGRUPACAO, DESC_AGRUPACAO_ITEM, NUM_QTD_ITEM, VALOR_ITEM, TXT_MENSAGEM "
            + "FROM TSOC_VW_COMPROVANTE_RENDIMENTO "
            + "WHERE NUM_CPF_CNPJ_PESSOA = ? AND ANO_REF = ? "
            + "ORDER BY COD_AGRUPACAO, NUM_ITEM_AGRUPACAO";

        RowMapper<ComprovanteItemDto> rm = (rs, rowNum) -> {
            ComprovanteItemDto d = new ComprovanteItemDto();
            d.setCodAgrupacao(rs.getString("COD_AGRUPACAO"));
            d.setDescAgrupacao(rs.getString("DESC_AGRUPACAO"));
            d.setNumItemAgrupacao(rs.getString("NUM_ITEM_AGRUPACAO"));
            d.setDescAgrupacaoItem(rs.getString("DESC_AGRUPACAO_ITEM"));
            BigDecimal valor = rs.getBigDecimal("VALOR_ITEM");
            d.setValorItem(valor != null ? valor : BigDecimal.ZERO);
            BigDecimal qtd = rs.getBigDecimal("NUM_QTD_ITEM");
            d.setNumQtdItem(qtd != null ? qtd : BigDecimal.ZERO);
            d.setTxtMensagem(rs.getString("TXT_MENSAGEM"));
            return d;
        };

        try {
            return jdbcTemplate.query(sql, rm, cpf, anoRef);
        } catch (BadSqlGrammarException bsg) {
            String causeMsg = bsg.getMostSpecificCause() != null ? bsg.getMostSpecificCause().getMessage() : bsg.getMessage();
            log.error("Bad SQL grammar ao consultar TSOC_VW_COMPROVANTE_RENDIMENTO por CPF. Detalhe: {}", causeMsg, bsg);
            throw new DataAccessResourceFailureException("Erro na consulta de comprovantes (por CPF): " + causeMsg, bsg);
        }
    }

    /**
     * Consulta a view do RRA / subreport (TSOC_VW_COMPROVANTE_REND_RRA)
     * Cada registro retornado pela view é um registro do subreport: DAT_PAGAMENTO + NUM_QTD_MESES
     * Não tentamos agregar VALOR_ITEM aqui nem usar colunas que a view não expõe.
     */
    private List<RraItemDto> queryRraPorCpf(String cpf, Integer anoRef) {
        String sql = ""
            + "SELECT COD_AGRUPACAO, NUM_ITEM_AGRUPACAO, DESC_AGRUPACAO_ITEM, DAT_PAGAMENTO, NUM_QTD_MESES "
            + "FROM TSOC_VW_COMPROVANTE_REND_RRA "
            + "WHERE NUM_CPF_CNPJ_PESSOA = ? AND ANO_REF = ? "
            + "ORDER BY DAT_PAGAMENTO";

        RowMapper<RraItemDto> rm = (rs, rowNum) -> {
            RraItemDto r = new RraItemDto();
            r.setCodAgrupacao(rs.getString("COD_AGRUPACAO"));
            r.setNumItemAgrupacao(rs.getString("NUM_ITEM_AGRUPACAO"));
            r.setDescAgrupacaoItem(rs.getString("DESC_AGRUPACAO_ITEM"));
            Date d = rs.getDate("DAT_PAGAMENTO");
            if (d != null) {
                String s = new SimpleDateFormat("dd/MM/yyyy").format(d);
                r.setVlDataPagamento(s);
                r.setDataPagamentoRRA(s);
            }
            BigDecimal qtd = rs.getBigDecimal("NUM_QTD_MESES");
            if (qtd != null) {
                r.setVlQuantidadeMeses(String.valueOf(qtd.intValue()));
                r.setNumQtdMeses(qtd.intValue());
            }
            return r;
        };

        try {
            return jdbcTemplate.query(sql, rm, cpf, anoRef);
        } catch (BadSqlGrammarException bsg) {
            String causeMsg = bsg.getMostSpecificCause() != null ? bsg.getMostSpecificCause().getMessage() : bsg.getMessage();
            log.error("Bad SQL grammar ao consultar TSOC_VW_COMPROVANTE_REND_RRA por CPF. Detalhe: {}", causeMsg, bsg);
            throw new DataAccessResourceFailureException("Erro na consulta RRA (por CPF): " + causeMsg, bsg);
        }
    }

    /**
     * Consulta a view de complementares (TSOC_VW_COMPROVANTE_REND_COMP)
     */
    private List<CompItemDto> queryComplementaresPorCpf(String cpf, Integer anoRef) {
        String sql = ""
            + "SELECT COD_AGRUPACAO, NUM_ITEM_AGRUPACAO, DESC_AGRUPACAO_ITEM, VALOR_ITEM "
            + "FROM TSOC_VW_COMPROVANTE_REND_COMP "
            + "WHERE NUM_CPF_CNPJ_PESSOA = ? AND ANO_REF = ? "
            + "ORDER BY COD_AGRUPACAO, NUM_ITEM_AGRUPACAO";

        RowMapper<CompItemDto> rm = (rs, rowNum) -> {
            CompItemDto c = new CompItemDto();
            c.setCodAgrupacao(rs.getString("COD_AGRUPACAO"));
            c.setNumItemAgrupacao(rs.getString("NUM_ITEM_AGRUPACAO"));
            c.setDescAgrupacaoItem(rs.getString("DESC_AGRUPACAO_ITEM").toString().trim());
            BigDecimal val = rs.getBigDecimal("VALOR_ITEM");
            c.setValorItem(val != null ? val : BigDecimal.ZERO);
            return c;
        };

        try {
            return jdbcTemplate.query(sql, rm, cpf, anoRef);
        } catch (BadSqlGrammarException bsg) {
            String causeMsg = bsg.getMostSpecificCause() != null ? bsg.getMostSpecificCause().getMessage() : bsg.getMessage();
            log.error("Bad SQL grammar ao consultar TSOC_VW_COMPROVANTE_REND_COMP por CPF. Detalhe: {}", causeMsg, bsg);
            throw new DataAccessResourceFailureException("Erro na consulta complementares (por CPF): " + causeMsg, bsg);
        }
    }

    // ---------------------- utilitários e formatação ----------------------

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String formatCurrency(BigDecimal v) {
        if (v == null) return "0,00";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(v);
    }

    private String normalizeNumber(String s) {
        if (s == null) return "";
        return s.replaceFirst("^0+(?!$)", "");
    }

    // ---------------------- simulações (apenas para teste / preview) ----------------------

    private List<ComprovanteItemDto> simularItensAgregados() {
        List<ComprovanteItemDto> lista = new ArrayList<>();

        ComprovanteItemDto i1 = new ComprovanteItemDto();
        i1.setCodAgrupacao("01");
        i1.setDescAgrupacao("VENCIMENTOS");
        i1.setNumItemAgrupacao("01");
        i1.setDescAgrupacaoItem("TOTAL DE RENDIMENTOS (INCLUSIVE FÉRIAS)");
        i1.setValorItem(new BigDecimal("20540.58"));
        i1.setNumQtdItem(new BigDecimal("6"));
        i1.setTxtMensagem("Este Informe de Rendimentos é referente ao ano-base 2025 e foi disponibilizado conforme cronograma legal.");
        lista.add(i1);

        ComprovanteItemDto i2 = new ComprovanteItemDto();
        i2.setCodAgrupacao("01");
        i2.setDescAgrupacao("VENCIMENTOS");
        i2.setNumItemAgrupacao("02");
        i2.setDescAgrupacaoItem("Contribuição previdenciária oficial");
        i2.setValorItem(new BigDecimal("7120.50"));
        i2.setNumQtdItem(new BigDecimal("12"));
        lista.add(i2);

        ComprovanteItemDto i3 = new ComprovanteItemDto();
        i3.setCodAgrupacao("02");
        i3.setDescAgrupacao("RRA");
        i3.setNumItemAgrupacao("01");
        i3.setDescAgrupacaoItem("RRA - Parcela");
        i3.setValorItem(new BigDecimal("12500.55"));
        i3.setNumQtdItem(new BigDecimal("1"));
        lista.add(i3);

        return lista;
    }

    private List<RraItemDto> simularRraItens() {
        List<RraItemDto> lista = new ArrayList<>();

        RraItemDto r1 = new RraItemDto();
        r1.setDataPagamentoRRA("10/05/2025");
        r1.setVlDataPagamento("10/05/2025");
        r1.setNumItemAgrupacao("1");
        r1.setVlQuantityAsString("5");
        r1.setVlQuantidadeMeses("5");
        lista.add(r1);

        RraItemDto r2 = new RraItemDto();
        r2.setDataPagamentoRRA("11/06/2025");
        r2.setVlDataPagamento("11/06/2025");
        r2.setNumItemAgrupacao("2");
        r2.setVlQuantityAsString("1");
        r2.setVlQuantidadeMeses("1");
        lista.add(r2);

        RraItemDto r3 = new RraItemDto();
        r3.setDataPagamentoRRA("12/07/2025");
        r3.setVlDataPagamento("12/07/2025");
        r3.setNumItemAgrupacao("3");
        r3.setVlQuantityAsString("1");
        r3.setVlQuantidadeMeses("1");
        lista.add(r3);

        return lista;
    }

    private List<CompItemDto> simularItensComplementares() {
        List<CompItemDto> lista = new ArrayList<>();
        CompItemDto c = new CompItemDto();
        c.setCodAgrupacao("07");
        c.setNumItemAgrupacao("01");
        c.setDescAgrupacaoItem("IAMSP");
        c.setValorItem(new BigDecimal("500"));
        lista.add(c);
        return lista;
    }

    // ---------------------- compile helper ----------------------

    private JasperReport compileReportForceCompile(File fileJrxml) {
        try {
            if (fileJrxml == null || !fileJrxml.exists()) {
                throw new IllegalStateException("Arquivo JRXML não encontrado: " + (fileJrxml == null ? "null" : fileJrxml.getAbsolutePath()));
            }

            try {
                String content = java.nio.file.Files.readString(fileJrxml.toPath(), StandardCharsets.UTF_8);
                long occurrences = java.util.regex.Pattern.compile("name\\s*=\\s*\"REPORT_LOCALE\"").matcher(content).results().count();
                if (occurrences > 1) {
                    log.warn("Detectadas {} declarações de REPORT_LOCALE no JRXML {}. Verifique se isto é intencional.", occurrences, fileJrxml.getName());
                }
            } catch (Exception ex) {
                log.debug("Não foi possível ler conteúdo do JRXML para checagem textual: {}", ex.getMessage());
            }

            try {
                return JasperCompileManager.compileReport(fileJrxml.getAbsolutePath());
            } catch (JRException jre) {
                log.error("Erro ao compilar JRXML {}: {}", fileJrxml.getAbsolutePath(), jre.getMessage(), jre);
                throw new IllegalStateException("Erro ao compilar o JRXML '" + fileJrxml.getName() + "'. Causa: " + jre.getMessage(), jre);
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("Falha ao preparar relatório: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao carregar/compilar relatório: " + e.getMessage(), e);
        }
    }

    // ---------------------- ENUM de mapeamento codAgr+numItem -> parâmetro JRXML ----------------------

    private enum ParamMapping {
        // Mapeamentos ajustados com base no Excel fornecido (Cod Par = num item, CodPar Associado = cod agrupacao)
        TOTAL_DE_RENDIMENTOS_INCLUSIVE_F_RIAS("01", "01", "vlTotalDeRendimentos"), // 01 - TOTAL DE RENDIMENTOS (INCLUSIVE FÉRIAS)
        CONTRIBUICAO_PREVIDENCIARIA_OFICIAL("01", "02", "vlContribuicaoPrevidenciariaOficial"), // 02 - CONTRIBUIÇÃO PREVIDENCIÁRIA OFICIAL
        IMPOSTO_DE_RENDA_RETIDO_NA_FONTE("01", "03", "vlImpostoDeRendaRetidoNaFonte"), // 03 - IMPOSTO DE RENDA RETIDO NA FONTE
        COMPL_PMSPPREV_COD_3533("01", "04", "vlComplPmspprevCod3533"), // 04 - COMPL-PMSPPREV-COD 3533
        PENS_AO_JUDICIAL_PA_VIA_RECIBO("01", "05", "vlPensaoJudicialPAViaRecibo"), // 05 - PENSÃO JUDICIAL / P.A. VIA RECIBO

        TOTAL_DOS_RENDIMENTOS_RRA("02", "06", "vlTotalDosRendimentosRra"), // 06 - TOTAL DOS RENDIMENTOS - RRA
        CONTRIBUICAO_PREVIDENCIARIA_OFICIAL_RRA("02", "07", "vlContribuicaoPrevidenciariaOficialRra"), // 07 - CONTRIBUIÇÃO PREVIDENCIÁRIA OFICIAL - RRA
        PENS_AO_ALIMENTICIA_RRA("02", "08", "vlPensaoAlimenticiaRra"), // 08 - PENSÃO ALIMENTÍCIA - RRA
        IMPOSTO_DE_RENDA_RETIDO_NA_FONTE_RRA("02", "09", "vlImpostoDeRendaRetidoNaFonteRra"), // 09 - IMPOSTO DE RENDA RETIDO NA FONTE - RRA
        QUANTIDADE_DE_MESES_RRA("02", "10", "vlQuantidadeDeMesesRra"), // 10 - QUANTIDADE DE MESES - RRA
        DATA_DE_RECEBIMENTO("02", "11", "vlDataDeRecebimento"), // 11 - DATA DE RECEBIMENTO

        AUXILIO_SAUDE_RES_8582008("03", "12", "vlAuxilioSaudeRes8582008"), // 12 - AUXÍLIO SAÚDE – RES 858/2008
        AUX_EDUCACAO_LC_1402_24("03", "13", "vlAuxEducacaoLc140224"), // 13 - AUX. EDUCAÇÃO – LC 1402/24
        AUXILIO_PRE_ESCOLAR("03", "14", "vlAuxilioPreEscolar"), // 14 - AUXÍLIO PRÉ-ESCOLAR
        AUXILIO_INCLUSAO_LC_1402_24("03", "15", "vlAuxilioInclusaoLc140224"), // 15 - AUXÍLIO INCLUSÃO – LC 1402/24
        INDENIZACOES("03", "16", "vlIndenizacoes"), // 16 - INDENIZAÇÕES
        DIARIAS_AJUDA_DE_CUSTO("03", "17", "vlDiariasAjudaDeCusto"), // 17 - DIÁRIAS E AJUDA DE CUSTO
        ASS_SAUDE_DEPUT_ATO_033_2024("03", "18", "vlAssSaudeDeputAto0332024"), // 18 - ASS SAÚDE DEPUT-ATO 033/2024

        DECIMO_TERCEIRO_SALARIO("04", "19", "vlDecimoTerceiroSalario"), // 19 - DÉCIMO-TERCEIRO SALÁRIO
        IMPOSTO_DE_RENDA_RETIDO_13("04", "20", "vlImpostoDeRendaRetido13"), // 20 - IMPOSTO DE RENDA RETIDO – 13

        PENSIONISTAS("05", "21", "vlPensionistas"), // 21 - PENSIONISTAS
        IAMSPE_BENEFICIARIOS("05", "22", "vlIamspeBeneficiarios"), // 22 - IAMSPE - BENEFICIÁRIOS
        IAMSPE_AGREGADOS_LEI_11125_2002("05", "23", "vlIamspeAgregadosLei111252002"), // 23 - IAMSPE - AGREGADOS LEI 11.125/2002
        IAMSPE_ODONTO("05", "24", "vlIamspeOdonto"), // 24 - IAMSPE - ODONTO

        ASS_SAUDE_DEPUT_ATO_17_2012("03", "25", "vlAssSaudeDeputAto172012"), // 25 - ASS SAÚDE DEPUT-ATO17/2012 (grupo 03 conforme planilha)
        DEPENDENTES("05", "26", "vlDependentes"), // 26 - DEPENDENTES
        IAMSPE("05", "27", "vlIamspe"), // 27 - IAMSPE
        DEDUCOES_13_SALARIO("02", "28", "vlDeducoes13Salario"), // 28 - DEDUCOES 13. SALARIO (grupo 02)
        IMPOSTO_DE_RENDA_RETIDO_13_SAL_RRA("02", "30", "vlImpostoDeRendaRetido13Sal"), // 30 - IMPOSTO DE RENDA RETIDO - 13. SAL. - RRA
        PREVIDENCIA_COMPLEM_SPPREVCOM("01", "31", "vlPrevidenciaComplemSpprevcom"); // 31 - PREVIDENCIA COMPLEM-SPPREVCOM

        final String cod;
        final String item;
        final String paramName;
        ParamMapping(String cod, String item, String paramName) {
            this.cod = cod;
            this.item = item;
            this.paramName = paramName;
        }

        static String findParam(String codIn, String itemIn) {
            if (codIn == null) codIn = "";
            if (itemIn == null) itemIn = "";
            String cod = codIn.trim();
            String item = itemIn.trim();
            // normalize removing left zeros
            cod = cod.replaceFirst("^0+(?!$)", "");
            item = item.replaceFirst("^0+(?!$)", "");
            for (ParamMapping m : values()) {
                if (m.cod.replaceFirst("^0+(?!$)", "").equals(cod) && m.item.replaceFirst("^0+(?!$)", "").equals(item)) return m.paramName;
            }
            // fallback: match only cod
            for (ParamMapping m : values()) {
                if (m.cod.replaceFirst("^0+(?!$)", "").equals(cod) && (m.item == null || m.item.isEmpty())) return m.paramName;
            }
            return null;
        }
    }

    // ---------------------- DTOs internos (RRA e COMP) ----------------------

    @Data
    public static class RraItemDto {
        private String codAgrupacao;
        private String numItemAgrupacao;
        private String descAgrupacaoItem;
        private String dataPagamentoRRA;
        private String vlDataPagamento;
        private String vlQuantidadeMeses;
        private Integer numQtdMeses;
        // mantendo opcional: se a sua view RRA não retorna valorItem, não será populado
        private BigDecimal valorItem;
        // helper simulação
        private String vlQuantityAsString;
        public void setVlQuantityAsString(String s){ this.vlQuantityAsString = s; }
    }

    @Data
    public static class CompItemDto {
        private String codAgrupacao;
        private String numItemAgrupacao;
        private String descAgrupacaoItem;
        private BigDecimal valorItem;
    }
    
    private String buscarNomeBeneficiarioPorCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return "";
        }

        String sql =
            "SELECT TRABALHADOR_NMTRAB " +
            "FROM ( " +
            "   SELECT A.TRABALHADOR_NMTRAB " +
            "   FROM VW_TSOC_CONS_PERIODOS_CPF A " +
            "   WHERE A.TRABALHADOR_CPFTRAB = ? " +
            ") WHERE ROWNUM = 1";

        try {
            return jdbcTemplate.queryForObject(sql, String.class, cpf);
        } catch (EmptyResultDataAccessException e) {
            // CPF existe mas não retornou nome
            log.warn("Nenhum nome encontrado na VW_TSOC_CONS_PERIODOS_CPF para CPF {}", cpf);
            return "";
        } catch (BadSqlGrammarException bsg) {
            String causeMsg = bsg.getMostSpecificCause() != null
                    ? bsg.getMostSpecificCause().getMessage()
                    : bsg.getMessage();
            log.error("Erro de SQL ao buscar nome do beneficiário. Detalhe: {}", causeMsg, bsg);
            throw new DataAccessResourceFailureException(
                "Erro ao buscar nome do beneficiário: " + causeMsg, bsg);
        }
    }

}
