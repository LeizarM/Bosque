package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dto.BitacoraCumplimientoDto;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El PDF de la bitácora de cumplimiento compila, se llena desde
 * {@link BitacoraCumplimientoDto} y dice lo que tiene que decir, sin Spring ni
 * base de datos.
 *
 * <p>Lo que se fija: que {@code estadoTexto} (un getter sin campo) llegue al
 * reporte, que la observación salga debajo de la tarea, y que los totales del
 * encabezado se impriman. Si alguien renombra un getter del DTO, el reporte no
 * falla al compilar: sale con la columna vacía. Este test es el que lo nota.
 */
class RptBitacoraCumplimientoSmokeTest {

    private static BitacoraCumplimientoDto fila(String cumplimiento, String tarea, String obs) {
        BitacoraCumplimientoDto f = new BitacoraCumplimientoDto();
        f.setIdBitTarea(1);
        f.setFechaPresentacion(new Date());
        f.setNombreEmpleado("QUISPE MAMANI RONALDO");
        f.setDescripcionCargo("CAJERO");
        f.setNombreSucursal("CENTRAL");
        f.setNombreTareaRutinaria(tarea);
        f.setDescripcionFrecuencia("Diario");
        f.setCumplimiento(cumplimiento);
        f.setObs(obs);
        return f;
    }

    @Test
    void compilaLlenaYMuestraEstadoObservacionYTotales() throws Exception {
        JasperReport reporte;
        try (InputStream jrxml = getClass().getResourceAsStream("/reports/RptBitacoraCumplimiento.jrxml")) {
            assertNotNull(jrxml, "No está reports/RptBitacoraCumplimiento.jrxml en el classpath.");
            reporte = JasperCompileManager.compileReport(jrxml);
        }

        List<BitacoraCumplimientoDto> filas = Arrays.asList(
                fila("R", "Arqueo de Caja", null),
                fila("N", "Verificar traspaso Caja AXA contra movimiento de caja", null),
                fila("A", "Verificar Traspaso de Efectivo Entre Sistemas",
                        "No aplica. Cerrada en bloque el 2026-09-11"),
                fila("P", "Cierre de Operaciones", "   "));

        Map<String, Object> params = new HashMap<>();
        params.put(JRParameter.REPORT_LOCALE, new Locale("es"));
        params.put("fechaGeneracion", new Date());
        params.put("rango", "Del 01/09/2026 al 11/09/2026   ·   Tu equipo");
        params.put("totales", "Realizadas 1   ·   No realizadas 1   ·   Cumplimiento 50 %");

        JasperPrint print = JasperFillManager.fillReport(reporte, params, new JRBeanCollectionDataSource(filas));
        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertTrue(pdf.length > 0);

        StringBuilder texto = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                texto.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
        }
        String t = texto.toString();

        assertTrue(t.contains("Realizada"), "Falta el estado en palabras (getEstadoTexto).");
        assertTrue(t.contains("No realizada"), "Falta 'No realizada'.");
        assertTrue(t.contains("En plazo"), "Falta 'En plazo'.");
        assertTrue(t.contains("Obs.: No aplica. Cerrada en bloque"), "La observación no salió debajo de la tarea.");
        assertTrue(!t.contains("Obs.:    "), "Una observación en blanco no debería imprimir 'Obs.:'.");
        assertTrue(t.contains("Cumplimiento 50 %"), "Faltan los totales del encabezado.");
        assertTrue(t.contains("Tu equipo"), "Falta el rango y el alcance.");
    }
}
