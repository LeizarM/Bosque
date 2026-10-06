package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChChequeReporte;
import bo.bosque.com.impexpap.dao.IPersonalCheque;
import bo.bosque.com.impexpap.dao.ISucursalCheque;
import bo.bosque.com.impexpap.dto.ChequeCobranzaRptDto;
import bo.bosque.com.impexpap.dto.ChequeCustodioRptDto;
import bo.bosque.com.impexpap.dto.ChequeRecibidoRptDto;
import bo.bosque.com.impexpap.dto.ChequeTraspasoRptDto;
import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.HoraTraspasoChequeDto;
import bo.bosque.com.impexpap.dto.NotaRemisionRptDto;
import bo.bosque.com.impexpap.dto.PersonalChequeDto;
import bo.bosque.com.impexpap.dto.ReciboChequeRptDto;
import bo.bosque.com.impexpap.dto.ReporteChequeRequest;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * Los reportes en PDF de Cheques. Dos clases de prueba en una:
 * <ul>
 *   <li><b>Plantillas de verdad</b>: cada una de las 6 se compila y se llena con Jasper real y filas de ejemplo
 *       (las dos empresas, que cambian los logos) y se lee el texto del PDF. Atrapa lo que un mock no ve: un tipo de
 *       campo distinto del que declara la plantilla ({@code ClassCastException} al llenar), un subreporte mal
 *       enlazado, un parametro que no existe, o un {@code null} impreso.</li>
 *   <li><b>Reglas del servicio</b> con el exportador simulado: permisos por boton, empresa y sucursal, titulos y rotulos
 *       del legacy, y lo que se pide a cada rama del procedimiento.</li>
 * </ul>
 */
class ChequeReporteServiceTest {

    private static final int USR = 7;
    private static final long SUC = 1;
    private static final Pattern PALABRA_NULL = Pattern.compile("\\bnull\\b");
    /** "Tue Sep 01 20:55:30" : una fecha en el formato crudo de {@code Date.toString()}. */
    private static final Pattern FECHA_JAVA_CRUDA = Pattern.compile(
            "(Mon|Tue|Wed|Thu|Fri|Sat|Sun) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{2} \\d{2}:\\d{2}:\\d{2}");

    private IChChequeReporte datos;
    private ChequeService cheques;
    private ISucursalCheque sucursales;
    private IPersonalCheque personal;
    private AccesoModuloHelper acceso;
    private Authentication auth;

    @BeforeEach
    void preparar() {
        datos = mock(IChChequeReporte.class);
        cheques = mock(ChequeService.class);
        sucursales = mock(ISucursalCheque.class);
        personal = mock(IPersonalCheque.class);
        acceso = mock(AccesoModuloHelper.class);

        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        // La empresa del login es la 6 (GENERAL): los reportes NO deben usarla.
        t.setDetails(new DatosToken(USR, 3, 6, "lim", "MARCELO JAIMES"));
        auth = t;

        when(cheques.empresas()).thenReturn(Arrays.asList(
                new EmpresaChequeDto(1, "IMPEXPAP"), new EmpresaChequeDto(5, "ESPPAPEL")));
        when(sucursales.sucursalesDeEmpresa(1, USR)).thenReturn(Arrays.asList(
                new SucursalChequeDto(1, "Central"), new SucursalChequeDto(3, "Sucursal 6 Cochabamba")));
        when(sucursales.sucursalesDeEmpresa(5, USR)).thenReturn(Collections.singletonList(
                new SucursalChequeDto(13, "Central EPP")));
    }

    // ------------------------------------------------------------------ utilidades

    private ChequeReporteService real() {
        return new ChequeReporteService(datos, cheques, sucursales, personal, acceso, new JasperReportExport(null));
    }

    private JasperReportExport jasperFalso() {
        JasperReportExport j = mock(JasperReportExport.class);
        when(j.exportPDFDesdeColeccion(anyString(), any(), any())).thenReturn(new byte[] {1});
        when(j.exportPDFDesdeColeccionConSubreportes(anyString(), any(), any(), any(String[].class)))
                .thenReturn(new byte[] {1});
        return j;
    }

    private ChequeReporteService conExportadorFalso(JasperReportExport j) {
        return new ChequeReporteService(datos, cheques, sucursales, personal, acceso, j);
    }

    private static ReporteChequeRequest req(int empresa, long sucursal) {
        ReporteChequeRequest r = new ReporteChequeRequest();
        r.setCodEmpresa(empresa);
        r.setCodSucursal(sucursal);
        return r;
    }

    /** {@code java.sql.Date}: sirve para el cuerpo de la peticion (es un {@code java.util.Date}) y para las filas (que la plantilla declara asi). */
    private static java.sql.Date dia(int anio, int mes, int d) {
        return java.sql.Date.valueOf(LocalDate.of(anio, mes, d));
    }

    private static Timestamp ts(int anio, int mes, int d, int h, int mi) {
        return Timestamp.valueOf(LocalDateTime.of(anio, mes, d, h, mi));
    }

    private static String texto(byte[] pdf) throws Exception {
        assertNotNull(pdf);
        assertTrue(pdf.length > 4 && pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F',
                "no es un PDF");
        StringBuilder sb = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(p))).append('\n');
            }
        }
        return sb.toString();
    }

    private static void sinBasura(String texto) {
        assertFalse(PALABRA_NULL.matcher(texto).find(), "Imprime \"null\" literal:\n" + texto);
        assertFalse(FECHA_JAVA_CRUDA.matcher(texto).find(), "Imprime una fecha en formato Java crudo:\n" + texto);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parametros(JasperReportExport j, String plantilla) {
        ArgumentCaptor<Map<String, Object>> c = ArgumentCaptor.forClass(Map.class);
        verify(j).exportPDFDesdeColeccion(eq(plantilla), any(Collection.class), c.capture());
        return c.getValue();
    }

    // ------------------------------------------------------------------ las plantillas de verdad

    @Test
    @DisplayName("los logos de las dos empresas existen como recursos")
    void losLogosExisten() {
        for (String logo : new String[] {"logoEmpresa.jpg", "logoEmpresaAgua.jpg", "logoEmpresaEPP.jpg", "logoBosque.jpg"}) {
            assertNotNull(getClass().getResourceAsStream("/logos/" + logo), "falta /logos/" + logo);
        }
    }

    @Test
    @DisplayName("Reporte (recibidos en caja): la plantilla compila y se llena, para Bosque y para EPP")
    void plantillaRecibidos() throws Exception {
        for (int empresa : new int[] {1, 5}) {
            long suc = empresa == 1 ? 1 : 13;
            when(datos.recibidos(eq(suc), any(), any())).thenReturn(Arrays.asList(
                    new ChequeRecibidoRptDto(1L, ts(2026, 10, 1, 9, 30), ts(2026, 10, 1, 11, 0), "000123",
                            "CLIENTE UNO SRL", "R-77", "JUAN PEREZ", "Banco Union", "8850123",
                            dia(2026, 9, 30), 1500.5, "BS", "recibido en caja"),
                    new ChequeRecibidoRptDto(2L, ts(2026, 10, 1, 10, 0), null, "000124",
                            "CLIENTE DOS SA", "0", null, "Mercantil", "12345",
                            dia(2026, 10, 1), 200.0, "SUS", null)));
            ReporteChequeRequest r = req(empresa, suc);
            r.setFechaDesde(dia(2026, 10, 1));
            r.setFechaHasta(dia(2026, 10, 1));

            String t = texto(real().recibidos(auth, r));
            sinBasura(t);
            assertTrue(t.contains("MARCELO JAIMES"), "falta quien imprime:\n" + t);
            assertTrue(t.contains(empresa == 1 ? "Central" : "Central EPP"), "falta la sucursal:\n" + t);
            assertTrue(t.contains("CLIENTE UNO SRL") && t.contains("CLIENTE DOS SA"), "faltan las filas:\n" + t);
            assertTrue(t.contains("Desde 01/10/2026"), "falta el titulo del legacy:\n" + t);
        }
    }

    @Test
    @DisplayName("Reporte Cheques (cobranzas): la plantilla compila y se llena")
    void plantillaCobranzas() throws Exception {
        when(datos.cobranzas(eq(SUC), any(), any(), any(), any())).thenReturn(Collections.singletonList(
                new ChequeCobranzaRptDto(1L, ts(2026, 10, 1, 9, 30), "F-1001", "CLIENTE UNO SRL", "R-77",
                        "JUAN PEREZ", "Banco Union", "8850123", dia(2026, 10, 15), 1500.5, "BS", "obs")));
        ReporteChequeRequest r = req(1, SUC);
        r.setFechaDesde(dia(2026, 10, 1));
        r.setFechaHasta(dia(2026, 10, 31));
        r.setEstado("PEN");
        r.setCodCliente("C0001");

        String t = texto(real().cobranzas(auth, r));
        sinBasura(t);
        assertTrue(t.contains("CLIENTE UNO SRL") && t.contains("F-1001"), "faltan las filas:\n" + t);
        assertTrue(t.contains("y son PENDIENTE (s)") && t.contains("C0001"), "falta el titulo del legacy:\n" + t);
    }

    @Test
    @DisplayName("Reporte Custodio: la plantilla y el SUBREPORTE de notas de remision se llenan")
    void plantillaCustodioConSubreporte() throws Exception {
        when(datos.custodio(eq(SUC), any(), any())).thenReturn(new ArrayList<>(Arrays.asList(
                new ChequeCustodioRptDto(1L, ts(2026, 10, 1, 14, 0), "CLIENTE UNO SRL", "Banco Union", "8850123",
                        "02/10/2026", 1500.5, "BS", 501, "TR-99", "Banco Bisa", null),
                new ChequeCustodioRptDto(2L, ts(2026, 10, 1, 14, 0), "CLIENTE UNO SRL", "Banco Union", "8850123",
                        "", 1500.5, "BS", 501, "", "", null),
                new ChequeCustodioRptDto(3L, ts(2026, 10, 1, 14, 0), "CLIENTE TRES", "Mercantil", "777",
                        "", 90.0, "SUS", 502, "", "", null))));
        when(datos.notasDeRemision(501)).thenReturn(Collections.singletonList(
                new NotaRemisionRptDto(501, "NR-1001", 4455, dia(2026, 9, 28), "28/09/2026")));
        when(datos.notasDeRemision(502)).thenReturn(Collections.<NotaRemisionRptDto>emptyList());
        ReporteChequeRequest r = req(1, SUC);
        r.setFecha(dia(2026, 10, 1));

        String t = texto(real().custodio(auth, r));
        sinBasura(t);
        assertTrue(t.contains("CLIENTE UNO SRL") && t.contains("CLIENTE TRES"), "faltan las filas:\n" + t);
        assertTrue(t.contains("NR-1001"), "el subreporte de notas de remision no se lleno:\n" + t);
        // Las notas se piden UNA vez por cheque, aunque el cheque salga en dos filas.
        verify(datos, times(1)).notasDeRemision(501);
        verify(datos, times(1)).notasDeRemision(502);
    }

    @Test
    @DisplayName("Recibo del ultimo cheque: la plantilla compila y se llena, para Bosque y para EPP")
    void plantillaRecibo() throws Exception {
        for (int empresa : new int[] {1, 5}) {
            long suc = empresa == 1 ? 1 : 13;
            when(datos.ultimoChequeDelUsuario(USR, suc)).thenReturn(501);
            when(datos.recibo(501, suc)).thenReturn(new ReciboChequeRptDto(501, "Nº  000123", ts(2026, 10, 1, 9, 30),
                    "CLIENTE UNO SRL    -    C0001", "Banco Union", "8850123", 1500.5,
                    " Son  : mil quinientos 50/100 Bolivianos", "Bs. ", "CAJERA UNO", "JUAN PEREZ"));

            String t = texto(real().reciboDelUltimoCheque(auth, req(empresa, suc)));
            sinBasura(t);
            assertTrue(t.contains("CLIENTE UNO SRL") && t.contains("000123"), "falta el recibo:\n" + t);
        }
    }

    @Test
    @DisplayName("Nomina del traspaso y su reimpresion: las dos plantillas compilan y se llenan")
    void plantillasDeTraspaso() throws Exception {
        ChequeTraspasoRptDto fila = new ChequeTraspasoRptDto(1L, ts(2026, 10, 1, 11, 0), "CLIENTE UNO SRL", "R-77",
                "JUAN PEREZ", "Banco Union", "8850123", dia(2026, 10, 15), 1500.5, "BS", "obs");
        when(datos.ultimoTraspaso(SUC)).thenReturn(Collections.singletonList(fila));
        when(datos.reimpresionTraspaso(SUC, 900)).thenReturn(Collections.singletonList(fila));

        String t1 = texto(real().nominaDelTraspaso(auth, req(1, SUC)));
        sinBasura(t1);
        assertTrue(t1.contains("CLIENTE UNO SRL"), t1);

        ReporteChequeRequest r = req(1, SUC);
        r.setCodAccion(900L);
        String t2 = texto(real().reimpresionDeTraspaso(auth, r));
        sinBasura(t2);
        // Esta plantilla trae su propio titulo y no imprime el parametro "titulo" (el legacy tambien lo mandaba en vano).
        assertTrue(t2.contains("CLIENTE UNO SRL") && t2.contains("TRASPASO"), t2);
        assertTrue(t2.contains("MARCELO JAIMES"), "falta quien imprime:\n" + t2);
    }

    @Test
    @DisplayName("sin filas cada plantilla igual da un PDF valido (el legacy imprime el reporte sin detalle)")
    void sinFilasDaPdf() throws Exception {
        ReporteChequeRequest r = req(1, SUC);
        sinBasura(texto(real().recibidos(auth, r)));
        sinBasura(texto(real().cobranzas(auth, r)));
        sinBasura(texto(real().custodio(auth, r)));
        sinBasura(texto(real().nominaDelTraspaso(auth, r)));
        r.setCodAccion(1L);
        sinBasura(texto(real().reimpresionDeTraspaso(auth, r)));
    }

    // ------------------------------------------------------------------ permisos

    @Test
    @DisplayName("cada reporte exige SU boton de la vista 42, y sin el no toca la base")
    void cadaReporteExigeSuBoton() {
        ReporteChequeRequest r = req(1, SUC);
        r.setCodAccion(1L);
        ChequeReporteService s = conExportadorFalso(jasperFalso());
        Object[][] casos = {
                {"btnRpt1CH", (Runnable) () -> s.recibidos(auth, r)},
                {"btnRpt2CH", (Runnable) () -> s.cobranzas(auth, r)},
                {"btnRpt3CH", (Runnable) () -> s.custodio(auth, r)},
                {"btnRpt4CH", (Runnable) () -> s.reciboDelUltimoCheque(auth, r)},
                {"btnRpt5CH", (Runnable) () -> s.reimpresionDeTraspaso(auth, r)},
                {"btnTraspasoCH", (Runnable) () -> s.nominaDelTraspaso(auth, r)},
        };
        for (Object[] caso : casos) {
            String boton = (String) caso[0];
            doThrow(new AccessDeniedException("sin boton")).when(acceso)
                    .exigirBoton(any(), eq(ChequeService.VISTA_CHEQUES), eq(boton));
            assertThrows(AccessDeniedException.class, ((Runnable) caso[1])::run, boton);
            verify(acceso).exigirBoton(auth, ChequeService.VISTA_CHEQUES, boton);
        }
        verifyNoInteractions(datos);
    }

    @Test
    @DisplayName("la lista de horas de traspaso exige btnRpt5CH, sucursal y fecha")
    void horasDeTraspaso() {
        ChequeReporteService s = conExportadorFalso(jasperFalso());
        when(datos.horasDeTraspaso(eq(SUC), any())).thenReturn(Collections.singletonList(new HoraTraspasoChequeDto(900, "11:00")));

        List<HoraTraspasoChequeDto> l = s.horasDeTraspaso(auth, SUC, dia(2026, 10, 1));
        assertEquals(1, l.size());
        verify(acceso).exigirBoton(auth, ChequeService.VISTA_CHEQUES, "btnRpt5CH");
        verify(cheques).exigirSucursalLectura(auth, SUC);

        assertThrows(SpBusinessException.class, () -> s.horasDeTraspaso(auth, SUC, null));
        assertThrows(SpBusinessException.class, () -> s.horasDeTraspaso(auth, null, dia(2026, 10, 1)));

        doThrow(new AccessDeniedException("sin boton")).when(acceso).exigirBoton(any(), anyInt(), eq("btnRpt5CH"));
        assertThrows(AccessDeniedException.class, () -> s.horasDeTraspaso(auth, SUC, dia(2026, 10, 1)));
    }

    @Test
    @DisplayName("la sucursal tiene que poder verse: si no, 403 y no toca la base")
    void sucursalNoVisible() {
        doThrow(new AccessDeniedException("solo su sucursal")).when(cheques).exigirSucursalLectura(any(), anyLong());
        ChequeReporteService s = conExportadorFalso(jasperFalso());
        assertThrows(AccessDeniedException.class, () -> s.recibidos(auth, req(1, 9)));
        verifyNoInteractions(datos);
    }

    @Test
    @DisplayName("la empresa tiene que ser una del combo (la del login, 6, no vale) y obligatoria")
    void empresaDelCombo() {
        ChequeReporteService s = conExportadorFalso(jasperFalso());
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> s.recibidos(auth, req(6, SUC)));
        assertTrue(e.getMessage().contains("no está habilitada"));
        assertThrows(SpBusinessException.class, () -> s.recibidos(auth, req(0, SUC)));
        assertThrows(SpBusinessException.class, () -> s.recibidos(auth, req(1, 0)));
        assertThrows(SpBusinessException.class, () -> s.recibidos(auth, null));
        verifyNoInteractions(datos);
    }

    // ------------------------------------------------------------------ lo que se pide y como se rotula

    @Test
    @DisplayName("recibidos: fechas dd/MM/yyyy al procedimiento, titulo y rotulos del legacy, logos de la empresa")
    void parametrosDeRecibidos() {
        JasperReportExport j = jasperFalso();
        ReporteChequeRequest r = req(1, SUC);
        r.setFechaDesde(dia(2026, 10, 1));
        r.setFechaHasta(dia(2026, 10, 31));

        conExportadorFalso(j).recibidos(auth, r);

        verify(datos).recibidos(SUC, "01/10/2026", "31/10/2026");
        Map<String, Object> p = parametros(j, "RptChequeCajaRecepcion");
        assertEquals(" Desde 01/10/2026 Hasta el 31/10/2026", p.get("titulo"));
        assertEquals("01/10/2026", p.get("fecha"));
        assertEquals("31/10/2026", p.get("fechaFin"));
        assertEquals("MARCELO JAIMES", p.get("usuario"), "el nombre sale del token");
        assertEquals(" ( Central )", p.get("sucursal"));
        assertEquals("1", p.get("codSucursal"), "la plantilla lo declara String");
        assertNotNull(p.get("logoEmpresa"));
    }

    @Test
    @DisplayName("recibidos sin fechas: no se mandan (el SP usa hoy) y el titulo queda vacio")
    void recibidosSinFechas() {
        JasperReportExport j = jasperFalso();
        conExportadorFalso(j).recibidos(auth, req(1, SUC));
        verify(datos).recibidos(SUC, null, null);
        Map<String, Object> p = parametros(j, "RptChequeCajaRecepcion");
        assertEquals("", p.get("titulo"));
        assertNull(p.get("fecha"));
        assertNull(p.get("fechaFin"));
    }

    @Test
    @DisplayName("cobranzas: '-' y vacio son 'todos' (no se filtra); estado y cliente se rotulan")
    void parametrosDeCobranzas() {
        JasperReportExport j = jasperFalso();
        ChequeReporteService s = conExportadorFalso(j);

        ReporteChequeRequest todos = req(1, SUC);
        todos.setEstado("-");
        todos.setCodCliente("");
        s.cobranzas(auth, todos);
        verify(datos).cobranzas(SUC, null, null, null, null);

        ReporteChequeRequest filtrado = req(1, SUC);
        filtrado.setEstado("cer");
        filtrado.setCodCliente(" C0001 ");
        filtrado.setFechaDesde(dia(2026, 10, 1));
        s.cobranzas(auth, filtrado);
        verify(datos).cobranzas(SUC, "01/10/2026", null, "cer", "C0001");
    }

    @Test
    @DisplayName("cobranzas: un estado que no es PEN ni CER se rechaza")
    void cobranzasEstadoInvalido() {
        ReporteChequeRequest r = req(1, SUC);
        r.setEstado("XXX");
        assertThrows(SpBusinessException.class, () -> conExportadorFalso(jasperFalso()).cobranzas(auth, r));
        verifyNoInteractions(datos);
    }

    @Test
    @DisplayName("custodio: el cobrador viaja como codigo y su nombre va en el titulo; 0 = todos")
    void parametrosDeCustodio() {
        JasperReportExport j = mock(JasperReportExport.class);
        when(j.exportPDFDesdeColeccionConSubreportes(anyString(), any(), any(), any(String[].class))).thenReturn(new byte[] {1});
        when(personal.responsablesDeCustodia(SUC)).thenReturn(Arrays.asList(
                new PersonalChequeDto(31, "LUIS COBRADOR"), new PersonalChequeDto(32, "OTRO")));
        ReporteChequeRequest r = req(1, SUC);
        r.setFecha(dia(2026, 10, 1));
        r.setCodEmpleado(31);

        conExportadorFalso(j).custodio(auth, r);

        verify(datos).custodio(SUC, "01/10/2026", 31);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> c = ArgumentCaptor.forClass(Map.class);
        verify(j).exportPDFDesdeColeccionConSubreportes(eq("RptChequeCajaSalida"), any(), c.capture(),
                eq("subRptChequeCajaSalidaFact"));
        assertEquals(" En Fecha : 01/10/2026   entregados al Cobrador : LUIS COBRADOR", c.getValue().get("titulo"));
        assertEquals("31", c.getValue().get("estado"), "el cobrador viaja en 'estado', como en el legacy");

        r.setCodEmpleado(0);
        conExportadorFalso(j).custodio(auth, r);
        verify(datos).custodio(SUC, "01/10/2026", null);
    }

    @Test
    @DisplayName("recibo del ultimo cheque: sin cheque del usuario es un error claro (el legacy imprimia un recibo en blanco)")
    void reciboSinCheque() {
        when(datos.ultimoChequeDelUsuario(USR, SUC)).thenReturn(null);
        SpBusinessException e = assertThrows(SpBusinessException.class,
                () -> conExportadorFalso(jasperFalso()).reciboDelUltimoCheque(auth, req(1, SUC)));
        assertTrue(e.getMessage().contains("Todavía no registraste ningún cheque"));
        verify(datos, never()).recibo(anyLong(), anyLong());
    }

    @Test
    @DisplayName("recibo del ultimo cheque: usa el cheque del TOKEN, el logo de la empresa y los parametros tipados")
    void parametrosDelRecibo() {
        JasperReportExport j = jasperFalso();
        when(datos.ultimoChequeDelUsuario(USR, 13)).thenReturn(777);
        when(datos.recibo(777, 13)).thenReturn(new ReciboChequeRptDto());

        conExportadorFalso(j).reciboDelUltimoCheque(auth, req(5, 13));

        Map<String, Object> p = parametros(j, "RptCheqCajRec");
        assertEquals(777, p.get("codigo"), "la plantilla lo declara Integer");
        assertEquals(13, p.get("codSucursal"), "la plantilla lo declara Integer");
        assertNotNull(p.get("logoEmpresa2"));
        assertEquals(" ( Central EPP )", p.get("sucursal"));
    }

    @Test
    @DisplayName("recibo del ultimo cheque: un cheque sin accion de recepcion (el SP no devuelve fila) da un error claro")
    void reciboSinRecepcion() {
        when(datos.ultimoChequeDelUsuario(USR, SUC)).thenReturn(501);
        when(datos.recibo(501, SUC)).thenReturn(null);
        assertThrows(SpBusinessException.class,
                () -> conExportadorFalso(jasperFalso()).reciboDelUltimoCheque(auth, req(1, SUC)));
    }

    @Test
    @DisplayName("reimpresion de un traspaso: exige la accion elegida y la manda como codigo (y como 'codCliente' de la plantilla)")
    void reimpresion() {
        JasperReportExport j = jasperFalso();
        ChequeReporteService s = conExportadorFalso(j);
        assertThrows(SpBusinessException.class, () -> s.reimpresionDeTraspaso(auth, req(1, SUC)));

        ReporteChequeRequest r = req(1, SUC);
        r.setCodAccion(900L);
        s.reimpresionDeTraspaso(auth, r);

        verify(datos).reimpresionTraspaso(SUC, 900L);
        Map<String, Object> p = parametros(j, "RptChkTraspasoAdm");
        assertEquals("900", p.get("codCliente"));
        assertEquals(1, p.get("codSucursal"), "la plantilla lo declara Integer");
    }

    @Test
    @DisplayName("sin el nombre en el token (token viejo) el 'impreso por' queda vacio, no null")
    void tokenSinNombre() {
        JasperReportExport j = jasperFalso();
        UsernamePasswordAuthenticationToken viejo = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        viejo.setDetails(new DatosToken(USR, 3, 6, "lim"));

        conExportadorFalso(j).recibidos(viejo, req(1, SUC));

        assertEquals("", parametros(j, "RptChequeCajaRecepcion").get("usuario"));
    }
}
