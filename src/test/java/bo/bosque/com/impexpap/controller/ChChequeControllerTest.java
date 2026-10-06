package bo.bosque.com.impexpap.controller;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.ChequePdfService;
import bo.bosque.com.impexpap.commons.ChequeReporteService;
import bo.bosque.com.impexpap.commons.ChequeService;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IPersonalCheque;
import bo.bosque.com.impexpap.dao.ISocionegocio;
import bo.bosque.com.impexpap.dto.BancoDto;
import bo.bosque.com.impexpap.dto.CatalogosChequeDto;
import bo.bosque.com.impexpap.dto.ChequeFiltroDto;
import bo.bosque.com.impexpap.dto.ChequePaginaDto;
import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.FiltroSucursalFechaDto;
import bo.bosque.com.impexpap.dto.HoraTraspasoChequeDto;
import bo.bosque.com.impexpap.dto.OpcionChequeDto;
import bo.bosque.com.impexpap.dto.ReporteChequeRequest;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.dto.TalonarioValidacionDto;
import bo.bosque.com.impexpap.dto.TalonarioValidacionRequest;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.model.SocioNegocio;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Los controladores de cheques y de bancos: forma de la respuesta, codigos HTTP y, sobre todo, que las
 * RUTAS sean las del contrato {@code API_CHEQUES.md}: el frontend las tiene escritas como constantes y un
 * cambio aqui rompe la pantalla sin que nada falle en compilacion.
 */
class ChChequeControllerTest {

    private ChequeService servicio;
    private ChequeReporteService reportes;
    private ChequePdfService pdfs;
    private ISocionegocio clientes;
    private AccesoModuloHelper acceso;
    private ChChequeController controlador;
    private Authentication auth;

    @BeforeEach
    void preparar() {
        servicio = mock(ChequeService.class);
        reportes = mock(ChequeReporteService.class);
        pdfs = mock(ChequePdfService.class);
        clientes = mock(ISocionegocio.class);
        acceso = mock(AccesoModuloHelper.class);
        controlador = new ChChequeController(servicio, reportes, pdfs, clientes);

        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        auth = t;
    }

    // ------------------------------------------------------------------ rutas = contrato

    /** Los endpoints de {@code API_CHEQUES.md}, tal cual. Si se agrega uno, se agrega aqui y al documento. */
    private static final Set<String> RUTAS_CHEQUE = new TreeSet<>(Arrays.asList(
            "/empresas", "/sucursal-inicial", "/sucursales", "/catalogos", "/clientes",
            "/personal/entregan", "/personal/custodia",
            "/listar", "/detalle", "/registrar",
            "/accion/fecha-cobro", "/accion/devolver", "/accion/cerrar", "/accion/eliminar",
            "/traspaso/pendientes", "/traspaso",
            "/custodia/cheques", "/custodia",
            "/dar-custodia/cheques", "/dar-custodia/entregas", "/dar-custodia",
            "/traspaso/horas", "/talonario/validar",
            "/reporte/recibidos", "/reporte/cobranzas", "/reporte/custodio", "/reporte/ultimo-recibo",
            "/reporte/traspaso", "/reporte/reimpresion-traspaso",
            "/pdf/estado", "/pdf/subir", "/pdf/descargar"));

    private static final Set<String> RUTAS_BANCO = new TreeSet<>(Arrays.asList(
            "/bancosX", "/bancosPlanilla", "/registrar", "/eliminar"));

    private static Set<String> rutasDe(Class<?> clase) {
        Set<String> rutas = new TreeSet<>();
        for (Method m : clase.getDeclaredMethods()) {
            PostMapping pm = m.getAnnotation(PostMapping.class);
            if (pm != null) rutas.addAll(Arrays.asList(pm.value()));
        }
        return rutas;
    }

    @Test
    @DisplayName("las rutas de /cheque son exactamente las del contrato, todas POST")
    void rutasDeCheque() {
        assertEquals("/cheque", ChChequeController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals(RUTAS_CHEQUE, rutasDe(ChChequeController.class));
        for (Method m : ChChequeController.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                assertNotNull(m.getAnnotation(PostMapping.class), "el handler " + m.getName() + " no es @PostMapping");
            }
        }
    }

    @Test
    @DisplayName("las rutas de /banco son las del contrato; las dos de lectura siguen siendo las de siempre")
    void rutasDeBanco() {
        assertEquals("/banco", ChBancoController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals(RUTAS_BANCO, rutasDe(ChBancoController.class));
    }

    @Test
    @DisplayName("/cheque exige ROLE_ADM o ROLE_LIM a nivel de clase")
    void seguridadDeCheque() {
        PreAuthorize pa = ChChequeController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(pa);
        assertTrue(pa.value().contains("ROLE_ADM") && pa.value().contains("ROLE_LIM"));
    }

    @Test
    @DisplayName("pdf/subir: el multipart trae la parte de texto 'codCheque' y el archivo en 'archivo' (el contrato de API_CHEQUES.md); las dos opcionales para dar un mensaje propio")
    void pdfSubirPartes() throws Exception {
        Method m = ChChequeController.class.getMethod("pdfSubir", String.class, MultipartFile.class, Authentication.class);
        RequestParam codCheque = (RequestParam) m.getParameterAnnotations()[0][0];
        RequestParam archivo = (RequestParam) m.getParameterAnnotations()[1][0];

        assertEquals("codCheque", codCheque.value());
        assertEquals("archivo", archivo.value());
        assertFalse(codCheque.required(), "una parte ausente la explica el servicio, no el error generico de Spring");
        assertFalse(archivo.required());
    }

    @Test
    @DisplayName("pdf/estado y pdf/descargar: pasan el codCheque del cuerpo al servicio (y el usuario del token); un cuerpo nulo no revienta")
    void pdfJson() {
        bo.bosque.com.impexpap.dto.ChequePdfRequest r = new bo.bosque.com.impexpap.dto.ChequePdfRequest();
        r.setCodCheque(18129);
        when(pdfs.estado(any(), any())).thenReturn(new bo.bosque.com.impexpap.dto.ChequePdfEstadoDto(false, "18129.pdf", null, null));

        controlador.pdfEstado(r, auth);
        verify(pdfs).estado(auth, 18129);

        controlador.pdfEstado(null, auth);
        verify(pdfs).estado(auth, null);
    }

    @Test
    @DisplayName("todos los endpoints de /banco exigen ROLE_ADM o ROLE_LIM")
    void seguridadDeBanco() {
        for (Method m : ChBancoController.class.getDeclaredMethods()) {
            if (m.getAnnotation(PostMapping.class) == null) continue;
            Secured s = m.getAnnotation(Secured.class);
            assertNotNull(s, m.getName() + " sin @Secured");
            assertEquals(new TreeSet<>(Arrays.asList("ROLE_ADM", "ROLE_LIM")), new TreeSet<>(Arrays.asList(s.value())));
        }
    }

    @Test
    @DisplayName("los listados de bancos devuelven una lista pelada, no un ApiResponse (contrato previo)")
    void bancosListaPelada() throws Exception {
        assertEquals(List.class, ChBancoController.class.getMethod("listadoX").getReturnType());
        assertEquals(List.class, ChBancoController.class.getMethod("listadoBancosPlanilla").getReturnType());
    }

    // ------------------------------------------------------------------ respuestas

    @SuppressWarnings("unchecked")
    private static <T> T datos(ResponseEntity<ApiResponse<?>> r) {
        return (T) r.getBody().getData();
    }

    @Test
    @DisplayName("catalogos: tipo PAG/RES, moneda BS/SUS, estados PEN/CER y los estados que permite cada boton")
    void catalogos() {
        ResponseEntity<ApiResponse<?>> r = controlador.catalogos();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        CatalogosChequeDto c = datos(r);

        assertEquals(Arrays.asList("PAG", "RES"), codigos(c.getTiposCheque()));
        assertEquals(Arrays.asList("BS", "SUS"), codigos(c.getMonedas()));
        assertEquals(Arrays.asList("PEN", "CER"), codigos(c.getEstadosCheque()));
        assertEquals(13, c.getEstadosAccion().size(), "los 12 de la vista mas VER");
        assertEquals(Arrays.asList("VEN", "ADE"), codigos(c.getAccionesFechaCobro()));
        assertEquals(Collections.singletonList("COB"), codigos(c.getAccionesCierreConVerificacion()));
        assertEquals(Arrays.asList("CEF", "CCH", "PAP", "DPR"), codigos(c.getAccionesCierreSinVerificacion()));
    }

    private static List<String> codigos(List<OpcionChequeDto> l) {
        List<String> r = new ArrayList<>();
        for (OpcionChequeDto o : l) r.add(o.getCodigo());
        return r;
    }

    @Test
    @DisplayName("listar devuelve 200 con la pagina del servicio")
    void listar() {
        ChequePaginaDto pagina = new ChequePaginaDto(45, 3, 20, new ArrayList<>());
        when(servicio.listar(any(), any())).thenReturn(pagina);
        ResponseEntity<ApiResponse<?>> r = controlador.listar(new ChequeFiltroDto(), auth);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(pagina, r.getBody().getData());
    }

    @Test
    @DisplayName("una lista vacia es 204 con data null, no un error")
    void listaVaciaEs204() {
        when(servicio.quienesEntregan(any(), anyLong())).thenReturn(new ArrayList<>());
        ResponseEntity<ApiResponse<?>> r = controlador.quienesEntregan(new FiltroIdDto(3), auth);
        assertEquals(HttpStatus.NO_CONTENT, r.getStatusCode());
        assertNull(r.getBody().getData());
    }

    @Test
    @DisplayName("empresas: lista del servicio (200); sin ninguna, 204")
    void empresas() {
        List<EmpresaChequeDto> dos = Arrays.asList(new EmpresaChequeDto(1, "IMPEXPAP"), new EmpresaChequeDto(5, "ESPPAPEL"));
        when(servicio.empresas()).thenReturn(dos);
        ResponseEntity<ApiResponse<?>> r = controlador.listarEmpresas();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(dos, r.getBody().getData());

        when(servicio.empresas()).thenReturn(new ArrayList<>());
        assertEquals(HttpStatus.NO_CONTENT, controlador.listarEmpresas().getStatusCode());
    }

    @Test
    @DisplayName("sucursales, sucursal inicial y clientes piden la empresa que mande el cliente; sin ella, ninguna (el servicio usa la primera del combo, no la del token)")
    void empresaPedida() {
        when(servicio.sucursales(any(), any())).thenReturn(Collections.singletonList(new SucursalChequeDto(13, "Central")));
        controlador.listarSucursales(new FiltroIdDto(5), auth);
        verify(servicio).sucursales(auth, 5);
        controlador.listarSucursales(null, auth);
        verify(servicio).sucursales(auth, null);
        controlador.listarSucursales(new FiltroIdDto(0), auth);
        verify(servicio, org.mockito.Mockito.times(2)).sucursales(auth, null);   // 0 = "no pidio ninguna"

        controlador.sucursalInicial(new FiltroIdDto(5), auth);
        verify(servicio).sucursalInicial(auth, 5);
        controlador.sucursalInicial(null, auth);
        verify(servicio).sucursalInicial(auth, null);

        when(servicio.empresaEfectiva(any())).thenReturn(5);
        when(clientes.obtenerSocioNegocio(5)).thenReturn(Collections.singletonList(new SocioNegocio()));
        assertEquals(HttpStatus.OK, controlador.listarClientes(new FiltroIdDto(5)).getStatusCode());
        verify(servicio).empresaEfectiva(5);
        verify(clientes).obtenerSocioNegocio(5);
    }

    @Test
    @DisplayName("los reportes responden con el PDF tal cual: 200, application/pdf y el largo")
    void reportesDevuelvenPdf() {
        byte[] pdf = {'%', 'P', 'D', 'F', 1, 2, 3};
        ReporteChequeRequest r = new ReporteChequeRequest();
        when(reportes.recibidos(any(), any())).thenReturn(pdf);
        when(reportes.cobranzas(any(), any())).thenReturn(pdf);
        when(reportes.custodio(any(), any())).thenReturn(pdf);
        when(reportes.reciboDelUltimoCheque(any(), any())).thenReturn(pdf);
        when(reportes.nominaDelTraspaso(any(), any())).thenReturn(pdf);
        when(reportes.reimpresionDeTraspaso(any(), any())).thenReturn(pdf);

        List<ResponseEntity<byte[]>> respuestas = Arrays.asList(
                controlador.reporteRecibidos(r, auth), controlador.reporteCobranzas(r, auth),
                controlador.reporteCustodio(r, auth), controlador.reporteUltimoRecibo(r, auth),
                controlador.reporteTraspaso(r, auth), controlador.reporteReimpresionTraspaso(r, auth));
        for (ResponseEntity<byte[]> x : respuestas) {
            assertEquals(HttpStatus.OK, x.getStatusCode());
            assertEquals(org.springframework.http.MediaType.APPLICATION_PDF, x.getHeaders().getContentType());
            assertEquals(pdf.length, x.getHeaders().getContentLength());
            assertEquals(pdf, x.getBody());
        }
        verify(reportes).recibidos(auth, r);
        verify(reportes).reimpresionDeTraspaso(auth, r);
    }

    @Test
    @DisplayName("un error de permiso o de negocio de un reporte sube tal cual (el GlobalExceptionHandler lo convierte en 403 / 400 en JSON)")
    void reporteConErrorSube() {
        when(reportes.recibidos(any(), any())).thenThrow(new AccessDeniedException("sin boton"));
        assertThrows(AccessDeniedException.class, () -> controlador.reporteRecibidos(new ReporteChequeRequest(), auth));
        when(reportes.reciboDelUltimoCheque(any(), any()))
                .thenThrow(new SpBusinessException("No hay ningun cheque registrado por usted en esta sucursal."));
        assertThrows(SpBusinessException.class, () -> controlador.reporteUltimoRecibo(new ReporteChequeRequest(), auth));
    }

    @Test
    @DisplayName("talonario/validar: 200 con {valido, mensaje, detalle} del servicio, tanto si es valido como si no (no es un error HTTP)")
    void validarTalonario() {
        TalonarioValidacionRequest r = new TalonarioValidacionRequest();
        TalonarioValidacionDto invalido = new TalonarioValidacionDto(false, "El talonario «ER1076» no pertenece a la empresa ESPPAPEL...", null);
        when(servicio.validarTalonario(any(), any())).thenReturn(invalido);

        ResponseEntity<ApiResponse<?>> res = controlador.validarTalonario(r, auth);

        assertEquals(HttpStatus.OK, res.getStatusCode(), "un par invalido es una respuesta normal, no un 400");
        assertEquals(invalido, res.getBody().getData());
        verify(servicio).validarTalonario(auth, r);

        TalonarioValidacionDto bueno = new TalonarioValidacionDto(true, null, "Talonario ER1076 (IMPEXPAP): recibos del 3751 al 3800.");
        when(servicio.validarTalonario(any(), any())).thenReturn(bueno);
        assertEquals(bueno, controlador.validarTalonario(r, auth).getBody().getData());
    }

    @Test
    @DisplayName("horas de traspaso: lista del servicio (200); sin traspasos, 204")
    void horasDeTraspaso() {
        FiltroSucursalFechaDto f = new FiltroSucursalFechaDto();
        f.setCodSucursal(1L);
        List<HoraTraspasoChequeDto> una = Collections.singletonList(new HoraTraspasoChequeDto(900, "11:00"));
        when(reportes.horasDeTraspaso(any(), any(), any())).thenReturn(una);
        ResponseEntity<ApiResponse<?>> r = controlador.horasDeTraspaso(f, auth);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(una, r.getBody().getData());

        when(reportes.horasDeTraspaso(any(), any(), any())).thenReturn(new ArrayList<>());
        assertEquals(HttpStatus.NO_CONTENT, controlador.horasDeTraspaso(f, auth).getStatusCode());
    }

    @Test
    @DisplayName("las escrituras devuelven 201 con el id o la cantidad en data")
    void escritura201() {
        when(servicio.traspasar(any(), anyLong())).thenReturn(12L);
        ResponseEntity<ApiResponse<?>> r = controlador.traspasar(new FiltroIdDto(3), auth);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(12L, (long) datos(r));

        when(servicio.darCustodia(any(), any())).thenReturn(901L);
        assertEquals(901L, (long) datos(controlador.darCustodia(null, auth)));
    }

    @Test
    @DisplayName("un error de negocio del servicio sube tal cual (el GlobalExceptionHandler lo convierte en 400)")
    void errorDeNegocioSube() {
        when(servicio.traspasar(any(), anyLong())).thenThrow(new SpBusinessException("No hay cheques pendientes de traspaso."));
        SpBusinessException e = assertThrows(SpBusinessException.class,
                () -> controlador.traspasar(new FiltroIdDto(3), auth));
        assertEquals("No hay cheques pendientes de traspaso.", e.getMessage());
    }

    @Test
    @DisplayName("Dar Custodia: las listas de apoyo pasan por el servicio (boton y sucursal) y el 403 sube")
    void darCustodiaListasPasanPorElServicio() {
        when(servicio.chequesParaDarCustodia(any(), anyLong())).thenThrow(new AccessDeniedException("sin boton"));
        assertThrows(AccessDeniedException.class, () -> controlador.chequesParaDarCustodia(new FiltroIdDto(3), auth));

        when(servicio.entregasDelDia(any(), any(), any())).thenThrow(new SpBusinessException("Debe indicar la sucursal y la fecha."));
        assertThrows(SpBusinessException.class, () -> controlador.entregasDelDia(null, auth));
    }

    // ------------------------------------------------------------------ bancos

    private IChBanco bancos;
    private ChBancoController controladorBanco() {
        bancos = mock(IChBanco.class);
        return new ChBancoController(bancos, acceso);
    }

    @Test
    @DisplayName("banco: alta con btnNuevoB, usuario del token y 201 con el id")
    void bancoAlta() {
        ChBancoController c = controladorBanco();
        when(bancos.registrar(any())).thenReturn(new RespuestaSp(0, "", 55L));
        ChBanco b = new ChBanco();
        b.setNombre("Banco Union SA");   // el validador del legacy no admite el punto: "S.A." se rechaza
        b.setAudUsuario(999);            // el cuerpo no manda: se pisa con el del token

        ResponseEntity<ApiResponse<?>> r = c.registrar(b, auth);

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(55L, (long) datos(r));
        assertEquals(7, b.getAudUsuario().intValue());
        verify(acceso).exigirBoton(auth, 43, "btnNuevoB");
    }

    @Test
    @DisplayName("banco: la edicion usa btnEditarB y exige que exista")
    void bancoEdicion() {
        ChBancoController c = controladorBanco();
        ChBanco b = new ChBanco();
        b.setCodBanco(9);
        b.setNombre("Banco Mercantil");
        when(bancos.obtener(9)).thenReturn(null);

        assertThrows(SpBusinessException.class, () -> c.registrar(b, auth));
        verify(acceso).exigirBoton(auth, 43, "btnEditarB");
        verify(bancos, never()).registrar(any());
    }

    @Test
    @DisplayName("banco: nombre vacio o con formato invalido se rechaza antes de escribir")
    void bancoNombre() {
        ChBancoController c = controladorBanco();
        ChBanco vacio = new ChBanco();
        vacio.setNombre("");
        assertThrows(SpBusinessException.class, () -> c.registrar(vacio, auth));

        ChBanco corto = new ChBanco();
        corto.setNombre("AB");
        assertThrows(SpBusinessException.class, () -> c.registrar(corto, auth));

        ChBanco raro = new ChBanco();
        raro.setNombre("Banco @ Nacional");
        assertThrows(SpBusinessException.class, () -> c.registrar(raro, auth));
        verify(bancos, never()).registrar(any());
    }

    @Test
    @DisplayName("banco: sin boton, 403 y no escribe")
    void bancoSinBoton() {
        ChBancoController c = controladorBanco();
        doThrow(new AccessDeniedException("sin boton")).when(acceso).exigirBoton(any(), anyInt(), anyString());
        ChBanco b = new ChBanco();
        b.setNombre("Banco Union SA");
        assertThrows(AccessDeniedException.class, () -> c.registrar(b, auth));
        assertThrows(AccessDeniedException.class, () -> c.eliminar(new FiltroIdDto(3), auth));
        verify(bancos, never()).registrar(any());
        verify(bancos, never()).eliminar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("banco: eliminar usa btnEliminarB, valida que exista y manda el usuario del token")
    void bancoEliminar() {
        ChBancoController c = controladorBanco();
        when(bancos.obtener(3)).thenReturn(new ChBanco());
        when(bancos.eliminar(3, 7)).thenReturn(new RespuestaSp(0, "", 3L));

        ResponseEntity<ApiResponse<?>> r = c.eliminar(new FiltroIdDto(3), auth);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        verify(acceso).exigirBoton(auth, 43, "btnEliminarB");
        verify(bancos).eliminar(3, 7);

        assertThrows(SpBusinessException.class, () -> c.eliminar(new FiltroIdDto(0), auth));
        when(bancos.obtener(4)).thenReturn(null);
        assertThrows(SpBusinessException.class, () -> c.eliminar(new FiltroIdDto(4), auth));
    }

    @Test
    @DisplayName("banco: los listados conservan la forma {codBanco, nombre, audUsuario, fila}")
    void bancoListados() {
        ChBancoController c = controladorBanco();
        when(bancos.listBancos()).thenReturn(Collections.singletonList(new BancoDto(2, "Banco Union", 0, 1)));
        BancoDto b = c.listadoX().get(0);
        assertEquals(2, b.getCodBanco());
        assertEquals("Banco Union", b.getNombre());
        assertEquals(1, b.getFila());
    }
}
