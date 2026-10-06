package bo.bosque.com.impexpap.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.AccesoPanelesCheque;
import bo.bosque.com.impexpap.commons.ChequeService;
import bo.bosque.com.impexpap.commons.MensajesCheque;
import bo.bosque.com.impexpap.commons.NotaRemisionChequeService;
import bo.bosque.com.impexpap.commons.PostergacionChequeService;
import bo.bosque.com.impexpap.commons.PostergacionPdfService;
import bo.bosque.com.impexpap.commons.TransaccionChequeService;
import bo.bosque.com.impexpap.config.GlobalExceptionHandler;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IChNotaRemision;
import bo.bosque.com.impexpap.dao.IChPostergacion;
import bo.bosque.com.impexpap.dao.IChTransaccionBancaria;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.NotaRemisionDto;
import bo.bosque.com.impexpap.dto.PostergacionDto;
import bo.bosque.com.impexpap.dto.TransaccionBancariaDto;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.model.ChNotaRemision;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.model.ChTransaccionBancaria;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Los tres paneles del detalle del cheque, <b>por HTTP</b> (MockMvc con el {@code GlobalExceptionHandler} real y los servicios
 * reales sobre DAO simulados): las rutas, la forma exacta de {@code data}, los codigos, los cuerpos, el multipart y como llegan
 * los errores. Es lo que el frontend tiene escrito como contrato en {@code API_CHEQUES.md} ("Paneles del detalle").
 */
class ChChequeSatelitesControllerTest {

    private static final int COD = 18129;
    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path carpeta;

    private IChCheque cheques;
    private ChequeService chequeService;
    private AccesoModuloHelper helper;
    private IChNotaRemision notas;
    private IChTransaccionBancaria transacciones;
    private IChPostergacion postergaciones;
    private IChBanco bancos;
    private Authentication auth;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        cheques = mock(IChCheque.class);
        chequeService = mock(ChequeService.class);
        helper = mock(AccesoModuloHelper.class);
        notas = mock(IChNotaRemision.class);
        transacciones = mock(IChTransaccionBancaria.class);
        postergaciones = mock(IChPostergacion.class);
        bancos = mock(IChBanco.class);

        ChequeFilaDto c = new ChequeFilaDto();
        c.setCodCheque(COD);
        c.setCodSucursal(3L);
        c.setEstado("PEN");
        when(cheques.obtener(COD)).thenReturn(c);
        ChBanco b = new ChBanco();
        b.setCodBanco(2);
        when(bancos.obtener(2)).thenReturn(b);

        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        auth = t;

        mvc = montar(carpeta.toString());
        conTodosLosBotones();
    }

    private MockMvc montar(String carpetaDePdf) {
        AccesoPanelesCheque acceso = new AccesoPanelesCheque(cheques, chequeService, helper);
        PostergacionPdfService pdfs = new PostergacionPdfService(postergaciones, acceso, carpetaDePdf);
        ChChequeSatelitesController controlador = new ChChequeSatelitesController(
                new NotaRemisionChequeService(notas, acceso), new TransaccionChequeService(transacciones, bancos, acceso),
                new PostergacionChequeService(postergaciones, pdfs, acceso), pdfs);
        // El mismo mapeador de la casa en lo que importa: fechas con la zona de La Paz (spring.jackson.time-zone).
        // Spring Boot lo arma con Jackson2ObjectMapperBuilder: FAIL_ON_UNKNOWN_PROPERTIES apagado, asi que un campo de mas se ignora.
        ObjectMapper mapeador = Jackson2ObjectMapperBuilder.json().timeZone(TimeZone.getTimeZone(LA_PAZ)).build();
        return MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new MappingJackson2HttpMessageConverter(mapeador)).build();
    }

    private void conTodosLosBotones() {
        doNothing().when(helper).exigirBoton(any(), anyInt(), anyString());
    }

    // ------------------------------------------------------------------ utilidades

    private static Date dia(int a, int m, int d) {
        return Date.from(LocalDate.of(a, m, d).atStartOfDay(LA_PAZ).toInstant());
    }

    private static Date hora(int a, int m, int d, int h, int mi, int s) {
        return Date.from(LocalDateTime.of(a, m, d, h, mi, s).atZone(LA_PAZ).toInstant());
    }

    private static LocalDate comoDia(Date d) {
        return d.toInstant().atZone(LA_PAZ).toLocalDate();
    }

    private static byte[] pdf(int bytes) {
        byte[] b = new byte[bytes];
        Arrays.fill(b, (byte) 'x');
        byte[] marca = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(marca, 0, b, 0, marca.length);
        return b;
    }

    private MvcResult json(String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(ruta).principal(auth).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL).content(cuerpo)).andReturn();
    }

    private MvcResult subirArchivo(String nombre, byte[] contenido) throws Exception {
        return mvc.perform(multipart("/cheque/postergacion/pdf/subir")
                .file(new MockMultipartFile("archivo", nombre, "application/pdf", contenido))
                .param("codPostergacion", "40").principal(auth).accept(MediaType.APPLICATION_JSON)).andReturn();
    }

    private static JsonNode cuerpo(MvcResult r) throws Exception {
        return JSON.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Set<String> claves(JsonNode objeto) {
        Set<String> k = new TreeSet<>();
        for (Iterator<String> it = objeto.fieldNames(); it.hasNext(); ) k.add(it.next());
        return k;
    }

    private static Set<String> set(String... s) {
        return new TreeSet<>(Arrays.asList(s));
    }

    private long archivosEnLaCarpeta() throws Exception {
        try (java.util.stream.Stream<Path> s = Files.list(carpeta)) {
            return s.count();
        }
    }

    private void postergacionConPdf() throws Exception {
        ChPostergacion p = new ChPostergacion();
        p.setCodPostergacion(40);
        p.setCodCheque(COD);
        when(postergaciones.obtener(40)).thenReturn(p);
    }

    // ------------------------------------------------------------------ rutas = contrato

    private static final Set<String> RUTAS = set(
            "/nota-remision/listar", "/nota-remision/registrar", "/nota-remision/eliminar",
            "/transaccion/listar", "/transaccion/registrar", "/transaccion/eliminar",
            "/postergacion/listar", "/postergacion/registrar", "/postergacion/eliminar",
            "/postergacion/pdf/estado", "/postergacion/pdf/subir", "/postergacion/pdf/descargar");

    @Test
    @DisplayName("las 12 rutas son exactamente las del contrato (API_CHEQUES.md), todas POST y bajo /cheque; ninguna expone la rama U de la postergacion")
    void rutas() {
        assertEquals("/cheque", ChChequeSatelitesController.class.getAnnotation(RequestMapping.class).value()[0]);
        Set<String> halladas = new TreeSet<>();
        for (Method m : ChChequeSatelitesController.class.getDeclaredMethods()) {
            PostMapping pm = m.getAnnotation(PostMapping.class);
            if (pm != null) halladas.addAll(Arrays.asList(pm.value()));
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                assertNotNull(pm, "el handler " + m.getName() + " no es @PostMapping");
            }
        }
        assertEquals(RUTAS, halladas);
        assertEquals(12, halladas.size());
    }

    @Test
    @DisplayName("exige ROLE_ADM o ROLE_LIM a nivel de clase")
    void seguridad() {
        PreAuthorize pa = ChChequeSatelitesController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(pa);
        assertTrue(pa.value().contains("ROLE_ADM") && pa.value().contains("ROLE_LIM"));
    }

    @Test
    @DisplayName("pdf/subir: la parte de texto es 'codPostergacion' y el archivo va en 'archivo'; las dos opcionales para dar un mensaje propio")
    void pdfSubirPartes() throws Exception {
        Method m = ChChequeSatelitesController.class.getMethod("pdfSubir", String.class, MultipartFile.class, Authentication.class);
        RequestParam cod = (RequestParam) m.getParameterAnnotations()[0][0];
        RequestParam archivo = (RequestParam) m.getParameterAnnotations()[1][0];

        assertEquals("codPostergacion", cod.value());
        assertEquals("archivo", archivo.value());
        assertFalse(cod.required());
        assertFalse(archivo.required());
    }

    // ------------------------------------------------------------------ notas de remision

    @Test
    @DisplayName("nota-remision/listar: 200 y data = lista con EXACTAMENTE {fila, codCheque, notaRemision, nroFactura, fechaFactura, audUsuario, audFecha}; fechas yyyy-MM-dd y yyyy-MM-dd'T'HH:mm:ss")
    void notasListar() throws Exception {
        NotaRemisionDto n = new NotaRemisionDto();
        n.setFila(1);
        n.setCodCheque(COD);
        n.setNotaRemision("262211881");
        n.setNroFactura(1856);
        n.setFechaFactura(dia(2026, 8, 31));
        n.setAudUsuario(66);
        n.setAudFecha(hora(2026, 9, 1, 9, 5, 17));
        when(notas.listarPorCheque(COD)).thenReturn(Collections.singletonList(n));

        MvcResult r = json("/cheque/nota-remision/listar", "{\"codCheque\":18129}");

        assertEquals(200, r.getResponse().getStatus());
        assertEquals("Operación realizada exitosamente", cuerpo(r).get("message").asText());
        assertEquals(200, cuerpo(r).get("status").asInt());
        JsonNode fila = cuerpo(r).get("data").get(0);
        assertEquals(set("fila", "codCheque", "notaRemision", "nroFactura", "fechaFactura", "audUsuario", "audFecha"), claves(fila));
        assertEquals(1, fila.get("fila").asInt());
        assertEquals("262211881", fila.get("notaRemision").asText());
        assertEquals(1856, fila.get("nroFactura").asInt());
        assertEquals("2026-08-31", fila.get("fechaFactura").asText());
        assertEquals("2026-09-01T09:05:17", fila.get("audFecha").asText());
    }

    @Test
    @DisplayName("listas vacias: 204 sin datos (las tres)")
    void listasVacias() throws Exception {
        assertEquals(204, json("/cheque/nota-remision/listar", "{\"codCheque\":18129}").getResponse().getStatus());
        assertEquals(204, json("/cheque/transaccion/listar", "{\"codCheque\":18129}").getResponse().getStatus());
        assertEquals(204, json("/cheque/postergacion/listar", "{\"codCheque\":18129}").getResponse().getStatus());
    }

    @Test
    @DisplayName("listar sin codCheque ({}): 400 con el mensaje del modulo")
    void listarSinCheque() throws Exception {
        for (String ruta : new String[] {"/cheque/nota-remision/listar", "/cheque/transaccion/listar", "/cheque/postergacion/listar"}) {
            MvcResult r = json(ruta, "{}");
            assertEquals(400, r.getResponse().getStatus(), ruta);
            assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, cuerpo(r).get("message").asText());
        }
    }

    @Test
    @DisplayName("nota-remision/registrar: 201, data=1 y el DAO recibe el usuario del TOKEN (7) aunque el cuerpo traiga otro audUsuario; la fecha es el dia pedido")
    void notasRegistrar() throws Exception {
        when(notas.registrar(any())).thenReturn(new RespuestaSp(0, "", 1L));

        MvcResult r = json("/cheque/nota-remision/registrar",
                "{\"codCheque\":18129,\"notaRemision\":\"262211881\",\"nroFactura\":1856,\"fechaFactura\":\"2026-08-31\",\"audUsuario\":99}");

        assertEquals(201, r.getResponse().getStatus());
        assertEquals(1, cuerpo(r).get("data").asInt());
        assertEquals(201, cuerpo(r).get("status").asInt());
        assertEquals("Nota de remisión registrada.", cuerpo(r).get("message").asText());
        ArgumentCaptor<ChNotaRemision> c = ArgumentCaptor.forClass(ChNotaRemision.class);
        verify(notas).registrar(c.capture());
        assertEquals(Integer.valueOf(7), c.getValue().getAudUsuario());
        assertEquals(LocalDate.of(2026, 8, 31), comoDia(c.getValue().getFechaFactura()));
        assertEquals(Integer.valueOf(1856), c.getValue().getNroFactura());
    }

    @Test
    @DisplayName("nota-remision/registrar con campos mal: 400 con TODOS los mensajes, uno por linea; nada se escribe")
    void notasRegistrarConErrores() throws Exception {
        MvcResult r = json("/cheque/nota-remision/registrar", "{\"codCheque\":18129,\"notaRemision\":\"12\",\"nroFactura\":0}");

        assertEquals(400, r.getResponse().getStatus());
        String[] lineas = cuerpo(r).get("message").asText().split("\n");
        assertEquals(3, lineas.length);
        assertTrue(lineas[0].startsWith("El número de la nota de remisión «12»"));
        assertTrue(lineas[1].startsWith("El número de factura debe ser mayor a cero"));
        assertEquals(MensajesCheque.FALTA_FECHA_FACTURA, lineas[2]);
        verify(notas, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar sin el boton: 403 con el motivo en message (btnNuevoNRCH), como el resto de /cheque")
    void registrarSinBoton() throws Exception {
        doThrow(new AccessDeniedException("sin boton")).when(helper).exigirBoton(any(), anyInt(), anyString());

        for (String[] caso : new String[][] {
                {"/cheque/nota-remision/registrar", "{\"codCheque\":18129,\"notaRemision\":\"262211881\",\"nroFactura\":1,\"fechaFactura\":\"2026-08-31\"}"},
                {"/cheque/transaccion/registrar", "{\"codCheque\":18129,\"nroTransaccion\":\"TT262318S5JD\",\"codBanco\":2,\"fechaTransaccion\":\"2026-08-31\"}"},
                {"/cheque/postergacion/registrar", "{\"codCheque\":18129,\"fecha\":\"2026-08-31\",\"observacion\":\"cliente pidio\"}"}}) {
            MvcResult r = json(caso[0], caso[1]);
            assertEquals(403, r.getResponse().getStatus(), caso[0]);
            assertEquals(MensajesCheque.sinPermiso("btnNuevoNRCH"), cuerpo(r).get("message").asText());
        }
        verify(notas, never()).registrar(any());
        verify(transacciones, never()).registrar(any());
        verify(postergaciones, never()).registrar(any());
    }

    @Test
    @DisplayName("nota-remision/eliminar: 201, data = filas eliminadas, y el message dice cuando se borraron repetidas")
    void notasEliminar() throws Exception {
        NotaRemisionDto a = new NotaRemisionDto();
        a.setNotaRemision("262211881");
        NotaRemisionDto b = new NotaRemisionDto();
        b.setNotaRemision("262211881");
        when(notas.listarPorCheque(COD)).thenReturn(Arrays.asList(a, b));
        when(notas.eliminar(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 2L));

        MvcResult r = json("/cheque/nota-remision/eliminar", "{\"codCheque\":18129,\"notaRemision\":\"262211881\"}");

        assertEquals(201, r.getResponse().getStatus());
        assertEquals(2, cuerpo(r).get("data").asInt());
        assertEquals(MensajesCheque.notaEliminada(2, "262211881"), cuerpo(r).get("message").asText());
        verify(notas).eliminar(COD, "262211881", 7);
    }

    @Test
    @DisplayName("nota-remision/eliminar una nota que el cheque no tiene: 400 con el dato concreto")
    void notasEliminarInexistente() throws Exception {
        MvcResult r = json("/cheque/nota-remision/eliminar", "{\"codCheque\":18129,\"notaRemision\":\"262211881\"}");

        assertEquals(400, r.getResponse().getStatus());
        assertEquals(MensajesCheque.notaNoEncontrada(COD, "262211881"), cuerpo(r).get("message").asText());
    }

    // ------------------------------------------------------------------ transacciones

    @Test
    @DisplayName("transaccion/listar: data con EXACTAMENTE {fila, codCheque, nroTransaccion, codBanco, fechaTransaccion, datoBanco}; audUsuario y audFecha no salen")
    void transaccionesListar() throws Exception {
        TransaccionBancariaDto t = new TransaccionBancariaDto();
        t.setFila(1);
        t.setCodCheque(COD);
        t.setNroTransaccion("TT262318S5JD");
        t.setCodBanco(2);
        t.setFechaTransaccion(dia(2026, 8, 19));
        t.setDatoBanco("BANCO UNION");
        when(transacciones.listarPorCheque(COD)).thenReturn(Collections.singletonList(t));

        MvcResult r = json("/cheque/transaccion/listar", "{\"codCheque\":18129}");

        assertEquals(200, r.getResponse().getStatus());
        JsonNode fila = cuerpo(r).get("data").get(0);
        assertEquals(set("fila", "codCheque", "nroTransaccion", "codBanco", "fechaTransaccion", "datoBanco"), claves(fila));
        assertEquals("2026-08-19", fila.get("fechaTransaccion").asText());
        assertEquals("BANCO UNION", fila.get("datoBanco").asText());
    }

    @Test
    @DisplayName("transaccion/registrar: 201 y data=1; el DAO recibe banco, fecha y el usuario del token")
    void transaccionesRegistrar() throws Exception {
        when(transacciones.registrar(any())).thenReturn(new RespuestaSp(0, "", 1L));

        MvcResult r = json("/cheque/transaccion/registrar",
                "{\"codCheque\":18129,\"nroTransaccion\":\"TT262318S5JD\",\"codBanco\":2,\"fechaTransaccion\":\"2026-08-19\",\"audUsuario\":99}");

        assertEquals(201, r.getResponse().getStatus());
        assertEquals(1, cuerpo(r).get("data").asInt());
        assertEquals("Transacción bancaria registrada.", cuerpo(r).get("message").asText());
        ArgumentCaptor<ChTransaccionBancaria> c = ArgumentCaptor.forClass(ChTransaccionBancaria.class);
        verify(transacciones).registrar(c.capture());
        assertEquals(Integer.valueOf(7), c.getValue().getAudUsuario());
        assertEquals(Integer.valueOf(2), c.getValue().getCodBanco());
        assertEquals(LocalDate.of(2026, 8, 19), comoDia(c.getValue().getFechaTransaccion()));
    }

    @Test
    @DisplayName("transaccion/registrar con numero corto y sin fecha: 400 (lo obligatorio primero, como el JSF)")
    void transaccionesRegistrarMal() throws Exception {
        MvcResult r = json("/cheque/transaccion/registrar", "{\"codCheque\":18129,\"nroTransaccion\":\"123\",\"codBanco\":2}");

        assertEquals(400, r.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_FECHA_TRANSACCION, cuerpo(r).get("message").asText());
    }

    @Test
    @DisplayName("transaccion/eliminar: 201 y data=filas; sin boton propio, con solo btnDetalleCH (el del panel)")
    void transaccionesEliminar() throws Exception {
        doThrow(new AccessDeniedException("sin boton")).when(helper).exigirBoton(any(), anyInt(), anyString());
        doNothing().when(helper).exigirBoton(any(), anyInt(), eq("btnDetalleCH"));
        TransaccionBancariaDto t = new TransaccionBancariaDto();
        t.setNroTransaccion("TT262318S5JD");
        when(transacciones.listarPorCheque(COD)).thenReturn(Collections.singletonList(t));
        when(transacciones.eliminar(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 1L));

        MvcResult r = json("/cheque/transaccion/eliminar", "{\"codCheque\":18129,\"nroTransaccion\":\"TT262318S5JD\"}");

        assertEquals(201, r.getResponse().getStatus());
        assertEquals(1, cuerpo(r).get("data").asInt());
        assertEquals("Transacción bancaria eliminada.", cuerpo(r).get("message").asText());
        verify(transacciones).eliminar(COD, "TT262318S5JD", 7);
    }

    // ------------------------------------------------------------------ postergaciones

    @Test
    @DisplayName("postergacion/listar: data con EXACTAMENTE {fila, codPostergacion, codCheque, fecha, observacion, nombreArchivo, audUsuario, audFecha, tienePdf}")
    void postergacionesListar() throws Exception {
        Files.write(carpeta.resolve("40.pdf"), pdf(100));
        PostergacionDto con = new PostergacionDto();
        con.setCodPostergacion(40);
        con.setCodCheque(COD);
        con.setFecha(dia(2025, 10, 30));
        con.setObservacion("cambio de cheque 277");
        con.setNombreArchivo("");
        con.setAudUsuario(30);
        con.setAudFecha(hora(2025, 10, 30, 16, 52, 19));
        con.setFila(2);
        PostergacionDto sin = new PostergacionDto();
        sin.setCodPostergacion(38);
        sin.setCodCheque(COD);
        sin.setFecha(dia(2025, 5, 14));
        sin.setObservacion("cheque cobrado en fecha");
        sin.setFila(1);
        when(postergaciones.listarPorCheque(COD)).thenReturn(Arrays.asList(sin, con));

        MvcResult r = json("/cheque/postergacion/listar", "{\"codCheque\":18129}");

        assertEquals(200, r.getResponse().getStatus());
        JsonNode datos = cuerpo(r).get("data");
        assertEquals(set("fila", "codPostergacion", "codCheque", "fecha", "observacion", "nombreArchivo", "audUsuario", "audFecha", "tienePdf"),
                claves(datos.get(1)));
        assertFalse(datos.get(0).get("tienePdf").asBoolean());
        assertTrue(datos.get(0).get("tienePdf").isBoolean());
        assertTrue(datos.get(1).get("tienePdf").asBoolean());
        assertEquals("2025-10-30", datos.get(1).get("fecha").asText());
        assertEquals("2025-10-30T16:52:19", datos.get(1).get("audFecha").asText());
        assertEquals("", datos.get(1).get("nombreArchivo").asText());
    }

    @Test
    @DisplayName("postergacion/listar con la carpeta de PDF sin configurar: 200 igual, con tienePdf = null (nunca falla por eso)")
    void postergacionesListarSinCarpeta() throws Exception {
        PostergacionDto p = new PostergacionDto();
        p.setCodPostergacion(40);
        p.setCodCheque(COD);
        when(postergaciones.listarPorCheque(COD)).thenReturn(Collections.singletonList(p));
        mvc = montar("");

        MvcResult r = json("/cheque/postergacion/listar", "{\"codCheque\":18129}");

        assertEquals(200, r.getResponse().getStatus());
        assertTrue(cuerpo(r).get("data").get(0).get("tienePdf").isNull());
    }

    @Test
    @DisplayName("postergacion/registrar: 201 y data = codPostergacion nuevo; el DAO recibe el usuario del token")
    void postergacionesRegistrar() throws Exception {
        when(postergaciones.registrar(any())).thenReturn(new RespuestaSp(0, "", 41L));

        MvcResult r = json("/cheque/postergacion/registrar",
                "{\"codCheque\":18129,\"fecha\":\"2025-10-30\",\"observacion\":\"cliente envió carta\\nautorizada\",\"audUsuario\":99}");

        assertEquals(201, r.getResponse().getStatus());
        assertEquals(41, cuerpo(r).get("data").asInt());
        assertEquals("Postergación registrada.", cuerpo(r).get("message").asText());
        ArgumentCaptor<ChPostergacion> c = ArgumentCaptor.forClass(ChPostergacion.class);
        verify(postergaciones).registrar(c.capture());
        assertEquals(Integer.valueOf(7), c.getValue().getAudUsuario());
        assertEquals("cliente envió carta\nautorizada", c.getValue().getObservacion());
        assertEquals(LocalDate.of(2025, 10, 30), comoDia(c.getValue().getFecha()));
    }

    @Test
    @DisplayName("postergacion/registrar con observacion corta: 400 con el texto completo")
    void postergacionesRegistrarMal() throws Exception {
        MvcResult r = json("/cheque/postergacion/registrar", "{\"codCheque\":18129,\"fecha\":\"2025-10-30\",\"observacion\":\"ab\"}");

        assertEquals(400, r.getResponse().getStatus());
        assertEquals(MensajesCheque.observacionPostergacionCorta("ab", 2), cuerpo(r).get("message").asText());
    }

    @Test
    @DisplayName("postergacion/eliminar: 201 y data = codPostergacion; una postergacion de otro cheque es 400 con el motivo")
    void postergacionesEliminar() throws Exception {
        ChPostergacion p = new ChPostergacion();
        p.setCodPostergacion(40);
        p.setCodCheque(COD);
        ChPostergacion ajena = new ChPostergacion();
        ajena.setCodPostergacion(41);
        ajena.setCodCheque(999);
        when(postergaciones.obtener(40)).thenReturn(p);
        when(postergaciones.obtener(41)).thenReturn(ajena);
        when(postergaciones.eliminar(anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 40L));

        MvcResult ok = json("/cheque/postergacion/eliminar", "{\"codCheque\":18129,\"codPostergacion\":40}");
        assertEquals(201, ok.getResponse().getStatus());
        assertEquals(40, cuerpo(ok).get("data").asInt());
        assertEquals("Postergación eliminada.", cuerpo(ok).get("message").asText());
        verify(postergaciones).eliminar(40, 7);

        MvcResult ajeno = json("/cheque/postergacion/eliminar", "{\"codCheque\":18129,\"codPostergacion\":41}");
        assertEquals(400, ajeno.getResponse().getStatus());
        assertEquals(MensajesCheque.postergacionDeOtroCheque(41, COD), cuerpo(ajeno).get("message").asText());
        verify(postergaciones, never()).eliminar(eq(41), anyInt());
    }

    // ------------------------------------------------------------------ PDF de la postergacion

    @Test
    @DisplayName("pdf/estado sin archivo: 200 y data con EXACTAMENTE {existe, nombreArchivo, tamanoBytes, fechaModificacion}, los dos ultimos null")
    void pdfEstadoSinArchivo() throws Exception {
        postergacionConPdf();

        MvcResult r = json("/cheque/postergacion/pdf/estado", "{\"codPostergacion\":40}");

        assertEquals(200, r.getResponse().getStatus());
        JsonNode data = cuerpo(r).get("data");
        assertEquals(set("existe", "nombreArchivo", "tamanoBytes", "fechaModificacion"), claves(data));
        assertFalse(data.get("existe").asBoolean());
        assertEquals("40.pdf", data.get("nombreArchivo").asText());
        assertTrue(data.get("tamanoBytes").isNull());
        assertTrue(data.get("fechaModificacion").isNull());
    }

    @Test
    @DisplayName("pdf/estado con archivo: existe=true, tamano numerico y fecha local sin zona yyyy-MM-dd'T'HH:mm:ss")
    void pdfEstadoConArchivo() throws Exception {
        postergacionConPdf();
        Files.write(carpeta.resolve("40.pdf"), pdf(5000));

        JsonNode data = cuerpo(json("/cheque/postergacion/pdf/estado", "{\"codPostergacion\":40}")).get("data");

        assertTrue(data.get("existe").asBoolean());
        assertEquals(5000, data.get("tamanoBytes").asLong());
        assertTrue(data.get("fechaModificacion").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    @DisplayName("pdf/subir: multipart con codPostergacion y 'archivo' -> 200 y data con EXACTAMENTE {nombreArchivo, tamanoBytes, reemplazo}; el mensaje dice guardado o reemplazado")
    void pdfSubir() throws Exception {
        postergacionConPdf();
        byte[] contenido = pdf(7000);

        MvcResult r = subirArchivo("Carta.pdf", contenido);

        assertEquals(200, r.getResponse().getStatus());
        JsonNode data = cuerpo(r).get("data");
        assertEquals(set("nombreArchivo", "tamanoBytes", "reemplazo"), claves(data));
        assertEquals("40.pdf", data.get("nombreArchivo").asText());
        assertEquals(7000, data.get("tamanoBytes").asLong());
        assertFalse(data.get("reemplazo").asBoolean());
        assertEquals("PDF de la postergación guardado.", cuerpo(r).get("message").asText());
        assertArrayEquals(contenido, Files.readAllBytes(carpeta.resolve("40.pdf")));

        MvcResult otra = subirArchivo("otra.PDF", pdf(2000));
        assertTrue(cuerpo(otra).get("data").get("reemplazo").asBoolean());
        assertEquals("PDF de la postergación reemplazado.", cuerpo(otra).get("message").asText());
        assertEquals(2000, Files.size(carpeta.resolve("40.pdf")));
    }

    @Test
    @DisplayName("pdf/subir sin la parte 'archivo' o sin 'codPostergacion', o con otro nombre de parte: 400 con un mensaje explicito")
    void pdfSubirIncompleto() throws Exception {
        postergacionConPdf();
        MvcResult sinArchivo = mvc.perform(multipart("/cheque/postergacion/pdf/subir").param("codPostergacion", "40").principal(auth)
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        assertEquals(400, sinArchivo.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_ARCHIVO_PDF_POSTERGACION, cuerpo(sinArchivo).get("message").asText());

        MvcResult sinCodigo = mvc.perform(multipart("/cheque/postergacion/pdf/subir")
                .file(new MockMultipartFile("archivo", "a.pdf", "application/pdf", pdf(100))).principal(auth)
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        assertEquals(400, sinCodigo.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_POSTERGACION_PDF, cuerpo(sinCodigo).get("message").asText());

        MvcResult otroNombre = mvc.perform(multipart("/cheque/postergacion/pdf/subir")
                .file(new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(100)))
                .param("codPostergacion", "40").principal(auth).accept(MediaType.APPLICATION_JSON)).andReturn();
        assertEquals(400, otroNombre.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_ARCHIVO_PDF_POSTERGACION, cuerpo(otroNombre).get("message").asText());
        assertFalse(Files.exists(carpeta.resolve("40.pdf")));
    }

    @Test
    @DisplayName("pdf/subir un archivo que no es PDF, y uno de mas de 2.010.000 bytes: 400 con el mensaje")
    void pdfSubirRechazos() throws Exception {
        postergacionConPdf();

        MvcResult noPdf = subirArchivo("foto.png", pdf(100));
        assertEquals(400, noPdf.getResponse().getStatus());
        assertEquals(MensajesCheque.archivoNoEsPdfPostergacion("foto.png"), cuerpo(noPdf).get("message").asText());

        MvcResult grande = subirArchivo("grande.pdf", pdf(2_010_001));
        assertEquals(400, grande.getResponse().getStatus());
        assertEquals(MensajesCheque.archivoPdfMuyGrande(2_010_001, 2_010_000), cuerpo(grande).get("message").asText());
        assertEquals(0, archivosEnLaCarpeta());
    }

    @Test
    @DisplayName("pdf/descargar: 200, application/pdf, los bytes tal cual y Content-Disposition: attachment; filename=\"Posterg_40_.pdf\"")
    void pdfDescargar() throws Exception {
        postergacionConPdf();
        byte[] contenido = pdf(3210);
        Files.write(carpeta.resolve("40.pdf"), contenido);

        MvcResult r = json("/cheque/postergacion/pdf/descargar", "{\"codPostergacion\":40}");

        assertEquals(200, r.getResponse().getStatus());
        assertEquals("application/pdf", r.getResponse().getContentType());
        assertEquals("attachment; filename=\"Posterg_40_.pdf\"", r.getResponse().getHeader("Content-Disposition"));
        assertEquals(3210, r.getResponse().getContentLength());
        assertArrayEquals(contenido, r.getResponse().getContentAsByteArray());
    }

    @Test
    @DisplayName("pdf/descargar sin archivo: NO es un 200 vacio sino un 400 con el mensaje en JSON")
    void pdfDescargarSinArchivo() throws Exception {
        postergacionConPdf();

        MvcResult r = json("/cheque/postergacion/pdf/descargar", "{\"codPostergacion\":40}");

        assertEquals(400, r.getResponse().getStatus());
        assertEquals(MensajesCheque.pdfPostergacionNoEncontrado(40), cuerpo(r).get("message").asText());
        assertEquals(400, cuerpo(r).get("status").asInt());
        assertTrue(r.getResponse().getContentType().startsWith("application/json"), r.getResponse().getContentType());
    }

    @Test
    @DisplayName("los tres PDF: sin permiso de sucursal 403 con el motivo; con la carpeta sin configurar 400 con el mensaje claro")
    void pdfErrores() throws Exception {
        postergacionConPdf();
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        for (MvcResult r : new MvcResult[] {json("/cheque/postergacion/pdf/estado", "{\"codPostergacion\":40}"),
                json("/cheque/postergacion/pdf/descargar", "{\"codPostergacion\":40}"), subirArchivo("a.pdf", pdf(100))}) {
            assertEquals(403, r.getResponse().getStatus());
            assertEquals(MensajesCheque.SOLO_SU_SUCURSAL, cuerpo(r).get("message").asText());
        }
        assertEquals(0, archivosEnLaCarpeta(), "nada se escribio");

        doNothing().when(chequeService).exigirSucursalLectura(any(), any());
        mvc = montar("");
        for (MvcResult r : new MvcResult[] {json("/cheque/postergacion/pdf/estado", "{\"codPostergacion\":40}"),
                json("/cheque/postergacion/pdf/descargar", "{\"codPostergacion\":40}"), subirArchivo("a.pdf", pdf(100))}) {
            assertEquals(400, r.getResponse().getStatus());
            assertEquals(MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA, cuerpo(r).get("message").asText());
        }
    }

    /** Las 12 llamadas, con un cuerpo valido para cada una: {ruta, cuerpo JSON o null para la subida del PDF}. */
    private static final String[][] LAS_DOCE = {
            {"/cheque/nota-remision/listar", "{\"codCheque\":18129}"},
            {"/cheque/nota-remision/registrar", "{\"codCheque\":18129,\"notaRemision\":\"262211881\",\"nroFactura\":1,\"fechaFactura\":\"2026-08-31\"}"},
            {"/cheque/nota-remision/eliminar", "{\"codCheque\":18129,\"notaRemision\":\"262211881\"}"},
            {"/cheque/transaccion/listar", "{\"codCheque\":18129}"},
            {"/cheque/transaccion/registrar", "{\"codCheque\":18129,\"nroTransaccion\":\"TT262318S5JD\",\"codBanco\":2,\"fechaTransaccion\":\"2026-08-31\"}"},
            {"/cheque/transaccion/eliminar", "{\"codCheque\":18129,\"nroTransaccion\":\"TT262318S5JD\"}"},
            {"/cheque/postergacion/listar", "{\"codCheque\":18129}"},
            {"/cheque/postergacion/registrar", "{\"codCheque\":18129,\"fecha\":\"2026-08-31\",\"observacion\":\"cliente pidio\"}"},
            {"/cheque/postergacion/eliminar", "{\"codCheque\":18129,\"codPostergacion\":40}"},
            {"/cheque/postergacion/pdf/estado", "{\"codPostergacion\":40}"},
            {"/cheque/postergacion/pdf/subir", null},
            {"/cheque/postergacion/pdf/descargar", "{\"codPostergacion\":40}"}};

    private MvcResult llamar(String[] caso) throws Exception {
        return caso[1] == null ? subirArchivo("a.pdf", pdf(100)) : json(caso[0], caso[1]);
    }

    private void nadaSeToco() throws Exception {
        verify(notas, never()).registrar(any());
        verify(notas, never()).eliminar(anyInt(), anyString(), anyInt());
        verify(notas, never()).listarPorCheque(anyInt());
        verify(transacciones, never()).registrar(any());
        verify(transacciones, never()).eliminar(anyInt(), anyString(), anyInt());
        verify(transacciones, never()).listarPorCheque(anyInt());
        verify(postergaciones, never()).registrar(any());
        verify(postergaciones, never()).eliminar(anyInt(), anyInt());
        verify(postergaciones, never()).listarPorCheque(anyInt());
        verify(cheques, never()).obtener(anyInt());
        assertEquals(0, archivosEnLaCarpeta(), "nada se escribio en la carpeta de PDF");
    }

    @Test
    @DisplayName("DIFERENCIA #4: sin btnDetalleCH, aunque tenga todos los demas botones, los 12 endpoints dan 403 y el motivo nombra btnDetalleCH; no se toca ni el cheque, ni las tablas ni la carpeta")
    void sinBtnDetalleCHLosDoceEndpointsDan403() throws Exception {
        postergacionConPdf();
        doThrow(new AccessDeniedException("sin detalle")).when(helper).exigirBoton(any(), anyInt(), eq("btnDetalleCH"));

        for (String[] caso : LAS_DOCE) {
            MvcResult r = llamar(caso);

            assertEquals(403, r.getResponse().getStatus(), caso[0]);
            assertEquals(MensajesCheque.sinPermiso("btnDetalleCH"), cuerpo(r).get("message").asText(), caso[0]);
        }
        assertEquals(12, LAS_DOCE.length);
        nadaSeToco();
    }

    @Test
    @DisplayName("sin ningun boton: los 4 endpoints con boton propio nombran ESE boton (btnNuevoNRCH / btnEliminarNRCH) y los otros 8 nombran btnDetalleCH")
    void sinNingunBotonCadaEndpointNombraSuBoton() throws Exception {
        postergacionConPdf();
        doThrow(new AccessDeniedException("sin boton")).when(helper).exigirBoton(any(), anyInt(), anyString());
        java.util.Map<String, String> propio = new java.util.HashMap<>();
        propio.put("/cheque/nota-remision/registrar", "btnNuevoNRCH");
        propio.put("/cheque/transaccion/registrar", "btnNuevoNRCH");
        propio.put("/cheque/postergacion/registrar", "btnNuevoNRCH");
        propio.put("/cheque/nota-remision/eliminar", "btnEliminarNRCH");

        for (String[] caso : LAS_DOCE) {
            MvcResult r = llamar(caso);

            assertEquals(403, r.getResponse().getStatus(), caso[0]);
            String esperado = propio.getOrDefault(caso[0], "btnDetalleCH");
            assertEquals(MensajesCheque.sinPermiso(esperado), cuerpo(r).get("message").asText(), caso[0]);
        }
        nadaSeToco();
    }

    @Test
    @DisplayName("con btnDetalleCH solo: los 8 endpoints sin boton propio pasan (listar x3, eliminar transaccion y postergacion, y los 3 del PDF); registrar x3 y eliminar nota siguen pidiendo su boton")
    void conSoloBtnDetalleCH() throws Exception {
        postergacionConPdf();
        doThrow(new AccessDeniedException("sin boton")).when(helper).exigirBoton(any(), anyInt(), anyString());
        doNothing().when(helper).exigirBoton(any(), anyInt(), eq("btnDetalleCH"));
        when(transacciones.listarPorCheque(COD)).thenReturn(Collections.emptyList());

        for (String[] caso : LAS_DOCE) {
            if (caso[0].endsWith("/registrar") || caso[0].equals("/cheque/nota-remision/eliminar")) {
                assertEquals(403, llamar(caso).getResponse().getStatus(), caso[0] + " sigue pidiendo su boton");
            } else {
                int estado = llamar(caso).getResponse().getStatus();
                assertTrue(estado != 403, caso[0] + " no debia dar 403 con btnDetalleCH (dio " + estado + ")");
            }
        }
    }

    @Test
    @DisplayName("un codPostergacion ausente, 0, o que no es numero: 400 del modulo (o del manejador global), no un 500")
    void codigoDePostergacionInvalido() throws Exception {
        MvcResult sin = json("/cheque/postergacion/pdf/estado", "{}");
        assertEquals(400, sin.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_POSTERGACION_PDF, cuerpo(sin).get("message").asText());

        MvcResult cero = json("/cheque/postergacion/pdf/estado", "{\"codPostergacion\":0}");
        assertEquals(MensajesCheque.codPostergacionPdfNoValido("0"), cuerpo(cero).get("message").asText());

        MvcResult letras = json("/cheque/postergacion/pdf/estado", "{\"codPostergacion\":\"abc\"}");
        assertEquals(400, letras.getResponse().getStatus());
        assertTrue(cuerpo(letras).get("message").asText().contains("codPostergacion"));
    }
}
