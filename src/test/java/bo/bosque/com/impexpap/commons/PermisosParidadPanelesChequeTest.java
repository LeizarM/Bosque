package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import bo.bosque.com.impexpap.commons.PermisosParidadChequeTest.Perfil;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.controller.ChChequeSatelitesController;
import bo.bosque.com.impexpap.controller.ChClientesSapController;
import bo.bosque.com.impexpap.controller.ChVerificacionController;
import bo.bosque.com.impexpap.dao.IChAccion;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IChNotaRemision;
import bo.bosque.com.impexpap.dao.IChPostergacion;
import bo.bosque.com.impexpap.dao.IChTransaccionBancaria;
import bo.bosque.com.impexpap.dao.IChVerificacionDeposito;
import bo.bosque.com.impexpap.dao.IPersonalCheque;
import bo.bosque.com.impexpap.dao.IProgramador;
import bo.bosque.com.impexpap.dao.ISocioNegocioSap;
import bo.bosque.com.impexpap.dao.ISucursalCheque;
import bo.bosque.com.impexpap.dao.ITalonario;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dto.AccionChequeRequest;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequeReferenciaRequest;
import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.dto.NotaRemisionDto;
import bo.bosque.com.impexpap.dto.NotaRemisionRequest;
import bo.bosque.com.impexpap.dto.PostergacionDto;
import bo.bosque.com.impexpap.dto.PostergacionPdfRequest;
import bo.bosque.com.impexpap.dto.PostergacionRequest;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.dto.TransaccionBancariaDto;
import bo.bosque.com.impexpap.dto.TransaccionBancariaRequest;
import bo.bosque.com.impexpap.dto.VerificacionRegistroDto;
import bo.bosque.com.impexpap.model.ChAccion;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.model.ChPostergacion;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Fase 2 de la auditoria de permisos del modulo Cheques (ver {@code PERMISOS_CHEQUES.md}, secciones 11 a 14):
 * los <b>paneles del detalle</b> (notas de remision, transacciones bancarias, postergaciones y el PDF de la postergacion),
 * <b>"Actualizar datos SAP"</b> y <b>Verificar Cheques</b> (vista 77), para los 12 perfiles reales, "sin botones" y el
 * administrador.
 *
 * <p>Misma arquitectura que {@link PermisosParidadChequeTest}: el {@link AccesoModuloHelper} REAL alimentado por dobles de los
 * dos procedimientos del gate, los servicios y los controladores reales, y solo los DAO simulados. Un caso "pasa" cuando NO
 * sale un {@link AccessDeniedException}.
 *
 * <p>La matriz cruza perfil x estado del cheque (PEN, CER) x sucursal (propia, ajena). La sucursal inicial de todos los
 * perfiles es la {@value #SUC}; la ajena es la {@value #SUC_AJENA}.
 */
class PermisosParidadPanelesChequeTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Clock RELOJ = Clock.fixed(
            LocalDate.of(2026, 10, 3).atTime(10, 0).atZone(ZONA).toInstant(), ZONA);

    private static final int VISTA_CHEQUES = 42;
    private static final int SUC = 3;
    private static final int SUC_AJENA = 9;

    /** Los cuatro cheques de la prueba: abierto y cerrado, de la sucursal propia y de una ajena. */
    private static final int PEN_PROPIO = 1;
    private static final int CER_PROPIO = 2;
    private static final int PEN_AJENO = 3;
    private static final int CER_AJENO = 4;

    private static final String NOTA = "262211881";

    // ---------------------------------------------------------------- dobles del gate (como en la fase 1)

    private final Map<String, Integer> loginACodigo = new HashMap<>();
    private final Map<Integer, List<String[]>> acl = new HashMap<>();   // codUsuario -> (boton, vista)
    private int consultasAlGate;

    private IChCheque cheques;
    private IChAccion acciones;
    private ISucursalCheque sucursales;
    private IChNotaRemision notasDao;
    private IChTransaccionBancaria transaccionesDao;
    private IChPostergacion postergacionesDao;
    private IChBanco bancosDao;
    private IChVerificacionDeposito verificacionesDao;
    private ISocioNegocioSap sapDao;
    private IProgramador programador;
    private IUsuarioBtn usuarioBtn;
    private AccesoModuloHelper acceso;

    private ChequeService servicio;
    private ChequePdfService pdfCheque;
    private ChChequeSatelitesController paneles;
    private ChClientesSapController sap;
    private ChVerificacionController verificacion;

    private void registrarUsuario(String login, int codUsuario, Iterable<String> botones42) {
        loginACodigo.put(login, codUsuario);
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] {"btnDetalles", "24"});   // ruido de otra vista
        for (String b : botones42) filas.add(new String[] {b, String.valueOf(VISTA_CHEQUES)});
        acl.put(codUsuario, filas);
    }

    @BeforeEach
    void preparar() throws Exception {
        cheques = mock(IChCheque.class);
        acciones = mock(IChAccion.class);
        sucursales = mock(ISucursalCheque.class);
        notasDao = mock(IChNotaRemision.class);
        transaccionesDao = mock(IChTransaccionBancaria.class);
        postergacionesDao = mock(IChPostergacion.class);
        bancosDao = mock(IChBanco.class);
        verificacionesDao = mock(IChVerificacionDeposito.class);
        sapDao = mock(ISocioNegocioSap.class);
        programador = mock(IProgramador.class);
        usuarioBtn = mock(IUsuarioBtn.class);

        for (Perfil p : Perfil.values()) {
            List<String> todos = new ArrayList<>(p.v42);
            registrarUsuario(p.login(), p.codUsuario(), todos);
            for (String b : p.v43) acl.get(p.codUsuario()).add(new String[] {b, "43"});
        }
        // p_list_trs_Programador 'E': codUsuario por login; 0 si no existe.
        when(programador.resolverPermiso(anyString())).thenAnswer(inv -> {
            consultasAlGate++;
            MiEquipoDto d = new MiEquipoDto();
            Integer cod = loginACodigo.get((String) inv.getArgument(0));
            d.setCodUsuario(cod == null ? 0L : cod.longValue());
            return d;
        });
        // p_list_UsuarioBtn 'A': solo filas con permiso != 0 (aqui todas las que se registran valen '1').
        when(usuarioBtn.botonesXUsuario(anyInt())).thenAnswer(inv -> {
            consultasAlGate++;
            List<UsuarioBtn> r = new ArrayList<>();
            for (String[] f : acl.getOrDefault((Integer) inv.getArgument(0), Collections.<String[]>emptyList())) {
                UsuarioBtn b = new UsuarioBtn();
                b.setBoton(f[0]);
                b.setPermiso(1);
                b.setPertenVist(Integer.parseInt(f[1]));
                r.add(b);
            }
            return r;
        });
        acceso = new AccesoModuloHelper(programador, usuarioBtn);

        // Sucursal: la inicial de todos es la SUC (asi la unica razon de una denegacion es la que se prueba).
        when(sucursales.empresasDeCheques()).thenReturn(Arrays.asList(
                new EmpresaChequeDto(1, "IMPEXPAP"), new EmpresaChequeDto(5, "ESPPAPEL")));
        when(sucursales.sucursalInicial(anyInt(), anyInt(), anyLong())).thenReturn((long) SUC);
        when(sucursales.sucursalesDeEmpresa(anyInt(), anyInt())).thenReturn(Arrays.asList(
                new SucursalChequeDto(SUC, "Sucursal 6 Cochabamba"), new SucursalChequeDto(SUC_AJENA, "Sucursal 12 Santa Cruz")));
        when(sucursales.puedeEscribir(anyLong(), anyInt())).thenReturn(true);   // p_list_Sucursal 'D' autoriza a cualquiera

        when(cheques.obtener(PEN_PROPIO)).thenReturn(cheque(PEN_PROPIO, "PEN", SUC));
        when(cheques.obtener(CER_PROPIO)).thenReturn(cheque(CER_PROPIO, "CER", SUC));
        when(cheques.obtener(PEN_AJENO)).thenReturn(cheque(PEN_AJENO, "PEN", SUC_AJENA));
        when(cheques.obtener(CER_AJENO)).thenReturn(cheque(CER_AJENO, "CER", SUC_AJENA));
        when(cheques.codigoBotones(anyInt())).thenReturn("1111");
        when(cheques.cambiarEstado(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));
        when(cheques.cambiarFechaCobro(anyInt(), any(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));
        ChAccion accion = new ChAccion();
        accion.setCodAccion(77);
        accion.setCodCheque(PEN_PROPIO);
        accion.setEstado("CUS");
        when(acciones.obtener(anyInt())).thenReturn(accion);
        when(acciones.contarPorCheque(anyInt())).thenReturn(3);
        when(acciones.traspasosDelCheque(anyInt())).thenReturn(1);
        when(acciones.devolucionesIgualAEntregas(anyInt())).thenReturn(true);
        when(acciones.registrar(any())).thenReturn(new RespuestaSp(0, "", 900L));
        when(acciones.eliminar(anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));

        // Paneles: una nota, una transaccion y una postergacion por cheque; la postergacion 100+n es del cheque n.
        when(notasDao.listarPorCheque(anyInt())).thenAnswer(inv -> {
            NotaRemisionDto n = new NotaRemisionDto();
            n.setCodCheque((Integer) inv.getArgument(0));
            n.setNotaRemision(NOTA);
            return Collections.singletonList(n);
        });
        when(notasDao.registrar(any())).thenReturn(new RespuestaSp(0, "", 1L));
        when(notasDao.eliminar(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 1L));
        when(transaccionesDao.listarPorCheque(anyInt())).thenAnswer(inv -> {
            TransaccionBancariaDto t = new TransaccionBancariaDto();
            t.setCodCheque((Integer) inv.getArgument(0));
            t.setNroTransaccion("TT26216QW3N3");
            return Collections.singletonList(t);
        });
        when(transaccionesDao.registrar(any())).thenReturn(new RespuestaSp(0, "", 1L));
        when(transaccionesDao.eliminar(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 1L));
        when(bancosDao.obtener(anyInt())).thenReturn(banco(7));
        when(postergacionesDao.listarPorCheque(anyInt())).thenAnswer(inv -> {
            PostergacionDto p = new PostergacionDto();
            p.setCodCheque((Integer) inv.getArgument(0));
            p.setCodPostergacion(100 + (Integer) inv.getArgument(0));
            return new ArrayList<>(Collections.singletonList(p));
        });
        when(postergacionesDao.obtener(anyInt())).thenAnswer(inv -> {
            ChPostergacion p = new ChPostergacion();
            p.setCodPostergacion((Integer) inv.getArgument(0));
            p.setCodCheque((Integer) inv.getArgument(0) - 100);
            return p;
        });
        when(postergacionesDao.registrar(any())).thenReturn(new RespuestaSp(0, "", 900L));
        when(postergacionesDao.eliminar(anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));

        servicio = new ChequeService(cheques, acciones, sucursales, mock(IPersonalCheque.class), mock(ITalonario.class),
                acceso, RELOJ);
        pdfCheque = new ChequePdfService(cheques, servicio, "");
        AccesoPanelesCheque accesoPaneles = new AccesoPanelesCheque(cheques, servicio, acceso);
        PostergacionPdfService postergacionPdf = new PostergacionPdfService(postergacionesDao, accesoPaneles, "");
        paneles = new ChChequeSatelitesController(
                new NotaRemisionChequeService(notasDao, accesoPaneles),
                new TransaccionChequeService(transaccionesDao, bancosDao, accesoPaneles),
                new PostergacionChequeService(postergacionesDao, postergacionPdf, accesoPaneles),
                postergacionPdf);
        sap = new ChClientesSapController(new ClientesSapService(sapDao, acceso));
        verificacion = new ChVerificacionController(new VerificacionChequeService(verificacionesDao, bancosDao, cheques));
        consultasAlGate = 0;
    }

    // ---------------------------------------------------------------- utilidades

    private static ChequeFilaDto cheque(int cod, String estado, int sucursal) {
        ChequeFilaDto c = new ChequeFilaDto();
        c.setCodCheque(cod);
        c.setNrocheque("5551");
        c.setCodCliente("C001");
        c.setaOrdenDe("IMPEXPAP SRL");
        c.setFechaCheque(d(2026, 9, 20));
        c.setFechaCobrar(d(2026, 9, 25));
        c.setMonto(1000.5);
        c.setMoneda("BS");
        c.setTipo("PAG");
        c.setEstado(estado);
        c.setCodBanco(2);
        c.setCodEmpleado(0);
        c.setReciboManual("0");
        c.setCodSucursal((long) sucursal);
        c.setNroRecibo(77L);
        c.setNroTalonario("0");
        c.setCodEmpresa(1);
        return c;
    }

    private static ChBanco banco(int cod) {
        ChBanco b = new ChBanco();
        b.setCodBanco(cod);
        b.setNombre("BANCO UNION");
        return b;
    }

    private static Date d(int anio, int mes, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(anio, mes, dia));
    }

    private Authentication auth(Perfil p) {
        return token(p.login(), p.codUsuario(), p.admin());
    }

    private Authentication token(String login, int codUsuario, boolean admin) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(login, null,
                Collections.singletonList(new SimpleGrantedAuthority(admin ? "ROLE_ADM" : "ROLE_LIM")));
        t.setDetails(new DatosToken(codUsuario, 3, 6, admin ? "adm" : "lim", "USUARIO " + login));
        return t;
    }

    /** Un usuario sintetico con SOLO esos botones de la vista 42 (y ninguno de los demas). */
    private Authentication soloCon(String... botones) {
        String login = "solo-" + String.join("-", botones);
        registrarUsuario(login, 5000 + Math.abs(login.hashCode() % 1000), Arrays.asList(botones));
        return token(login, loginACodigo.get(login), false);
    }

    private static final class Resultado {
        final boolean denegado;
        final Throwable causa;

        Resultado(boolean denegado, Throwable causa) {
            this.denegado = denegado;
            this.causa = causa;
        }
    }

    private static Resultado correr(Runnable r) {
        try {
            r.run();
            return new Resultado(false, null);
        } catch (AccessDeniedException e) {
            return new Resultado(true, e);
        } catch (RuntimeException e) {
            return new Resultado(false, e);   // paso el ACL; sigue un error de negocio o un dato de prueba
        }
    }

    private static String[] b(String... botones) {
        return botones;
    }

    // ---------------------------------------------------------------- los casos de los paneles

    /** Un endpoint de los paneles: el boton que el legacy exigia (vacio = ninguno) y la llamada sobre un cheque. */
    private static final class Caso {
        final String ruta;
        final String legacy;
        final String[] botones;
        final BiFunction<Authentication, Integer, Object> llamada;

        Caso(String ruta, String legacy, String[] botones, BiFunction<Authentication, Integer, Object> llamada) {
            this.ruta = ruta;
            this.legacy = legacy;
            this.botones = botones;
            this.llamada = llamada;
        }
    }

    private ChequeReferenciaRequest ref(int cheque) {
        ChequeReferenciaRequest r = new ChequeReferenciaRequest();
        r.setCodCheque(cheque);
        return r;
    }

    private NotaRemisionRequest nota(int cheque) {
        NotaRemisionRequest r = new NotaRemisionRequest();
        r.setCodCheque(cheque);
        r.setNotaRemision(NOTA);
        r.setNroFactura(1856);
        r.setFechaFactura(d(2026, 8, 31));
        return r;
    }

    private TransaccionBancariaRequest transaccion(int cheque) {
        TransaccionBancariaRequest r = new TransaccionBancariaRequest();
        r.setCodCheque(cheque);
        r.setNroTransaccion("TT26216QW3N3");
        r.setCodBanco(7);
        r.setFechaTransaccion(d(2026, 8, 4));
        return r;
    }

    private PostergacionRequest postergacion(int cheque) {
        PostergacionRequest r = new PostergacionRequest();
        r.setCodCheque(cheque);
        r.setCodPostergacion(100 + cheque);
        r.setFecha(d(2026, 10, 20));
        r.setObservacion("Cliente envio carta solicitando postergacion.");
        return r;
    }

    private PostergacionPdfRequest pdfPost(int cheque) {
        PostergacionPdfRequest r = new PostergacionPdfRequest();
        r.setCodPostergacion(100 + cheque);
        return r;
    }

    /**
     * El mapeo endpoint a boton PROPIO de los paneles, con el componente del legacy que lo origina
     * ({@code cheque.xhtml} y {@code WizardCheque}). Ademas, los doce piden {@code btnDetalleCH} (los paneles solo se alcanzan
     * con "Completar"): DIFERENCIA #4, corregida; ver {@link #debePasar}.
     */
    private List<Caso> casos() {
        List<Caso> c = new ArrayList<>();
        c.add(new Caso("/nota-remision/listar", "dtNotRem (L281): sin ACL", b(), (a, ch) -> paneles.listarNotas(ref(ch), a)));
        c.add(new Caso("/nota-remision/registrar", "Nuevo (L279) + saveNotaRemision", b("btnNuevoNRCH"),
                (a, ch) -> paneles.registrarNota(nota(ch), a)));
        c.add(new Caso("/nota-remision/eliminar", "Eliminar nota (L295): esAutorizadoB, el estado se ignora", b("btnEliminarNRCH"),
                (a, ch) -> paneles.eliminarNota(nota(ch), a)));
        c.add(new Caso("/transaccion/listar", "dtTransacc (L313): sin ACL", b(), (a, ch) -> paneles.listarTransacciones(ref(ch), a)));
        c.add(new Caso("/transaccion/registrar", "Nuevo (L311) + saveNroTransaccion", b("btnNuevoNRCH"),
                (a, ch) -> paneles.registrarTransaccion(transaccion(ch), a)));
        c.add(new Caso("/transaccion/eliminar", "Eliminar transaccion (L327): sin ACL", b(),
                (a, ch) -> paneles.eliminarTransaccion(transaccion(ch), a)));
        c.add(new Caso("/postergacion/listar", "dtPostegcns (L344): sin ACL", b(), (a, ch) -> paneles.listarPostergaciones(ref(ch), a)));
        c.add(new Caso("/postergacion/registrar", "Nuevo (L342) + savePostePostrgcn", b("btnNuevoNRCH"),
                (a, ch) -> paneles.registrarPostergacion(postergacion(ch), a)));
        c.add(new Caso("/postergacion/eliminar", "Eliminar postergacion (L356): sin ACL", b(),
                (a, ch) -> paneles.eliminarPostergacion(postergacion(ch), a)));
        c.add(new Caso("/postergacion/pdf/estado", "Descargar PDF (L369): sin ACL", b(),
                (a, ch) -> paneles.pdfEstado(pdfPost(ch), a)));
        c.add(new Caso("/postergacion/pdf/descargar", "Descargar PDF (L369): sin ACL", b(),
                (a, ch) -> paneles.pdfDescargar(pdfPost(ch), a)));
        c.add(new Caso("/postergacion/pdf/subir", "Cargar PDF (L365): sin ACL", b(),
                (a, ch) -> paneles.pdfSubir(String.valueOf(100 + ch),
                        new MockMultipartFile("archivo", "carta.pdf", "application/pdf", "%PDF-1.4 x".getBytes()), a)));
        return c;
    }

    private static boolean propio(int cheque) {
        return cheque == PEN_PROPIO || cheque == CER_PROPIO;
    }

    private static String estado(int cheque) {
        return cheque == PEN_PROPIO || cheque == PEN_AJENO ? "PEN" : "CER";
    }

    /**
     * Lo que da el legacy: el boton del componente (o ninguno), llegar al panel con "Completar" (btnDetalleCH; desde la correccion
     * de la diferencia #4 el servidor tambien lo exige) y que el cheque sea de una sucursal que el usuario ve.
     */
    private static boolean debePasar(Perfil p, Caso c, int cheque) {
        boolean boton = c.botones.length == 0 || p.puedeAlguno(c.botones);
        boolean detalle = p.puede("btnDetalleCH");
        boolean sucursal = propio(cheque) || p.puede("btnChqSucrs");   // el combo solo se habilita con btnChqSucrs
        return boton && detalle && sucursal;
    }

    /** El orden de las comprobaciones del servidor: boton propio, btnDetalleCH, cheque y sucursal. */
    private static final String SUCURSAL = "<sucursal>";

    private static String motivoEsperado(Perfil p, Caso c) {
        if (c.botones.length > 0 && !p.puedeAlguno(c.botones)) return c.botones[0];
        if (!p.puede("btnDetalleCH")) return "btnDetalleCH";
        return SUCURSAL;
    }

    /** Los 20 botones de la vista 42 (tb_vistaBtn), menos btnDetalleCH. */
    private static final String[] TODOS_MENOS_DETALLE = {
            "btnNuevoCH", "btnNuevo2CH", "btnNuevoNRCH", "btnNuevoSegCH", "btnEditar1CH", "btnEditar2CH", "btnEditar3CH",
            "btnEditarSegCH", "btnEliminarNRCH", "btnEliminarSegCH", "btnCustodiaCH", "btnCustodia2CH", "btnTraspasoCH",
            "btnChqSucrs", "btnRpt1CH", "btnRpt2CH", "btnRpt3CH", "btnRpt4CH", "btnRpt5CH"};

    // ===================================================================== //
    //          1. LOS PANELES: perfil x estado x sucursal x endpoint        //
    // ===================================================================== //

    @ParameterizedTest(name = "{0}: los paneles exigen lo que exigia el legacy")
    @EnumSource(Perfil.class)
    @DisplayName("cada endpoint de los paneles: boton del legacy, btnDetalleCH (Completar), sin mirar el estado del cheque, y sucursal visible")
    void paneles(Perfil p) {
        Authentication a = auth(p);
        List<String> fallos = new ArrayList<>();
        for (Caso c : casos()) {
            for (int cheque : new int[] {PEN_PROPIO, CER_PROPIO, PEN_AJENO, CER_AJENO}) {
                Resultado r = correr(() -> c.llamada.apply(a, cheque));
                boolean debe = debePasar(p, c, cheque);
                String donde = c.ruta + " (" + estado(cheque) + ", sucursal " + (propio(cheque) ? "propia" : "ajena") + ")";
                if (debe && r.denegado) fallos.add(donde + ": exige DE MAS (" + r.causa.getMessage() + ")");
                if (!debe && !r.denegado) fallos.add(donde + ": exige DE MENOS (paso)");
                if (!debe && r.denegado) {
                    String motivo = motivoEsperado(p, c);
                    if (!(r.causa instanceof SinPermisoException)) {
                        fallos.add(donde + ": el 403 no lleva motivo (" + r.causa.getClass().getSimpleName() + ")");
                    } else if (!SUCURSAL.equals(motivo) && !r.causa.getMessage().equals(MensajesCheque.sinPermiso(motivo))) {
                        fallos.add(donde + ": el 403 deberia nombrar el boton " + motivo + " y dice: " + r.causa.getMessage());
                    } else if (SUCURSAL.equals(motivo) && !MensajesCheque.SOLO_SU_SUCURSAL.equals(r.causa.getMessage())) {
                        fallos.add(donde + ": el 403 no explica lo de la sucursal: " + r.causa.getMessage());
                    }
                }
            }
        }
        assertTrue(fallos.isEmpty(), p + " (" + p.v42 + "):\n  " + String.join("\n  ", fallos));
    }

    @Test
    @DisplayName("el administrador pasa en todos los paneles, con cualquier estado y en cualquier sucursal, sin consultar el ACL")
    void adminPasaEnLosPaneles() {
        Authentication a = auth(Perfil.ADMIN);
        consultasAlGate = 0;
        for (Caso c : casos()) {
            for (int cheque : new int[] {PEN_PROPIO, CER_PROPIO, PEN_AJENO, CER_AJENO}) {
                Resultado r = correr(() -> c.llamada.apply(a, cheque));
                assertFalse(r.denegado, c.ruta + " cheque " + cheque + ": " + (r.causa == null ? "" : r.causa.getMessage()));
            }
        }
        assertEquals(0, consultasAlGate);
    }

    @Test
    @DisplayName("las rutas de ChChequeSatelitesController estan todas clasificadas aqui")
    void todasLasRutasDePanelesEstanClasificadas() {
        java.util.Set<String> declaradas = new java.util.TreeSet<>();
        for (java.lang.reflect.Method m : ChChequeSatelitesController.class.getDeclaredMethods()) {
            org.springframework.web.bind.annotation.PostMapping pm =
                    m.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
            if (pm != null) declaradas.addAll(Arrays.asList(pm.value()));
        }
        java.util.Set<String> clasificadas = new java.util.TreeSet<>();
        for (Caso c : casos()) clasificadas.add(c.ruta);
        assertEquals(declaradas, clasificadas);
    }

    @Test
    @DisplayName("el estado del cheque no cambia nada en los paneles: con CER se anota, se elimina y se sube el PDF igual que con PEN")
    void elEstadoNoCuenta() {
        for (Perfil p : Perfil.values()) {
            Authentication a = auth(p);
            for (Caso c : casos()) {
                boolean conPen = !correr(() -> c.llamada.apply(a, PEN_PROPIO)).denegado;
                boolean conCer = !correr(() -> c.llamada.apply(a, CER_PROPIO)).denegado;
                assertEquals(conPen, conCer, p + " " + c.ruta);
            }
        }
    }

    // ===================================================================== //
    //     2. "Actualizar datos SAP": btnNuevoCH (y solo ese), sin sucursal  //
    // ===================================================================== //

    @ParameterizedTest(name = "{0}: Actualizar datos SAP = btnNuevoCH")
    @EnumSource(Perfil.class)
    @DisplayName("Actualizar datos SAP: wInfoCenter.esAutorizado('btnNuevoCH') = autorizarBtn; no depende de la sucursal ni del estado")
    void sapExigeBtnNuevoCH(Perfil p) {
        // sin sucursal inicial en ninguna empresa: el boton del legacy tampoco la mira
        when(sucursales.sucursalInicial(anyInt(), anyInt(), anyLong())).thenReturn(0L);
        Resultado r = correr(() -> sap.actualizarDesdeSap(auth(p)));
        assertEquals(p.puede("btnNuevoCH"), !r.denegado, p + " " + p.v42);
        if (r.denegado) {
            assertTrue(r.causa instanceof SinPermisoException, "el 403 lleva motivo");
            assertTrue(r.causa.getMessage().contains("btnNuevoCH"), r.causa.getMessage());
        } else {
            verify(sapDao).actualizarDesdeSap(p.codUsuario());   // llega al procedimiento, con el usuario del token
        }
    }

    @Test
    @DisplayName("btnNuevo2CH (Registre) NO alcanza para Actualizar datos SAP: el legacy lo evalua con btnNuevoCH")
    void btnNuevo2CHNoBastaParaSap() {
        Authentication a = soloCon("btnNuevo2CH", "btnEditar1CH", "btnDetalleCH");
        assertTrue(correr(() -> sap.actualizarDesdeSap(a)).denegado);
        assertFalse(correr(() -> sap.actualizarDesdeSap(soloCon("btnNuevoCH"))).denegado);
    }

    // ===================================================================== //
    //       3. Verificar Cheques (vista 77): sin botones, sin sucursal       //
    // ===================================================================== //

    @ParameterizedTest(name = "{0}: Verificar Cheques no pide botones")
    @EnumSource(Perfil.class)
    @DisplayName("/cheque/verificacion/**: ningun perfil es cortado (el legacy no tiene botones ni mira la sucursal)")
    void verificarChequesNoPideNada(Perfil p) {
        Authentication a = auth(p);
        List<String> cortados = new ArrayList<>();
        Object[][] llamadas = {
                {"/listar", (Runnable) () -> verificacion.listar(null)},
                {"/pendientes", (Runnable) () -> verificacion.pendientes(null)},
                {"/preparar", (Runnable) () -> verificacion.preparar(new FiltroIdDto(PEN_AJENO))},
                {"/registrar", (Runnable) () -> verificacion.registrar(new VerificacionRegistroDto(), a)},
                {"/anular", (Runnable) () -> verificacion.anular(new FiltroIdDto(7), a)},
        };
        for (Object[] l : llamadas) {
            if (correr((Runnable) l[1]).denegado) cortados.add((String) l[0]);
        }
        assertTrue(cortados.isEmpty(), p + " cortado en " + cortados);
    }

    // ===================================================================== //
    //     4. El resto del detalle y btnDetalleCH (para decidir lo de los     //
    //        paneles): que exige cada accion cuando NO se tiene btnDetalleCH //
    // ===================================================================== //

    @Test
    @DisplayName("sin btnDetalleCH: detalle, devolver, cerrar y los 12 endpoints de los paneles se cortan; fecha de cobro, eliminar accion y el PDF del cheque NO dependen de el")
    void queDependeDeBtnDetalleCH() {
        AccionChequeRequest dev = accionReq(PEN_PROPIO, "DEV");
        AccionChequeRequest cerrar = accionReq(PEN_PROPIO, "COB");
        cerrar.setConVerificacion(true);
        AccionChequeRequest fecha = accionReq(PEN_PROPIO, "VEN");

        Authentication sinDetalle = soloCon("btnNuevoNRCH", "btnEliminarNRCH", "btnRpt1CH");   // cualquier cosa menos Detalle
        assertTrue(correr(() -> servicio.detalle(sinDetalle, PEN_PROPIO)).denegado, "detalle exige btnDetalleCH");
        assertTrue(correr(() -> servicio.devolver(sinDetalle, dev)).denegado, "devolver exige btnDetalleCH");
        assertTrue(correr(() -> servicio.cerrar(sinDetalle, cerrar)).denegado, "cerrar exige btnDetalleCH");

        // «Fecha Cobro»: el boton de la fila (btnEditar2CH), el de administrador (btnEditar3CH) o el panel (btnDetalleCH).
        assertFalse(correr(() -> servicio.fechaCobro(soloCon("btnEditar2CH"), fecha)).denegado, "solo btnEditar2CH basta");
        assertFalse(correr(() -> servicio.fechaCobro(soloCon("btnEditar3CH"), fecha)).denegado, "solo btnEditar3CH basta");
        assertTrue(correr(() -> servicio.fechaCobro(sinDetalle, fecha)).denegado, "ninguno de los tres: se corta");

        // «Eliminar accion»: su propio boton, sin btnDetalleCH.
        assertFalse(correr(() -> servicio.eliminarAccion(soloCon("btnEliminarSegCH"), 77)).denegado,
                "solo btnEliminarSegCH basta (el legacy exigia llegar con Completar)");
        assertTrue(correr(() -> servicio.eliminarAccion(sinDetalle, 77)).denegado);

        // PDF del cheque: ningun boton (esta en la fila del legacy, no en el panel).
        assertFalse(correr(() -> pdfCheque.estado(soloCon(), PEN_PROPIO)).denegado, "el PDF del cheque no pide boton");

        // Los paneles (desde la correccion de la #4): TODOS piden btnDetalleCH, como el detalle, devolver y cerrar.
        Authentication soloDetalle = soloCon("btnDetalleCH");
        Authentication ninguno = soloCon();
        for (Caso c : casos()) {
            boolean tieneBotonPropio = c.botones.length > 0;
            assertEquals(!tieneBotonPropio, !correr(() -> c.llamada.apply(soloDetalle, PEN_PROPIO)).denegado,
                    c.ruta + " con solo btnDetalleCH: pasa si no tiene boton propio");
            assertTrue(correr(() -> c.llamada.apply(ninguno, PEN_PROPIO)).denegado, c.ruta + " sin ningun boton");
            if (tieneBotonPropio) {
                // el boton propio solo NO alcanza: sin btnDetalleCH no se llega al panel
                Authentication soloElPropio = soloCon(c.botones);
                assertTrue(correr(() -> c.llamada.apply(soloElPropio, PEN_PROPIO)).denegado,
                        c.ruta + " con solo " + c.botones[0] + " (sin btnDetalleCH)");
                // y con los dos pasa
                String[] ambos = Arrays.copyOf(c.botones, c.botones.length + 1);
                ambos[ambos.length - 1] = "btnDetalleCH";
                Authentication conAmbos = soloCon(ambos);
                assertFalse(correr(() -> c.llamada.apply(conAmbos, PEN_PROPIO)).denegado, c.ruta + " con " + String.join("+", ambos));
            }
        }
    }

    private AccionChequeRequest accionReq(int cheque, String estado) {
        AccionChequeRequest r = new AccionChequeRequest();
        r.setCodCheque(cheque);
        r.setEstado(estado);
        r.setFecha(d(2026, 10, 3));
        r.setNuevaFechaCobro(d(2026, 10, 5));
        r.setObservacion("prueba");
        r.setNroSap("12345");
        return r;
    }

    @Test
    @DisplayName("ningun perfil real tiene un boton del detalle (btnNuevoNRCH, btnEliminarNRCH, btnEliminarSegCH, btnEditar2CH) sin btnDetalleCH")
    void ningunPerfilRealTieneBotonDelDetalleSinBtnDetalleCH() {
        String[] delDetalle = {"btnNuevoNRCH", "btnEliminarNRCH", "btnEliminarSegCH", "btnEditar2CH", "btnEditar3CH"};
        List<String> afectados = new ArrayList<>();
        for (Perfil p : Perfil.values()) {
            if (p.admin()) continue;
            if (!p.tiene42("btnDetalleCH") && p.puedeAlguno(delDetalle)) afectados.add(p.name());
        }
        assertTrue(afectados.isEmpty(), "exigir btnDetalleCH en los paneles dejaria fuera a " + afectados);
    }

    // ===================================================================== //
    //                 Matriz impresa para el documento                       //
    // ===================================================================== //

    @Test
    @EnabledIfSystemProperty(named = "matriz", matches = "true")
    @DisplayName("imprime la matriz de los paneles, SAP y Verificar Cheques contra el backend (target/matriz-permisos-paneles-backend.md)")
    void imprimeLaMatriz() throws Exception {
        StringBuilder sb = new StringBuilder("| Endpoint (componente del legacy) | cheque / sucursal");
        for (Perfil p : Perfil.values()) sb.append(" | ").append(p == Perfil.SIN_BOTONES ? "SIN" : p == Perfil.ADMIN ? "ADM" : p.name());
        sb.append(" |\n|---|---");
        for (int i = 0; i < Perfil.values().length; i++) sb.append("|:-:");
        sb.append("|\n");
        int dif = 0;
        for (Caso c : casos()) {
            for (int cheque : new int[] {PEN_PROPIO, CER_PROPIO, PEN_AJENO}) {
                sb.append("| `").append(c.ruta).append("` · ").append(c.legacy).append(" | ").append(estado(cheque))
                        .append(propio(cheque) ? " propia" : " ajena");
                for (Perfil p : Perfil.values()) {
                    Resultado r = correr(() -> c.llamada.apply(auth(p), cheque));
                    boolean debe = p.admin() || debePasar(p, c, cheque);
                    String celda;
                    if (r.denegado == !debe) celda = r.denegado ? "403" : "✔";
                    else { celda = "DIF"; dif++; }
                    sb.append(" | ").append(celda);
                }
                sb.append(" |\n");
            }
        }
        // SAP y Verificar Cheques
        sb.append("| `/cheque/clientes/actualizar-sap` · ACTUALIZAR DATOS SAP (L119, btnNuevoCH) | cualquiera");
        for (Perfil p : Perfil.values()) {
            boolean pasa = !correr(() -> sap.actualizarDesdeSap(auth(p))).denegado;
            sb.append(" | ").append(pasa == p.puede("btnNuevoCH") ? (pasa ? "✔" : "403") : "DIF");
            if (pasa != p.puede("btnNuevoCH")) dif++;
        }
        sb.append(" |\n| `/cheque/verificacion/**` · Verificar Cheques (sin botones) | cualquiera");
        for (Perfil p : Perfil.values()) {
            boolean pasa = !correr(() -> verificacion.listar(null)).denegado && !correr(() -> verificacion.pendientes(null)).denegado;
            sb.append(" | ").append(pasa ? "✔" : "DIF");
            if (!pasa) dif++;
        }
        sb.append(" |\n");
        java.nio.file.Files.write(java.nio.file.Paths.get("target", "matriz-permisos-paneles-backend.md"),
                sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(0, dif);
    }

    // ===================================================================== //
    //   DIFERENCIA #4 (CORREGIDA): los paneles se alcanzan solo con           //
    //   "Completar" (btnDetalleCH). Pruebas permanentes. Ver PERMISOS_CHEQUES //
    // ===================================================================== //

    private static final String[] LOS_DOCE_ENDPOINTS = {
            "/nota-remision/listar", "/nota-remision/registrar", "/nota-remision/eliminar",
            "/transaccion/listar", "/transaccion/registrar", "/transaccion/eliminar",
            "/postergacion/listar", "/postergacion/registrar", "/postergacion/eliminar",
            "/postergacion/pdf/estado", "/postergacion/pdf/subir", "/postergacion/pdf/descargar"};

    private Caso caso(String ruta) {
        for (Caso c : casos()) if (c.ruta.equals(ruta)) return c;
        throw new AssertionError("no hay un caso para " + ruta);
    }

    @ParameterizedTest(name = "{0}: sin btnDetalleCH, 403 que nombra el boton")
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/nota-remision/listar", "/nota-remision/registrar", "/nota-remision/eliminar",
            "/transaccion/listar", "/transaccion/registrar", "/transaccion/eliminar",
            "/postergacion/listar", "/postergacion/registrar", "/postergacion/eliminar",
            "/postergacion/pdf/estado", "/postergacion/pdf/subir", "/postergacion/pdf/descargar"})
    @DisplayName("DIFERENCIA #4 (corregida): sin btnDetalleCH, aunque tenga TODOS los demas botones de la vista 42 (btnChqSucrs incluido), el endpoint da 403 y el motivo nombra btnDetalleCH")
    void diferencia4_sinBtnDetalleCH_403ConElMotivo(String ruta) {
        Caso c = caso(ruta);
        Authentication todosMenosDetalle = soloCon(TODOS_MENOS_DETALLE);
        for (int cheque : new int[] {PEN_PROPIO, CER_PROPIO, PEN_AJENO, CER_AJENO}) {
            Resultado r = correr(() -> c.llamada.apply(todosMenosDetalle, cheque));

            assertTrue(r.denegado, ruta + " cheque " + cheque + ": con todos los botones menos btnDetalleCH debia cortar");
            assertTrue(r.causa instanceof SinPermisoException, ruta + ": el 403 lleva motivo");
            assertEquals(MensajesCheque.sinPermiso("btnDetalleCH"), r.causa.getMessage(), ruta + " cheque " + cheque);
        }
        // y no se llego a tocar nada: ni el cheque ni la tabla del panel
        verify(cheques, org.mockito.Mockito.never()).obtener(anyInt());
        verify(notasDao, org.mockito.Mockito.never()).registrar(any());
        verify(notasDao, org.mockito.Mockito.never()).eliminar(anyInt(), anyString(), anyInt());
        verify(transaccionesDao, org.mockito.Mockito.never()).registrar(any());
        verify(transaccionesDao, org.mockito.Mockito.never()).eliminar(anyInt(), anyString(), anyInt());
        verify(postergacionesDao, org.mockito.Mockito.never()).registrar(any());
        verify(postergacionesDao, org.mockito.Mockito.never()).eliminar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("DIFERENCIA #4 (corregida): son 12 endpoints, el parametrizado de arriba los cubre todos y son los de la API")
    void diferencia4_losDoceEstanCubiertos() {
        java.util.Set<String> clasificadas = new java.util.TreeSet<>();
        for (Caso c : casos()) clasificadas.add(c.ruta);
        assertEquals(new java.util.TreeSet<>(Arrays.asList(LOS_DOCE_ENDPOINTS)), clasificadas);
        assertEquals(12, LOS_DOCE_ENDPOINTS.length);
    }

    @Test
    @DisplayName("DIFERENCIA #4 (corregida): el usuario real que la motivaba (sin ningun boton y con sucursal) ya no llega a ningun panel; el boton propio va primero")
    void diferencia4_elUsuarioSinBotonesYaNoLlegaAPaneles() {
        Authentication sinNada = auth(Perfil.SIN_BOTONES);
        for (Caso c : casos()) {
            Resultado r = correr(() -> c.llamada.apply(sinNada, PEN_PROPIO));

            assertTrue(r.denegado, c.ruta);
            String esperado = c.botones.length > 0 ? c.botones[0] : "btnDetalleCH";
            assertEquals(MensajesCheque.sinPermiso(esperado), r.causa.getMessage(), c.ruta);
        }
    }

    @Test
    @DisplayName("DIFERENCIA #4 (corregida): el administrador sigue pasando los 12 endpoints sin btnDetalleCH y sin consultar el ACL")
    void diferencia4_elAdminNoNecesitaElBoton() {
        consultasAlGate = 0;
        Authentication adm = auth(Perfil.ADMIN);
        for (Caso c : casos()) {
            assertFalse(correr(() -> c.llamada.apply(adm, PEN_AJENO)).denegado, c.ruta);
        }
        assertEquals(0, consultasAlGate);
    }
}
