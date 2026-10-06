package bo.bosque.com.impexpap.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import bo.bosque.com.impexpap.commons.MensajesVerificacion;
import bo.bosque.com.impexpap.commons.VerificacionChequeService;
import bo.bosque.com.impexpap.dto.ChequePendienteFilaDto;
import bo.bosque.com.impexpap.dto.ChequePendienteFiltroDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.PrepararVerificacionDto;
import bo.bosque.com.impexpap.dto.VerificacionDepositoFilaDto;
import bo.bosque.com.impexpap.dto.VerificacionFiltroDto;
import bo.bosque.com.impexpap.dto.VerificacionPaginaDto;
import bo.bosque.com.impexpap.dto.VerificacionRegistroDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * El controlador de Verificar Cheques: forma de la respuesta, codigos HTTP y, sobre todo, que las RUTAS y el JSON sean los
 * del contrato {@code API_CHEQUES.md} ("Verificar Cheques (vista 77)"): el frontend las tiene escritas como constantes y
 * un cambio aqui rompe la pantalla sin que nada falle en compilacion.
 *
 * <p>El JSON se prueba con un {@code ObjectMapper} <b>pelado</b> a proposito: las fechas ({@code LocalDate}) llevan su
 * propio serializador en el campo, porque el mapper de la aplicacion apaga el descubrimiento de modulos.
 */
class ChVerificacionControllerTest {

    /** Los endpoints del contrato, tal cual. Si se agrega uno, se agrega aqui y a API_CHEQUES.md. */
    private static final Set<String> RUTAS = new TreeSet<>(Arrays.asList(
            "/listar", "/pendientes", "/preparar", "/registrar", "/anular"));

    private VerificacionChequeService servicio;
    private ChVerificacionController controlador;
    private Authentication auth;

    private final ObjectMapper json = new ObjectMapper()
            .setTimeZone(TimeZone.getTimeZone("America/La_Paz"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);   // como Spring Boot

    @BeforeEach
    void preparar() {
        servicio = mock(VerificacionChequeService.class);
        controlador = new ChVerificacionController(servicio);
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        auth = t;
    }

    // ------------------------------------------------------------------ rutas = contrato

    @Test
    @DisplayName("las rutas son exactamente las del contrato, bajo /cheque/verificacion, todas POST")
    void rutas() {
        assertEquals("/cheque/verificacion", ChVerificacionController.class.getAnnotation(RequestMapping.class).value()[0]);
        Set<String> rutas = new TreeSet<>();
        for (Method m : ChVerificacionController.class.getDeclaredMethods()) {
            PostMapping pm = m.getAnnotation(PostMapping.class);
            if (pm != null) rutas.addAll(Arrays.asList(pm.value()));
            if (Modifier.isPublic(m.getModifiers())) assertNotNull(pm, "el handler " + m.getName() + " no es @PostMapping");
        }
        assertEquals(RUTAS, rutas);
    }

    @Test
    @DisplayName("exige ROLE_ADM o ROLE_LIM a nivel de clase (sin permiso de boton: la vista 77 no tiene botones)")
    void seguridad() {
        PreAuthorize pa = ChVerificacionController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(pa);
        assertTrue(pa.value().contains("ROLE_ADM") && pa.value().contains("ROLE_LIM"));
    }

    // ------------------------------------------------------------------ respuestas

    @SuppressWarnings("unchecked")
    private static <T> T datos(ResponseEntity<ApiResponse<?>> r) {
        return (T) r.getBody().getData();
    }

    @Test
    @DisplayName("listar y pendientes: 200 con la pagina del servicio; sin cuerpo no revienta (filtro null)")
    void listas() {
        VerificacionPaginaDto<VerificacionDepositoFilaDto> p1 = new VerificacionPaginaDto<>(0, 1, 20, new ArrayList<>());
        when(servicio.listar(any())).thenReturn(p1);
        ResponseEntity<ApiResponse<?>> r = controlador.listar(null);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(p1, r.getBody().getData(), "sin filas tambien es 200, no 204");

        VerificacionPaginaDto<ChequePendienteFilaDto> p2 = new VerificacionPaginaDto<>(0, 1, 20, new ArrayList<>());
        when(servicio.pendientes(any())).thenReturn(p2);
        assertEquals(HttpStatus.OK, controlador.pendientes(new ChequePendienteFiltroDto()).getStatusCode());
        assertEquals(p2, controlador.pendientes(null).getBody().getData());
    }

    @Test
    @DisplayName("preparar: 200 con {cheque, fechaBanco}; el id sale del cuerpo")
    void preparar_() {
        PrepararVerificacionDto dto = new PrepararVerificacionDto(new ChequePendienteFilaDto(), LocalDate.of(2026, 10, 3));
        when(servicio.preparar(321L)).thenReturn(dto);

        ResponseEntity<ApiResponse<?>> r = controlador.preparar(new FiltroIdDto(321));

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(dto, r.getBody().getData());
    }

    @Test
    @DisplayName("registrar: 201 con el codvd en data; el mensaje distingue alta y edicion")
    void registrar() {
        when(servicio.registrar(any(), any())).thenReturn(77L);
        VerificacionRegistroDto alta = new VerificacionRegistroDto();

        ResponseEntity<ApiResponse<?>> r = controlador.registrar(alta, auth);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(77L, (long) datos(r));
        assertEquals("Verificación registrada.", r.getBody().getMessage());
        assertEquals(201, r.getBody().getStatus());

        VerificacionRegistroDto edicion = new VerificacionRegistroDto();
        edicion.setCodvd(55);
        assertEquals("Verificación actualizada.", controlador.registrar(edicion, auth).getBody().getMessage());
        verify(servicio).registrar(auth, alta);
    }

    @Test
    @DisplayName("anular: 201 con el id; el mensaje dice si la anulo o si ya estaba anulada")
    void anular() {
        when(servicio.anular(any(), anyLong())).thenReturn(true);
        ResponseEntity<ApiResponse<?>> r = controlador.anular(new FiltroIdDto(55), auth);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(55L, (long) datos(r));
        assertEquals("Verificación anulada.", r.getBody().getMessage());

        when(servicio.anular(any(), anyLong())).thenReturn(false);
        ResponseEntity<ApiResponse<?>> ya = controlador.anular(new FiltroIdDto(55), auth);
        assertEquals(HttpStatus.CREATED, ya.getStatusCode());
        assertEquals(MensajesVerificacion.YA_ESTABA_ANULADA, ya.getBody().getMessage());
        assertEquals(55L, (long) datos(ya));
    }

    // ------------------------------------------------------------------ JSON = contrato

    private static Set<String> campos(JsonNode n) {
        Set<String> s = new TreeSet<>();
        for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) s.add(it.next());
        return s;
    }

    @Test
    @DisplayName("la fila de la lista principal trae exactamente los campos del contrato: fechas yyyy-MM-dd, nroCheque texto, montoCheque numero")
    void jsonDeLaFilaPrincipal() throws Exception {
        VerificacionDepositoFilaDto f = new VerificacionDepositoFilaDto();
        f.setFila(1);
        f.setCodvd(55);
        f.setCodCheque(321L);
        f.setCodBanco(2L);
        f.setFechaBanco(LocalDate.of(2026, 10, 3));
        f.setObservacion("Deposito OK");
        f.setEstado("Y");
        f.setDatoBanco("Union BU");
        f.setNroCheque("12-345");
        f.setMontoCheque(1500.5);
        f.setDatoBancoCheque("Mercantil Santa Cruz");
        f.setDatoEstadoCheque("PENDIENTE");
        f.setFechaCobrarCheque(LocalDate.of(2026, 10, 5));
        f.setChequeCerrado(false);
        f.setDatoEstado("Valido");
        f.setMoneda("BS");
        f.setDescMoneda("Bs");

        JsonNode n = json.readTree(json.writeValueAsString(f));

        assertEquals(new TreeSet<>(Arrays.asList("fila", "codvd", "codCheque", "codBanco", "fechaBanco", "observacion", "estado",
                "audUsuario", "audFecha", "datoBanco", "nroCheque", "montoCheque", "datoBancoCheque", "datoEstadoCheque",
                "fechaCobrarCheque", "chequeCerrado", "datoEstado", "moneda", "descMoneda")), campos(n));
        assertEquals("2026-10-03", n.get("fechaBanco").asText());
        assertTrue(n.get("fechaBanco").isTextual(), "una fecha es texto, nunca el arreglo [2026,10,3]");
        assertEquals("2026-10-05", n.get("fechaCobrarCheque").asText());
        assertTrue(n.get("nroCheque").isTextual(), "el numero de cheque es texto");
        assertEquals("12-345", n.get("nroCheque").asText());
        assertTrue(n.get("montoCheque").isNumber());
        assertEquals(1500.5, n.get("montoCheque").asDouble(), 0.0001);
        assertEquals(55, n.get("codvd").asInt());
        assertTrue(n.get("audUsuario").isNull(), "la rama A no trae auditoria");
        assertTrue(n.get("audFecha").isNull());
        assertFalse(n.get("chequeCerrado").asBoolean());
        assertEquals("Bs", n.get("descMoneda").asText());
    }

    @Test
    @DisplayName("la fila de pendientes trae exactamente los campos del contrato")
    void jsonDeLaFilaPendiente() throws Exception {
        ChequePendienteFilaDto f = new ChequePendienteFilaDto();
        f.setFila(3);
        f.setCodCheque(321L);
        f.setCodBanco(2L);
        f.setNroCheque("5551");
        f.setMontoCheque(99.0);
        f.setDatoBancoCheque("Union BU");
        f.setDatoEstadoCheque("CERRADO");
        f.setFechaCobrarCheque(LocalDate.of(2026, 10, 3));
        f.setChequeCerrado(true);
        f.setMoneda("SUS");
        f.setDescMoneda("$us");

        JsonNode n = json.readTree(json.writeValueAsString(f));

        assertEquals(new TreeSet<>(Arrays.asList("fila", "codCheque", "codBanco", "nroCheque", "montoCheque", "datoBancoCheque",
                "datoEstadoCheque", "fechaCobrarCheque", "chequeCerrado", "moneda", "descMoneda")), campos(n));
        assertEquals("2026-10-03", n.get("fechaCobrarCheque").asText());
        assertTrue(n.get("chequeCerrado").asBoolean());
        assertEquals("$us", n.get("descMoneda").asText());
    }

    @Test
    @DisplayName("la pagina y la respuesta de preparar: {total, pagina, tamanio, filas} y {cheque, fechaBanco}")
    void jsonDePaginaYPreparar() throws Exception {
        JsonNode p = json.readTree(json.writeValueAsString(new VerificacionPaginaDto<>(45, 3, 20, new ArrayList<>())));
        assertEquals(new TreeSet<>(Arrays.asList("total", "pagina", "tamanio", "filas")), campos(p));
        assertTrue(p.get("filas").isArray());
        assertEquals(45, p.get("total").asInt());

        JsonNode r = json.readTree(json.writeValueAsString(
                new PrepararVerificacionDto(new ChequePendienteFilaDto(), LocalDate.of(2026, 10, 3))));
        assertEquals(new TreeSet<>(Arrays.asList("cheque", "fechaBanco")), campos(r));
        assertEquals("2026-10-03", r.get("fechaBanco").asText());
    }

    @Test
    @DisplayName("los cuerpos se leen con fechas yyyy-MM-dd; el de registrar NO tiene estado ni usuario (el cliente no los manda)")
    void jsonDeLosCuerpos() throws Exception {
        VerificacionFiltroDto f = json.readValue("{\"fechaBanco\":\"2026-10-03\",\"pagina\":2,\"tamanio\":50}", VerificacionFiltroDto.class);
        assertEquals(LocalDate.of(2026, 10, 3), f.getFechaBanco());
        assertEquals(2, f.getPagina().intValue());
        assertEquals(50, f.getTamanio().intValue());

        VerificacionFiltroDto sinFecha = json.readValue("{}", VerificacionFiltroDto.class);
        assertEquals(null, sinFecha.getFechaBanco(), "ausente = todas");

        ChequePendienteFiltroDto pf = json.readValue("{\"estado\":\"PEN\",\"soloCobranzaHoy\":false,\"pagina\":1}", ChequePendienteFiltroDto.class);
        assertEquals("PEN", pf.getEstado());
        assertEquals(Boolean.FALSE, pf.getSoloCobranzaHoy());

        VerificacionRegistroDto r = json.readValue("{\"codvd\":0,\"codCheque\":321,\"codBanco\":2,"
                + "\"fechaBanco\":\"2026-10-03\",\"observacion\":\"Deposito OK\",\"estado\":\"N\",\"audUsuario\":99}", VerificacionRegistroDto.class);
        assertTrue(r.esAlta());
        assertEquals(321L, r.getCodCheque().longValue());
        assertEquals(LocalDate.of(2026, 10, 3), r.getFechaBanco());

        Set<String> declarados = new TreeSet<>();
        for (Field c : VerificacionRegistroDto.class.getDeclaredFields()) declarados.add(c.getName());
        assertEquals(new TreeSet<>(Arrays.asList("codvd", "codCheque", "codBanco", "fechaBanco", "observacion")), declarados,
                "el cuerpo no puede traer estado ni audUsuario");
    }
}
