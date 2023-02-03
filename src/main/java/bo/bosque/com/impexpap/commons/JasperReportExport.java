package bo.bosque.com.impexpap.commons;



import net.sf.jasperreports.engine.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Repository
public class JasperReportExport {

    private static final String REPORT_FOLDER = "reports";
    private static final String JRXML = ".jrxml";



    private JdbcTemplate jdbcTemplate;
    private Connection conn;
    public JasperReportExport( JdbcTemplate jdbcTemplate ) {
        this.jdbcTemplate = jdbcTemplate;
    }



    /**
     * Metodo para exporta a pdf
     * @param fileName
     * @param params
     * @return
     */
    public byte[] exportPDF( String fileName, Map<String, Object> params)    {

        byte[] reportBytes;
        //String path = "\\"+REPORT_FOLDER+"\\"+fileName+JRXML;
        String path = REPORT_FOLDER+"\\"+fileName+JRXML;
        System.out.println(path);
        try {
            this.conn = Objects.requireNonNull(this.jdbcTemplate.getDataSource()).getConnection();

            JasperPrint report = JasperFillManager.fillReport
                            (
                                    JasperCompileManager.compileReport(
                                            ResourceUtils.getFile("classpath:"+path)
                                                    .getAbsolutePath()) // path of the jasper report
                                    , params // dynamic parameters
                                    , this.conn
                            );



            //create the report in PDF format
            reportBytes = JasperExportManager.exportReportToPdf( report );



        }catch (Exception e){
            System.out.println(e.getMessage() + "exportPDF msg: "+ e.getCause());
            reportBytes = null;
        }
        this.jdbcTemplate = null;
        return reportBytes;

    }



}
