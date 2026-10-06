package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;

import bo.bosque.com.impexpap.config.GlobalExceptionHandler;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.controller.ChBancoController;
import bo.bosque.com.impexpap.controller.ChChequeController;
import bo.bosque.com.impexpap.controller.ChVerificacionController;
import bo.bosque.com.impexpap.dao.IChAccion;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IChChequeReporte;
import bo.bosque.com.impexpap.dao.IPersonalCheque;
import bo.bosque.com.impexpap.dao.IProgramador;
import bo.bosque.com.impexpap.dao.ISocionegocio;
import bo.bosque.com.impexpap.dao.ISucursalCheque;
import bo.bosque.com.impexpap.dao.ITalonario;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dto.AccionChequeRequest;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequeFiltroDto;
import bo.bosque.com.impexpap.dto.ChequeRegistroDto;
import bo.bosque.com.impexpap.dto.CustodiaChequeRequest;
import bo.bosque.com.impexpap.dto.DarCustodiaRequest;
import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.FiltroIdDto;
import bo.bosque.com.impexpap.dto.FiltroSucursalFechaDto;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.dto.ReporteChequeRequest;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.dto.TalonarioValidacionRequest;
import bo.bosque.com.impexpap.model.ChAccion;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Paridad de PERMISOS del modulo Cheques entre el legacy (JSF) y Spring para quien <b>no es administrador</b>.
 * Documento con la tabla de verdad, la matriz y las diferencias: {@code PERMISOS_CHEQUES.md} del espacio de migracion.
 *
 * <h3>Que se prueba</h3>
 * <ol>
 *   <li><b>El gate</b>: el {@link AccesoModuloHelper} REAL, alimentado por dos dobles que reproducen lo que hacen los
 *       procedimientos ({@code p_list_trs_Programador 'E'}: login a codUsuario, y {@code p_list_UsuarioBtn 'A'}: el ACL con
 *       {@code nivelAcceso != 0}), da lo mismo que {@code Loggin.autorizarBtn} del legacy para los 12 perfiles reales de
 *       PRUEBA/produccion, para quien no tiene ningun boton y para el administrador.</li>
 *   <li><b>Cada endpoint</b> de {@code /cheque}, {@code /banco} y {@code /cheque/verificacion}, llamado por el
 *       CONTROLADOR con los servicios reales (solo los DAO son simulados): responde 403 con el motivo cuando falta el boton
 *       que el legacy exigia y pasa cuando lo tiene. El mapeo endpoint a boton queda fijado en {@link #casos()}.</li>
 *   <li><b>Las diferencias encontradas</b> (omitidas por defecto; se ven con {@code -Ddiferencias=true}).</li>
 * </ol>
 *
 * <p>Un caso "pasa" cuando NO sale un {@link AccessDeniedException}: puede salir un error de negocio (el cuerpo de la prueba
 * no es un cheque valido) o un exito; lo que se mide aqui es el ACL, no las reglas de campos (esas estan en
 * {@code ChequeServiceTest}). Los dobles dejan abierta la sucursal (la inicial del usuario es la del cheque) para que ninguna
 * denegacion se deba a otra cosa que al boton.
 */
class PermisosParidadChequeTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Clock RELOJ = Clock.fixed(
            LocalDate.of(2026, 10, 3).atTime(10, 0).atZone(ZONA).toInstant(), ZONA);

    private static final int VISTA_CHEQUES = 42;
    private static final int VISTA_BANCOS = 43;
    private static final int SUC = 3;
    private static final int SUC_AJENA = 9;
    private static final int PEN = 1;   // codCheque de un cheque abierto
    private static final int CER = 2;   // codCheque de un cheque cerrado

    /** Los 20 botones de la vista 42 de tb_vistaBtn. */
    private static final String[] BOTONES_42 = {
            "btnNuevoCH", "btnNuevo2CH", "btnEditar1CH", "btnEditar2CH", "btnEditar3CH", "btnDetalleCH",
            "btnNuevoNRCH", "btnEliminarNRCH", "btnNuevoSegCH", "btnEditarSegCH", "btnEliminarSegCH",
            "btnTraspasoCH", "btnCustodiaCH", "btnCustodia2CH", "btnRpt1CH", "btnRpt2CH", "btnRpt3CH",
            "btnRpt4CH", "btnRpt5CH", "btnChqSucrs"};
    /** Los 3 de la vista 43. */
    private static final String[] BOTONES_43 = {"btnNuevoB", "btnEditarB", "btnEliminarB"};

    // ===================================================================== //
    //                       LOS PERFILES REALES (anonimos)                  //
    // ===================================================================== //

    /**
     * Los perfiles de botones de usuarios NO administradores, medidos en {@code BOSQUE2PRUEBA} y
     * {@code BOSQUE-2_0} el 2026-10-03 con {@code replicar_gate_cheques.ps1} (vista 42 con {@code nivelAcceso <> '0'};
     * vista 43 la del mismo usuario). Los numeros son perfiles del documento, no personas.
     */
    enum Perfil {
        P1("btnCustodiaCH,btnDetalleCH,btnEditar2CH,btnNuevoCH,btnNuevoNRCH,btnNuevoSegCH,btnRpt1CH,btnRpt2CH,btnRpt3CH,btnRpt4CH,btnTraspasoCH",
                "btnNuevoB"),
        P2("btnRpt1CH", ""),
        P3("btnChqSucrs,btnCustodiaCH,btnDetalleCH,btnEditar2CH,btnNuevoNRCH,btnNuevoSegCH,btnRpt1CH,btnRpt2CH,btnRpt3CH", ""),
        P4("btnCustodiaCH,btnDetalleCH,btnEditar2CH,btnEliminarNRCH,btnNuevoNRCH,btnNuevoSegCH,btnRpt1CH,btnRpt2CH,btnRpt3CH", ""),
        P5("btnDetalleCH,btnEditar1CH,btnNuevoCH,btnRpt1CH,btnRpt4CH,btnTraspasoCH", "btnNuevoB"),
        P6("btnDetalleCH,btnEditar1CH,btnNuevoCH,btnRpt1CH,btnTraspasoCH", "btnNuevoB"),
        P7("btnDetalleCH,btnRpt1CH", ""),
        P8("btnDetalleCH", "btnNuevoB"),
        P9("btnDetalleCH,btnEditarSegCH,btnNuevoCH", ""),
        P10("btnDetalleCH,btnNuevoCH,btnRpt1CH,btnRpt4CH,btnTraspasoCH", "btnNuevoB"),
        P11("btnChqSucrs,btnCustodia2CH,btnCustodiaCH,btnDetalleCH,btnEditar1CH,btnEditar2CH,btnNuevoNRCH,btnNuevoSegCH,btnRpt1CH,btnRpt2CH,btnRpt3CH,btnRpt5CH",
                ""),
        P12("btnChqSucrs,btnCustodiaCH,btnDetalleCH,btnEditar1CH,btnEditar2CH,btnEliminarNRCH,btnNuevoCH,btnNuevoNRCH,btnNuevoSegCH,btnRpt1CH,btnRpt2CH,btnRpt3CH,btnRpt4CH",
                ""),
        /** 83 usuarios activos y 20 bloqueados de PRUEBA (63 y 40 en produccion): sin ninguna fila con permiso. */
        SIN_BOTONES("", ""),
        /** tipoUsuario = 'adm' (ROLE_ADM): pasa siempre, tenga o no filas en tb_usuarioBtn. */
        ADMIN("", "");

        final Set<String> v42;
        final Set<String> v43;

        Perfil(String v42, String v43) {
            this.v42 = conjunto(v42);
            this.v43 = conjunto(v43);
        }

        boolean admin() {
            return this == ADMIN;
        }

        /** codUsuario sintetico, distinto por perfil. */
        int codUsuario() {
            return 1000 + ordinal();
        }

        String login() {
            return "login-" + name().toLowerCase();
        }

        boolean tiene42(String boton) {
            return v42.contains(boton);
        }

        /** El legacy y Spring: el boton cuenta con el fallback de administrador. */
        boolean puede(String boton) {
            return admin() || v42.contains(boton) || v43.contains(boton);
        }

        boolean puedeAlguno(String... botones) {
            for (String b : botones) if (puede(b)) return true;
            return false;
        }
    }

    private static Set<String> conjunto(String csv) {
        Set<String> s = new LinkedHashSet<>();
        for (String b : csv.split(",")) if (!b.trim().isEmpty()) s.add(b.trim());
        return s;
    }

    // ===================================================================== //
    //                    DOBLES DE LOS DOS PROCEDIMIENTOS DEL GATE          //
    // ===================================================================== //

    /** Una fila cruda de {@code tb_usuarioBtn} unida a {@code tb_vistaBtn}: nivelAcceso es varchar(2), como en la tabla. */
    private static final class FilaAcl {
        final String boton;
        final String nivelAcceso;
        final int vista;

        FilaAcl(String boton, String nivelAcceso, int vista) {
            this.boton = boton;
            this.nivelAcceso = nivelAcceso;
            this.vista = vista;
        }
    }

    /**
     * {@code p_list_UsuarioBtn 'A'}: {@code WHERE nivelAcceso != 0}. SQL Server convierte el varchar a int: ' ' vale 0,
     * '01' vale 1, '2' vale 2 y un texto no numerico revienta toda la consulta (error 245).
     */
    private static int comoSqlServer(String nivel) {
        String t = nivel.trim();
        if (t.isEmpty()) return 0;
        return Integer.parseInt(t);
    }

    private final Map<String, Integer> loginACodigo = new HashMap<>();
    private final Map<Integer, List<FilaAcl>> acl = new HashMap<>();
    private int consultasAlGate;

    /** Las filas que devolveria el procedimiento (ya filtradas por {@code != 0}), con {@code rs.getInt(2)}. */
    private List<UsuarioBtn> procedimientoA(int codUsuario) {
        consultasAlGate++;
        List<UsuarioBtn> r = new ArrayList<>();
        for (FilaAcl f : acl.getOrDefault(codUsuario, Collections.<FilaAcl>emptyList())) {
            int permiso = comoSqlServer(f.nivelAcceso);
            if (permiso != 0) {
                UsuarioBtn b = new UsuarioBtn();
                b.setBoton(f.boton);
                b.setPermiso(permiso);
                b.setPertenVist(f.vista);
                r.add(b);
            }
        }
        return r;
    }

    /** {@code Loggin.autorizarBtn} (Loggin.java:385-412): PRIMERA fila con ese nombre, y el respaldo {@code tipoUsuario.equals("adm")}. */
    private static boolean autorizarBtnDelLegacy(List<UsuarioBtn> filasDelUsuario, String tipoUsuario, String boton) {
        int permiso = 0;
        for (UsuarioBtn f : filasDelUsuario) {
            if (f.getBoton().equals(boton)) {
                permiso = f.getPermiso();
                break;
            }
        }
        return permiso != 0 || tipoUsuario.equals("adm");
    }

    // ===================================================================== //
    //                                 ARNES                                 //
    // ===================================================================== //

    private IChCheque cheques;
    private IChAccion acciones;
    private ISucursalCheque sucursales;
    private IPersonalCheque personal;
    private ITalonario talonarios;
    private IChBanco bancosDao;
    private IChChequeReporte datosReporte;
    private IProgramador programador;
    private IUsuarioBtn usuarioBtn;
    private AccesoModuloHelper acceso;

    private ChequeService servicio;
    private ChChequeController controlador;
    private ChBancoController bancos;

    @BeforeEach
    void preparar() throws Exception {
        cheques = mock(IChCheque.class);
        acciones = mock(IChAccion.class);
        sucursales = mock(ISucursalCheque.class);
        personal = mock(IPersonalCheque.class);
        talonarios = mock(ITalonario.class);
        bancosDao = mock(IChBanco.class);
        datosReporte = mock(IChChequeReporte.class);
        programador = mock(IProgramador.class);
        usuarioBtn = mock(IUsuarioBtn.class);

        for (Perfil p : Perfil.values()) {
            loginACodigo.put(p.login(), p.codUsuario());
            List<FilaAcl> filas = new ArrayList<>();
            // ruido: botones de otras vistas que el procedimiento tambien devuelve
            filas.add(new FilaAcl("btnDetalles", "1", 24));
            for (String b : p.v42) filas.add(new FilaAcl(b, "1", VISTA_CHEQUES));
            for (String b : p.v43) filas.add(new FilaAcl(b, "1", VISTA_BANCOS));
            // y filas con permiso 0, que el procedimiento NO devuelve
            for (String b : BOTONES_42) if (!p.v42.contains(b)) filas.add(new FilaAcl(b, "0", VISTA_CHEQUES));
            acl.put(p.codUsuario(), filas);
        }
        // p_list_trs_Programador 'E': TOP 1 codUsuario por login; si no existe, codUsuario = 0 (ISNULL(@codUsu, 0)).
        when(programador.resolverPermiso(anyString())).thenAnswer(inv -> {
            consultasAlGate++;
            MiEquipoDto d = new MiEquipoDto();
            Integer cod = loginACodigo.get((String) inv.getArgument(0));
            d.setCodUsuario(cod == null ? 0L : cod.longValue());
            return d;
        });
        when(usuarioBtn.botonesXUsuario(anyInt())).thenAnswer(inv -> procedimientoA((Integer) inv.getArgument(0)));
        acceso = new AccesoModuloHelper(programador, usuarioBtn);

        // La pantalla: dos empresas, y la sucursal 3 es la inicial de todos (asi ninguna denegacion es por sucursal).
        when(sucursales.empresasDeCheques()).thenReturn(Arrays.asList(
                new EmpresaChequeDto(1, "IMPEXPAP"), new EmpresaChequeDto(5, "ESPPAPEL")));
        when(sucursales.sucursalInicial(anyInt(), anyInt(), anyLong())).thenReturn((long) SUC);
        when(sucursales.sucursalesDeEmpresa(anyInt(), anyInt())).thenReturn(Arrays.asList(
                new SucursalChequeDto(SUC, "Sucursal 6 Cochabamba"), new SucursalChequeDto(SUC_AJENA, "Sucursal 12 Santa Cruz")));
        when(sucursales.puedeEscribir(anyLong(), anyInt())).thenReturn(true);   // p_list_Sucursal 'D' autoriza a cualquiera

        when(cheques.obtener(PEN)).thenReturn(cheque(PEN, "PEN"));
        when(cheques.obtener(CER)).thenReturn(cheque(CER, "CER"));
        when(cheques.codigoBotones(anyInt())).thenReturn("1111");
        when(cheques.alta(any(), any())).thenReturn(new RespuestaSp(0, "", 501L));
        when(cheques.actualizar(any(), any())).thenReturn(new RespuestaSp(0, "", 0L));
        when(cheques.cambiarEstado(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));
        when(cheques.cambiarFechaCobro(anyInt(), any(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));
        ChAccion accion = new ChAccion();
        accion.setCodAccion(77);
        accion.setCodCheque(PEN);
        accion.setEstado("CUS");
        when(acciones.obtener(anyInt())).thenReturn(accion);
        when(acciones.contarPorCheque(anyInt())).thenReturn(3);
        when(acciones.traspasosDelCheque(anyInt())).thenReturn(1);
        when(acciones.devolucionesIgualAEntregas(anyInt())).thenReturn(true);
        when(acciones.registrar(any())).thenReturn(new RespuestaSp(0, "", 900L));
        when(acciones.traspasar(anyLong(), anyInt())).thenReturn(new RespuestaSp(0, "", 4L));
        when(acciones.copiar(anyInt(), anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 901L));
        when(acciones.eliminar(anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));
        when(bancosDao.obtener(anyInt())).thenReturn(banco(7));
        when(bancosDao.registrar(any())).thenReturn(new RespuestaSp(0, "", 7L));
        when(bancosDao.eliminar(anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 0L));

        servicio = new ChequeService(cheques, acciones, sucursales, personal, talonarios, acceso, RELOJ);
        bo.bosque.com.impexpap.commons.JasperReportExport jasper = mock(bo.bosque.com.impexpap.commons.JasperReportExport.class);
        when(jasper.exportPDFDesdeColeccion(anyString(), any(), any())).thenReturn(new byte[] {1});
        when(jasper.exportPDFDesdeColeccionConSubreportes(anyString(), any(), any(), any(String[].class))).thenReturn(new byte[] {1});
        ChequeReporteService reportes = new ChequeReporteService(datosReporte, servicio, sucursales, personal, acceso, jasper);
        ChequePdfService pdfs = new ChequePdfService(cheques, servicio, "");
        controlador = new ChChequeController(servicio, reportes, pdfs, mock(ISocionegocio.class));
        bancos = new ChBancoController(bancosDao, acceso);
        consultasAlGate = 0;
    }

    private static ChequeFilaDto cheque(int cod, String estado) {
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
        c.setCodSucursal((long) SUC);
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
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(p.login(), null,
                Collections.singletonList(new SimpleGrantedAuthority(p.admin() ? "ROLE_ADM" : "ROLE_LIM")));
        t.setDetails(new DatosToken(p.codUsuario(), 3, 6, p.admin() ? "adm" : "lim", "USUARIO " + p.name()));
        return t;
    }

    // ===================================================================== //
    //                           1. EL GATE                                  //
    // ===================================================================== //

    @ParameterizedTest(name = "{0}: tieneBoton (Spring) == autorizarBtn (legacy) en los 23 botones de las vistas 42 y 43")
    @EnumSource(Perfil.class)
    @DisplayName("el gate de Spring da lo mismo que Loggin.autorizarBtn para cada perfil real")
    void gateIgualAlDelLegacy(Perfil p) {
        List<UsuarioBtn> filasDelUsuario = procedimientoA(p.codUsuario());   // lo que lee Loggin al iniciar sesion
        String tipo = p.admin() ? "adm" : "lim";
        List<String> distintos = new ArrayList<>();
        for (String b : concat(BOTONES_42, BOTONES_43)) {
            int vista = Arrays.asList(BOTONES_42).contains(b) ? VISTA_CHEQUES : VISTA_BANCOS;
            boolean legacy = autorizarBtnDelLegacy(filasDelUsuario, tipo, b);
            boolean spring = acceso.tieneBoton(auth(p), vista, b);
            if (legacy != spring) distintos.add(b + " legacy=" + legacy + " spring=" + spring);
            assertEquals(p.puede(b), spring, p + " " + b + ": lo que dice la tabla de verdad");
        }
        assertTrue(distintos.isEmpty(), p + ": " + distintos);
    }

    private static List<String> concat(String[] a, String[] b) {
        List<String> l = new ArrayList<>(Arrays.asList(a));
        l.addAll(Arrays.asList(b));
        return l;
    }

    @Test
    @DisplayName("nivelAcceso: '1', '01' y '2' autorizan; '0' y ' ' (espacios) no; igual que != 0 del procedimiento y de autorizarBtn")
    void nivelAccesoComoElProcedimiento() {
        int usuario = 4242;
        loginACodigo.put("mixto", usuario);
        acl.put(usuario, Arrays.asList(
                new FilaAcl("btnNuevoCH", "1", VISTA_CHEQUES),
                new FilaAcl("btnEditar1CH", "01", VISTA_CHEQUES),
                new FilaAcl("btnEditar2CH", "2", VISTA_CHEQUES),
                new FilaAcl("btnDetalleCH", "0", VISTA_CHEQUES),
                new FilaAcl("btnTraspasoCH", " ", VISTA_CHEQUES),
                new FilaAcl("btnCustodiaCH", "0 ", VISTA_CHEQUES)));
        Authentication a = autenticado("mixto", usuario, "ROLE_LIM", "lim");
        List<UsuarioBtn> filas = procedimientoA(usuario);

        for (String b : new String[] {"btnNuevoCH", "btnEditar1CH", "btnEditar2CH"}) {
            assertTrue(acceso.tieneBoton(a, VISTA_CHEQUES, b), b);
            assertTrue(autorizarBtnDelLegacy(filas, "lim", b), b + " (legacy)");
        }
        for (String b : new String[] {"btnDetalleCH", "btnTraspasoCH", "btnCustodiaCH", "btnRpt1CH"}) {
            assertFalse(acceso.tieneBoton(a, VISTA_CHEQUES, b), b);
            assertFalse(autorizarBtnDelLegacy(filas, "lim", b), b + " (legacy)");
        }
    }

    @Test
    @DisplayName("un texto no numerico en nivelAcceso hace fallar toda la consulta del ACL (error 245 de SQL Server): no autoriza a nadie por accidente")
    void nivelAccesoNoNumericoNoAutoriza() {
        int usuario = 4343;
        loginACodigo.put("raro", usuario);
        acl.put(usuario, Collections.singletonList(new FilaAcl("btnNuevoCH", "x", VISTA_CHEQUES)));
        // El procedimiento real falla; en Spring la excepcion sube (500) y el boton no queda autorizado.
        assertThrows(NumberFormatException.class,
                () -> acceso.tieneBoton(autenticado("raro", usuario, "ROLE_LIM", "lim"), VISTA_CHEQUES, "btnNuevoCH"));
    }

    @Test
    @DisplayName("un usuario sin ninguna fila en tb_usuarioBtn no tiene ningun boton; con ROLE_ADM si, aunque no tenga filas")
    void sinFilasNiBotones() {
        int usuario = 4444;
        loginACodigo.put("vacio", usuario);   // existe en tb_usuario, sin filas en tb_usuarioBtn
        Authentication a = autenticado("vacio", usuario, "ROLE_LIM", "lim");
        for (String b : BOTONES_42) assertFalse(acceso.tieneBoton(a, VISTA_CHEQUES, b), b);

        Authentication adm = autenticado("vacio", usuario, "ROLE_ADM", "adm");
        for (String b : BOTONES_42) assertTrue(acceso.tieneBoton(adm, VISTA_CHEQUES, b), b);
    }

    @Test
    @DisplayName("un login que no existe en tb_usuario resuelve a codUsuario 0 (ISNULL del SP): falla cerrado, sin botones")
    void loginDesconocidoFallaCerrado() {
        Authentication a = autenticado("no-existe", 5, "ROLE_LIM", "lim");
        for (String b : BOTONES_42) assertFalse(acceso.tieneBoton(a, VISTA_CHEQUES, b), b);
        assertThrows(SinPermisoException.class, () -> servicio.detalle(a, PEN));
    }

    @Test
    @DisplayName("el administrador pasa sin preguntarle nada al ACL ni al puente login a codUsuario")
    void adminNoConsultaElGate() {
        Authentication adm = auth(Perfil.ADMIN);
        consultasAlGate = 0;
        for (String b : BOTONES_42) assertTrue(acceso.tieneBoton(adm, VISTA_CHEQUES, b), b);
        for (String b : BOTONES_43) assertTrue(acceso.tieneBoton(adm, VISTA_BANCOS, b), b);
        assertEquals(0, consultasAlGate, "ROLE_ADM se resuelve con las authorities del token");
    }

    @Test
    @DisplayName("el boton se busca en SU vista: el mismo nombre en otra vista no autoriza (anyMatch por vista); en cheques los nombres no se repiten")
    void elBotonSeBuscaEnSuVista() {
        int usuario = 4545;
        loginACodigo.put("otra", usuario);
        acl.put(usuario, Collections.singletonList(new FilaAcl("btnNuevoCH", "1", 24)));   // vista 24, no la 42
        assertFalse(acceso.tieneBoton(autenticado("otra", usuario, "ROLE_LIM", "lim"), VISTA_CHEQUES, "btnNuevoCH"));
    }

    @Test
    @DisplayName("ROLE_ADM de Spring es tipoUsuario 'adm' del legacy: hoy tb_usuario solo tiene 'adm' y 'lim' (la comparacion de Spring no distingue mayusculas, la del legacy si)")
    void esAdminEsElTipoAdm() {
        assertTrue(acceso.esAdmin(auth(Perfil.ADMIN)));
        for (Perfil p : Perfil.values()) {
            if (p != Perfil.ADMIN) assertFalse(acceso.esAdmin(auth(p)), p.name());
        }
    }

    private Authentication autenticado(String login, int codUsuario, String rol, String tipo) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(login, null,
                Collections.singletonList(new SimpleGrantedAuthority(rol)));
        t.setDetails(new DatosToken(codUsuario, 3, 6, tipo, "X"));
        return t;
    }

    // ===================================================================== //
    //                 2. ENDPOINT -> BOTON (mapeo fijado)                   //
    // ===================================================================== //

    /** Un endpoint, el boton (o cualquiera de los botones) que el legacy exigia para ese flujo y la llamada. */
    private static final class Caso {
        final String ruta;
        /** Cualquiera de estos habilita; vacio = sin boton. */
        final String[] botones;
        /** true: ningun no administrador pasa (p. ej. editar un cheque cerrado con el formulario estandar). */
        final boolean soloAdmin;
        final Function<Authentication, Object> llamada;
        final String legacy;

        Caso(String ruta, String legacy, String[] botones, boolean soloAdmin, Function<Authentication, Object> llamada) {
            this.ruta = ruta;
            this.legacy = legacy;
            this.botones = botones;
            this.soloAdmin = soloAdmin;
            this.llamada = llamada;
        }

        boolean sinBoton() {
            return botones.length == 0 && !soloAdmin;
        }

        @Override
        public String toString() {
            return ruta + (botones.length == 0 ? "" : " " + Arrays.toString(botones));
        }
    }

    private static String[] b(String... botones) {
        return botones;
    }

    private ChequeRegistroDto registro(String modo, Integer codCheque) {
        ChequeRegistroDto r = new ChequeRegistroDto();
        r.setModo(modo);
        r.setCodCheque(codCheque);
        r.setNrocheque("00123");
        r.setCodCliente("C001");
        r.setaOrdenDe("IMPEXPAP SRL");
        r.setFechaCheque(d(2026, 10, 1));
        r.setMonto(250.0);
        r.setMoneda("BS");
        r.setTipo("PAG");
        r.setCodBanco(2);
        r.setCodEmpleado(0);
        r.setReciboManual("0");
        r.setNroTalonario("0");
        r.setCodEmpresa(1);
        r.setCodSucursal((long) SUC);
        r.setObservacion("recibido");
        return r;
    }

    private AccionChequeRequest accion(int cod, String estado) {
        AccionChequeRequest r = new AccionChequeRequest();
        r.setCodCheque(cod);
        r.setEstado(estado);
        r.setFecha(d(2026, 10, 3));
        r.setNuevaFechaCobro(d(2026, 10, 5));
        r.setObservacion("prueba");
        r.setNroSap("12345");
        return r;
    }

    private ReporteChequeRequest reporte() {
        ReporteChequeRequest r = new ReporteChequeRequest();
        r.setCodEmpresa(1);
        r.setCodSucursal((long) SUC);
        r.setFechaDesde(d(2026, 10, 1));
        r.setFechaHasta(d(2026, 10, 3));
        r.setFecha(d(2026, 10, 3));
        r.setCodAccion(5L);
        return r;
    }

    private FiltroSucursalFechaDto sucursalYFecha() {
        FiltroSucursalFechaDto f = new FiltroSucursalFechaDto();
        f.setCodSucursal((long) SUC);
        f.setFecha(d(2026, 10, 3));
        return f;
    }

    /**
     * El mapeo endpoint a boton, endpoint por endpoint, con el componente del legacy que lo origina
     * ({@code cheque.xhtml} linea / {@code WizardCheque} metodo). Si se agrega un endpoint, hay que agregarlo aqui: la prueba de
     * rutas lo exige.
     */
    private List<Caso> casos() {
        List<Caso> c = new ArrayList<>();
        // ---- sin boton: combos y listados (el legacy los carga para todos; el paso por sucursal lo cubre el servicio)
        c.add(new Caso("/empresas", "combo Empresa (L79), sin ACL", b(), false, a -> controlador.listarEmpresas()));
        c.add(new Caso("/sucursal-inicial", "WizardCheque() constructor", b(), false, a -> controlador.sucursalInicial(null, a)));
        c.add(new Caso("/sucursales", "combo Sucursal (L87)", b(), false, a -> controlador.listarSucursales(null, a)));
        c.add(new Caso("/catalogos", "combos fijos, sin ACL", b(), false, a -> controlador.catalogos()));
        c.add(new Caso("/clientes", "combo Cliente, sin ACL", b(), false, a -> controlador.listarClientes(null)));
        c.add(new Caso("/personal/entregan", "combo Entregado por, sin ACL", b(), false,
                a -> controlador.quienesEntregan(new FiltroIdDto(SUC), a)));
        c.add(new Caso("/personal/custodia", "combo Responsable, sin ACL", b(), false,
                a -> controlador.responsablesDeCustodia(new FiltroIdDto(SUC), a)));
        c.add(new Caso("/listar", "dtCheques (L123), solo vistaActiva==1", b(), false, a -> {
            ChequeFiltroDto f = new ChequeFiltroDto();
            f.setCodSucursal((long) SUC);
            return controlador.listar(f, a);
        }));
        // ---- detalle
        c.add(new Caso("/detalle", "Completar (L198)", b("btnDetalleCH"), false, a -> controlador.detalle(new FiltroIdDto(PEN), a)));
        // ---- registrar / editar (los tres formularios)
        c.add(new Caso("/registrar [alta ESTANDAR]", "Registrar (L99)", b("btnNuevoCH"), false,
                a -> controlador.registrar(registro("ESTANDAR", null), a)));
        c.add(new Caso("/registrar [alta ADMIN]", "Registre (L113)", b("btnNuevo2CH"), false,
                a -> controlador.registrar(registro("ADMIN", null), a)));
        // (El alta con el formulario TALONARIO es un 400 para todos, administrador incluido: no es un tema de permisos.)
        c.add(new Caso("/registrar [edicion ESTANDAR, abierto]", "Editar (L176)", b("btnEditar1CH"), false,
                a -> controlador.registrar(registro("ESTANDAR", PEN), a)));
        c.add(new Caso("/registrar [edicion ESTANDAR, cerrado]", "Editar no se dibuja con CER", b(), true,
                a -> controlador.registrar(registro("ESTANDAR", CER), a)));
        c.add(new Caso("/registrar [edicion TALONARIO, cerrado]", "Editar talonario (L179)", b("btnEditar1CH"), false,
                a -> controlador.registrar(registro("TALONARIO", CER), a)));
        c.add(new Caso("/registrar [edicion TALONARIO, abierto]", "Editar talonario no se dibuja con PEN", b(), true,
                a -> controlador.registrar(registro("TALONARIO", PEN), a)));
        c.add(new Caso("/registrar [edicion ADMIN, abierto]", "Edite (L186)", b("btnEditar3CH"), false,
                a -> controlador.registrar(registro("ADMIN", PEN), a)));
        c.add(new Caso("/registrar [edicion ADMIN, cerrado]", "Edite no se dibuja con CER", b(), true,
                a -> controlador.registrar(registro("ADMIN", CER), a)));
        TalonarioValidacionRequest tv = new TalonarioValidacionRequest();
        tv.setCodEmpresa(1);
        tv.setNroTalonario("ER1076");
        tv.setReciboManual("3752");
        c.add(new Caso("/talonario/validar", "no existe en el legacy (aviso temprano; mismo permiso que los formularios)",
                b("btnNuevoCH", "btnNuevo2CH", "btnEditar1CH", "btnEditar3CH"), false, a -> controlador.validarTalonario(tv, a)));
        // ---- acciones del detalle
        c.add(new Caso("/accion/fecha-cobro", "Fecha Cobro de la fila (L182, L190) y del panel (L386)",
                b("btnEditar2CH", "btnEditar3CH", "btnDetalleCH"), false, a -> controlador.fechaCobro(accion(PEN, "VEN"), a)));
        c.add(new Caso("/accion/devolver", "Devolver (L392): solo la rama K, se llega con Completar", b("btnDetalleCH"), false,
                a -> controlador.devolver(accion(PEN, "DEV"), a)));
        c.add(new Caso("/accion/cerrar [con verificacion]", "Cerrar con verificacion (L397)", b("btnDetalleCH"), false, a -> {
            AccionChequeRequest r = accion(PEN, "COB");
            r.setConVerificacion(true);
            return controlador.cerrar(r, a);
        }));
        c.add(new Caso("/accion/cerrar [sin verificacion]", "Cerrar sin verificacion (L401)", b("btnDetalleCH"), false, a -> {
            AccionChequeRequest r = accion(PEN, "CEF");
            r.setConVerificacion(false);
            return controlador.cerrar(r, a);
        }));
        c.add(new Caso("/accion/eliminar", "Eliminar accion (L427, esAutorizadoB)", b("btnEliminarSegCH"), false,
                a -> controlador.eliminarAccion(new FiltroIdDto(77), a)));
        // ---- traspaso
        c.add(new Caso("/traspaso/pendientes", "Traspaso (L101): cargarModalEntrega", b("btnTraspasoCH"), false,
                a -> controlador.pendientesDeTraspaso(new FiltroIdDto(SUC), a)));
        c.add(new Caso("/traspaso", "Generar (L1192): entregarCheqsCaja", b("btnTraspasoCH"), false,
                a -> controlador.traspasar(new FiltroIdDto(SUC), a)));
        c.add(new Caso("/reporte/traspaso", "Generar Pdf del traspaso (L1194)", b("btnTraspasoCH"), false,
                a -> controlador.reporteTraspaso(reporte(), a)));
        // ---- custodia
        c.add(new Caso("/custodia/cheques", "A Custodio (L103): cargarListaCustodios", b("btnCustodiaCH"), false,
                a -> controlador.sinCustodio(new FiltroIdDto(SUC), a)));
        CustodiaChequeRequest cu = new CustodiaChequeRequest();
        cu.setCodSucursal((long) SUC);
        cu.setCodEmpleado(12);
        cu.setCodCheques(Collections.singletonList(PEN));
        c.add(new Caso("/custodia", "saveCustodios (custodioModal)", b("btnCustodiaCH"), false, a -> controlador.custodia(cu, a)));
        c.add(new Caso("/dar-custodia/cheques", "Dar Custodia (L117): custodiaMad", b("btnCustodia2CH"), false,
                a -> controlador.chequesParaDarCustodia(new FiltroIdDto(SUC), a)));
        c.add(new Caso("/dar-custodia/entregas", "Dar Custodia: cargarListCustDia", b("btnCustodia2CH"), false,
                a -> controlador.entregasDelDia(sucursalYFecha(), a)));
        DarCustodiaRequest dc = new DarCustodiaRequest();
        dc.setCodSucursal((long) SUC);
        dc.setCodAccionOrigen(77);
        dc.setCodCheque(PEN);
        c.add(new Caso("/dar-custodia", "saveCustMad", b("btnCustodia2CH"), false, a -> controlador.darCustodia(dc, a)));
        // ---- reportes
        c.add(new Caso("/reporte/recibidos", "Reporte (L105)", b("btnRpt1CH"), false, a -> controlador.reporteRecibidos(reporte(), a)));
        c.add(new Caso("/reporte/cobranzas", "Reporte Cheques (L107)", b("btnRpt2CH"), false, a -> controlador.reporteCobranzas(reporte(), a)));
        c.add(new Caso("/reporte/custodio", "Reporte Custodio (L109)", b("btnRpt3CH"), false, a -> controlador.reporteCustodio(reporte(), a)));
        c.add(new Caso("/reporte/ultimo-recibo", "Recibo del Ultimo Cheque (L111)", b("btnRpt4CH"), false,
                a -> controlador.reporteUltimoRecibo(reporte(), a)));
        c.add(new Caso("/reporte/reimpresion-traspaso", "Imp Traspso (L115)", b("btnRpt5CH"), false,
                a -> controlador.reporteReimpresionTraspaso(reporte(), a)));
        c.add(new Caso("/traspaso/horas", "Imp Traspso: cargarListTrapDia", b("btnRpt5CH"), false,
                a -> controlador.horasDeTraspaso(sucursalYFecha(), a)));
        // ---- documento PDF del cheque: sin ACL de boton (L202, L206)
        c.add(new Caso("/pdf/estado", "Cargar/Descargar Documento PDF (L202, L206), sin ACL", b(), false,
                a -> controlador.pdfEstado(pdfRequest(), a)));
        c.add(new Caso("/pdf/descargar", "Descargar Documento PDF, sin ACL", b(), false,
                a -> controlador.pdfDescargar(pdfRequest(), a)));
        c.add(new Caso("/pdf/subir", "Cargar Documento PDF, sin ACL", b(), false, a -> controlador.pdfSubir("1", null, a)));
        return c;
    }

    private static bo.bosque.com.impexpap.dto.ChequePdfRequest pdfRequest() {
        bo.bosque.com.impexpap.dto.ChequePdfRequest r = new bo.bosque.com.impexpap.dto.ChequePdfRequest();
        r.setCodCheque(PEN);
        return r;
    }

    /** Resultado de una llamada: paso el ACL o lo cortaron (y con que excepcion). */
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
            return new Resultado(false, e);   // paso el ACL; otro error (de negocio o de dato de prueba)
        }
    }

    @ParameterizedTest(name = "{0}: cada endpoint exige el boton del legacy")
    @EnumSource(Perfil.class)
    @DisplayName("cada endpoint de /cheque responde 403 con el motivo cuando falta su boton, y pasa cuando lo tiene")
    void cadaEndpointExigeSuBoton(Perfil p) {
        Authentication a = auth(p);
        List<String> fallos = new ArrayList<>();
        for (Caso c : casos()) {
            Resultado r = correr(() -> c.llamada.apply(a));
            boolean debePasar = c.soloAdmin ? p.admin() : (c.botones.length == 0 || p.puedeAlguno(c.botones));
            if (debePasar && r.denegado) fallos.add(c + ": exige DE MAS (" + r.causa.getMessage() + ")");
            if (!debePasar && !r.denegado) fallos.add(c + ": exige DE MENOS (paso sin el boton)");
            if (!debePasar && r.denegado) {
                // 403 con motivo: SinPermisoException (el manejador responde SU mensaje), y el mensaje nombra un boton
                if (!(r.causa instanceof SinPermisoException)) {
                    fallos.add(c + ": el 403 no lleva motivo (" + r.causa.getClass().getSimpleName() + ")");
                } else if (!c.soloAdmin && !mencionaAlguno(r.causa.getMessage(), c.botones)) {
                    fallos.add(c + ": el 403 no nombra el boton que falta: " + r.causa.getMessage());
                }
            }
        }
        assertTrue(fallos.isEmpty(), p + " (" + p.v42 + "):\n  " + String.join("\n  ", fallos));
    }

    private static boolean mencionaAlguno(String mensaje, String[] botones) {
        for (String b : botones) if (mensaje != null && mensaje.contains(b)) return true;
        return false;
    }

    @Test
    @DisplayName("el administrador pasa en todos los endpoints, con cualquiera de los tres formularios y con el cheque abierto o cerrado")
    void adminPasaSiempre() {
        Authentication a = auth(Perfil.ADMIN);
        consultasAlGate = 0;
        for (Caso c : casos()) {
            Resultado r = correr(() -> c.llamada.apply(a));
            assertFalse(r.denegado, c + " no debe cortar al administrador: " + (r.causa == null ? "" : r.causa.getMessage()));
        }
        assertEquals(0, consultasAlGate, "el administrador no paga las consultas del gate");
    }

    @Test
    @DisplayName("las rutas de ChChequeController estan todas clasificadas aqui: un endpoint nuevo obliga a decidir su boton")
    void todasLasRutasEstanClasificadas() {
        Set<String> declaradas = new TreeSet<>();
        for (Method m : ChChequeController.class.getDeclaredMethods()) {
            PostMapping pm = m.getAnnotation(PostMapping.class);
            if (pm != null) declaradas.addAll(Arrays.asList(pm.value()));
        }
        Set<String> clasificadas = new TreeSet<>();
        for (Caso c : casos()) clasificadas.add(c.ruta.replaceAll(" \\[.*$", ""));
        assertEquals(declaradas, clasificadas);
    }

    @Test
    @DisplayName("sin ningun boton el usuario puede LISTAR y ver el documento PDF (como en el legacy), pero nada mas")
    void sinBotonesSoloListaYPdf() {
        Authentication a = auth(Perfil.SIN_BOTONES);
        List<String> pasan = new ArrayList<>();
        for (Caso c : casos()) {
            if (!correr(() -> c.llamada.apply(a)).denegado) pasan.add(c.ruta);
        }
        List<String> esperadas = new ArrayList<>();
        for (Caso c : casos()) if (c.sinBoton()) esperadas.add(c.ruta);
        assertEquals(esperadas, pasan);
    }

    // ===================================================================== //
    //                    3. BANCOS (vista 43) Y VERIFICAR (vista 77)        //
    // ===================================================================== //

    @ParameterizedTest(name = "{0}: bancos")
    @EnumSource(Perfil.class)
    @DisplayName("bancos: Nuevo=btnNuevoB, Editar=btnEditarB, Eliminar=btnEliminarB (banco.xhtml L23, L33, L36); los listados sin boton")
    void bancosPorBoton(Perfil p) {
        Authentication a = auth(p);
        ChBanco alta = banco(0);
        alta.setCodBanco(null);
        ChBanco edicion = banco(7);

        assertEquals(p.puede("btnNuevoB"), !correr(() -> bancos.registrar(alta, a)).denegado, p + " alta");
        assertEquals(p.puede("btnEditarB"), !correr(() -> bancos.registrar(edicion, a)).denegado, p + " edicion");
        assertEquals(p.puede("btnEliminarB"), !correr(() -> bancos.eliminar(new FiltroIdDto(7), a)).denegado, p + " baja");

        // Los listados no llevan Authentication ni boton: los ve cualquiera con el rol (como getListaBancos del legacy)
        bancos.listadoX();
        bancos.listadoBancosPlanilla();
        verify(bancosDao).listBancos();
        verify(bancosDao).listBancosPlanilla();
    }

    @Test
    @DisplayName("bancos: el 403 de una baja sin permiso lleva el motivo y el boton")
    void bancoSinPermisoExplica() {
        SinPermisoException e = assertThrows(SinPermisoException.class,
                () -> bancos.eliminar(new FiltroIdDto(7), auth(Perfil.P1)));
        assertTrue(e.getMessage().contains("btnEliminarB"), e.getMessage());
    }

    @Test
    @DisplayName("Verificar Cheques (vista 77) no tiene botones en tb_vistaBtn: ni el controlador ni el servicio dependen del ACL")
    void verificarChequesNoTieneAcl() {
        for (Class<?> clase : new Class<?>[] {ChVerificacionController.class, VerificacionChequeService.class}) {
            for (Field f : clase.getDeclaredFields()) {
                assertFalse(AccesoModuloHelper.class.isAssignableFrom(f.getType()) || IUsuarioBtn.class.isAssignableFrom(f.getType()),
                        clase.getSimpleName() + "." + f.getName() + " consulta el ACL: el legacy no lo hace");
            }
        }
        // y se protege por rol a nivel de clase, como el resto
        assertNotNull(ChVerificacionController.class.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class));
    }

    // ===================================================================== //
    //                       403 CON MOTIVO (lo que ve el usuario)           //
    // ===================================================================== //

    @Test
    @DisplayName("el 403 de un boton que falta responde con SU mensaje (no con el texto fijo) y nombra el boton")
    void elManejadorDevuelveElMotivo() {
        SinPermisoException e = assertThrows(SinPermisoException.class,
                () -> servicio.traspasar(auth(Perfil.P12), SUC));   // P12 no tiene btnTraspasoCH
        ResponseEntity<ApiResponse<?>> r = new GlobalExceptionHandler().handleSinPermisoException(e);
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertTrue(r.getBody().getMessage().contains("btnTraspasoCH"), r.getBody().getMessage());
        assertFalse(r.getBody().getMessage().contains("No tienes los permisos necesarios"), "ya no es el texto fijo");
    }

    /**
     * Escribe {@code target/matriz-permisos-backend.md}: endpoint x perfil, para pegar en PERMISOS_CHEQUES.md.
     * Solo con {@code -Dmatriz=true}. Una celda es {@code ✔} (pasa) o {@code 403} cuando coincide con el legacy y {@code DIF}
     * cuando no.
     */
    @Test
    @EnabledIfSystemProperty(named = "matriz", matches = "true")
    @DisplayName("imprime la matriz legacy vs backend (la que va en PERMISOS_CHEQUES.md)")
    void imprimeLaMatrizDelBackend() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("| Endpoint (componente del legacy)");
        for (Perfil p : Perfil.values()) sb.append(" | ").append(p == Perfil.SIN_BOTONES ? "SIN" : p == Perfil.ADMIN ? "ADM" : p.name());
        sb.append(" |\n|---");
        for (int i = 0; i < Perfil.values().length; i++) sb.append("|:-:");
        sb.append("|\n");
        int dif = 0;
        for (Caso c : casos()) {
            sb.append("| `").append(c.ruta).append("` · ").append(c.legacy);
            for (Perfil p : Perfil.values()) {
                Resultado r = correr(() -> c.llamada.apply(auth(p)));
                boolean debe = c.soloAdmin ? p.admin() : (c.botones.length == 0 || p.puedeAlguno(c.botones));
                String celda;
                if (r.denegado == !debe) {
                    celda = r.denegado ? "403" : "✔";
                } else {
                    celda = "DIF";
                    dif++;
                }
                sb.append(" | ").append(celda);
            }
            sb.append(" |\n");
        }
        java.nio.file.Path destino = java.nio.file.Paths.get("target", "matriz-permisos-backend.md");
        java.nio.file.Files.write(destino, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(0, dif, "celdas que discrepan del legacy (ver " + destino + ")");
    }

    // ===================================================================== //
    //          4. DIFERENCIAS: #1, #2, #2b y #2c corregidas (siempre corren);  //
    //          la #3 es decision de negocio y sigue omitida (-Ddiferencias=true) //
    //          Ver PERMISOS_CHEQUES.md, seccion "Diferencias".              //
    // ===================================================================== //

    @Test
    @DisplayName("#1 (corregida): un alta en la sucursal inicial del usuario (E) que no esta en su combo (C) se acepta, como en el legacy")
    void diferencia1_altaEnLaSucursalInicialFueraDelCombo() {
        // Usuario 49 de PRUEBA y produccion (perfil P1): p_list_Sucursal 'E' = 9 (empresa 1) y 'C' = [1].
        Perfil p = Perfil.P1;
        when(sucursales.sucursalInicial(eq(1), eq(p.codUsuario()), anyLong())).thenReturn(9L);
        when(sucursales.sucursalesDeEmpresa(1, p.codUsuario()))
                .thenReturn(Collections.singletonList(new SucursalChequeDto(1, "Central")));
        ChequeRegistroDto r = registro("ESTANDAR", null);
        r.setCodSucursal(9L);

        // Legacy: chmbEdit.setCodSucursal(this.codSucursal) con codSucursal = E = 9, sin mirar el combo (WizardCheque.java:636).
        servicio.registrar(auth(p), r);
        verify(cheques).alta(any(), any());
    }

    @Test
    @DisplayName("#2 (corregida): sin btnChqSucrs solo se escribe en la sucursal propia, como el combo deshabilitado del legacy")
    void diferencia2_escrituraEnSucursalAjenaSinBtnChqSucrs() {
        // P1 tiene btnTraspasoCH y NO btnChqSucrs; su sucursal inicial es SUC y pide traspasar la SUC_AJENA.
        Perfil p = Perfil.P1;
        when(sucursales.sucursalInicial(anyInt(), eq(p.codUsuario()), anyLong())).thenReturn((long) SUC);

        // En el legacy el valor del combo deshabilitado no viaja: la sucursal es la suya. Aqui deberia cortar igual que la lectura.
        assertThrows(AccessDeniedException.class, () -> servicio.traspasar(auth(p), SUC_AJENA),
                "traspasar acepto una sucursal que el usuario ni siquiera puede ver");
        assertThrows(AccessDeniedException.class, () -> servicio.pendientesDeTraspaso(auth(p), SUC_AJENA),
                "control: la lectura si corta (exigirSucursalLectura)");
    }

    @Test
    @DisplayName("#2b (corregida): sin btnChqSucrs el alta no acepta otra sucursal del combo C que no sea la inicial E")
    void diferencia2b_altaEnOtraSucursalDelComboSinBtnChqSucrs() {
        Perfil p = Perfil.P10;   // btnNuevoCH, sin btnChqSucrs
        when(sucursales.sucursalInicial(anyInt(), eq(p.codUsuario()), anyLong())).thenReturn((long) SUC);
        ChequeRegistroDto r = registro("ESTANDAR", null);
        r.setCodSucursal((long) SUC_AJENA);   // esta en el combo C, no es la suya

        assertThrows(AccessDeniedException.class, () -> servicio.registrar(auth(p), r));
    }

    @Test
    @DisplayName("#2c (corregida): con solo btnDetalleCH no se puede devolver ni cerrar un cheque de OTRA sucursal por su codCheque")
    void diferencia2c_cerrarUnChequeDeOtraSucursal() {
        Perfil p = Perfil.P8;   // solo btnDetalleCH, sin btnChqSucrs; su sucursal es la SUC
        ChequeFilaDto ajeno = cheque(3, "PEN");
        ajeno.setCodSucursal((long) SUC_AJENA);
        when(cheques.obtener(3)).thenReturn(ajeno);
        AccionChequeRequest r = accion(3, "COB");
        r.setConVerificacion(true);

        // En el legacy el cheque solo se alcanza desde la grilla de la sucursal del usuario.
        assertThrows(AccessDeniedException.class, () -> servicio.cerrar(auth(p), r),
                "cerro un cheque de una sucursal que el usuario no puede ver");
        verify(cheques, org.mockito.Mockito.never()).cambiarEstado(eq(3), anyString(), anyInt());
    }

    @Test
    @EnabledIfSystemProperty(named = "diferencias", matches = "true")
    @DisplayName("DIFERENCIA #3 (decision de negocio, NO aplicada): con btnChqSucrs el legacy solo deja trabajar las sucursales de la tabla de p_list_Sucursal 'E'; Spring deja cualquiera del combo")
    void diferencia3_btnChqSucrsYSucursalFueraDeLaTablaE() {
        // Usuarios 26 y 36 (perfiles P11 y P3): la sucursal 2 sale en el combo C pero no esta en su tabla E, y el legacy la
        // devuelve a 0 en cada dibujo del combo (WizardCheque.getListaSucursales, lineas 406-408).
        Perfil p = Perfil.P11;
        when(sucursales.sucursalInicial(anyInt(), eq(p.codUsuario()), eq(2L))).thenReturn(0L);   // E con previa=2: no esta
        when(sucursales.sucursalInicial(anyInt(), eq(p.codUsuario()), eq(0L))).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> servicio.sinCustodio(auth(p), 2),
                "btnChqSucrs no debe abrir sucursales que E no concede");
    }
}
