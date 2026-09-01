package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dto.AsistenciaDiaDto;
import bo.bosque.com.impexpap.dto.HorarioVigenteEmpleadoDto;
import bo.bosque.com.impexpap.dto.ResumenAsistenciaEmpleadoDto;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirma que los .jrxml del módulo Biométrico compilan y llenan de verdad
 * (no sólo que el XML es válido), sin arrancar Spring ni tocar la BD.
 * Regresión real, no descartable — atrapó tres bugs la primera vez que se
 * corrió cada uno: orden de bandas (pageFooter antes de summary), el
 * property de blankWhenNull que no alcanzaba solo, y hubiera atrapado
 * cualquier campo/getter mal escrito en el datasource.
 */
class RptBiometricoDetalladoSmokeTest {

    private static final Pattern PALABRA_NULL = Pattern.compile("\\bnull\\b");

    @Test
    void detalladoCompilaLlenaYNoImprimeNullLiteral() throws Exception {
        List<AsistenciaDiaDto> filas = new ArrayList<>();
        String[] estados = {
                "TRABAJADO", "FALTA", "FERIADO", "SABADO_LIBRE",
                "PERMISO", "VACACION", "SIN_HORARIO",
        };
        Date ahora = new Date();
        for (String estado : estados) {
            AsistenciaDiaDto d = new AsistenciaDiaDto();
            d.setFecha(ahora);
            d.setEstado(estado);
            d.setMotivo(estado.equals("TRABAJADO") || estado.equals("FALTA") ? null : "Motivo de prueba");
            // SIN_HORARIO no tiene ni hora esperada (no hay turno ese día) —
            // el resto sí, aunque no haya marcación real. Cubre los dos
            // caminos de null que existían en el reporte real.
            if (!estado.equals("SIN_HORARIO")) {
                d.setHoraEntradaEsperada(ahora);
                d.setHoraSalidaEsperada(ahora);
            }
            if (estado.equals("TRABAJADO")) {
                d.setHoraEntradaReal(ahora);
                d.setHoraSalidaReal(ahora);
                // Ejercita el resaltado de marcación manual (dorado/negrita)
                // y el de Min. Atraso (rojo/negrita) sobre fondo blanco.
                d.setEntradaManual(true);
                d.setMinutosAtraso(45);
            }
            if (estado.equals("FALTA")) {
                // Mismo resaltado de atraso, pero encima del fondo rojo de
                // FALTA — confirma que las dos condiciones (fondo por estado
                // + texto por atraso) se aplican juntas, no que una pisa a
                // la otra.
                d.setMinutosAtraso(180);
            }
            // PERMISO/VACACION son los únicos con ventana de permiso — el resto
            // debe quedar en blanco (no "null" literal). minutosAtraso ya no es
            // siempre null (es int, no Integer) — sólo queda en 0 para los
            // estados que no son TRABAJADO/FALTA, isBlankWhenNull no aplica ahí.
            if (estado.equals("PERMISO") || estado.equals("VACACION")) {
                d.setHoraInicioPermiso(ahora);
                d.setHoraFinPermiso(ahora);
            }
            filas.add(d);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("nombreEmpleado", "PRUEBA APELLIDO NOMBRE");
        params.put("mesAnio", "Agosto 2026");

        String texto = compilarLlenarYExtraerTexto("RptBiometricoDetallado", filas, params);
        assertFalse(
                PALABRA_NULL.matcher(texto).find(),
                "El PDF imprime \"null\" literal en vez de la celda vacía:\n" + texto
        );
        assertTrue(texto.contains("45"), "Falta el Min. Atraso de la fila TRABAJADO:\n" + texto);
        assertTrue(texto.contains("180"), "Falta el Min. Atraso de la fila FALTA:\n" + texto);
    }

    @Test
    void resumenMensualCompilaYLlena() throws Exception {
        List<ResumenAsistenciaEmpleadoDto> filas = new ArrayList<>();
        for (String nombre : new String[]{"PRUEBA UNO", "PRUEBA DOS"}) {
            ResumenAsistenciaEmpleadoDto r = new ResumenAsistenciaEmpleadoDto();
            r.setNombreEmpleado(nombre);
            r.setDiasAsignados(26);
            r.setDiasNoMarcados(1);
            r.setMinutosAtraso(45);
            // Una fila sin observaciones (null) y otra con — cubre el camino
            // que antes imprimía "null" literal si isBlankWhenNull faltaba.
            r.setObservaciones(nombre.equals("PRUEBA DOS") ? "1 feriado, 2 permiso" : null);
            filas.add(r);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("mesAnio", "Agosto 2026");

        String texto = compilarLlenarYExtraerTexto("RptBiometricoResumenMensual", filas, params);
        assertFalse(PALABRA_NULL.matcher(texto).find(), "Imprime \"null\" literal:\n" + texto);
        assertTrue(texto.contains("PRUEBA UNO"), "No aparece la primera fila:\n" + texto);
    }

    /**
     * {@link JasperReportExport#exportPDFDesdeColeccionesMultiples} — el reporte
     * detallado, pero de VARIOS empleados a la vez, uno por lote. Código nuevo
     * de verdad (no sólo el .jrxml ya probado arriba): la primera versión no
     * compilaba porque {@code SimpleExporterInput} no tiene un constructor
     * {@code List<JasperPrint>} en esta versión de JasperReports, sólo la
     * fábrica estática {@code getInstance(...)}. Sin este test ese tipo de
     * error sólo aparecería recién en producción, con 400 empleados reales.
     */
    @Test
    void detalladoTodosLosEmpleadosUneVariosLotesEnUnPdf() throws Exception {
        JasperReportExport export = new JasperReportExport(null);

        List<AsistenciaDiaDto> diasUno = new ArrayList<>();
        AsistenciaDiaDto d1 = new AsistenciaDiaDto();
        d1.setFecha(new Date());
        d1.setEstado("TRABAJADO");
        diasUno.add(d1);

        List<AsistenciaDiaDto> diasDos = new ArrayList<>();
        AsistenciaDiaDto d2 = new AsistenciaDiaDto();
        d2.setFecha(new Date());
        d2.setEstado("FALTA");
        diasDos.add(d2);

        Map<String, Object> paramsUno = new HashMap<>();
        paramsUno.put("nombreEmpleado", "EMPLEADO UNO");
        paramsUno.put("mesAnio", "Agosto 2026");
        Map<String, Object> paramsDos = new HashMap<>();
        paramsDos.put("nombreEmpleado", "EMPLEADO DOS");
        paramsDos.put("mesAnio", "Agosto 2026");

        byte[] pdf = export.exportPDFDesdeColeccionesMultiples(
                "RptBiometricoDetallado",
                Arrays.asList(diasUno, diasDos),
                Arrays.asList(paramsUno, paramsDos));

        String texto;
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertTrue(doc.getNumberOfPages() >= 2, "Debería tener al menos una página por empleado");
            StringBuilder sb = new StringBuilder();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(p))).append('\n');
            }
            texto = sb.toString();
        }
        assertTrue(texto.contains("EMPLEADO UNO"), "Falta el primer empleado:\n" + texto);
        assertTrue(texto.contains("EMPLEADO DOS"), "Falta el segundo empleado:\n" + texto);
        assertFalse(PALABRA_NULL.matcher(texto).find(), "Imprime \"null\" literal:\n" + texto);
    }

    /**
     * Un mes de 31 días (el más largo posible) tiene que caber en UNA página:
     * pedido explícito del usuario ("trata de que cada empleado quepa en una
     * sola hoja"), y necesario además para que "todos los empleados" (ver
     * {@code exportPDFDesdeColeccionesMultiples}) no multiplique una página de
     * más por cada persona. Antes de comprimir bandas (60/18/15/40 a 42/16/13/40)
     * un mes de 31 días desbordaba a una segunda página con sólo la última fila
     * o el resumen — este test es el que lo hubiera atrapado.
     */
    @Test
    void unMesDe31DiasCabeEnUnaSolaPagina() throws Exception {
        List<AsistenciaDiaDto> filas = new ArrayList<>();
        Date ahora = new Date();
        for (int dia = 1; dia <= 31; dia++) {
            AsistenciaDiaDto d = new AsistenciaDiaDto();
            d.setFecha(ahora);
            // Alternando estados para que las filas no queden todas iguales,
            // igual que se verían en un mes real con feriados/permisos mezclados.
            String estado;
            switch (dia % 4) {
                case 0: estado = "FALTA"; break;
                case 1: estado = "FERIADO"; break;
                case 2: estado = "PERMISO"; break;
                default: estado = "TRABAJADO"; break;
            }
            d.setEstado(estado);
            d.setHoraEntradaEsperada(ahora);
            d.setHoraSalidaEsperada(ahora);
            if ("TRABAJADO".equals(estado)) {
                d.setHoraEntradaReal(ahora);
                d.setHoraSalidaReal(ahora);
            }
            if ("PERMISO".equals(estado)) {
                d.setHoraInicioPermiso(ahora);
                d.setHoraFinPermiso(ahora);
                d.setMotivo("Permiso de prueba");
            }
            d.setMinutosAtraso(dia % 4 == 0 ? 45 : 0);
            filas.add(d);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("nombreEmpleado", "PRUEBA MES COMPLETO");
        params.put("mesAnio", "Agosto 2026");

        JasperPrint print = compilarYLlenar("RptBiometricoDetallado", filas, params);
        assertEquals(1, print.getPages().size(),
                "Un mes de 31 días no entró en una sola página — quedó en " + print.getPages().size());

        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        String texto = extraerTexto(pdf);
        assertTrue(texto.contains("Total Atraso"), "Falta el total de atraso en el resumen:\n" + texto);
        assertFalse(PALABRA_NULL.matcher(texto).find(), "Imprime \"null\" literal:\n" + texto);
    }

    /** Nuevo reporte (2026-09-01) — "qué horario tiene cada empleado HOY", no un reporte mensual (sin campo mes/período). */
    @Test
    void horarioVigenteCompilaYLlena() throws Exception {
        List<HorarioVigenteEmpleadoDto> filas = new ArrayList<>();

        HorarioVigenteEmpleadoDto conHorario = new HorarioVigenteEmpleadoDto();
        conHorario.setNombreEmpleado("PRUEBA CON HORARIO");
        conHorario.setNombreHorarioSemanal("ADM CONT 1");
        conHorario.setVigenteDesde(new Date());
        conHorario.setLunes("08:00–17:00");
        conHorario.setMartes("08:00–17:00");
        conHorario.setMiercoles("08:00–17:00");
        conHorario.setJueves("08:00–17:00");
        conHorario.setViernes("08:00–17:00");
        conHorario.setSabado("09:00–12:00");
        conHorario.setDomingo("—"); // sin turno ese día — no debe imprimir "null"
        filas.add(conHorario);

        HorarioVigenteEmpleadoDto sinDatos = new HorarioVigenteEmpleadoDto();
        sinDatos.setNombreEmpleado("PRUEBA SIN NOMBRE DE HORARIO");
        // nombreHorarioSemanal y los 7 días quedan null a propósito — cubre el
        // camino que antes imprimía "null" literal si isBlankWhenNull faltaba.
        filas.add(sinDatos);

        Map<String, Object> params = new HashMap<>();
        params.put("fechaGeneracion", new Date());

        String texto = compilarLlenarYExtraerTexto("RptBiometricoHorarioVigente", filas, params);
        assertFalse(PALABRA_NULL.matcher(texto).find(), "Imprime \"null\" literal:\n" + texto);
        assertTrue(texto.contains("PRUEBA CON HORARIO"), "No aparece la primera fila:\n" + texto);
        assertTrue(texto.contains("ADM CONT 1"), "Falta el nombre del horario semanal:\n" + texto);
        assertTrue(texto.contains("08:00"), "Falta el detalle de horas por día:\n" + texto);
    }

    /** Compila el .jrxml, lo llena con {@code filas} y devuelve el texto plano del PDF resultante. */
    private static String compilarLlenarYExtraerTexto(
            String nombreReporte, Collection<?> filas, Map<String, Object> params) throws Exception {
        JasperPrint print = compilarYLlenar(nombreReporte, filas, params);
        assertTrue(print.getPages().size() >= 1, "El reporte no generó ninguna página");
        return extraerTexto(JasperExportManager.exportReportToPdf(print));
    }

    private static JasperPrint compilarYLlenar(
            String nombreReporte, Collection<?> filas, Map<String, Object> params) throws Exception {
        try (InputStream in = RptBiometricoDetalladoSmokeTest.class
                .getResourceAsStream("/reports/" + nombreReporte + ".jrxml")) {
            assertNotNull(in, "No se encontró " + nombreReporte + ".jrxml en el classpath");
            JasperReport reporte = JasperCompileManager.compileReport(in);

            Map<String, Object> paramsCopy = new HashMap<>(params);
            paramsCopy.put(JRParameter.REPORT_LOCALE, new Locale("es"));

            return JasperFillManager.fillReport(
                    reporte, paramsCopy, new JRBeanCollectionDataSource(filas));
        }
    }

    private static String extraerTexto(byte[] pdf) throws Exception {
        StringBuilder texto = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            for (int pagina = 1; pagina <= doc.getNumberOfPages(); pagina++) {
                texto.append(PdfTextExtractor.getTextFromPage(doc.getPage(pagina))).append('\n');
            }
        }
        return texto.toString();
    }
}
