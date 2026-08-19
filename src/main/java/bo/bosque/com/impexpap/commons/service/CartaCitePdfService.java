package bo.bosque.com.impexpap.commons.service;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Genera los PDF del módulo Cartas CITE a partir de los reportes Jasper que
 * venían con el módulo JSF.
 *
 * <p><b>Por qué no usa {@code JasperReportExport.exportPDFStatic}:</b> ese
 * método carga los {@code .jasper} ya compilados. Los del módulo viejo se
 * compilaron con iReport hace años y este backend corre JasperReports 6.21.2;
 * un {@code .jasper} es un objeto Java serializado, así que una diferencia de
 * versión revienta al deserializar y no hay forma de arreglarlo sin volver a
 * compilar. Acá se compilan los {@code .jrxml} —que son XML y sí son estables
 * entre versiones— la primera vez que se pide cada reporte, y el resultado
 * queda cacheado en memoria.
 *
 * <p>Los subreportes son el motivo del directorio temporal: dentro del JRXML
 * están referenciados como {@code $P{SUBREPORT_DIR} + "subRptX.jasper"}, o sea
 * por ruta de archivo. Se compilan una vez, se escriben en un temporal y
 * {@code SUBREPORT_DIR} apunta ahí.
 *
 * <p>Las consultas de los reportes siguen llamando al SP viejo
 * {@code p_list_registroDoc} (ACCIONes C, D, E, F, K), que no se tocó. Los
 * PDF salen idénticos a los del sistema anterior — que es justamente lo que
 * se quiere: son documentos que ya se archivaron en papel.
 */
@Service
public class CartaCitePdfService {

    private static final Logger logger = LoggerFactory.getLogger(CartaCitePdfService.class);

    /** Subreportes que hay que dejar compilados en disco antes de llenar. */
    private static final List<String> SUBREPORTES = Arrays.asList(
            "subRptCCArch", "subRptCopiaEncabezado", "subRptRemitente");

    /**
     * Carpeta en el classpath de las imágenes que son del formato y no de la
     * empresa: el logo al agua y el logo izquierdo. Sólo las imprime este
     * módulo, así que viajan adentro del jar con los jrxml.
     */
    private static final String DIR_LOGOS = "/logos/";

    /** Membrete de respaldo cuando la empresa no tiene el suyo en disco. */
    private static final String MEMBRETE_POR_DEFECTO = DIR_LOGOS + "logoEmpresa.jpg";  // IMPEXPAP

    private final JdbcTemplate jdbcTemplate;

    /**
     * Raíz de los archivos subidos ({@code ./uploads} en dev, {@code /app/uploads}
     * en producción). De ahí cuelga {@code logos/}; ver {@link #membreteDe(Object)}.
     */
    private final String uploadsDir;

    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();

    /** Se resuelve una sola vez, la primera vez que se genera un PDF. */
    private volatile Path dirSubreportes;

    public CartaCitePdfService(JdbcTemplate jdbcTemplate,
                               @Value("${uploads.dir:/app/uploads}") String uploadsDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.uploadsDir = uploadsDir;
    }

    /**
     * Llena un reporte contra la base y devuelve el PDF.
     *
     * @param nombreReporte nombre del jrxml sin extensión, en {@code resources/reports}
     * @param params        parámetros del reporte; se le agregan los logos y SUBREPORT_DIR
     */
    public byte[] generar(String nombreReporte, Map<String, Object> params) {
        try {
            prepararSubreportes();

            Map<String, Object> p = new HashMap<>(params);
            p.put("SUBREPORT_DIR", dirSubreportes.toAbsolutePath() + File.separator);

            /* Los logos son java.io.InputStream y se consumen al leerlos, así
               que se abren nuevos en cada generación. Cachearlos daría un PDF
               con logo la primera vez y sin logo el resto. */
            p.put("logoEmpresa",   membreteDe(params.get("codEmpresa")));
            p.put("logoIzquierdo", recurso(DIR_LOGOS + "logoIzquierdo.jpg"));
            p.put("logoAgua",      recurso(DIR_LOGOS + "logoEmpresaAgua.jpg"));

            /* RptCarta hace $P{logo}.equals("SI") sin chequear null: si el
               parámetro no llega, el reporte tira NullPointerException en vez
               de imprimir sin logo. */
            if (p.get("logo") == null) p.put("logo", "SI");

            JasperReport reporte = compilar(nombreReporte);

            try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
                JasperPrint print = JasperFillManager.fillReport(reporte, p, conn);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JasperExportManager.exportReportToPdfStream(print, baos);
                return baos.toByteArray();
            }

        } catch (Exception e) {
            logger.error("Error generando el PDF {} con params {}", nombreReporte, params, e);
            throw new RuntimeException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    /** Compila un jrxml de {@code resources/reports} y lo cachea. */
    private JasperReport compilar(String nombre) {
        return cache.computeIfAbsent(nombre, n -> {
            try (InputStream in = recurso("/reports/" + n + ".jrxml")) {
                logger.info("Compilando reporte CITE: {}.jrxml", n);
                return JasperCompileManager.compileReport(in);
            } catch (Exception e) {
                throw new RuntimeException("No se pudo compilar el reporte " + n + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Compila los subreportes a un directorio temporal la primera vez.
     *
     * <p>Doble chequeo con {@code volatile}: esto se llama desde peticiones
     * HTTP concurrentes y compilar tres reportes dos veces en paralelo, además
     * de desperdiciado, deja archivos a medio escribir.
     */
    private void prepararSubreportes() throws Exception {
        if (dirSubreportes != null) return;

        synchronized (this) {
            if (dirSubreportes != null) return;

            Path dir = Files.createTempDirectory("bosque-cite-reportes");
            dir.toFile().deleteOnExit();

            for (String sub : SUBREPORTES) {
                try (InputStream in = recurso("/reports/" + sub + ".jrxml")) {
                    JasperReport compilado = JasperCompileManager.compileReport(in);
                    File destino = dir.resolve(sub + ".jasper").toFile();
                    JRSaver.saveObject(compilado, destino);
                    destino.deleteOnExit();
                }
            }

            logger.info("Subreportes CITE compilados en {}", dir);
            dirSubreportes = dir;
        }
    }

    /**
     * Membrete de la empresa, leído de {@code <uploads.dir>/logos/<codEmpresa>.png}.
     *
     * <p>Es la misma convención que ya usa {@code RptPapeletaPago}, que arma la
     * ruta con {@code RUTA_LOGOS}. Un solo lugar para los logos de empresa: dar
     * de alta una es dejar caer su png ahí, sin tocar código ni recompilar.
     *
     * <p>Si el archivo no está —una empresa nueva, o un despliegue donde el
     * volumen de uploads todavía no se montó— cae en el de IMPEXPAP, que es lo
     * que imprimía el módulo JSF para todas las empresas.
     *
     * @param codEmpresa el parámetro {@code codEmpresa} del reporte, que puede
     *                   venir nulo: los formatos por documento no lo declaran y
     *                   lo reciben sólo para elegir acá.
     */
    private InputStream membreteDe(Object codEmpresa) {
        if (codEmpresa instanceof Number) {
            Path archivo = Paths.get(uploadsDir, "logos",
                    ((Number) codEmpresa).intValue() + ".png");
            if (Files.isReadable(archivo)) {
                try {
                    return Files.newInputStream(archivo);
                } catch (Exception e) {
                    logger.warn("No se pudo leer el membrete {}; se usa el de IMPEXPAP",
                            archivo, e);
                }
            } else {
                logger.debug("Sin membrete propio en {}; se usa el de IMPEXPAP", archivo);
            }
        }
        return recurso(MEMBRETE_POR_DEFECTO);
    }

    private InputStream recurso(String ruta) {
        InputStream in = getClass().getResourceAsStream(ruta);
        if (in == null) throw new RuntimeException("Recurso no encontrado en el classpath: " + ruta);
        return in;
    }
}
