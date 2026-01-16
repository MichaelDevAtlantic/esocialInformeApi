package com.atlantic.esocial.teste;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

@Service
public class TesteService {
       

 public Map<String, Object> gerar() {

    String jrxmlFileName = "Teste_Relatorio";

    HashMap<String, Object> parameters = new HashMap();
    parameters.put("cpf",  "11111111111");
    parameters.put("nome", "Francibelde");

    String encodedString = Base64.getEncoder().withoutPadding().encodeToString(gerarDocumento(parameters, jrxmlFileName));

    Map<String, Object> mapaRetorno = new HashMap<String, Object>();
    mapaRetorno.put("formato", "base64");
    mapaRetorno.put("PDF", encodedString);
    return mapaRetorno;
 }

 private byte[] gerarDocumento(Map<String, Object> reportParam, String jrxmlFileName) {

  byte[] pdfStream = null;

  try {

   // Locale locale = RCU.getLocale(RCH.currentRequestAttributes().getRequest());
   reportParam.put("REPORT_LOCALE", new Locale("pt", "BR"));

   File fileJrxml = ResourceUtils.getFile("classpath:reports/"+jrxmlFileName+".jrxml");
   //File fileJrxml = ResourceUtils.getFile("/u01/reports/" + jrxmlFileName + ".jrxml");
   File fileJasper = null;
   //try {
   //fileJasper = ResourceUtils.getFile("classpath:reports/"+jrxmlFileName+".jasper");
   //fileJasper = ResourceUtils.getFile("/u01/reports/" + jrxmlFileName + ".jasper");
   //} catch (Exception e) {
   // e.printStackTrace();
   //}

   File filePath = ResourceUtils.getFile("classpath:reports/");
   //File filePath = ResourceUtils.getFile("/u01/reports/");

   //File fileBrasao = ResourceUtils.getFile("classpath:static/images/teste.png");
   //File fileBrasao = ResourceUtils.getFile("/u01/reports/images/80px-Brasao_Estado_SaoPaulo_Brasil_svg.png");
   
   JasperReport jasperReport;
   if (fileJasper != null && fileJasper.exists()) {
    jasperReport = (JasperReport) JRLoader.loadObject(fileJasper);
   } else {
    jasperReport = JasperCompileManager.compileReport(fileJrxml.getAbsolutePath());
   }

   reportParam.put("REPORT_BASE_PATH", filePath.getAbsolutePath() + "/");
   //reportParam.put("PARAM_FIG1", fileBrasao.getAbsolutePath());

   JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParam, new JREmptyDataSource());

   pdfStream = JasperExportManager.exportReportToPdf(jasperPrint);

  } catch (Exception e) {
   throw new RuntimeException("It's not possible to generate the pdf report.", e);
  } finally {
  }

  return pdfStream;
 }
}
