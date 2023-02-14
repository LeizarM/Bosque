package bo.bosque.com.impexpap.commons;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;


@Repository
public class JasperReportExport {

    private static final String REPORT_FOLDER = "reports";
    private static final String SUBREPORT_DIR = "D:\\Proyectos\\Bosque\\Bosque\\src\\main\\resources\\reports\\";
    private static final String JRXML = ".jrxml";
    private static final String JASPER = ".jasper";


    private JdbcTemplate jdbcTemplate;
    private Connection conn;



    public JasperReportExport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Metodo para exporta a pdf
     * @param fileName
     * @param params
     * @return
     */
    public byte[] exportPDF( String fileName, Map<String, Object> params ){

        byte[] reportBytes;
        String path = "\\"+REPORT_FOLDER+"\\"+fileName+JRXML;

        params.put("SUBREPORT_DIR", SUBREPORT_DIR);
        try {

            this.conn = this.jdbcTemplate.getDataSource().getConnection();

            JasperPrint report = JasperFillManager.fillReport
                            (
                                    JasperCompileManager.compileReport(
                                            ResourceUtils.getFile("classpath:"+path)
                                                    .getAbsolutePath()) // path of the jasper report
                                    , params // dynamic parameters
                                    , this.conn
                            );

            //create the report in PDF format
            //reportBytes = JasperExportManager.exportReportToPdf( report );

            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(report));
            SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(new ByteArrayOutputStream());
            exporter.setExporterOutput(exporterOutput);
            exporter.exportReport();
            ByteArrayOutputStream byteArrayOutputStream = this.getByteArrayOutputStream(report);
            reportBytes = byteArrayOutputStream.toByteArray();


        }catch (Exception e){
            System.out.println(e.getMessage() + "  exportPDF msg: "+ e.getCause());
            reportBytes = null;
        }finally {
            try {
                this.conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage() + "  exportPDF close connection msg: " + e.getCause());
            }
        }
        this.jdbcTemplate = null;
        this.conn = null;
        return reportBytes;

    }


    /**
     * Regresara el reporte en Bytes
     * @param jasperPrint
     * @return
     * @throws JRException
     */
    protected ByteArrayOutputStream getByteArrayOutputStream( JasperPrint jasperPrint ) throws JRException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);
        return byteArrayOutputStream;
    }


}
