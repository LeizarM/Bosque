package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.ITcpeResultado;
import bo.bosque.com.impexpap.dto.TcpeEscritoFilaDto;
import bo.bosque.com.impexpap.dto.TcpeExamenCabeceraDto;
import bo.bosque.com.impexpap.dto.TcpeSolucionarioDto;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PDF del examen escrito y del solucionario ({@code RptTcpeExamenEscrito.jrxml}) con datos simulados de
 * {@code p_list_tcpe_Resultado} E y S, sin Spring ni base: compila el jrxml del classpath, lo llena desde la colección
 * y verifica que salga un PDF con lo que tiene que decir.
 */
class TcpeExamenEscritoPdfTest {

    private ITcpeResultado resultados;
    private TcpeEscritoService servicio;

    @BeforeEach
    void preparar() {
        resultados = mock(ITcpeResultado.class);
        servicio = new TcpeEscritoService(resultados, new JasperReportExport(null));

        TcpeExamenCabeceraDto conNombre = cabecera(101L, 1L, "QUISPE MAMANI ANA", "1234567");
        TcpeExamenCabeceraDto anonimo = cabecera(102L, 2L, null, null);
        when(resultados.cabeceras(30L, null)).thenReturn(Arrays.asList(conNombre, anonimo));
        when(resultados.solucionario(null, 30L)).thenReturn(Arrays.asList(
                opcion(101L, 11L, 1, "¿Cuál es la capital de Bolivia?", 1L, "La Paz", false),
                opcion(101L, 11L, 1, "¿Cuál es la capital de Bolivia?", 2L, "Sucre", true),
                conModelo(abierta(101L, 12L, 2, "Explique el proceso de cierre de caja."),
                        "Contar el efectivo y cuadrar con el sistema."),
                opcion(102L, 21L, 1, "¿Cuánto es 2 + 2?", 3L, "4", true),
                opcion(102L, 21L, 1, "¿Cuánto es 2 + 2?", 4L, "5", false)));
    }

    private static TcpeExamenCabeceraDto cabecera(long codExamen, long numero, String nombre, String ci) {
        TcpeExamenCabeceraDto c = new TcpeExamenCabeceraDto();
        c.setCodExamen(codExamen);
        c.setNumero(numero);
        c.setTituloLote("Evaluación de cajeros");
        c.setNombreArea("Finanzas");
        c.setNombreClasificacion("Caja");
        c.setFecha(LocalDate.of(2026, 10, 20));
        c.setDuracionMinutos(60);
        c.setNotaAprobacion(new BigDecimal("51.00"));
        c.setNombreCompleto(nombre);
        c.setCi(ci);
        return c;
    }

    private static TcpeSolucionarioDto opcion(long codExamen, long codPreguntaH, int orden, String pregunta,
                                              long codRsptaOpH, String texto, boolean valida) {
        TcpeSolucionarioDto s = abierta(codExamen, codPreguntaH, orden, pregunta);
        s.setEsCerrada(true);
        s.setCodRsptaOpH(codRsptaOpH);
        s.setOpcionRspta(texto);
        s.setEsValido(valida);
        return s;
    }

    private static TcpeSolucionarioDto conModelo(TcpeSolucionarioDto s, String modelo) {
        s.setRespuestaModelo(modelo);
        return s;
    }

    private static TcpeSolucionarioDto abierta(long codExamen, long codPreguntaH, int orden, String pregunta) {
        TcpeSolucionarioDto s = new TcpeSolucionarioDto();
        s.setCodExamen(codExamen);
        s.setCodPreguntaH(codPreguntaH);
        s.setOrden(orden);
        s.setPregunta(pregunta);
        s.setEsCerrada(false);
        return s;
    }

    private static String texto(byte[] pdf) throws Exception {
        StringBuilder t = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                t.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
        }
        return t.toString();
    }

    private static void esPdf(byte[] pdf) {
        assertTrue(pdf != null && pdf.length > 0, "El PDF salió vacío.");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("examen escrito: un PDF no vacío, una hoja por examen, nombre y CI o líneas, opciones a) b) sin marcar")
    void examenEscrito() throws Exception {
        byte[] pdf = servicio.pdf(30L, null, false);

        esPdf(pdf);
        String t = texto(pdf);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertEquals(2, doc.getNumberOfPages(), "Cada examen arranca en hoja nueva.");
        }
        assertTrue(t.contains("Examen N.º 1") && t.contains("Examen N.º 2"), t);
        assertTrue(t.contains("QUISPE MAMANI ANA") && t.contains("1234567"), "Falta el evaluado con nombre.");
        assertTrue(t.contains("CI: ____"), "El examen sin usuario lleva líneas para completar.");
        assertTrue(t.contains("a)") && t.contains("b)") && t.contains("Sucre"), "Faltan las opciones.");
        assertTrue(t.contains("2. Explique el proceso de cierre de caja."), "Falta la abierta.");
        assertFalse(t.contains("(X)"), "El examen no marca las correctas.");
        assertFalse(t.contains("Contar el efectivo"), "El examen nunca muestra la respuesta modelo.");
    }

    @Test
    @DisplayName("solucionario: un PDF no vacío con las correctas marcadas con (X)")
    void solucionario() throws Exception {
        byte[] pdf = servicio.pdf(30L, null, true);

        esPdf(pdf);
        String t = texto(pdf);
        assertTrue(t.contains("SOLUCIONARIO"), t);
        assertTrue(t.contains("(X)  b)  Sucre"), "La correcta no salió marcada.\n" + t);
        assertTrue(t.contains("(X)  a)  4"), "La correcta del segundo examen no salió marcada.\n" + t);
        assertFalse(t.contains("(X)  a)  La Paz"), "Una incorrecta salió marcada.");
        assertTrue(t.contains("Contar el efectivo y cuadrar con el sistema."), "Falta la respuesta modelo de la abierta.\n" + t);
    }

    @Test
    @DisplayName("sin exámenes que imprimir: arreglo vacío y no se llena el reporte")
    void sinDatos() {
        when(resultados.cabeceras(31L, null)).thenReturn(Collections.<TcpeExamenCabeceraDto>emptyList());
        JasperReportExport jasper = mock(JasperReportExport.class);

        byte[] pdf = new TcpeEscritoService(resultados, jasper).pdf(31L, null, false);

        assertEquals(0, pdf.length);
        verify(jasper, never()).exportPDFDesdeColeccion(any(), any(), any());
    }

    @Test
    @DisplayName("filas: letras por pregunta, la abierta sin letra y solo los exámenes con cabecera")
    void filas() {
        List<TcpeEscritoFilaDto> f = TcpeEscritoService.filas(resultados.cabeceras(30L, null),
                resultados.solucionario(null, 30L));

        assertEquals(5, f.size());
        assertEquals("a", f.get(0).getLetra());
        assertEquals("b", f.get(1).getLetra());
        assertNull(f.get(2).getLetra());
        assertEquals("a", f.get(3).getLetra());
        assertEquals(Long.valueOf(2L), f.get(3).getNumero());
        assertEquals("z", TcpeEscritoService.letra(25));
        assertEquals("27", TcpeEscritoService.letra(26));
    }
}
