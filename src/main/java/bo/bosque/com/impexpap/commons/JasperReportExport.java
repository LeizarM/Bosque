package bo.bosque.com.impexpap.commons;

import net.sf.jasperreports.engine.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;


import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.Connection;

import java.util.Map;



@Service
public class JasperReportExport {

    private static final Logger logger = LoggerFactory.getLogger(JasperReportExport.class);
    private static final String REPORT_FOLDER = "reports";
    private static final String SUBREPORT_DIR = "reports/";
    private static final String JRXML = ".jrxml";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public JasperReportExport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Method to export a Jasper Report
     * @param fileName
     * @param params
     * @return
     */
    public byte[] exportPDF(String fileName, Map<String, Object> params) {
        byte[] reportBytes;
        String path = REPORT_FOLDER + "/" + fileName + JRXML;
        params.put("SUBREPORT_DIR", SUBREPORT_DIR);
        try (
                Connection conn = jdbcTemplate.getDataSource().getConnection();
                InputStream reportInputStream = new ClassPathResource(path).getInputStream();
        ) {
            JasperReport jasperReport = JasperCompileManager.compileReport(reportInputStream);

            JasperPrint report = JasperFillManager.fillReport(jasperReport, params, conn);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(report, byteArrayOutputStream);
            reportBytes = byteArrayOutputStream.toByteArray();

        } catch (Exception e) {
            logger.error("Error exporting PDF: {}", e.getMessage(), e);
            reportBytes = null;
        }

        return reportBytes;
    }


    /**
     * Regresara el reporte en Bytes
     * @param jasperPrint
     * @return
     * @throws JRException

    protected ByteArrayOutputStream getByteArrayOutputStream( JasperPrint jasperPrint ) throws JRException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);
        return byteArrayOutputStream;
    } */


}
