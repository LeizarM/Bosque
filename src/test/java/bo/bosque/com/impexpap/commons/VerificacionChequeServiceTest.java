package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dao.IChVerificacionDeposito;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequePendienteFilaDto;
import bo.bosque.com.impexpap.dto.ChequePendienteFiltroDto;
import bo.bosque.com.impexpap.dto.PrepararVerificacionDto;
import bo.bosque.com.impexpap.dto.VerificacionDepositoFilaDto;
import bo.bosque.com.impexpap.dto.VerificacionFiltroDto;
import bo.bosque.com.impexpap.dto.VerificacionPaginaDto;
import bo.bosque.com.impexpap.dto.VerificacionRegistroDto;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.model.ChVerificacionDeposito;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * El servicio de Verificar Cheques con los DAO simulados: lo que se prueba son las REGLAS (cheque cerrado, una sola
 * verificacion valida, formato de la observacion, la edicion que conserva cheque y estado, "Cancelar" idempotente), no el
 * SQL.
 *
 * <p>Fecha fija: 2026-10-03 10:00 (America/La_Paz). Usuario 7 del token.
 */
class VerificacionChequeServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 3);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZONA).toInstant(), ZONA);

    private IChVerificacionDeposito dao;
    private IChBanco bancos;
    private IChCheque cheques;
    private VerificacionChequeService servicio;
    private Authentication usuario;

    @BeforeEach
    void preparar() {
        dao = mock(IChVerificacionDeposito.class);
        bancos = mock(IChBanco.class);
        cheques = mock(IChCheque.class);
        servicio = new VerificacionChequeService(dao, bancos, cheques, RELOJ);
        usuario = token();

        when(bancos.obtener(2)).thenReturn(new ChBanco(2, "Mercantil Santa Cruz", 1, null));
        when(dao.registrar(any())).thenReturn(new RespuestaSp(0, "", 77L));
        when(dao.actualizar(any())).thenReturn(new RespuestaSp(0, "", 0L));
    }

    // ------------------------------------------------------------------ utilidades

    private static Authentication token() {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        return t;
    }

    private static ChequePendienteFilaDto pendiente(long cod, String estadoDescripcion) {
        ChequePendienteFilaDto c = new ChequePendienteFilaDto();
        c.setCodCheque(cod);
        c.setCodBanco(2L);
        c.setNroCheque("5551");
        c.setMontoCheque(1500.5);
        c.setDatoBancoCheque("Mercantil Santa Cruz");
        c.setDatoEstadoCheque(estadoDescripcion);
        c.setFechaCobrarCheque(HOY);
        return c;
    }

    private static VerificacionDepositoFilaDto verificacion(int codvd, long codCheque, String estado) {
        VerificacionDepositoFilaDto v = new VerificacionDepositoFilaDto();
        v.setCodvd(codvd);
        v.setCodCheque(codCheque);
        v.setCodBanco(2L);
        v.setFechaBanco(LocalDate.of(2026, 10, 1));
        v.setObservacion("ya estaba");
        v.setEstado(estado);
        v.setNroCheque("5551");
        v.setDatoEstadoCheque("PENDIENTE");
        v.setDatoEstado("Y".equals(estado) ? "Valido" : "Anulado");
        return v;
    }

    private static VerificacionRegistroDto alta(long codCheque) {
        VerificacionRegistroDto d = new VerificacionRegistroDto();
        d.setCodCheque(codCheque);
        d.setCodBanco(2L);
        d.setFechaBanco(HOY);
        d.setObservacion("Deposito OK");
        return d;
    }

    private static ChequeFilaDto chequeConMoneda(String moneda, String descripcion) {
        ChequeFilaDto c = new ChequeFilaDto();
        c.setMoneda(moneda);
        c.setDescMoneda(descripcion);
        return c;
    }

    private ChVerificacionDeposito escritaPorRegistrar() {
        ArgumentCaptor<ChVerificacionDeposito> cap = ArgumentCaptor.forClass(ChVerificacionDeposito.class);
        verify(dao).registrar(cap.capture());
        return cap.getValue();
    }

    private ChVerificacionDeposito escritaPorActualizar() {
        ArgumentCaptor<ChVerificacionDeposito> cap = ArgumentCaptor.forClass(ChVerificacionDeposito.class);
        verify(dao).actualizar(cap.capture());
        return cap.getValue();
    }

    private String errorDe(Runnable r) {
        return assertThrows(SpBusinessException.class, r::run).getMessage();
    }

    // ------------------------------------------------------------------ lista principal

    @Test
    @DisplayName("listar: pasa el dia al procedimiento (null = todas), ordena por codvd descendente y numera 1..n")
    void listarOrdenaYNumera() {
        VerificacionFiltroDto f = new VerificacionFiltroDto();
        f.setFechaBanco(HOY);
        when(dao.listar(HOY)).thenReturn(new ArrayList<>(java.util.Arrays.asList(
                verificacion(3, 10, "Y"), verificacion(9, 11, "Y"), verificacion(5, 12, "N"))));

        VerificacionPaginaDto<VerificacionDepositoFilaDto> p = servicio.listar(f);

        assertEquals(3, p.getTotal());
        assertEquals(1, p.getPagina());
        assertEquals(20, p.getTamanio());
        assertEquals(java.util.Arrays.asList(9, 5, 3),
                java.util.Arrays.asList(p.getFilas().get(0).getCodvd(), p.getFilas().get(1).getCodvd(), p.getFilas().get(2).getCodvd()));
        assertEquals(java.util.Arrays.asList(1, 2, 3),
                java.util.Arrays.asList(p.getFilas().get(0).getFila(), p.getFilas().get(1).getFila(), p.getFilas().get(2).getFila()));

        servicio.listar(null);
        verify(dao).listar(null);
    }

    @Test
    @DisplayName("listar: pagina en el servidor (45 filas, 20 por pagina: la 3 trae 5, numeradas 41..45); tamanio maximo 200; pagina fuera de rango = la ultima")
    void listarPagina() {
        List<VerificacionDepositoFilaDto> filas = new ArrayList<>();
        for (int i = 1; i <= 45; i++) filas.add(verificacion(i, 100 + i, "Y"));
        when(dao.listar(any())).thenReturn(filas);

        VerificacionFiltroDto f = new VerificacionFiltroDto();
        f.setPagina(3);
        VerificacionPaginaDto<VerificacionDepositoFilaDto> p = servicio.listar(f);
        assertEquals(45, p.getTotal());
        assertEquals(3, p.getPagina());
        assertEquals(5, p.getFilas().size());
        assertEquals(41, p.getFilas().get(0).getFila().intValue());
        assertEquals(45, p.getFilas().get(4).getFila().intValue());
        assertEquals(5, p.getFilas().get(0).getCodvd().intValue(), "el orden es codvd descendente: la pagina 3 trae del 5 al 1");

        f.setPagina(99);
        assertEquals(3, servicio.listar(f).getPagina(), "una pagina pasada del final es la ultima");

        f.setPagina(1);
        f.setTamanio(500);
        VerificacionPaginaDto<VerificacionDepositoFilaDto> grande = servicio.listar(f);
        assertEquals(200, grande.getTamanio());
        assertEquals(45, grande.getFilas().size());

        f.setTamanio(0);
        f.setPagina(0);
        VerificacionPaginaDto<VerificacionDepositoFilaDto> defecto = servicio.listar(f);
        assertEquals(20, defecto.getTamanio());
        assertEquals(1, defecto.getPagina());
    }

    @Test
    @DisplayName("listar sin filas: 200 con la lista vacia, total 0 y pagina 1")
    void listarVacio() {
        when(dao.listar(any())).thenReturn(new ArrayList<>());
        VerificacionPaginaDto<VerificacionDepositoFilaDto> p = servicio.listar(new VerificacionFiltroDto());
        assertEquals(0, p.getTotal());
        assertEquals(1, p.getPagina());
        assertTrue(p.getFilas().isEmpty());
    }

    @Test
    @DisplayName("listar completa la moneda leyendo el cheque UNA vez por cheque distinto, y la marca de cheque cerrado")
    void listarCompletaMoneda() {
        VerificacionDepositoFilaDto a = verificacion(2, 10, "Y");
        VerificacionDepositoFilaDto b = verificacion(1, 10, "N");   // el mismo cheque
        VerificacionDepositoFilaDto c = verificacion(3, 11, "Y");
        c.setDatoEstadoCheque("CERRADO");
        when(dao.listar(any())).thenReturn(new ArrayList<>(java.util.Arrays.asList(a, b, c)));
        when(cheques.obtener(10)).thenReturn(chequeConMoneda("BS", "Bs"));
        when(cheques.obtener(11)).thenReturn(chequeConMoneda("SUS", "$us"));

        VerificacionPaginaDto<VerificacionDepositoFilaDto> p = servicio.listar(new VerificacionFiltroDto());

        verify(cheques, times(1)).obtener(10);
        verify(cheques, times(1)).obtener(11);
        assertEquals("$us", p.getFilas().get(0).getDescMoneda(), "la fila 3 es la primera (orden descendente)");
        assertEquals("SUS", p.getFilas().get(0).getMoneda());
        assertTrue(p.getFilas().get(0).isChequeCerrado());
        assertEquals("Bs", p.getFilas().get(1).getDescMoneda());
        assertEquals("BS", p.getFilas().get(2).getMoneda());
        assertFalse(p.getFilas().get(1).isChequeCerrado());
    }

    @Test
    @DisplayName("listar solo lee la moneda de la PAGINA, no de toda la lista")
    void listarSoloLeeLaPagina() {
        List<VerificacionDepositoFilaDto> filas = new ArrayList<>();
        for (int i = 1; i <= 30; i++) filas.add(verificacion(i, 100 + i, "Y"));
        when(dao.listar(any())).thenReturn(filas);
        VerificacionFiltroDto f = new VerificacionFiltroDto();
        f.setTamanio(5);

        servicio.listar(f);

        verify(cheques, times(5)).obtener(anyInt());
    }

    @Test
    @DisplayName("listar: si leer el cheque falla o no lo encuentra, la moneda queda en null y el listado NO se rompe")
    void listarToleraFalloDeMoneda() {
        when(dao.listar(any())).thenReturn(new ArrayList<>(java.util.Arrays.asList(
                verificacion(2, 10, "Y"), verificacion(1, 11, "Y"))));
        when(cheques.obtener(10)).thenThrow(new RuntimeException("la base no contesta"));
        when(cheques.obtener(11)).thenReturn(null);

        VerificacionPaginaDto<VerificacionDepositoFilaDto> p = servicio.listar(new VerificacionFiltroDto());

        assertEquals(2, p.getFilas().size());
        assertNull(p.getFilas().get(0).getMoneda());
        assertNull(p.getFilas().get(0).getDescMoneda());
        assertNull(p.getFilas().get(1).getMoneda());
    }

    // ------------------------------------------------------------------ pendientes (el modal de seleccion)

    @Test
    @DisplayName("pendientes sin filtro: solo la fecha de cobranza de HOY (el '2' del JSF), todos los estados, con el hoy de La Paz")
    void pendientesPorDefecto() {
        when(dao.sinRegularizarDelDia(any(), any())).thenReturn(new ArrayList<>());

        servicio.pendientes(null);
        verify(dao).sinRegularizarDelDia(HOY, null);
        verify(dao, never()).sinRegularizarHastaHoy(any());

        servicio.pendientes(new ChequePendienteFiltroDto());
        verify(dao, times(2)).sinRegularizarDelDia(HOY, null);
    }

    @Test
    @DisplayName("pendientes: soloCobranzaHoy=false usa la rama D (anteriores y de hoy); true, la C con la fecha de hoy; el estado va tal cual")
    void pendientesElijeRama() {
        when(dao.sinRegularizarDelDia(any(), any())).thenReturn(new ArrayList<>());
        when(dao.sinRegularizarHastaHoy(any())).thenReturn(new ArrayList<>());
        ChequePendienteFiltroDto f = new ChequePendienteFiltroDto();
        f.setEstado("PEN");

        f.setSoloCobranzaHoy(false);
        servicio.pendientes(f);
        verify(dao).sinRegularizarHastaHoy("PEN");
        verify(dao, never()).sinRegularizarDelDia(any(), any());

        f.setSoloCobranzaHoy(true);
        f.setEstado("CER");
        servicio.pendientes(f);
        verify(dao).sinRegularizarDelDia(HOY, "CER");
    }

    @Test
    @DisplayName("pendientes: el estado se acepta en minusculas y vacio = todos; otro valor es 400 con las opciones")
    void pendientesEstado() {
        when(dao.sinRegularizarDelDia(any(), any())).thenReturn(new ArrayList<>());
        ChequePendienteFiltroDto f = new ChequePendienteFiltroDto();

        f.setEstado(" pen ");
        servicio.pendientes(f);
        verify(dao).sinRegularizarDelDia(HOY, "PEN");

        f.setEstado("   ");
        servicio.pendientes(f);
        verify(dao).sinRegularizarDelDia(HOY, null);

        f.setEstado("XYZ");
        String m = errorDe(() -> servicio.pendientes(f));
        assertTrue(m.contains("«XYZ»") && m.contains("PEN o CER"), m);
    }

    @Test
    @DisplayName("pendientes: conserva el orden del procedimiento, numera, pagina y completa la moneda")
    void pendientesOrdenPaginaYMoneda() {
        List<ChequePendienteFilaDto> filas = new ArrayList<>();
        for (int i = 1; i <= 25; i++) filas.add(pendiente(500 + i, "PENDIENTE"));
        filas.get(0).setDatoEstadoCheque("CERRADO");
        when(dao.sinRegularizarDelDia(any(), any())).thenReturn(filas);
        when(cheques.obtener(anyInt())).thenReturn(chequeConMoneda("BS", "Bs"));
        ChequePendienteFiltroDto f = new ChequePendienteFiltroDto();
        f.setPagina(2);

        VerificacionPaginaDto<ChequePendienteFilaDto> p = servicio.pendientes(f);

        assertEquals(25, p.getTotal());
        assertEquals(2, p.getPagina());
        assertEquals(5, p.getFilas().size());
        assertEquals(21, p.getFilas().get(0).getFila().intValue());
        assertEquals(521L, p.getFilas().get(0).getCodCheque().longValue(), "el orden es el del procedimiento, sin reordenar");
        assertEquals("Bs", p.getFilas().get(0).getDescMoneda());

        f.setPagina(1);
        assertTrue(servicio.pendientes(f).getFilas().get(0).isChequeCerrado(), "CERRADO: no se ofrece 'Seleccionar'");
    }

    // ------------------------------------------------------------------ preparar ("Seleccionar")

    @Test
    @DisplayName("preparar: devuelve el cheque, con su moneda, y hoy como fecha sugerida")
    void prepararFeliz() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(pendiente(321, "PENDIENTE"));
        when(cheques.obtener(321)).thenReturn(chequeConMoneda("SUS", "$us"));

        PrepararVerificacionDto r = servicio.preparar(321L);

        assertEquals(321L, r.getCheque().getCodCheque().longValue());
        assertEquals(2L, r.getCheque().getCodBanco().longValue(), "el banco DEL CHEQUE, que el formulario propone");
        assertEquals("$us", r.getCheque().getDescMoneda());
        assertFalse(r.getCheque().isChequeCerrado());
        assertEquals(HOY, r.getFechaBanco());
    }

    @Test
    @DisplayName("preparar: sin id, cheque inexistente, ya verificado y cerrado dan 400 con su explicacion")
    void prepararErrores() {
        assertEquals(MensajesVerificacion.FALTA_CHEQUE_A_PREPARAR, errorDe(() -> servicio.preparar(null)));
        assertEquals(MensajesVerificacion.FALTA_CHEQUE_A_PREPARAR, errorDe(() -> servicio.preparar(0L)));

        when(dao.sinRegularizarDeUnCheque(404L)).thenReturn(null);
        when(dao.tieneVerificacionValida(404L)).thenReturn(false);
        assertEquals(MensajesVerificacion.chequeNoEncontrado(404L), errorDe(() -> servicio.preparar(404L)));

        when(dao.sinRegularizarDeUnCheque(405L)).thenReturn(null);
        when(dao.tieneVerificacionValida(405L)).thenReturn(true);
        assertEquals(MensajesVerificacion.chequeYaVerificado(405L), errorDe(() -> servicio.preparar(405L)));

        when(dao.sinRegularizarDeUnCheque(406L)).thenReturn(pendiente(406, "CERRADO"));
        String cerrado = errorDe(() -> servicio.preparar(406L));
        assertEquals(MensajesVerificacion.chequeCerrado("5551", 406L), cerrado);
    }

    // ------------------------------------------------------------------ registrar: alta

    @Test
    @DisplayName("alta: escribe estado Y, el cheque, el banco, la fecha y el usuario del TOKEN; devuelve el codvd")
    void altaFeliz() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(pendiente(321, "PENDIENTE"));

        long id = servicio.registrar(usuario, alta(321));

        assertEquals(77L, id);
        ChVerificacionDeposito v = escritaPorRegistrar();
        assertEquals("Y", v.getEstado());
        assertEquals(321L, v.getCodCheque().longValue());
        assertEquals(2L, v.getCodBanco().longValue());
        assertEquals(HOY, v.getFechaBanco());
        assertEquals("Deposito OK", v.getObservacion());
        assertEquals(7, v.getAudUsuario().intValue(), "el usuario sale del token");
        assertNull(v.getCodvd(), "es un alta");
        verify(dao, never()).actualizar(any());
    }

    @Test
    @DisplayName("alta: sin observacion (null o solo espacios) se escribe vacio, como el legacy (que no guarda NULL)")
    void altaSinObservacion() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(pendiente(321, "PENDIENTE"));
        VerificacionRegistroDto d = alta(321);

        d.setObservacion(null);
        servicio.registrar(usuario, d);
        d.setObservacion("   ");
        servicio.registrar(usuario, d);

        ArgumentCaptor<ChVerificacionDeposito> cap = ArgumentCaptor.forClass(ChVerificacionDeposito.class);
        verify(dao, times(2)).registrar(cap.capture());
        assertEquals("", cap.getAllValues().get(0).getObservacion());
        assertEquals("", cap.getAllValues().get(1).getObservacion());
    }

    @Test
    @DisplayName("alta: un cheque CERRADO no se verifica (el JSF no dibujaba 'Seleccionar')")
    void altaChequeCerrado() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(pendiente(321, "CERRADO"));

        String m = errorDe(() -> servicio.registrar(usuario, alta(321)));

        assertEquals(MensajesVerificacion.chequeCerrado("5551", 321L), m);
        verify(dao, never()).registrar(any());
    }

    @Test
    @DisplayName("alta: un cheque que YA tiene una verificacion valida (Y) no admite una segunda")
    void altaSegundaValida() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(null);
        when(dao.tieneVerificacionValida(321L)).thenReturn(true);

        String m = errorDe(() -> servicio.registrar(usuario, alta(321)));

        assertEquals(MensajesVerificacion.chequeYaVerificado(321L), m);
        verify(dao, never()).registrar(any());
    }

    @Test
    @DisplayName("alta: un cheque inexistente se rechaza con un mensaje que lo nombra")
    void altaChequeInexistente() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(null);
        when(dao.tieneVerificacionValida(321L)).thenReturn(false);

        String m = errorDe(() -> servicio.registrar(usuario, alta(321)));

        assertEquals(MensajesVerificacion.chequeNoEncontrado(321L), m);
        verify(dao, never()).registrar(any());
    }

    @Test
    @DisplayName("alta: el banco de la verificacion tiene que existir")
    void altaBancoInexistente() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(pendiente(321, "PENDIENTE"));
        VerificacionRegistroDto d = alta(321);
        d.setCodBanco(99L);
        when(bancos.obtener(99)).thenReturn(null);

        assertEquals(MensajesVerificacion.bancoNoEncontrado(99L), errorDe(() -> servicio.registrar(usuario, d)));

        d.setCodBanco(5_000_000_000L);   // fuera del rango de int: no existe y no revienta
        assertEquals(MensajesVerificacion.bancoNoEncontrado(5_000_000_000L), errorDe(() -> servicio.registrar(usuario, d)));
        verify(dao, never()).registrar(any());
    }

    @Test
    @DisplayName("alta: los errores de negocio se acumulan, uno por linea (banco inexistente Y cheque cerrado)")
    void altaAcumulaErroresDeNegocio() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(pendiente(321, "CERRADO"));
        VerificacionRegistroDto d = alta(321);
        d.setCodBanco(99L);

        String m = errorDe(() -> servicio.registrar(usuario, d));

        assertEquals(MensajesVerificacion.bancoNoEncontrado(99L) + "\n" + MensajesVerificacion.chequeCerrado("5551", 321L), m);
    }

    @Test
    @DisplayName("alta: faltan fecha, banco y cheque -> los tres mensajes juntos y no se consulta ni se escribe nada")
    void altaCamposFaltantes() {
        VerificacionRegistroDto d = new VerificacionRegistroDto();

        String m = errorDe(() -> servicio.registrar(usuario, d));

        assertEquals(MensajesVerificacion.FALTA_FECHA + "\n" + MensajesVerificacion.FALTA_BANCO + "\n" + MensajesVerificacion.FALTA_CHEQUE, m);
        verify(dao, never()).registrar(any());
        verify(dao, never()).sinRegularizarDeUnCheque(anyLong());
        verify(bancos, never()).obtener(anyInt());
    }

    @Test
    @DisplayName("alta: el banco 0 o negativo cuenta como faltante; sin cuerpo es 400")
    void altaBancoCeroYSinCuerpo() {
        VerificacionRegistroDto d = alta(321);
        d.setCodBanco(0L);
        assertEquals(MensajesVerificacion.FALTA_BANCO, errorDe(() -> servicio.registrar(usuario, d)));

        assertEquals(MensajesVerificacion.SIN_DATOS, errorDe(() -> servicio.registrar(usuario, null)));
    }

    @Test
    @DisplayName("la observacion con tilde, ene o punto y coma se rechaza, junto con la fecha faltante, y NO se llega a la base")
    void observacionInvalidaSeAcumulaConLosCampos() {
        VerificacionRegistroDto d = alta(321);
        d.setFechaBanco(null);
        d.setObservacion("Depósito; año");

        String m = errorDe(() -> servicio.registrar(usuario, d));

        String[] lineas = m.split("\n");
        assertEquals(2, lineas.length, m);
        assertEquals(MensajesVerificacion.FALTA_FECHA, lineas[0]);
        assertTrue(lineas[1].startsWith("El texto de la observación «Depósito; año» no es válido"), lineas[1]);
        verify(dao, never()).sinRegularizarDeUnCheque(anyLong());
        verify(dao, never()).registrar(any());
    }

    @Test
    @DisplayName("la observacion de 51 caracteres se rechaza; la de 50 pasa")
    void observacionLargo() {
        when(dao.sinRegularizarDeUnCheque(321L)).thenReturn(pendiente(321, "PENDIENTE"));
        VerificacionRegistroDto d = alta(321);
        StringBuilder cincuenta = new StringBuilder();
        for (int i = 0; i < 50; i++) cincuenta.append('a');

        d.setObservacion(cincuenta + "a");
        assertTrue(errorDe(() -> servicio.registrar(usuario, d)).contains("es demasiado largo (51 caracteres)"));
        verify(dao, never()).registrar(any());

        d.setObservacion(cincuenta.toString());
        servicio.registrar(usuario, d);
        assertEquals(cincuenta.toString(), escritaPorRegistrar().getObservacion());
    }

    // ------------------------------------------------------------------ registrar: edicion

    @Test
    @DisplayName("edicion: conserva el cheque y el estado de la verificacion cargada, cambia banco/fecha/observacion y devuelve el codvd")
    void edicionConservaChequeYEstado() {
        when(dao.obtener(55)).thenReturn(verificacion(55, 321, "Y"));
        VerificacionRegistroDto d = new VerificacionRegistroDto();
        d.setCodvd(55);
        d.setCodCheque(999L);        // el cliente no puede cambiar el cheque: se ignora
        d.setCodBanco(2L);
        d.setFechaBanco(HOY);
        d.setObservacion("corregida");

        long id = servicio.registrar(usuario, d);

        assertEquals(55L, id);
        ChVerificacionDeposito v = escritaPorActualizar();
        assertEquals(55, v.getCodvd().intValue());
        assertEquals(321L, v.getCodCheque().longValue(), "el del registro cargado, no el que mando el cliente");
        assertEquals("Y", v.getEstado());
        assertEquals("corregida", v.getObservacion());
        assertEquals(HOY, v.getFechaBanco());
        assertEquals(7, v.getAudUsuario().intValue());
        verify(dao, never()).registrar(any());
    }

    @Test
    @DisplayName("edicion de una verificacion ANULADA: queda anulada (editar no la reactiva, como el JSF)")
    void edicionDeAnuladaSigueAnulada() {
        when(dao.obtener(56)).thenReturn(verificacion(56, 322, "N"));
        VerificacionRegistroDto d = alta(322);
        d.setCodvd(56);

        servicio.registrar(usuario, d);

        assertEquals("N", escritaPorActualizar().getEstado());
    }

    @Test
    @DisplayName("edicion: NO vuelve a exigir cheque abierto ni 'sin otra Y' (el legacy no lo hacia): ni mira el cheque")
    void edicionNoVuelveAExigirAbierto() {
        VerificacionDepositoFilaDto actual = verificacion(57, 323, "Y");
        actual.setDatoEstadoCheque("CERRADO");   // el cheque ya se cerro con esta misma verificacion
        when(dao.obtener(57)).thenReturn(actual);
        VerificacionRegistroDto d = alta(323);
        d.setCodvd(57);

        servicio.registrar(usuario, d);

        verify(dao).actualizar(any());
        verify(dao, never()).sinRegularizarDeUnCheque(anyLong());
        verify(dao, never()).tieneVerificacionValida(anyLong());
    }

    @Test
    @DisplayName("edicion de una verificacion que no existe: 400 que la nombra, sin escribir")
    void edicionInexistente() {
        when(dao.obtener(58)).thenReturn(null);
        VerificacionRegistroDto d = alta(321);
        d.setCodvd(58);

        assertEquals(MensajesVerificacion.verificacionNoEncontrada(58L), errorDe(() -> servicio.registrar(usuario, d)));
        verify(dao, never()).actualizar(any());
    }

    @Test
    @DisplayName("edicion: el banco tambien tiene que existir y la fecha es obligatoria")
    void edicionValidaBancoYFecha() {
        when(dao.obtener(55)).thenReturn(verificacion(55, 321, "Y"));
        VerificacionRegistroDto d = alta(321);
        d.setCodvd(55);
        d.setCodBanco(99L);
        assertEquals(MensajesVerificacion.bancoNoEncontrado(99L), errorDe(() -> servicio.registrar(usuario, d)));

        d.setCodBanco(2L);
        d.setFechaBanco(null);
        assertEquals(MensajesVerificacion.FALTA_FECHA, errorDe(() -> servicio.registrar(usuario, d)));
        verify(dao, never()).actualizar(any());
    }

    // ------------------------------------------------------------------ anular ("Cancelar")

    @Test
    @DisplayName("anular: escribe estado N con los mismos banco, fecha, observacion y cheque, y el usuario del token; NO borra")
    void anularNormal() {
        when(dao.obtener(55)).thenReturn(verificacion(55, 321, "Y"));

        boolean escribio = servicio.anular(usuario, 55L);

        assertTrue(escribio);
        ChVerificacionDeposito v = escritaPorActualizar();
        assertEquals("N", v.getEstado());
        assertEquals(55, v.getCodvd().intValue());
        assertEquals(321L, v.getCodCheque().longValue());
        assertEquals(2L, v.getCodBanco().longValue());
        assertEquals(LocalDate.of(2026, 10, 1), v.getFechaBanco());
        assertEquals("ya estaba", v.getObservacion());
        assertEquals(7, v.getAudUsuario().intValue());
        verify(dao, never()).registrar(any());
    }

    @Test
    @DisplayName("anular una que YA estaba anulada: no escribe nada y lo dice (idempotente)")
    void anularYaAnulada() {
        when(dao.obtener(55)).thenReturn(verificacion(55, 321, "N"));

        assertFalse(servicio.anular(usuario, 55L));

        verify(dao, never()).actualizar(any());
    }

    @Test
    @DisplayName("anular: inexistente o sin id es 400 con su mensaje; un id fuera del rango de int tampoco existe")
    void anularErrores() {
        when(dao.obtener(404)).thenReturn(null);
        assertEquals(MensajesVerificacion.verificacionNoEncontrada(404L), errorDe(() -> servicio.anular(usuario, 404L)));
        assertEquals(MensajesVerificacion.FALTA_VERIFICACION_A_ANULAR, errorDe(() -> servicio.anular(usuario, null)));
        assertEquals(MensajesVerificacion.FALTA_VERIFICACION_A_ANULAR, errorDe(() -> servicio.anular(usuario, 0L)));
        assertEquals(MensajesVerificacion.verificacionNoEncontrada(9_999_999_999L),
                errorDe(() -> servicio.anular(usuario, 9_999_999_999L)));
        verify(dao, never()).actualizar(any());
    }

    @Test
    @DisplayName("anular una verificacion de un cheque CERRADO se permite (el legacy no lo impedia)")
    void anularDeChequeCerrado() {
        VerificacionDepositoFilaDto v = verificacion(55, 321, "Y");
        v.setDatoEstadoCheque("CERRADO");
        when(dao.obtener(55)).thenReturn(v);

        assertTrue(servicio.anular(usuario, 55L));
    }

    // ------------------------------------------------------------------ usuario

    @Test
    @DisplayName("sin identidad en el token las escrituras se cortan con 403 (nunca una auditoria anonima) y no tocan la base")
    void sinIdentidad() {
        Authentication anonimo = new UsernamePasswordAuthenticationToken("x", null, Collections.<SimpleGrantedAuthority>emptyList());

        assertThrows(AccessDeniedException.class, () -> servicio.registrar(anonimo, alta(321)));
        assertThrows(AccessDeniedException.class, () -> servicio.anular(anonimo, 55L));
        verify(dao, never()).registrar(any());
        verify(dao, never()).actualizar(any());
        verify(dao, never()).obtener(eq(55));
    }

    // ------------------------------------------------------------------ paginar

    @Test
    @DisplayName("paginar: tamanios y paginas raras se acotan")
    void paginar() {
        List<Integer> cien = new ArrayList<>();
        for (int i = 1; i <= 100; i++) cien.add(i);
        List<Integer> vistos = new ArrayList<>();

        VerificacionPaginaDto<Integer> p = VerificacionChequeService.paginar(cien, -4, -1, (n, fila) -> vistos.add(fila));
        assertEquals(20, p.getTamanio());
        assertEquals(1, p.getPagina());
        assertEquals(20, p.getFilas().size());
        assertEquals(20, vistos.size());

        VerificacionPaginaDto<Integer> ultima = VerificacionChequeService.paginar(cien, 100, 30, (n, fila) -> { });
        assertEquals(4, ultima.getPagina());
        assertEquals(10, ultima.getFilas().size());
        assertEquals(100, ultima.getTotal());
    }
}
