package bo.bosque.com.impexpap.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import bo.bosque.com.impexpap.commons.ChequePdfService;
import bo.bosque.com.impexpap.commons.ChequeReporteService;
import bo.bosque.com.impexpap.commons.ChequeService;
import bo.bosque.com.impexpap.commons.MensajesCheque;
import bo.bosque.com.impexpap.config.GlobalExceptionHandler;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.ISocionegocio;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * El documento PDF del cheque, <b>por HTTP</b> (MockMvc con el {@code GlobalExceptionHandler} real): la ruta, la forma exacta de
 * {@code data}, el multipart con {@code codCheque} y {@code archivo}, las cabeceras de la descarga y como llegan los errores.
 * Es lo que el frontend tiene escrito como contrato en {@code API_CHEQUES.md} ("Documento PDF del cheque"); las rutas del
 * controlador estan ademas fijadas en {@code ChChequeControllerTest}.
 *
 * <p>El servicio es el real, sobre una carpeta temporal; solo los cheques y el permiso de sucursal estan simulados.
 */
class ChChequePdfControllerTest {

    private static final int COD = 18129;
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path carpeta;

    private ChequeService chequeService;
    private Authentication auth;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        IChCheque cheques = mock(IChCheque.class);
        ChequeFilaDto c = new ChequeFilaDto();
        c.setCodCheque(COD);
        c.setCodSucursal(3L);
        when(cheques.obtener(COD)).thenReturn(c);
        chequeService = mock(ChequeService.class);

        ChequePdfService pdfs = new ChequePdfService(cheques, chequeService, carpeta.toString());
        mvc = montar(pdfs);

        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        auth = t;
    }

    private MockMvc montar(ChequePdfService pdfs) {
        ChChequeController controlador = new ChChequeController(mock(ChequeService.class), mock(ChequeReporteService.class),
                pdfs, mock(ISocionegocio.class));
        return MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // ------------------------------------------------------------------ utilidades

    private static byte[] pdf(int bytes) {
        byte[] b = new byte[bytes];
        Arrays.fill(b, (byte) 'x');
        byte[] marca = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(marca, 0, b, 0, marca.length);
        return b;
    }

    /** Lo mismo que manda Dio en las peticiones JSON del resto del modulo. */
    private MvcResult json(String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(ruta).principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL)
                .content(cuerpo)).andReturn();
    }

    private MvcResult subirArchivo(String nombre, byte[] contenido) throws Exception {
        return mvc.perform(multipart("/cheque/pdf/subir")
                .file(new MockMultipartFile("archivo", nombre, "application/pdf", contenido))
                .param("codCheque", String.valueOf(COD))
                .principal(auth)
                .accept(MediaType.APPLICATION_JSON)).andReturn();
    }

    private long archivosEnLaCarpeta() throws Exception {
        try (java.util.stream.Stream<Path> s = Files.list(carpeta)) {
            return s.count();
        }
    }

    private static JsonNode cuerpo(MvcResult r) throws Exception {
        return JSON.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Set<String> claves(JsonNode objeto) {
        Set<String> k = new TreeSet<>();
        for (Iterator<String> it = objeto.fieldNames(); it.hasNext(); ) k.add(it.next());
        return k;
    }

    // ------------------------------------------------------------------ POST /cheque/pdf/estado

    @Test
    @DisplayName("estado sin archivo: 200 y data con EXACTAMENTE {existe, nombreArchivo, tamanoBytes, fechaModificacion}, los dos ultimos null")
    void estadoSinArchivo() throws Exception {
        MvcResult r = json("/cheque/pdf/estado", "{ \"codCheque\": 18129 }");

        assertEquals(200, r.getResponse().getStatus());
        JsonNode data = cuerpo(r).get("data");
        assertEquals(new TreeSet<>(Arrays.asList("existe", "nombreArchivo", "tamanoBytes", "fechaModificacion")), claves(data));
        assertFalse(data.get("existe").asBoolean());
        assertEquals("18129.pdf", data.get("nombreArchivo").asText());
        assertTrue(data.get("tamanoBytes").isNull());
        assertTrue(data.get("fechaModificacion").isNull());
    }

    @Test
    @DisplayName("estado con archivo: existe=true, tamano numerico y fecha local sin zona yyyy-MM-dd'T'HH:mm:ss")
    void estadoConArchivo() throws Exception {
        Files.write(carpeta.resolve("18129.pdf"), pdf(5000));

        JsonNode data = cuerpo(json("/cheque/pdf/estado", "{\"codCheque\":18129}")).get("data");

        assertTrue(data.get("existe").asBoolean());
        assertTrue(data.get("tamanoBytes").isIntegralNumber());
        assertEquals(5000, data.get("tamanoBytes").asLong());
        assertTrue(data.get("fechaModificacion").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"),
                data.get("fechaModificacion").asText());
    }

    // ------------------------------------------------------------------ POST /cheque/pdf/subir

    @Test
    @DisplayName("subir: multipart con la parte de texto codCheque y el archivo en 'archivo' -> 200 y data con EXACTAMENTE {nombreArchivo, tamanoBytes, reemplazo}")
    void subir() throws Exception {
        byte[] contenido = pdf(7000);

        MvcResult r = subirArchivo("Contrato.pdf", contenido);

        assertEquals(200, r.getResponse().getStatus());
        JsonNode data = cuerpo(r).get("data");
        assertEquals(new TreeSet<>(Arrays.asList("nombreArchivo", "tamanoBytes", "reemplazo")), claves(data));
        assertEquals("18129.pdf", data.get("nombreArchivo").asText());
        assertEquals(7000, data.get("tamanoBytes").asLong());
        assertTrue(data.get("reemplazo").isBoolean());
        assertFalse(data.get("reemplazo").asBoolean());
        assertArrayEquals(contenido, Files.readAllBytes(carpeta.resolve("18129.pdf")));
        assertEquals("PDF del cheque guardado.", cuerpo(r).get("message").asText());
    }

    @Test
    @DisplayName("subir por segunda vez: reemplazo=true y el mensaje lo dice")
    void subirReemplaza() throws Exception {
        subirArchivo("uno.pdf", pdf(1000));

        MvcResult r = subirArchivo("dos.PDF", pdf(2000));

        assertEquals(200, r.getResponse().getStatus());
        assertTrue(cuerpo(r).get("data").get("reemplazo").asBoolean());
        assertEquals("PDF del cheque reemplazado.", cuerpo(r).get("message").asText());
        assertEquals(2000, Files.size(carpeta.resolve("18129.pdf")));
    }

    @Test
    @DisplayName("subir sin la parte 'archivo' o sin 'codCheque': 400 con un mensaje explicito (no el error generico de una parte ausente)")
    void subirIncompleto() throws Exception {
        MvcResult sinArchivo = mvc.perform(multipart("/cheque/pdf/subir").param("codCheque", "18129").principal(auth)
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        assertEquals(400, sinArchivo.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_ARCHIVO_PDF, cuerpo(sinArchivo).get("message").asText());

        MvcResult sinCheque = mvc.perform(multipart("/cheque/pdf/subir")
                .file(new MockMultipartFile("archivo", "a.pdf", "application/pdf", pdf(100))).principal(auth)
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        assertEquals(400, sinCheque.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_CHEQUE_PDF, cuerpo(sinCheque).get("message").asText());
    }

    @Test
    @DisplayName("subir un archivo con otro nombre de parte (p. ej. 'file') no se toma por el PDF: 400 'no se recibio ningun archivo'")
    void subirConOtroNombreDeParte() throws Exception {
        MvcResult r = mvc.perform(multipart("/cheque/pdf/subir")
                .file(new MockMultipartFile("file", "a.pdf", "application/pdf", pdf(100)))
                .param("codCheque", "18129").principal(auth).accept(MediaType.APPLICATION_JSON)).andReturn();

        assertEquals(400, r.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_ARCHIVO_PDF, cuerpo(r).get("message").asText());
        assertEquals(0, archivosEnLaCarpeta());
    }

    @Test
    @DisplayName("subir un archivo que no es PDF: 400 con el mensaje de las palabras del legacy")
    void subirNoPdf() throws Exception {
        MvcResult r = subirArchivo("foto.png", pdf(100));

        assertEquals(400, r.getResponse().getStatus());
        assertTrue(cuerpo(r).get("message").asText().startsWith("Solo se permiten pdf"));
        assertTrue(cuerpo(r).get("data").isNull());
    }

    // ------------------------------------------------------------------ POST /cheque/pdf/descargar

    @Test
    @DisplayName("descargar: 200, application/pdf, los bytes tal cual y Content-Disposition: attachment; filename=\"18129_.pdf\" (sin el espacio del legacy)")
    void descargar() throws Exception {
        byte[] contenido = pdf(3210);
        Files.write(carpeta.resolve("18129.pdf"), contenido);

        MvcResult r = json("/cheque/pdf/descargar", "{\"codCheque\":18129}");

        assertEquals(200, r.getResponse().getStatus());
        assertEquals("application/pdf", r.getResponse().getContentType());
        assertEquals("attachment; filename=\"18129_.pdf\"", r.getResponse().getHeader("Content-Disposition"));
        assertEquals(3210, r.getResponse().getContentLength());
        assertArrayEquals(contenido, r.getResponse().getContentAsByteArray());
    }

    @Test
    @DisplayName("descargar sin archivo: NO es un 200 vacio sino un 400 con el mensaje en JSON (el frontend lo rescata de los bytes)")
    void descargarSinArchivo() throws Exception {
        MvcResult r = json("/cheque/pdf/descargar", "{\"codCheque\":18129}");

        assertEquals(400, r.getResponse().getStatus());
        JsonNode c = cuerpo(r);
        assertEquals(MensajesCheque.pdfNoEncontrado(COD), c.get("message").asText());
        assertEquals(400, c.get("status").asInt());
        assertTrue(r.getResponse().getContentType().startsWith("application/json"), r.getResponse().getContentType());
    }

    // ------------------------------------------------------------------ errores comunes

    @Test
    @DisplayName("sin permiso de sucursal: 403 con el motivo en message (SinPermisoException), igual que el resto de /cheque")
    void sinPermisoDeSucursal() throws Exception {
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());

        for (MvcResult r : new MvcResult[] {json("/cheque/pdf/estado", "{\"codCheque\":18129}"),
                json("/cheque/pdf/descargar", "{\"codCheque\":18129}"), subirArchivo("a.pdf", pdf(100))}) {
            assertEquals(403, r.getResponse().getStatus());
            assertEquals(MensajesCheque.SOLO_SU_SUCURSAL, cuerpo(r).get("message").asText());
        }
        assertEquals(0, archivosEnLaCarpeta(), "nada se escribio");
    }

    @Test
    @DisplayName("carpeta no configurada: 400 con el mensaje claro en los tres endpoints")
    void carpetaNoConfigurada() throws Exception {
        // El cheque tiene que existir para llegar a la comprobacion de la carpeta.
        IChCheque cheques = mock(IChCheque.class);
        ChequeFilaDto c = new ChequeFilaDto();
        c.setCodSucursal(3L);
        when(cheques.obtener(COD)).thenReturn(c);
        MockMvc sinCarpeta = montar(new ChequePdfService(cheques, chequeService, ""));

        for (MvcResult r : new MvcResult[] {
                sinCarpeta.perform(post("/cheque/pdf/estado").principal(auth).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content("{\"codCheque\":18129}")).andReturn(),
                sinCarpeta.perform(post("/cheque/pdf/descargar").principal(auth).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content("{\"codCheque\":18129}")).andReturn(),
                sinCarpeta.perform(multipart("/cheque/pdf/subir")
                        .file(new MockMultipartFile("archivo", "a.pdf", "application/pdf", pdf(100)))
                        .param("codCheque", "18129").principal(auth).accept(MediaType.APPLICATION_JSON)).andReturn()}) {
            assertEquals(400, r.getResponse().getStatus());
            assertEquals(MensajesCheque.CARPETA_PDF_NO_CONFIGURADA, cuerpo(r).get("message").asText());
        }
    }

    @Test
    @DisplayName("un cuerpo JSON con un codCheque que no es numero: 400 del manejador global, no un 500")
    void codChequeQueNoEsNumero() throws Exception {
        MvcResult r = json("/cheque/pdf/estado", "{\"codCheque\":\"abc\"}");

        assertEquals(400, r.getResponse().getStatus());
        assertTrue(cuerpo(r).get("message").asText().contains("codCheque"), cuerpo(r).get("message").asText());
    }

    @Test
    @DisplayName("un codCheque ausente o 0: 400 con el mensaje del modulo")
    void codChequeAusente() throws Exception {
        MvcResult sin = json("/cheque/pdf/estado", "{}");
        assertEquals(400, sin.getResponse().getStatus());
        assertEquals(MensajesCheque.FALTA_CHEQUE_PDF, cuerpo(sin).get("message").asText());

        MvcResult cero = json("/cheque/pdf/estado", "{\"codCheque\":0}");
        assertEquals(400, cero.getResponse().getStatus());
        assertEquals(MensajesCheque.codChequePdfNoValido("0"), cuerpo(cero).get("message").asText());
    }
}
