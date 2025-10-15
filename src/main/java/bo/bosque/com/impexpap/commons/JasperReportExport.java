package bo.bosque.com.impexpap.commons;

import net.sf.jasperreports.engine.*;

import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.Connection;
import java.util.Map;
import java.util.HashMap;

@Service
public class JasperReportExport {

    private static final Logger logger = LoggerFactory.getLogger(JasperReportExport.class);
    private static final String REPORT_FOLDER = "reports";
    private static final String SUBREPORT_DIR = "reports/";
    private static final String JRXML = ".jrxml";
    private static final String JASPER = ".jasper";

    private JdbcTemplate jdbcTemplate;

    @Value("${uploads.dir:/app/uploads}")  // Lee de properties, default a /app/uploads
    private String uploadsDir;

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

    /**
     * Exporta el reporte a PDF compilando runtime y usando params.
     * @param fileName Path del JRXML principal (e.g., "reports/RptFichaTrabajador.jrxml")
     * @param params Mapa de parámetros (incluye codEmpleado, etc.)
     * @return byte[] del PDF generado
     */
    public byte[] exportPDF(String fileName, Map<String, Object> params) {
        Connection conn = null;
        try {
            // conn = dataSource.getConnection();  // Descomenta si usas DataSource

            logger.info("Iniciando exportPDF para: " + fileName);

            // Compilando subreportes (con chequeo de null)
            logger.info("Compilando subreportes...");
            InputStream subStream = getClass().getResourceAsStream("/reports/subRptDependiente.jrxml");  // Asegura path completo
            if (subStream == null) {
                throw new RuntimeException("Subreporte no encontrado: /reports/subRptDependiente.jrxml");
            }
            JasperReport subRptDependientes = JasperCompileManager.compileReport(subStream);
            params.put("subRptDependientes", subRptDependientes);
            logger.info("Subreporte subRptDependientes compilado correctamente");

            // Agrega más subreportes si es necesario, e.g.:
            // InputStream subAnexosStream = getClass().getResourceAsStream("/reports/subRptAnexos.jrxml");
            // if (subAnexosStream == null) throw new RuntimeException("Subreporte no encontrado: /reports/subRptAnexos.jrxml");
            // JasperReport subRptAnexos = JasperCompileManager.compileReport(subAnexosStream);
            // params.put("subRptAnexos", subRptAnexos);

            // Compilando reporte principal (chequea null)
            logger.info("Compilando reporte principal: " + fileName);
            InputStream reportStream = getClass().getResourceAsStream("/" + fileName);  // "/" asegura root de classpath
            if (reportStream == null) {
                logger.error("Reporte principal no encontrado – verifica si existe en src/main/resources/" + fileName + " y en el JAR");
                throw new RuntimeException("Reporte principal no encontrado en classpath: /" + fileName);
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // Para precompilado (opcional: cambia fileName a "reports/RptFichaTrabajador.jasper" y usa esto):
            // JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportStream);

            // Configura UPLOADS_DIR
            String baseDir = uploadsDir.endsWith("/") ? uploadsDir : uploadsDir + "/";
            params.put("UPLOADS_DIR", baseDir);

            Object codEmpleado = params.get("codEmpleado");
            if (codEmpleado != null) {
                logger.info("Mapeando UPLOADS_DIR para imágenes: " + baseDir + " (ejemplo: " + baseDir + codEmpleado + ".jpg)");
            }

            params.put("SUBREPORT_DIR", "reports/");

            // Fill report
            logger.info("Iniciando fillReport con params: " + params.keySet());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, conn);

            // Export to PDF
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(byteArrayOutputStream));

            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            configuration.setCompressed(true);
            exporter.setConfiguration(configuration);

            exporter.exportReport();
            logger.info("PDF generado exitosamente");

            return byteArrayOutputStream.toByteArray();

        } catch (JRException e) {
            logger.error("Error en exportPDF (posible issue con path o XML en JRXML): " + e.getMessage(), e);
            throw new RuntimeException("Error exporting PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error inesperado en exportPDF: " + e.getMessage(), e);
            throw new RuntimeException("Error exporting PDF: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }


    /**
     * Compila el reporte principal
     */
    private JasperReport compileMainReport(String path) throws JRException, IOException {
        logger.info("Compilando reporte principal: {}", path);

        try (InputStream reportInputStream = new ClassPathResource(path).getInputStream()) {
            return JasperCompileManager.compileReport(reportInputStream);
        }
    }

    /**
     * Compila todos los subreportes y los añade como parámetros
     */
    private void compileSubreports(Map<String, Object> params) {
        logger.info("Compilando subreportes...");

        // Lista de subreportes conocidos - añade aquí todos los que uses
        String[] subreportNames = {
                "subRptDependientes",
                // Añade otros subreportes aquí si los tienes
        };

        for (String subreportName : subreportNames) {
            try {
                JasperReport compiledSubreport = compileSubreport(subreportName);
                if (compiledSubreport != null) {
                    // Usar diferentes variaciones del nombre del parámetro
                    params.put(subreportName, compiledSubreport);
                    params.put(subreportName + ".jasper", compiledSubreport);
                    params.put(subreportName.toUpperCase(), compiledSubreport);
                    params.put("SUB_" + subreportName.toUpperCase(), compiledSubreport);

                    logger.info("Subreporte {} compilado correctamente", subreportName);
                }
            } catch (Exception e) {
                logger.warn("No se pudo compilar el subreporte {}: {}", subreportName, e.getMessage());
            }
        }

        // También establecer el directorio de subreportes
        params.put("SUBREPORT_DIR", SUBREPORT_DIR);
    }

    /**
     * Compila un subreporte específico
     */
    private JasperReport compileSubreport(String subreportName) throws JRException, IOException {
        String subreportPath = REPORT_FOLDER + "/" + subreportName + JRXML;

        logger.debug("Intentando compilar subreporte: {}", subreportPath);

        Resource resource = new ClassPathResource(subreportPath);
        if (!resource.exists()) {
            logger.warn("Subreporte no encontrado: {}", subreportPath);
            return null;
        }

        try (InputStream subreportInputStream = resource.getInputStream()) {
            return JasperCompileManager.compileReport(subreportInputStream);
        }
    }

    /**
     * Método alternativo si prefieres usar archivos .jasper precompilados
     */
    public byte[] exportPDFPrecompiled(String fileName, Map<String, Object> params) {
        byte[] reportBytes;
        String jasperPath = REPORT_FOLDER + "/" + fileName + JASPER;

        Map<String, Object> reportParams = new HashMap<>(params);
        reportParams.put("SUBREPORT_DIR", SUBREPORT_DIR);

        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {

            // Cargar subreportes precompilados
            loadPrecompiledSubreports(reportParams);

            // Cargar reporte principal precompilado
            try (InputStream reportInputStream = new ClassPathResource(jasperPath).getInputStream()) {
                JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportInputStream);

                JasperPrint report = JasperFillManager.fillReport(jasperReport, reportParams, conn);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                JasperExportManager.exportReportToPdfStream(report, byteArrayOutputStream);
                reportBytes = byteArrayOutputStream.toByteArray();
            }

        } catch (Exception e) {
            logger.error("Error exporting PDF with precompiled reports: {}", e.getMessage(), e);
            reportBytes = null;
        }

        return reportBytes;
    }

    public byte[] exportPDFStatic(String fileName, Map<String, Object> params) {
        byte[] reportBytes = null;
        String jasperPath = REPORT_FOLDER + "/" + fileName + JASPER;  // e.g., reports/RptFichaTrabajador.jasper

        // Log para depurar el valor inyectado de uploadsDir
        logger.info("Valor inyectado de uploadsDir: " + uploadsDir);  // Debería mostrar el valor o null

        // Asegura SUBREPORT_DIR
        if (!params.containsKey("SUBREPORT_DIR")) {
            params.put("SUBREPORT_DIR", REPORT_FOLDER + "/");  // "reports/"
        }

        // Configura UPLOADS_DIR con chequeo de null
        String baseDir;
        if (uploadsDir == null) {
            logger.warn("uploadsDir no inyectado (es null) – usando default hardcoded: /app/uploads/");
            baseDir = "/app/uploads/";  // Default hardcoded – ajusta si necesitas otro path (e.g., "/uploads/")
        } else {
            baseDir = uploadsDir.endsWith("/") ? uploadsDir : uploadsDir + "/";
        }
        params.put("UPLOADS_DIR", baseDir);
        logger.info("UPLOADS_DIR configurado como: " + baseDir);  // Log para confirmar

        Object codEmpleado = params.get("codEmpleado");
        if (codEmpleado != null) {
            logger.info("Mapeando UPLOADS_DIR para imágenes: " + baseDir + " (ejemplo: " + baseDir + codEmpleado + ".jpg)");
        } else {
            logger.warn("codEmpleado no proporcionado en params – verifica el llamado desde el controller");
        }

        Connection conn = null;
        try {
            if (jdbcTemplate == null || jdbcTemplate.getDataSource() == null) {
                throw new RuntimeException("jdbcTemplate o DataSource es null – verifica la inyección en el constructor");
            }
            conn = jdbcTemplate.getDataSource().getConnection();
            logger.info("Conexión a DB obtenida exitosamente");

            // Carga reporte principal
            InputStream reportStream = getClass().getResourceAsStream("/" + jasperPath);
            if (reportStream == null) {
                logger.error("Reporte principal .jasper no encontrado – verifica src/main/resources/" + jasperPath + " y en el JAR");
                throw new RuntimeException("Reporte principal no encontrado: /" + jasperPath);
            }
            logger.info("InputStream para reporte principal obtenido: " + jasperPath);

            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportStream);
            logger.info("Reporte principal cargado estáticamente: " + jasperPath);

            // Jasper carga subreportes de SUBREPORT_DIR automáticamente durante fill
            JasperPrint report = JasperFillManager.fillReport(jasperReport, params, conn);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(report, baos);
            reportBytes = baos.toByteArray();
            logger.info("Reporte generado sin compilación runtime");
        } catch (Exception e) {
            logger.error("Error en exportPDFStatic: " + e.getMessage(), e);
            throw new RuntimeException("Error exporting PDF: " + e.getMessage(), e);  // Relanza para que el controller lo capture
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    logger.info("Conexión a DB cerrada");
                } catch (Exception ignored) {
                    logger.warn("Error al cerrar conexión: " + ignored.getMessage());
                }
            }
        }
        return reportBytes;
    }

    /**
     * Carga subreportes precompilados (.jasper)
     */
    private void loadPrecompiledSubreports(Map<String, Object> params) {
        String[] subreportNames = {
                "subRptDependientes",
                // Añade otros subreportes aquí
        };

        for (String subreportName : subreportNames) {
            try {
                JasperReport subreport = loadPrecompiledSubreport(subreportName);
                if (subreport != null) {
                    params.put(subreportName, subreport);
                    logger.info("Subreporte precompilado {} cargado correctamente", subreportName);
                }
            } catch (Exception e) {
                logger.warn("No se pudo cargar el subreporte precompilado {}: {}", subreportName, e.getMessage());
            }
        }
    }

    /**
     * Carga un subreporte precompilado específico
     */
    private JasperReport loadPrecompiledSubreport(String subreportName) throws JRException, IOException {
        String subreportPath = REPORT_FOLDER + "/" + subreportName + JASPER;

        Resource resource = new ClassPathResource(subreportPath);
        if (!resource.exists()) {
            logger.warn("Subreporte precompilado no encontrado: {}", subreportPath);
            return null;
        }

        try (InputStream subreportInputStream = resource.getInputStream()) {
            return (JasperReport) JRLoader.loadObject(subreportInputStream);
        }
    }
}