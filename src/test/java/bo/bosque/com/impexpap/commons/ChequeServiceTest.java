package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChAccion;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IPersonalCheque;
import bo.bosque.com.impexpap.dao.ITalonario;
import bo.bosque.com.impexpap.dao.ISucursalCheque;
import bo.bosque.com.impexpap.dto.AccionChequeRequest;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequeFiltroDto;
import bo.bosque.com.impexpap.dto.ChequePaginaDto;
import bo.bosque.com.impexpap.dto.ChequeRegistroDto;
import bo.bosque.com.impexpap.dto.CustodiaChequeRequest;
import bo.bosque.com.impexpap.dto.DarCustodiaRequest;
import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.dto.TalonarioValidacionDto;
import bo.bosque.com.impexpap.dto.TalonarioValidacionRequest;
import bo.bosque.com.impexpap.model.ChAccion;
import bo.bosque.com.impexpap.model.ChCheque;
import bo.bosque.com.impexpap.model.Talonario;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * El servicio de cheques con los DAO simulados: lo que se prueba son las REGLAS (permisos, rama K,
 * campos de cada formulario, paridad con el legacy), no el SQL.
 *
 * <p>Fecha fija: 2026-10-02 10:00 (America/La_Paz). Usuario 7, empresa 1, rol ROLE_LIM salvo que el
 * caso diga admin. El ACL se simula con {@code AccesoModuloHelper}: por defecto el usuario no tiene
 * ningun boton.
 */
class ChequeServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Clock RELOJ = Clock.fixed(
            LocalDate.of(2026, 10, 2).atTime(10, 0).atZone(ZONA).toInstant(), ZONA);
    private static final int SUC = 3;

    private IChCheque cheques;
    private IChAccion acciones;
    private ISucursalCheque sucursales;
    private IPersonalCheque personal;
    private ITalonario talonarios;
    private AccesoModuloHelper acceso;
    private ChequeService servicio;

    private Authentication usuario;
    private Authentication admin;

    @BeforeEach
    void preparar() {
        cheques = mock(IChCheque.class);
        acciones = mock(IChAccion.class);
        sucursales = mock(ISucursalCheque.class);
        acceso = mock(AccesoModuloHelper.class);
        personal = mock(IPersonalCheque.class);
        talonarios = mock(ITalonario.class);
        servicio = new ChequeService(cheques, acciones, sucursales, personal, talonarios, acceso, RELOJ);

        usuario = token("ROLE_LIM");
        admin = token("ROLE_ADM");
        when(acceso.esAdmin(admin)).thenReturn(true);

        // El combo de la pantalla: dos empresas, y las sucursales que el usuario 7 ve en cada una.
        when(sucursales.empresasDeCheques()).thenReturn(Arrays.asList(
                new EmpresaChequeDto(1, "IMPEXPAP"), new EmpresaChequeDto(5, "ESPPAPEL")));
        when(sucursales.sucursalesDeEmpresa(1, 7)).thenReturn(Arrays.asList(
                new SucursalChequeDto(1, "Central"), new SucursalChequeDto(SUC, "Sucursal 6 Cochabamba")));
        when(sucursales.sucursalesDeEmpresa(5, 7)).thenReturn(Collections.singletonList(
                new SucursalChequeDto(13, "Central")));

        // Su sucursal inicial ('E') en la empresa 1: sin btnChqSucrs el combo del legacy esta deshabilitado y solo se opera esa
        // (auditoria de permisos, diferencias #2/#2b/#2c: las escrituras tambien exigen poder ver la sucursal).
        when(sucursales.sucursalInicial(eq(1), eq(7), anyLong())).thenReturn((long) SUC);
        when(sucursales.sucursalInicial(eq(5), eq(7), anyLong())).thenReturn(13L);

        // Los SP devuelven el id generado.
        when(cheques.alta(any(), any())).thenReturn(new RespuestaSp(0, "", 501L));
        when(cheques.actualizar(any(), any())).thenReturn(new RespuestaSp(0, "", 0L));
        when(acciones.registrar(any())).thenReturn(new RespuestaSp(0, "", 900L));
        when(acciones.copiar(anyInt(), anyInt(), anyInt())).thenReturn(new RespuestaSp(0, "", 901L));
    }

    // ------------------------------------------------------------------ utilidades

    private static Authentication token(String rol) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority(rol)));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        return t;
    }

    private static Date d(int anio, int mes, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(anio, mes, dia));
    }

    /** El usuario tiene estos botones de la vista 42. */
    private void conBotones(String... botones) {
        for (String b : botones) {
            when(acceso.tieneBoton(usuario, ChequeService.VISTA_CHEQUES, b)).thenReturn(true);
        }
        // exigirBoton: corta con 403 si falta (como el real). Los que tiene, pasan.
        doThrow(new AccessDeniedException("sin boton")).when(acceso).exigirBoton(any(), anyInt(), anyString());
        for (String b : botones) {
            org.mockito.Mockito.doNothing().when(acceso).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, b);
        }
    }

    private void puedeEscribirEnSucursal() {
        when(sucursales.puedeEscribir(eq((long) SUC), eq(7))).thenReturn(true);
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
        c.setObservacion("recibido en caja");
        return c;
    }

    private static ChequeRegistroDto altaValida() {
        ChequeRegistroDto r = new ChequeRegistroDto();
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

    private void cheque(ChequeFilaDto c) {
        when(cheques.obtener(c.getCodCheque())).thenReturn(c);
    }

    private static String mensaje(Throwable t) {
        return t.getMessage();
    }

    // ------------------------------------------------------------------ ALTA

    @Test
    @DisplayName("alta estandar: estado PEN, cobro = fecha del cheque, sin ceros, entregado por cliente = 0, usuario del token")
    void altaEstandar() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();

        long id = servicio.registrar(usuario, altaValida());

        assertEquals(501L, id);
        ArgumentCaptor<ChCheque> c = ArgumentCaptor.forClass(ChCheque.class);
        verify(cheques).alta(c.capture(), eq("recibido"));
        assertEquals("PEN", c.getValue().getEstado());
        assertEquals("123", c.getValue().getNrocheque(), "se quitan los ceros a la izquierda");
        assertEquals(d(2026, 10, 1), c.getValue().getFechaCobrar(), "Fecha Cobrar se llena con la del cheque");
        assertEquals(0, c.getValue().getCodEmpleado().intValue());
        assertEquals(7, c.getValue().getAudUsuario().intValue(), "el usuario sale del token");
        assertEquals(SUC, c.getValue().getCodSucursal().longValue());
    }

    @Test
    @DisplayName("alta: en el formulario estandar la fecha de cobro del cuerpo se ignora")
    void altaIgnoraFechaCobroDelCuerpo() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setFechaCobrar(d(2026, 12, 31));   // el campo esta deshabilitado en el JSF

        servicio.registrar(usuario, r);

        ArgumentCaptor<ChCheque> c = ArgumentCaptor.forClass(ChCheque.class);
        verify(cheques).alta(c.capture(), any());
        assertEquals(d(2026, 10, 1), c.getValue().getFechaCobrar());
    }

    @Test
    @DisplayName("alta sin btnNuevoCH: 403 y no escribe")
    void altaSinBoton() {
        conBotones();   // ninguno
        puedeEscribirEnSucursal();
        assertThrows(AccessDeniedException.class, () -> servicio.registrar(usuario, altaValida()));
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("alta en sucursal sin permiso: mensaje del legacy y no escribe")
    void altaSinPermisoDeSucursal() {
        conBotones("btnNuevoCH");
        when(sucursales.puedeEscribir(anyLong(), anyInt())).thenReturn(false);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, altaValida()));
        assertEquals(MensajesCheque.SUCURSAL_AJENA, e.getMessage());
        assertTrue(e.getMessage().contains("otra sucursal"), "dice que es OTRA sucursal y que hacer");
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("el administrador escribe en cualquier sucursal sin consultar el SP")
    void adminNoConsultaSucursal() {
        // admin: exigirBoton no corta (el helper real da paso al admin)
        servicio.registrar(admin, altaValida());
        verify(sucursales, never()).puedeEscribir(anyLong(), anyInt());
        verify(cheques).alta(any(), any());
    }

    // ------------------------------------------------------------------ MENSAJES QUE EXPLICAN

    private static Talonario talonarioDe(String nro, int empresa, int inicial, int fin) {
        Talonario t = new Talonario();
        t.setNroTalonario(nro);
        t.setCodEmpresa(empresa);
        t.setNumeracionInicial(inicial);
        t.setNumeracionFinal(fin);
        return t;
    }

    /** Un alta en ESPPAPEL (empresa 5, sucursal 13) con un recibo y un talonario manuales, entregado por un empleado. */
    private ChequeRegistroDto altaEnEsppapelConTalonario(String talonario, String recibo) {
        conBotones("btnNuevoCH");
        when(sucursales.puedeEscribir(eq(13L), eq(7))).thenReturn(true);
        ChequeRegistroDto r = altaValida();
        r.setCodEmpresa(5);
        r.setCodSucursal(13L);
        r.setCodEmpleado(4);
        r.setNroTalonario(talonario);
        r.setReciboManual(recibo);
        return r;
    }

    @Test
    @DisplayName("EL CASO REPORTADO: talonario ER1076 (de IMPEXPAP) en un cheque de ESPPAPEL: el rechazo dice por que y que hacer")
    void talonarioDeOtraEmpresaSeExplica() {
        ChequeRegistroDto r = altaEnEsppapelConTalonario("ER1076", "3752");
        when(talonarios.buscarPorNroTalonario("ER1076")).thenReturn(
                Collections.singletonList(talonarioDe("ER1076", 1, 3751, 3800)));
        // cheques.talonarioYReciboValidos(..., 5) -> false (la rama M no lo encuentra para ESPPAPEL)

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));

        assertTrue(e.getMessage().contains("no pertenece a la empresa ESPPAPEL"), e.getMessage());
        assertTrue(e.getMessage().contains("está registrado en la empresa IMPEXPAP"), e.getMessage());
        assertTrue(e.getMessage().contains("Cambia la empresa en los filtros"), e.getMessage());
        assertFalse(e.getMessage().contains("Verifique"), "ya no es el mensaje ambiguo del legacy");
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("el recibo fuera de la numeracion del talonario dice cual es la numeracion")
    void reciboFueraDeNumeracionSeExplica() {
        ChequeRegistroDto r = altaEnEsppapelConTalonario("ER2000", "9");
        when(talonarios.buscarPorNroTalonario("ER2000")).thenReturn(
                Collections.singletonList(talonarioDe("ER2000", 5, 100, 150)));

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(e.getMessage().contains("El recibo 9 no pertenece al talonario «ER2000»"), e.getMessage());
        assertTrue(e.getMessage().contains("del 100 al 150"), e.getMessage());
    }

    @Test
    @DisplayName("si no se puede consultar el modulo de talonarios para explicar, el rechazo se mantiene con un mensaje generico")
    void fallaLaExplicacionPeroSigueRechazando() {
        ChequeRegistroDto r = altaEnEsppapelConTalonario("ER1076", "3752");
        when(talonarios.buscarPorNroTalonario(anyString())).thenThrow(new RuntimeException("el procedimiento no responde"));

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(e.getMessage().contains("no coinciden con ningún talonario registrado para la empresa ESPPAPEL"), e.getMessage());
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("si el par talonario/recibo es valido para la empresa, no se consulta el modulo de talonarios para nada")
    void talonarioValidoNoExplicaNada() {
        ChequeRegistroDto r = altaEnEsppapelConTalonario("ER2000", "120");
        when(cheques.talonarioYReciboValidos("ER2000", "120", 5)).thenReturn(true);

        assertEquals(501L, servicio.registrar(usuario, r));
        verify(talonarios, never()).buscarPorNroTalonario(anyString());
    }

    @Test
    @DisplayName("sin el boton de registrar: 403 CON motivo (cual boton falta y que hacer), no el texto fijo")
    void altaSinBotonDiceCualFalta() {
        conBotones();   // ninguno
        puedeEscribirEnSucursal();

        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, altaValida()));
        assertTrue(e.getMessage().contains("registrar cheques") && e.getMessage().contains("btnNuevoCH"), e.getMessage());
        assertTrue(e.getMessage().contains("pídele al administrador"), e.getMessage());
    }

    @Test
    @DisplayName("editar un cheque CERRADO con el formulario estandar: dice que esta cerrado y que solo se corrigen talonario y recibo")
    void editarCerradoConFormularioEstandar() {
        conBotones("btnEditar1CH");
        cheque(cheque(40, "CER"));
        ChequeRegistroDto r = altaValida();
        r.setCodCheque(40);
        r.setModo("ESTANDAR");

        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, r));
        assertTrue(e.getMessage().contains("está cerrado") && e.getMessage().contains("talonario y el recibo"), e.getMessage());
    }

    @Test
    @DisplayName("el formulario de talonario en un cheque ABIERTO: dice que es solo para cerrados")
    void talonarioEnUnoAbierto() {
        conBotones("btnEditar1CH");
        cheque(cheque(40, "PEN"));
        ChequeRegistroDto r = altaValida();
        r.setCodCheque(40);
        r.setModo("TALONARIO");

        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, r));
        assertTrue(e.getMessage().contains("solo para cheques cerrados") && e.getMessage().contains("«Editar»"), e.getMessage());
    }

    @Test
    @DisplayName("ver otra sucursal sin btnChqSucrs: 403 que dice que solo puedes ver la tuya y que permiso pedir")
    void verOtraSucursalDiceQuePedir() {
        conBotones();
        when(sucursales.sucursalInicial(1, 7, 0L)).thenReturn((long) SUC);
        ChequeFiltroDto ajena = new ChequeFiltroDto();
        ajena.setCodSucursal(9L);

        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.listar(usuario, ajena));
        assertEquals(MensajesCheque.SOLO_SU_SUCURSAL, e.getMessage());
        assertTrue(e.getMessage().contains("btnChqSucrs"));
    }

    @Test
    @DisplayName("el alta rechaza un recibo con simbolos diciendo cual simbolo sobra (con el valor escrito)")
    void formatoDelReciboNombraElSimbolo() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setCodEmpleado(4);
        r.setNroTalonario("ER1");
        r.setReciboManual("37-52");

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(e.getMessage().contains("El recibo manual «37-52» no es válido"), e.getMessage());
        assertTrue(e.getMessage().contains("(«-»)"), e.getMessage());
    }

    // ------------------------------------------------------------------ COMPROBAR EL TALONARIO MIENTRAS SE ESCRIBE

    private static TalonarioValidacionRequest paraComprobar(int empresa, String talonario, String recibo) {
        TalonarioValidacionRequest r = new TalonarioValidacionRequest();
        r.setCodEmpresa(empresa);
        r.setNroTalonario(talonario);
        r.setReciboManual(recibo);
        return r;
    }

    @Test
    @DisplayName("comprobar: un par correcto es valido y dice a que talonario y rango pertenece")
    void comprobarParCorrecto() {
        conBotones("btnNuevoCH");
        when(cheques.talonarioYReciboValidos("ER1076", "3752", 1)).thenReturn(true);
        when(talonarios.buscarPorNroTalonario("ER1076")).thenReturn(
                Collections.singletonList(talonarioDe("ER1076", 1, 3751, 3800)));

        TalonarioValidacionDto v = servicio.validarTalonario(usuario, paraComprobar(1, "ER1076", "3752"));

        assertTrue(v.isValido());
        assertNull(v.getMensaje());
        assertEquals("Talonario ER1076 (IMPEXPAP): recibos del 3751 al 3800.", v.getDetalle());
    }

    @Test
    @DisplayName("comprobar EL CASO REPORTADO: ER1076 en ESPPAPEL no es valido y el mensaje dice a que empresa pertenece")
    void comprobarElCasoReportado() {
        conBotones("btnNuevoCH");
        when(talonarios.buscarPorNroTalonario("ER1076")).thenReturn(
                Collections.singletonList(talonarioDe("ER1076", 1, 3751, 3800)));

        TalonarioValidacionDto v = servicio.validarTalonario(usuario, paraComprobar(5, "ER1076", "3752"));

        assertFalse(v.isValido());
        assertTrue(v.getMensaje().contains("no pertenece a la empresa ESPPAPEL") && v.getMensaje().contains("IMPEXPAP"), v.getMensaje());
        assertNull(v.getDetalle());
    }

    @Test
    @DisplayName("comprobar: es el MISMO mensaje que dara el guardado (un solo texto para los dos caminos)")
    void comprobarYGuardarDicenLoMismo() {
        ChequeRegistroDto alta = altaEnEsppapelConTalonario("ER1076", "3752");
        when(talonarios.buscarPorNroTalonario("ER1076")).thenReturn(
                Collections.singletonList(talonarioDe("ER1076", 1, 3751, 3800)));

        String alGuardar = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, alta)).getMessage();
        String alEscribir = servicio.validarTalonario(usuario, paraComprobar(5, "ER1076", "3752")).getMensaje();

        assertTrue(alGuardar.contains(alEscribir), "el aviso temprano coincide con el rechazo:\n" + alEscribir + "\n---\n" + alGuardar);
    }

    @Test
    @DisplayName("comprobar: talonario o recibo vacios o en 0 no consultan la base y dan valido (lo demas lo cubren las reglas de 'Entregado por')")
    void comprobarSinNadaQueComprobar() {
        conBotones("btnNuevoCH");
        for (String[] par : new String[][] {{"0", "3752"}, {"ER1076", "0"}, {"0", "0"}, {"", "3752"}, {"ER1076", ""}, {null, null}, {"  ", "  "}}) {
            TalonarioValidacionDto v = servicio.validarTalonario(usuario, paraComprobar(1, par[0], par[1]));
            assertTrue(v.isValido(), "par " + Arrays.toString(par));
            assertNull(v.getMensaje());
            assertNull(v.getDetalle());
        }
        verify(cheques, never()).talonarioYReciboValidos(anyString(), anyString(), anyInt());
        verify(talonarios, never()).buscarPorNroTalonario(anyString());
    }

    @Test
    @DisplayName("comprobar: un formato invalido se explica sin consultar la base")
    void comprobarFormatoInvalido() {
        conBotones("btnNuevoCH");

        TalonarioValidacionDto v = servicio.validarTalonario(usuario, paraComprobar(1, "ER-1076", "37-52"));

        assertFalse(v.isValido());
        assertTrue(v.getMensaje().contains("El talonario manual «ER-1076» no es válido"), v.getMensaje());
        assertTrue(v.getMensaje().contains("El recibo manual «37-52» no es válido"), "las dos lineas: " + v.getMensaje());
        assertEquals(2, v.getMensaje().split("\n").length);
        verify(cheques, never()).talonarioYReciboValidos(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("comprobar: un recibo con letras no se puede comprobar contra el talonario y se dice")
    void comprobarReciboConLetras() {
        conBotones("btnNuevoCH");
        TalonarioValidacionDto v = servicio.validarTalonario(usuario, paraComprobar(1, "ER1076", "37A2"));
        assertFalse(v.isValido());
        assertTrue(v.getMensaje().contains("debe ser un número"), v.getMensaje());
    }

    @Test
    @DisplayName("comprobar: recorta los espacios, igual que el formulario al guardar")
    void comprobarRecortaEspacios() {
        conBotones("btnNuevoCH");
        when(cheques.talonarioYReciboValidos("ER1076", "3752", 1)).thenReturn(true);

        assertTrue(servicio.validarTalonario(usuario, paraComprobar(1, "  ER1076 ", " 3752  ")).isValido());
        verify(cheques).talonarioYReciboValidos("ER1076", "3752", 1);
    }

    @Test
    @DisplayName("comprobar: si falla la consulta del detalle, el par igual es valido (el detalle es un extra)")
    void comprobarSinDetalle() {
        conBotones("btnNuevoCH");
        when(cheques.talonarioYReciboValidos("ER1076", "3752", 1)).thenReturn(true);
        when(talonarios.buscarPorNroTalonario(anyString())).thenThrow(new RuntimeException("no responde"));

        TalonarioValidacionDto v = servicio.validarTalonario(usuario, paraComprobar(1, "ER1076", "3752"));
        assertTrue(v.isValido());
        assertNull(v.getDetalle());
    }

    @Test
    @DisplayName("comprobar: la empresa es obligatoria y tiene que ser una del combo (la del login, 6, no vale)")
    void comprobarEmpresa() {
        conBotones("btnNuevoCH");
        assertThrows(SpBusinessException.class, () -> servicio.validarTalonario(usuario, paraComprobar(6, "ER1076", "3752")));
        TalonarioValidacionRequest sinEmpresa = paraComprobar(1, "ER1076", "3752");
        sinEmpresa.setCodEmpresa(null);
        assertTrue(assertThrows(SpBusinessException.class, () -> servicio.validarTalonario(usuario, sinEmpresa))
                .getMessage().contains("empresa"));
        assertThrows(SpBusinessException.class, () -> servicio.validarTalonario(usuario, null));
    }

    @Test
    @DisplayName("comprobar: quien registra o edita puede, con cualquiera de los 4 botones del formulario")
    void comprobarPermiso() {
        for (String boton : new String[] {"btnNuevoCH", "btnNuevo2CH", "btnEditar1CH", "btnEditar3CH"}) {
            conBotones(boton);
            assertTrue(servicio.validarTalonario(usuario, paraComprobar(1, "0", "0")).isValido(), boton);
        }
    }

    @Test
    @DisplayName("comprobar: sin ninguno de esos botones, 403 con motivo y no consulta nada")
    void comprobarSinPermiso() {
        conBotones();
        SinPermisoException e = assertThrows(SinPermisoException.class,
                () -> servicio.validarTalonario(usuario, paraComprobar(1, "ER1076", "3752")));
        assertTrue(e.getMessage().contains("registrar o editar cheques"), e.getMessage());
        verify(cheques, never()).talonarioYReciboValidos(anyString(), anyString(), anyInt());
    }

    // ------------------------------------------------------------------ EMPRESA (combo de la pantalla)

    /** El usuario 7, pero con la empresa que el login resolvio para su cargo (no la de la pantalla). */
    private static Authentication conEmpresaDeLogin(int codEmpresa) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, codEmpresa, "lim"));
        return t;
    }

    @Test
    @DisplayName("empresaEfectiva: la pedida; sin pedir, la primera del combo; sin empresas, error")
    void empresaEfectiva() {
        assertEquals(5, servicio.empresaEfectiva(5));
        assertEquals(1, servicio.empresaEfectiva(null));
        assertEquals(1, servicio.empresaEfectiva(0), "0 = no pidio ninguna");

        when(sucursales.empresasDeCheques()).thenReturn(new ArrayList<EmpresaChequeDto>());
        assertThrows(SpBusinessException.class, () -> servicio.empresaEfectiva(null));
    }

    @Test
    @DisplayName("el login de mjaimes resuelve la empresa 6 (GENERAL): la pantalla NO la usa, trabaja con la primera del combo")
    void laEmpresaDelLoginNoSeUsa() {
        Authentication mjaimes = conEmpresaDeLogin(6);
        when(sucursales.sucursalInicial(1, 7, 0L)).thenReturn((long) SUC);

        assertEquals(SUC, servicio.sucursalInicial(mjaimes, null));
        verify(sucursales).sucursalInicial(1, 7, 0L);
        verify(sucursales, never()).sucursalInicial(eq(6), anyInt(), anyLong());

        servicio.sucursales(mjaimes, null);
        verify(sucursales).sucursalesDeEmpresa(1, 7);
        verify(sucursales, never()).sucursalesDeEmpresa(eq(6), anyInt());
    }

    @Test
    @DisplayName("la sucursal inicial y las sucursales se piden para la empresa elegida en la pantalla")
    void sucursalesDeLaEmpresaElegida() {
        when(sucursales.sucursalInicial(5, 7, 0L)).thenReturn(13L);
        assertEquals(13L, servicio.sucursalInicial(usuario, 5));

        List<SucursalChequeDto> lista = servicio.sucursales(usuario, 5);
        assertEquals(1, lista.size());
        assertEquals(13, lista.get(0).getCodSucursal());
    }

    @Test
    @DisplayName("alta en la otra empresa (ESPPAPEL) con una sucursal de ESA empresa: se registra")
    void altaEnLaOtraEmpresa() {
        conBotones("btnNuevoCH");
        when(sucursales.puedeEscribir(eq(13L), eq(7))).thenReturn(true);
        ChequeRegistroDto r = altaValida();
        r.setCodEmpresa(5);
        r.setCodSucursal(13L);

        assertEquals(501L, servicio.registrar(usuario, r));
        ArgumentCaptor<ChCheque> c = ArgumentCaptor.forClass(ChCheque.class);
        verify(cheques).alta(c.capture(), any());
        assertEquals(5, c.getValue().getCodEmpresa().intValue());
        assertEquals(13L, c.getValue().getCodSucursal().longValue());
    }

    @Test
    @DisplayName("alta sin empresa: se rechaza y no escribe")
    void altaSinEmpresa() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setCodEmpresa(null);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(mensaje(e).contains("empresa"));
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("alta en una empresa que no es del combo (6, GENERAL): se rechaza aunque el login la traiga")
    void altaEnEmpresaNoHabilitada() {
        Authentication mjaimes = conEmpresaDeLogin(6);
        when(acceso.tieneBoton(mjaimes, ChequeService.VISTA_CHEQUES, "btnNuevoCH")).thenReturn(true);
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setCodEmpresa(6);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(mjaimes, r));
        assertTrue(mensaje(e).contains("no está habilitada"));
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("alta con una sucursal que no es de la empresa elegida: se rechaza y no escribe")
    void altaSucursalDeOtraEmpresa() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();            // la 3 es de la empresa 1
        ChequeRegistroDto r = altaValida();
        r.setCodEmpresa(5);                   // ... pero se dice empresa 5

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(mensaje(e).contains("no corresponde"));
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("el administrador tampoco se salta el combo: empresa y sucursal tienen que ser coherentes")
    void adminTambienValidaElCombo() {
        ChequeRegistroDto r = altaValida();
        r.setCodEmpresa(5);
        assertThrows(SpBusinessException.class, () -> servicio.registrar(admin, r));
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("listar: la 'sucursal propia' se mira en cada empresa del combo (la inicial en la empresa 5 tambien vale)")
    void listarPropiaEnLaOtraEmpresa() {
        conBotones();
        when(sucursales.sucursalInicial(1, 7, 0L)).thenReturn(0L);
        when(sucursales.sucursalInicial(5, 7, 0L)).thenReturn(13L);
        when(cheques.listar(any())).thenReturn(filas(1));

        ChequeFiltroDto enLaCinco = new ChequeFiltroDto();
        enLaCinco.setCodSucursal(13L);
        assertEquals(1, servicio.listar(usuario, enLaCinco).getTotal());

        ChequeFiltroDto ajena = new ChequeFiltroDto();
        ajena.setCodSucursal(9L);
        assertThrows(AccessDeniedException.class, () -> servicio.listar(usuario, ajena));
    }

    @Test
    @DisplayName("alta duplicada (mismo nro, cliente, banco, sucursal y fecha): 'ya está registrado'")
    void altaDuplicada() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        when(cheques.existeDuplicado(eq("123"), eq("C001"), eq(2), eq((long) SUC), any())).thenReturn(true);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, altaValida()));
        assertTrue(mensaje(e).contains("ya está registrado"));
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("alta: los mensajes de recibo, talonario y duplicado se acumulan")
    void altaAcumulaMensajes() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setReciboManual("55");      // cliente con recibo  -> regla 1; recibo sin talonario -> regla 3
        r.setNroTalonario("0");
        when(cheques.existeDuplicado(anyString(), anyString(), anyInt(), anyLong(), any())).thenReturn(true);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(mensaje(e).contains("Cliente"));
        assertTrue(mensaje(e).contains("talonario"));
        assertTrue(mensaje(e).contains("ya está registrado"));
    }

    @Test
    @DisplayName("alta con recibo y talonario: se consulta tmto_talonario y si no coincide se rechaza")
    void altaTalonarioInvalido() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setCodEmpleado(9);
        r.setReciboManual("55");
        r.setNroTalonario("T1");
        when(cheques.talonarioYReciboValidos("T1", "55", 1)).thenReturn(false);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(mensaje(e).contains("No existe ningún talonario"), mensaje(e));
    }

    @Test
    @DisplayName("alta con empleado, recibo y talonario validos: se guarda")
    void altaConEmpleado() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setCodEmpleado(9);
        r.setReciboManual("55");
        r.setNroTalonario("T1");
        when(cheques.talonarioYReciboValidos("T1", "55", 1)).thenReturn(true);

        servicio.registrar(usuario, r);
        verify(cheques).alta(any(), any());
    }

    @Test
    @DisplayName("alta: Nro de cheque solo ceros se rechaza (el legacy reventaba)")
    void altaNroSoloCeros() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setNrocheque("000");
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(mensaje(e).contains("solo ceros"));
    }

    @Test
    @DisplayName("alta: Nro de cheque con letras, a la orden con numeros y campos vacios se rechazan antes de la logica")
    void altaFormatos() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setNrocheque("AB-1");
        r.setaOrdenDe("Juan 123");
        r.setNroTalonario("");
        r.setMonto(null);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(mensaje(e).contains("número de cheque"));
        assertTrue(mensaje(e).contains("A la orden de"));
        assertTrue(mensaje(e).contains("talonario manual"));
        assertTrue(mensaje(e).contains("monto"));
        verify(cheques, never()).existeDuplicado(any(), any(), anyInt(), anyLong(), any());
    }

    @Test
    @DisplayName("alta: tipo o moneda fuera del catalogo se rechazan")
    void altaCatalogos() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setTipo("XXX");
        r.setMoneda("EUR");
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(mensaje(e).contains("tipo de cheque"));
        assertTrue(mensaje(e).contains("moneda"));
    }

    @Test
    @DisplayName("alta administrador: usa btnNuevo2CH y respeta la fecha de cobro dentro de +-28 dias")
    void altaAdminForm() {
        conBotones("btnNuevo2CH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setModo("ADMIN");
        r.setFechaCobrar(d(2026, 10, 20));

        servicio.registrar(usuario, r);

        ArgumentCaptor<ChCheque> c = ArgumentCaptor.forClass(ChCheque.class);
        verify(cheques).alta(c.capture(), any());
        assertEquals(d(2026, 10, 20), c.getValue().getFechaCobrar());
    }

    @Test
    @DisplayName("alta administrador con cobro a mas de 28 dias: 'Fechas Cobro Fuera de Rango'")
    void altaAdminFueraDeRango() {
        conBotones("btnNuevo2CH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setModo("ADMIN");
        r.setFechaCobrar(d(2026, 12, 25));

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(usuario, r));
        assertTrue(e.getMessage().contains("fecha de cobro") && e.getMessage().contains("28 días"), e.getMessage());
    }

    @Test
    @DisplayName("un cheque nuevo no se registra en modo talonario")
    void altaTalonarioNo() {
        ChequeRegistroDto r = altaValida();
        r.setModo("TALONARIO");
        assertThrows(SpBusinessException.class, () -> servicio.registrar(admin, r));
    }

    // ------------------------------------------------------------------ EDICION

    @Test
    @DisplayName("edicion estandar: monto, tipo, moneda, a la orden, entregado por y fechas NO cambian aunque el cuerpo los traiga")
    void edicionEstandarBloqueaCampos() {
        conBotones("btnEditar1CH");
        puedeEscribirEnSucursal();
        cheque(cheque(40, "PEN"));

        ChequeRegistroDto r = altaValida();
        r.setCodCheque(40);
        r.setMonto(999999.0);
        r.setTipo("RES");
        r.setMoneda("SUS");
        r.setaOrdenDe("Otro Nombre");
        r.setCodEmpleado(12);
        r.setFechaCheque(d(2026, 1, 1));
        r.setFechaCobrar(d(2026, 1, 1));
        r.setNrocheque("7788");
        r.setCodCliente("C777");
        r.setCodBanco(5);
        r.setObservacion("nueva obs");

        servicio.registrar(usuario, r);

        ArgumentCaptor<ChCheque> c = ArgumentCaptor.forClass(ChCheque.class);
        verify(cheques).actualizar(c.capture(), eq("nueva obs"));
        ChCheque g = c.getValue();
        assertEquals(1000.5, g.getMonto(), 0.0001, "monto bloqueado");
        assertEquals("PAG", g.getTipo());
        assertEquals("BS", g.getMoneda());
        assertEquals("IMPEXPAP SRL", g.getaOrdenDe());
        assertEquals(0, g.getCodEmpleado().intValue());
        assertEquals(d(2026, 9, 20), g.getFechaCheque());
        assertEquals(d(2026, 9, 25), g.getFechaCobrar());
        // lo que SI se edita
        assertEquals("7788", g.getNrocheque());
        assertEquals("C777", g.getCodCliente());
        assertEquals(5, g.getCodBanco().intValue());
        // y el estado y la empresa se conservan
        assertEquals("PEN", g.getEstado());
        assertEquals(1, g.getCodEmpresa().intValue());
        verify(cheques, never()).alta(any(), any());
    }

    @Test
    @DisplayName("edicion: una observacion ausente conserva la de la accion REC")
    void edicionObservacionAusente() {
        conBotones("btnEditar1CH");
        puedeEscribirEnSucursal();
        cheque(cheque(40, "PEN"));
        ChequeRegistroDto r = altaValida();
        r.setCodCheque(40);
        r.setObservacion(null);

        servicio.registrar(usuario, r);
        verify(cheques).actualizar(any(), eq("recibido en caja"));
    }

    @Test
    @DisplayName("edicion estandar de un cheque CERRADO sin ser admin: 403 (esa es la edicion de talonario)")
    void edicionEstandarCerradoNo() {
        conBotones("btnEditar1CH");
        puedeEscribirEnSucursal();
        cheque(cheque(40, "CER"));
        ChequeRegistroDto r = altaValida();
        r.setCodCheque(40);

        assertThrows(AccessDeniedException.class, () -> servicio.registrar(usuario, r));
        verify(cheques, never()).actualizar(any(), any());
    }

    @Test
    @DisplayName("talonario de un cheque CERRADO: solo cambian talonario y recibo, y no se exige el rango de fechas")
    void talonarioCerrado() {
        conBotones("btnEditar1CH");
        puedeEscribirEnSucursal();
        ChequeFilaDto c = cheque(40, "CER");
        c.setFechaCobrar(d(2027, 6, 1));   // fuera de +-28 dias: en modo talonario no se mira
        c.setCodEmpleado(4);               // lo trajo un empleado, asi que el recibo puede ser distinto de 0
        cheque(c);
        when(cheques.talonarioYReciboValidos("T9", "88", 1)).thenReturn(true);

        ChequeRegistroDto r = new ChequeRegistroDto();
        r.setCodCheque(40);
        r.setModo("TALONARIO");
        r.setCodEmpleado(4);
        r.setNroTalonario("T9");
        r.setReciboManual("88");
        r.setMonto(1.0);              // se ignora
        r.setNrocheque("999");        // se ignora

        servicio.registrar(usuario, r);

        ArgumentCaptor<ChCheque> cap = ArgumentCaptor.forClass(ChCheque.class);
        verify(cheques).actualizar(cap.capture(), eq("recibido en caja"));
        assertEquals("T9", cap.getValue().getNroTalonario());
        assertEquals("88", cap.getValue().getReciboManual());
        assertEquals("5551", cap.getValue().getNrocheque());
        assertEquals(1000.5, cap.getValue().getMonto(), 0.0001);
        assertEquals("CER", cap.getValue().getEstado());
    }

    @Test
    @DisplayName("talonario de un cheque abierto: 403 (eso es la edicion estandar)")
    void talonarioAbiertoNo() {
        conBotones("btnEditar1CH");
        puedeEscribirEnSucursal();
        cheque(cheque(40, "PEN"));
        ChequeRegistroDto r = new ChequeRegistroDto();
        r.setCodCheque(40);
        r.setModo("TALONARIO");
        assertThrows(AccessDeniedException.class, () -> servicio.registrar(usuario, r));
    }

    @Test
    @DisplayName("el administrador edita un cheque cerrado en cualquiera de los tres modos")
    void adminEditaCerrado() {
        cheque(cheque(40, "CER"));
        ChequeRegistroDto r = altaValida();
        r.setCodCheque(40);
        r.setModo("ADMIN");
        r.setFechaCobrar(d(2026, 10, 5));
        r.setMonto(5.0);
        r.setTipo("PAG");
        r.setMoneda("BS");

        servicio.registrar(admin, r);

        ArgumentCaptor<ChCheque> c = ArgumentCaptor.forClass(ChCheque.class);
        verify(cheques).actualizar(c.capture(), any());
        assertEquals(5.0, c.getValue().getMonto(), 0.0001, "modo ADMIN: todo editable");
        assertEquals("CER", c.getValue().getEstado(), "pero el estado no se toca");
    }

    @Test
    @DisplayName("editar un cheque que no existe")
    void edicionInexistente() {
        ChequeRegistroDto r = altaValida();
        r.setCodCheque(999);
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.registrar(admin, r));
        assertTrue(e.getMessage().contains("No se encontró el cheque"), e.getMessage());
    }

    // ------------------------------------------------------------------ FECHA DE COBRO

    private AccionChequeRequest fechaCobroReq() {
        AccionChequeRequest r = new AccionChequeRequest();
        r.setCodCheque(40);
        r.setEstado("VEN");
        r.setNuevaFechaCobro(d(2026, 10, 10));
        r.setObservacion("por pedido del cliente");
        return r;
    }

    private void chequeEnCobranza(String codigoK) {
        conBotones("btnEditar2CH");
        puedeEscribirEnSucursal();
        cheque(cheque(40, "PEN"));
        when(cheques.codigoBotones(40)).thenReturn(codigoK);
        when(acciones.traspasosDelCheque(40)).thenReturn(1);
        when(acciones.devolucionesIgualAEntregas(40)).thenReturn(true);
    }

    @Test
    @DisplayName("fecha de cobro: cambia el cheque y registra la accion VEN en ese orden, con el prefijo del legacy y codEmpleado 0")
    void fechaCobroOk() {
        chequeEnCobranza("1000");

        long id = servicio.fechaCobro(usuario, fechaCobroReq());

        assertEquals(900L, id);
        InOrder orden = inOrder(cheques, acciones);
        orden.verify(cheques).cambiarFechaCobro(40, d(2026, 10, 10), 7);
        ArgumentCaptor<ChAccion> a = ArgumentCaptor.forClass(ChAccion.class);
        orden.verify(acciones).registrar(a.capture());
        assertEquals("VEN", a.getValue().getEstado());
        assertEquals("Nueva Fecha de Cobro = 10/10/2026 . por pedido del cliente", a.getValue().getObservacion());
        assertEquals(0, a.getValue().getCodEmpleado().intValue(), "el legacy escribe 0, no NULL");
        assertEquals(7, a.getValue().getAudUsuario().intValue());
    }

    @Test
    @DisplayName("rama K: sin el boton 1 habilitado no se puede cambiar la fecha de cobro (el cliente no es frontera de confianza)")
    void fechaCobroSinBotonK() {
        chequeEnCobranza("0101");   // en cobranza: Devolver y Cerrar sin verificacion, no Fecha Cobro
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.fechaCobro(usuario, fechaCobroReq()));
        assertTrue(e.getMessage().contains("está en cobranza") && e.getMessage().contains("Devolver"), e.getMessage());
        verify(cheques, never()).cambiarFechaCobro(anyInt(), any(), anyInt());
        verify(acciones, never()).registrar(any());
    }

    @Test
    @DisplayName("fecha de cobro: el ADMINISTRADOR puede moverla en un cheque CERRADO (K = 0000), como en el legacy; el usuario comun no")
    void fechaCobroAdminSobreCheque() {
        ChequeFilaDto cerrado = cheque(40, "CER");
        cheque(cerrado);
        when(cheques.codigoBotones(40)).thenReturn("0000");   // la K de un cheque cerrado
        when(acciones.traspasosDelCheque(40)).thenReturn(1);
        when(acciones.devolucionesIgualAEntregas(40)).thenReturn(true);

        // el administrador pasa: saveFechacobro solo exige un traspaso y devoluciones = entregas
        servicio.fechaCobro(admin, fechaCobroReq());
        verify(cheques).cambiarFechaCobro(40, d(2026, 10, 10), 7);

        // el usuario comun con btnEditar2CH: la fila no se la ofrece (estado CER) y el panel tampoco (K)
        conBotones("btnEditar2CH");
        puedeEscribirEnSucursal();
        assertThrows(SpBusinessException.class, () -> servicio.fechaCobro(usuario, fechaCobroReq()));
    }

    @Test
    @DisplayName("fecha de cobro del administrador: igual exige un traspaso y devoluciones = entregas")
    void fechaCobroAdminSigueExigiendoTraspaso() {
        cheque(cheque(40, "CER"));
        when(acciones.traspasosDelCheque(40)).thenReturn(0);
        when(acciones.devolucionesIgualAEntregas(40)).thenReturn(true);
        assertThrows(SpBusinessException.class, () -> servicio.fechaCobro(admin, fechaCobroReq()));
        verify(cheques, never()).cambiarFechaCobro(anyInt(), any(), anyInt());
    }

    @Test
    @DisplayName("devolver y cerrar siguen la rama K tambien para el administrador (son botones del panel)")
    void devolverAdminSigueLaK() {
        cheque(cheque(40, "CER"));
        when(cheques.codigoBotones(40)).thenReturn("0000");
        when(acciones.contarPorCheque(40)).thenReturn(5);
        assertThrows(SpBusinessException.class, () -> servicio.devolver(admin, accionReq()));
        verify(acciones, never()).registrar(any());
    }

    @Test
    @DisplayName("la observacion ausente se escribe como cadena vacia (el legacy no guarda NULL)")
    void observacionVaciaNoNula() {
        conBotones("btnNuevoCH");
        puedeEscribirEnSucursal();
        ChequeRegistroDto r = altaValida();
        r.setObservacion(null);
        servicio.registrar(usuario, r);
        verify(cheques).alta(any(), eq(""));

        conBotones("btnDetalleCH");
        cheque(cheque(40, "PEN"));
        when(cheques.codigoBotones(40)).thenReturn("0101");
        when(acciones.contarPorCheque(40)).thenReturn(3);
        AccionChequeRequest dev = new AccionChequeRequest();
        dev.setCodCheque(40);   // sin observacion
        servicio.devolver(usuario, dev);
        ArgumentCaptor<ChAccion> a = ArgumentCaptor.forClass(ChAccion.class);
        verify(acciones).registrar(a.capture());
        assertEquals("", a.getValue().getObservacion());
    }

    @Test
    @DisplayName("fecha de cobro: el legacy exige EXACTAMENTE un traspaso")
    void fechaCobroTraspasoUnico() {
        chequeEnCobranza("1000");
        when(acciones.traspasosDelCheque(40)).thenReturn(2);
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.fechaCobro(usuario, fechaCobroReq()));
        assertTrue(mensaje(e).contains("traspaso"));
        verify(cheques, never()).cambiarFechaCobro(anyInt(), any(), anyInt());
    }

    @Test
    @DisplayName("fecha de cobro: si no se devolvio el cheque, se rechaza")
    void fechaCobroSinDevolucion() {
        chequeEnCobranza("1000");
        when(acciones.devolucionesIgualAEntregas(40)).thenReturn(false);
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.fechaCobro(usuario, fechaCobroReq()));
        assertTrue(mensaje(e).contains("Devolver"));
    }

    @Test
    @DisplayName("fecha de cobro: sin btnEditar3CH la nueva fecha tiene que estar a +-28 dias de la del cheque")
    void fechaCobroRango() {
        chequeEnCobranza("1000");
        AccionChequeRequest r = fechaCobroReq();
        r.setNuevaFechaCobro(d(2026, 12, 25));   // la fecha del cheque es 2026-09-20
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.fechaCobro(usuario, r));
        assertTrue(mensaje(e).contains("fuera de lo permitido"));
    }

    @Test
    @DisplayName("fecha de cobro: con btnEditar3CH (variante del administrador) no hay limite")
    void fechaCobroSinLimiteConBtn3() {
        chequeEnCobranza("1000");
        conBotones("btnEditar2CH", "btnEditar3CH");
        AccionChequeRequest r = fechaCobroReq();
        r.setNuevaFechaCobro(d(2026, 12, 25));
        servicio.fechaCobro(usuario, r);
        verify(cheques).cambiarFechaCobro(40, d(2026, 12, 25), 7);
    }

    @Test
    @DisplayName("fecha de cobro: estado fuera de VEN/ADE y fecha anterior a hoy se rechazan")
    void fechaCobroEstadoYFecha() {
        chequeEnCobranza("1000");
        AccionChequeRequest r = fechaCobroReq();
        r.setEstado("COB");
        r.setFecha(d(2026, 10, 1));
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.fechaCobro(usuario, r));
        assertTrue(mensaje(e).contains("estado de la acción"));
        assertTrue(mensaje(e).contains("anterior a hoy"));
    }

    // ------------------------------------------------------------------ DEVOLVER / CERRAR

    private void chequeConHistorial(String codigoK, int cantidadAcciones) {
        conBotones("btnDetalleCH");
        puedeEscribirEnSucursal();
        cheque(cheque(40, "PEN"));
        when(cheques.codigoBotones(40)).thenReturn(codigoK);
        when(acciones.contarPorCheque(40)).thenReturn(cantidadAcciones);
    }

    private AccionChequeRequest accionReq() {
        AccionChequeRequest r = new AccionChequeRequest();
        r.setCodCheque(40);
        r.setObservacion("lo trajo el cobrador");
        return r;
    }

    @Test
    @DisplayName("devolver: registra DEV con codEmpleado 0 y NO cambia el estado del cheque")
    void devolverOk() {
        chequeConHistorial("0101", 3);
        long id = servicio.devolver(usuario, accionReq());
        assertEquals(900L, id);
        ArgumentCaptor<ChAccion> a = ArgumentCaptor.forClass(ChAccion.class);
        verify(acciones).registrar(a.capture());
        assertEquals("DEV", a.getValue().getEstado());
        assertEquals(0, a.getValue().getCodEmpleado().intValue());
        assertNull(a.getValue().getNroSAP());
        verify(cheques, never()).cambiarEstado(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("rama K: un cheque que esta en la oficina (1000) no se puede devolver")
    void devolverFueraDeCobranza() {
        chequeConHistorial("1000", 3);
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.devolver(usuario, accionReq()));
        assertTrue(e.getMessage().contains("está en la oficina") && e.getMessage().contains("A Custodio"), e.getMessage());
        verify(acciones, never()).registrar(any());
    }

    @Test
    @DisplayName("rama K: un cheque cerrado (0000) no admite acciones")
    void devolverCerrado() {
        chequeConHistorial("0000", 5);
        assertThrows(SpBusinessException.class, () -> servicio.devolver(usuario, accionReq()));
        verify(acciones, never()).registrar(any());
    }

    @Test
    @DisplayName("devolver: con menos de 3 acciones (no salio de caja o no se dio en custodia), se rechaza")
    void devolverPocasAcciones() {
        chequeConHistorial("0101", 2);
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.devolver(usuario, accionReq()));
        assertTrue(mensaje(e).contains("salido de caja"));
    }

    @Test
    @DisplayName("devolver exige btnDetalleCH")
    void devolverSinBoton() {
        conBotones();
        cheque(cheque(40, "PEN"));
        assertThrows(AccessDeniedException.class, () -> servicio.devolver(usuario, accionReq()));
    }

    @Test
    @DisplayName("devolver en otra sucursal sin permiso: se rechaza")
    void devolverSinSucursal() {
        conBotones("btnDetalleCH");
        when(sucursales.puedeEscribir(anyLong(), anyInt())).thenReturn(false);
        cheque(cheque(40, "PEN"));
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.devolver(usuario, accionReq()));
        assertEquals(ChequeService.MSG_SUCURSAL, e.getMessage());
    }

    @Test
    @DisplayName("cerrar con verificacion: accion COB y cheque a CER, en ese orden")
    void cerrarConVerificacion() {
        chequeConHistorial("0010", 4);
        AccionChequeRequest r = accionReq();
        r.setConVerificacion(true);
        r.setEstado("COB");
        r.setNroSap("100234");

        servicio.cerrar(usuario, r);

        InOrder orden = inOrder(acciones, cheques);
        ArgumentCaptor<ChAccion> a = ArgumentCaptor.forClass(ChAccion.class);
        orden.verify(acciones).registrar(a.capture());
        orden.verify(cheques).cambiarEstado(40, "CER", 7);
        assertEquals("COB", a.getValue().getEstado());
        assertEquals("100234", a.getValue().getNroSAP());
    }

    @Test
    @DisplayName("cerrar sin verificacion usa el boton 4 de la rama K y estados CEF/CCH/PAP/DPR")
    void cerrarSinVerificacion() {
        chequeConHistorial("0101", 4);
        AccionChequeRequest r = accionReq();
        r.setConVerificacion(false);
        r.setEstado("CEF");
        r.setNroSap("100234");
        servicio.cerrar(usuario, r);
        verify(cheques).cambiarEstado(40, "CER", 7);
    }

    @Test
    @DisplayName("cerrar sin verificacion un cheque que ya esta verificado (0010): se rechaza, hay que cerrarlo con verificacion")
    void cerrarSinVerificacionPeroVerificado() {
        chequeConHistorial("0010", 4);
        AccionChequeRequest r = accionReq();
        r.setConVerificacion(false);
        r.setEstado("CEF");
        r.setNroSap("100234");
        assertThrows(SpBusinessException.class, () -> servicio.cerrar(usuario, r));
        verify(acciones, never()).registrar(any());
        verify(cheques, never()).cambiarEstado(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("cerrar: el Nro SAP es obligatorio")
    void cerrarSinNroSap() {
        chequeConHistorial("0010", 4);
        AccionChequeRequest r = accionReq();
        r.setConVerificacion(true);
        r.setEstado("COB");
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.cerrar(usuario, r));
        assertTrue(mensaje(e).contains("número de SAP"));
    }

    @Test
    @DisplayName("cerrar con verificacion solo admite COB; con CEF se rechaza")
    void cerrarEstadoIncorrecto() {
        chequeConHistorial("0010", 4);
        AccionChequeRequest r = accionReq();
        r.setConVerificacion(true);
        r.setEstado("CEF");
        r.setNroSap("100234");
        assertThrows(SpBusinessException.class, () -> servicio.cerrar(usuario, r));
    }

    @Test
    @DisplayName("si falla el cambio de estado, la excepcion sube (y @Transactional revierte la accion)")
    void cerrarPropagaErrorDelCheque() {
        chequeConHistorial("0010", 4);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(cheques).cambiarEstado(anyInt(), anyString(), anyInt());
        AccionChequeRequest r = accionReq();
        r.setConVerificacion(true);
        r.setEstado("COB");
        r.setNroSap("100234");
        assertThrows(RuntimeException.class, () -> servicio.cerrar(usuario, r));
    }

    // ------------------------------------------------------------------ ELIMINAR ACCION

    @Test
    @DisplayName("eliminar accion exige btnEliminarSegCH")
    void eliminarSinBoton() {
        conBotones();
        assertThrows(AccessDeniedException.class, () -> servicio.eliminarAccion(usuario, 10));
        verify(acciones, never()).eliminar(anyInt(), anyInt());
    }

    @Test
    @DisplayName("eliminar accion: se conserva el legacy, se borra aunque el cheque este CERRADO y aunque sea REC")
    void eliminarConservaLegacy() {
        conBotones("btnEliminarSegCH", "btnChqSucrs");
        ChAccion rec = new ChAccion();
        rec.setCodAccion(10);
        rec.setCodCheque(40);
        rec.setEstado("REC");
        when(acciones.obtener(10)).thenReturn(rec);
        cheque(cheque(40, "CER"));

        servicio.eliminarAccion(usuario, 10);
        verify(acciones).eliminar(10, 7);
    }

    @Test
    @DisplayName("eliminar una accion que no existe")
    void eliminarInexistente() {
        conBotones("btnEliminarSegCH");
        when(acciones.obtener(10)).thenReturn(null);
        assertThrows(SpBusinessException.class, () -> servicio.eliminarAccion(usuario, 10));
    }

    // ------------------------------------------------------------------ TRASPASO

    @Test
    @DisplayName("traspaso: devuelve cuantos cheques se traspasaron")
    void traspaso() {
        conBotones("btnTraspasoCH");
        puedeEscribirEnSucursal();
        when(acciones.traspasar(SUC, 7)).thenReturn(new RespuestaSp(0, "", 12L));
        assertEquals(12L, servicio.traspasar(usuario, SUC));
    }

    @Test
    @DisplayName("traspaso sin cheques pendientes: el legacy lo mostraba como 'NO fue Almacenado'")
    void traspasoVacio() {
        conBotones("btnTraspasoCH");
        puedeEscribirEnSucursal();
        when(acciones.traspasar(SUC, 7)).thenReturn(new RespuestaSp(0, "", 0L));
        assertThrows(SpBusinessException.class, () -> servicio.traspasar(usuario, SUC));
    }

    @Test
    @DisplayName("traspaso exige btnTraspasoCH y permiso de escritura en la sucursal")
    void traspasoPermisos() {
        conBotones();
        assertThrows(AccessDeniedException.class, () -> servicio.traspasar(usuario, SUC));

        conBotones("btnTraspasoCH");
        when(sucursales.puedeEscribir(anyLong(), anyInt())).thenReturn(false);
        assertThrows(SpBusinessException.class, () -> servicio.traspasar(usuario, SUC));
        verify(acciones, never()).traspasar(anyLong(), anyInt());
    }

    // ------------------------------------------------------------------ CUSTODIA

    @Test
    @DisplayName("A Custodio: una accion CUS por cheque, con el responsable y la observacion del legacy")
    void custodia() {
        conBotones("btnCustodiaCH");
        puedeEscribirEnSucursal();
        CustodiaChequeRequest r = new CustodiaChequeRequest();
        r.setCodSucursal((long) SUC);
        r.setCodEmpleado(21);
        r.setCodCheques(Arrays.asList(40, 41, 42));
        when(cheques.sinCustodio(eq((long) SUC), any())).thenReturn(
                Arrays.asList(cheque(40, "PEN"), cheque(41, "PEN"), cheque(42, "PEN")));

        assertEquals(3, servicio.custodia(usuario, r));

        ArgumentCaptor<ChAccion> a = ArgumentCaptor.forClass(ChAccion.class);
        verify(acciones, times(3)).registrar(a.capture());
        for (ChAccion x : a.getAllValues()) {
            assertEquals("CUS", x.getEstado());
            assertEquals(21, x.getCodEmpleado().intValue());
            assertEquals(" Entregado en Para Cobranza", x.getObservacion());
            assertEquals(7, x.getAudUsuario().intValue());
        }
        assertEquals(Arrays.asList(40, 41, 42),
                Arrays.asList(a.getAllValues().get(0).getCodCheque(), a.getAllValues().get(1).getCodCheque(),
                        a.getAllValues().get(2).getCodCheque()));
    }

    @Test
    @DisplayName("A Custodio: sin responsable o sin cheques se rechaza antes de escribir")
    void custodiaSinDatos() {
        conBotones("btnCustodiaCH");
        puedeEscribirEnSucursal();
        CustodiaChequeRequest r = new CustodiaChequeRequest();
        r.setCodSucursal((long) SUC);
        r.setCodCheques(Collections.singletonList(40));
        assertThrows(SpBusinessException.class, () -> servicio.custodia(usuario, r));

        r.setCodEmpleado(21);
        r.setCodCheques(new ArrayList<Integer>());
        assertThrows(SpBusinessException.class, () -> servicio.custodia(usuario, r));
        verify(acciones, never()).registrar(any());
    }

    @Test
    @DisplayName("A Custodio: un cheque que no esta en la lista de hoy (ajeno, cerrado o de otra fecha) se rechaza y no escribe nada")
    void custodiaChequeAjeno() {
        conBotones("btnCustodiaCH");
        puedeEscribirEnSucursal();
        when(cheques.sinCustodio(eq((long) SUC), any())).thenReturn(Arrays.asList(cheque(40, "PEN")));
        CustodiaChequeRequest r = new CustodiaChequeRequest();
        r.setCodSucursal((long) SUC);
        r.setCodEmpleado(21);
        r.setCodCheques(Arrays.asList(40, 999));   // el 999 no esta en la lista

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.custodia(usuario, r));
        assertTrue(mensaje(e).contains("999"));
        verify(acciones, never()).registrar(any());   // ni siquiera el 40: se valida todo antes de escribir
    }

    @Test
    @DisplayName("A Custodio: los ids repetidos cuentan una vez")
    void custodiaSinRepetidos() {
        conBotones("btnCustodiaCH");
        puedeEscribirEnSucursal();
        when(cheques.sinCustodio(eq((long) SUC), any())).thenReturn(Arrays.asList(cheque(40, "PEN")));
        CustodiaChequeRequest r = new CustodiaChequeRequest();
        r.setCodSucursal((long) SUC);
        r.setCodEmpleado(21);
        r.setCodCheques(Arrays.asList(40, 40, 40));

        assertEquals(1, servicio.custodia(usuario, r));
        verify(acciones, times(1)).registrar(any());
    }

    private void darCustodiaPreparada() {
        conBotones("btnCustodia2CH");
        puedeEscribirEnSucursal();
        ChAccion cus = new ChAccion();
        cus.setCodAccion(30);
        cus.setCodCheque(39);
        cus.setEstado("CUS");
        when(acciones.obtener(30)).thenReturn(cus);
        cheque(cheque(39, "PEN"));   // el cheque de la entrega, de la sucursal 3
        cheque(cheque(40, "PEN"));   // el que la recibe, de la sucursal 3
    }

    private DarCustodiaRequest darCustodiaReq() {
        DarCustodiaRequest r = new DarCustodiaRequest();
        r.setCodSucursal((long) SUC);
        r.setCodAccionOrigen(30);
        r.setCodCheque(40);
        return r;
    }

    @Test
    @DisplayName("Dar Custodia: solo se puede copiar una entrega CUS, entre cheques de la sucursal")
    void darCustodia() {
        darCustodiaPreparada();
        assertEquals(901L, servicio.darCustodia(usuario, darCustodiaReq()));
        verify(acciones).copiar(30, 40, 7);
    }

    @Test
    @DisplayName("Dar Custodia: un cheque de otra sucursal se rechaza")
    void darCustodiaChequeDeOtraSucursal() {
        darCustodiaPreparada();
        ChequeFilaDto ajeno = cheque(40, "PEN");
        ajeno.setCodSucursal(9L);
        cheque(ajeno);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.darCustodia(usuario, darCustodiaReq()));
        assertTrue(mensaje(e).contains("no es de la sucursal"));
        verify(acciones, never()).copiar(anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Dar Custodia: una entrega de otra sucursal se rechaza")
    void darCustodiaEntregaDeOtraSucursal() {
        darCustodiaPreparada();
        ChequeFilaDto ajeno = cheque(39, "PEN");
        ajeno.setCodSucursal(9L);
        cheque(ajeno);

        assertThrows(SpBusinessException.class, () -> servicio.darCustodia(usuario, darCustodiaReq()));
        verify(acciones, never()).copiar(anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Dar Custodia: las listas de apoyo exigen btnCustodia2CH y poder ver la sucursal")
    void darCustodiaListas() {
        conBotones("btnCustodia2CH");
        when(sucursales.sucursalInicial(1, 7, 0L)).thenReturn(0L);   // sin sucursal propia ni btnChqSucrs
        assertThrows(AccessDeniedException.class, () -> servicio.chequesParaDarCustodia(usuario, 9));
        assertThrows(AccessDeniedException.class, () -> servicio.entregasDelDia(usuario, 9L, d(2026, 10, 2)));

        conBotones("btnCustodia2CH", "btnChqSucrs");
        servicio.chequesParaDarCustodia(usuario, 9);
        verify(cheques).chequesDeSucursal(9L);
        assertThrows(SpBusinessException.class, () -> servicio.entregasDelDia(usuario, 9L, null));

        conBotones();   // sin el boton
        assertThrows(AccessDeniedException.class, () -> servicio.chequesParaDarCustodia(usuario, 9));
    }

    @Test
    @DisplayName("el personal de una sucursal solo se lista si se puede ver esa sucursal")
    void personalConPermisoDeSucursal() {
        conBotones();
        when(sucursales.sucursalInicial(1, 7, 0L)).thenReturn((long) SUC);
        servicio.quienesEntregan(usuario, SUC);
        servicio.responsablesDeCustodia(usuario, SUC);
        verify(personal).quienesEntregan(SUC);
        verify(personal).responsablesDeCustodia(SUC);

        assertThrows(AccessDeniedException.class, () -> servicio.quienesEntregan(usuario, 9));
        verify(personal, never()).quienesEntregan(9L);
    }

    @Test
    @DisplayName("Dar Custodia: copiar un cobro (COB) a otro cheque se rechaza")
    void darCustodiaNoCus() {
        conBotones("btnCustodia2CH");
        puedeEscribirEnSucursal();
        ChAccion cob = new ChAccion();
        cob.setCodAccion(31);
        cob.setEstado("COB");
        when(acciones.obtener(31)).thenReturn(cob);
        DarCustodiaRequest r = new DarCustodiaRequest();
        r.setCodSucursal((long) SUC);
        r.setCodAccionOrigen(31);
        r.setCodCheque(40);

        assertThrows(SpBusinessException.class, () -> servicio.darCustodia(usuario, r));
        verify(acciones, never()).copiar(anyInt(), anyInt(), anyInt());
    }

    // ------------------------------------------------------------------ LECTURAS

    private static List<ChequeFilaDto> filas(int n) {
        List<ChequeFilaDto> l = new ArrayList<>();
        for (int i = 1; i <= n; i++) l.add(cheque(i, "PEN"));
        return l;
    }

    /** Un cheque de la sucursal recibido ese dia (la fecha de recepcion es un instante: se compara por dia). */
    private static ChequeFilaDto recibido(int cod, int anio, int mes, int dia, int hora, int minuto) {
        ChequeFilaDto c = cheque(cod, "CER");
        c.setFechaRecepcion(java.sql.Timestamp.valueOf(java.time.LocalDateTime.of(anio, mes, dia, hora, minuto)));
        return c;
    }

    private ChequeFiltroDto filtroDeRecepcion(Date desde, Date hasta) {
        ChequeFiltroDto f = new ChequeFiltroDto();
        f.setCodSucursal((long) SUC);
        f.setFechaRecepcionDesde(desde);
        f.setFechaRecepcionHasta(hasta);
        return f;
    }

    @Test
    @DisplayName("listar por rango de recepcion: los dos extremos entran, por dia y sin importar la hora; total y paginas son del rango")
    void listarPorRangoDeRecepcion() {
        conBotones("btnChqSucrs");
        when(cheques.listar(any())).thenReturn(Arrays.asList(
                recibido(1, 2026, 7, 2, 23, 59),     // un dia antes del piso: fuera
                recibido(2, 2026, 7, 3, 0, 0),       // el piso, a las 00:00: dentro
                recibido(3, 2026, 8, 15, 12, 0),
                recibido(4, 2026, 10, 3, 23, 59),    // el techo, a las 23:59: dentro
                recibido(5, 2026, 10, 4, 0, 0)));    // un dia despues del techo: fuera

        ChequePaginaDto p = servicio.listar(usuario, filtroDeRecepcion(d(2026, 7, 3), d(2026, 10, 3)));

        assertEquals(3, p.getTotal());
        assertEquals(Arrays.asList(2, 3, 4), Arrays.asList(
                p.getFilas().get(0).getCodCheque().intValue(), p.getFilas().get(1).getCodCheque().intValue(),
                p.getFilas().get(2).getCodCheque().intValue()), "conserva el orden del SP");
    }

    @Test
    @DisplayName("listar por rango: un solo extremo limita solo de ese lado; sin extremos no filtra nada")
    void listarRangoConUnSoloExtremo() {
        conBotones("btnChqSucrs");
        when(cheques.listar(any())).thenReturn(Arrays.asList(
                recibido(1, 2026, 1, 10, 9, 0), recibido(2, 2026, 6, 10, 9, 0), recibido(3, 2026, 9, 10, 9, 0)));

        assertEquals(2, servicio.listar(usuario, filtroDeRecepcion(d(2026, 6, 10), null)).getTotal(), "solo piso");
        assertEquals(2, servicio.listar(usuario, filtroDeRecepcion(null, d(2026, 6, 10))).getTotal(), "solo techo");
        assertEquals(3, servicio.listar(usuario, filtroDeRecepcion(null, null)).getTotal(), "sin rango: todas");
        assertEquals(1, servicio.listar(usuario, filtroDeRecepcion(d(2026, 6, 10), d(2026, 6, 10))).getTotal(),
                "un dia suelto = las dos fechas iguales");
    }

    @Test
    @DisplayName("listar por rango: el rango se aplica ANTES de paginar (45 de 100 filas dan 3 paginas, no 5)")
    void listarRangoPaginaElRango() {
        conBotones("btnChqSucrs");
        List<ChequeFilaDto> todas = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            todas.add(i <= 45 ? recibido(i, 2026, 9, 1, 9, 0) : recibido(i, 2025, 1, 1, 9, 0));
        }
        when(cheques.listar(any())).thenReturn(todas);
        ChequeFiltroDto f = filtroDeRecepcion(d(2026, 7, 3), d(2026, 10, 3));
        f.setPagina(3);

        ChequePaginaDto p = servicio.listar(usuario, f);

        assertEquals(45, p.getTotal());
        assertEquals(3, p.getPagina());
        assertEquals(5, p.getFilas().size(), "la ultima pagina tiene las 5 que sobran");
    }

    @Test
    @DisplayName("listar por rango: una fecha inicial posterior a la final se rechaza y no consulta la base")
    void listarRangoInvertido() {
        conBotones("btnChqSucrs");
        SpBusinessException e = assertThrows(SpBusinessException.class,
                () -> servicio.listar(usuario, filtroDeRecepcion(d(2026, 10, 3), d(2026, 7, 3))));
        assertTrue(e.getMessage().contains("inicial"));
        verify(cheques, never()).listar(any());
    }

    @Test
    @DisplayName("listar: pagina de 20 por defecto, con el total de todas las filas")
    void listarPaginado() {
        conBotones("btnChqSucrs");
        when(cheques.listar(any())).thenReturn(filas(45));
        ChequeFiltroDto f = new ChequeFiltroDto();
        f.setCodSucursal((long) SUC);
        f.setPagina(3);

        ChequePaginaDto p = servicio.listar(usuario, f);
        assertEquals(45, p.getTotal());
        assertEquals(20, p.getTamanio());
        assertEquals(3, p.getPagina());
        assertEquals(5, p.getFilas().size());
        assertEquals(41, p.getFilas().get(0).getCodCheque().intValue());
    }

    @Test
    @DisplayName("listar: una pagina mas alla de la ultima devuelve la ultima")
    void listarPaginaExcedida() {
        conBotones("btnChqSucrs");
        when(cheques.listar(any())).thenReturn(filas(45));
        ChequeFiltroDto f = new ChequeFiltroDto();
        f.setCodSucursal((long) SUC);
        f.setPagina(99);
        assertEquals(3, servicio.listar(usuario, f).getPagina());
    }

    @Test
    @DisplayName("listar sin resultados: pagina vacia, no error")
    void listarVacio() {
        conBotones("btnChqSucrs");
        when(cheques.listar(any())).thenReturn(new ArrayList<ChequeFilaDto>());
        ChequeFiltroDto f = new ChequeFiltroDto();
        f.setCodSucursal((long) SUC);
        ChequePaginaDto p = servicio.listar(usuario, f);
        assertEquals(0, p.getTotal());
        assertTrue(p.getFilas().isEmpty());
    }

    @Test
    @DisplayName("listar: sin btnChqSucrs solo se ve la sucursal propia (la que da p_list_Sucursal E)")
    void listarSoloLaPropia() {
        conBotones();
        when(sucursales.sucursalInicial(1, 7, 0L)).thenReturn((long) SUC);
        when(cheques.listar(any())).thenReturn(filas(1));

        ChequeFiltroDto propia = new ChequeFiltroDto();
        propia.setCodSucursal((long) SUC);
        assertEquals(1, servicio.listar(usuario, propia).getTotal());

        ChequeFiltroDto ajena = new ChequeFiltroDto();
        ajena.setCodSucursal(9L);
        assertThrows(AccessDeniedException.class, () -> servicio.listar(usuario, ajena));
    }

    @Test
    @DisplayName("listar: un usuario sin sucursal propia (0) y sin btnChqSucrs no ve nada")
    void listarSinSucursalPropia() {
        conBotones();
        when(sucursales.sucursalInicial(1, 7, 0L)).thenReturn(0L);
        ChequeFiltroDto f = new ChequeFiltroDto();
        f.setCodSucursal((long) SUC);
        assertThrows(AccessDeniedException.class, () -> servicio.listar(usuario, f));
        verify(cheques, never()).listar(any());
    }

    @Test
    @DisplayName("listar exige la sucursal")
    void listarSinSucursal() {
        assertThrows(SpBusinessException.class, () -> servicio.listar(admin, new ChequeFiltroDto()));
    }

    @Test
    @DisplayName("detalle: entrega cheque, historial y botones; exige btnDetalleCH")
    void detalle() {
        conBotones("btnDetalleCH", "btnChqSucrs");
        cheque(cheque(40, "PEN"));
        when(acciones.listarPorCheque(40)).thenReturn(new ArrayList<>());
        when(cheques.codigoBotones(40)).thenReturn("0101");

        assertEquals("0101", servicio.detalle(usuario, 40).getBotones().getCodigo());
        assertTrue(servicio.detalle(usuario, 40).getBotones().isDevolver());

        conBotones();
        assertThrows(AccessDeniedException.class, () -> servicio.detalle(usuario, 40));
    }

    @Test
    @DisplayName("el instante de una accion de hoy es ahora; el de otro dia es esa fecha")
    void marcaDeTiempo() {
        conBotones("btnDetalleCH");
        puedeEscribirEnSucursal();
        cheque(cheque(40, "PEN"));
        when(cheques.codigoBotones(40)).thenReturn("0101");
        when(acciones.contarPorCheque(40)).thenReturn(3);

        AccionChequeRequest hoy = accionReq();
        hoy.setFecha(d(2026, 10, 2));
        servicio.devolver(usuario, hoy);
        AccionChequeRequest manana = accionReq();
        manana.setFecha(d(2026, 10, 3));
        servicio.devolver(usuario, manana);

        ArgumentCaptor<ChAccion> a = ArgumentCaptor.forClass(ChAccion.class);
        verify(acciones, times(2)).registrar(a.capture());
        assertEquals(Date.from(Instant.from(RELOJ.instant())), a.getAllValues().get(0).getFecha());
        assertEquals(d(2026, 10, 3), a.getAllValues().get(1).getFecha());
    }
}
