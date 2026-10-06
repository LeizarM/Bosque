package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.dao.IChTransaccionBancaria;
import bo.bosque.com.impexpap.dto.TransaccionBancariaDto;
import bo.bosque.com.impexpap.dto.TransaccionBancariaRequest;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.model.ChTransaccionBancaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * El panel de transacciones bancarias con los DAO simulados: reglas (permisos, orden de saveNroTransaccion, campos, paridad con
 * el legacy) y mensajes, no SQL.
 */
class TransaccionChequeServiceTest extends PanelesChequeBase {

    private IChTransaccionBancaria transacciones;
    private IChBanco bancos;
    private TransaccionChequeService servicio;

    @BeforeEach
    void preparar() {
        transacciones = mock(IChTransaccionBancaria.class);
        bancos = mock(IChBanco.class);
        servicio = new TransaccionChequeService(transacciones, bancos, acceso);
        when(transacciones.registrar(any())).thenReturn(new RespuestaSp(0, "", 1L));
        when(transacciones.eliminar(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 1L));
        ChBanco b = new ChBanco();
        b.setCodBanco(2);
        b.setNombre("BANCO UNION");
        when(bancos.obtener(2)).thenReturn(b);
    }

    private static TransaccionBancariaRequest tr(String nro, Integer banco, Date fecha) {
        TransaccionBancariaRequest r = new TransaccionBancariaRequest();
        r.setCodCheque(COD);
        r.setNroTransaccion(nro);
        r.setCodBanco(banco);
        r.setFechaTransaccion(fecha);
        return r;
    }

    private static TransaccionBancariaRequest valida() {
        return tr("TT262318S5JD", 2, d(2026, 8, 19));
    }

    private static TransaccionBancariaDto fila(int n, String nro) {
        TransaccionBancariaDto f = new TransaccionBancariaDto();
        f.setFila(n);
        f.setCodCheque(COD);
        f.setNroTransaccion(nro);
        f.setCodBanco(2);
        f.setDatoBanco("BANCO UNION");
        f.setFechaTransaccion(d(2026, 8, 19));
        return f;
    }

    private static String mensaje(Runnable accion) {
        return assertThrows(SpBusinessException.class, accion::run).getMessage();
    }

    // ------------------------------------------------------------------ listar

    @Test
    @DisplayName("listar: las transacciones del cheque; sin boton propio, solo btnDetalleCH, cheque existente y sucursal visible")
    void listar() {
        List<TransaccionBancariaDto> filas = Arrays.asList(fila(1, "TT262318S5JD"), fila(2, "14910211612"));
        when(transacciones.listarPorCheque(COD)).thenReturn(filas);

        assertSame(filas, servicio.listar(usuario, COD));

        verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        verificarSoloSePidioElDetalle(usuario);
    }

    @Test
    @DisplayName("listar: cheque inexistente, sin codigo, o de una sucursal que no se ve: no consulta la tabla")
    void listarSinAcceso() {
        assertEquals(MensajesCheque.chequeNoEncontrado(5), mensaje(() -> servicio.listar(usuario, 5)));
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.listar(usuario, null)));
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        assertThrows(SinPermisoException.class, () -> servicio.listar(usuario, COD));
        verify(transacciones, never()).listarPorCheque(anyInt());
    }

    // ------------------------------------------------------------------ registrar

    @Test
    @DisplayName("registrar: guarda con el cheque y el usuario del TOKEN; orden: btnNuevoNRCH, btnDetalleCH, cheque, sucursal visible, sucursal de escritura, banco")
    void registrar() {
        conBotones("btnNuevoNRCH");

        assertEquals(1, servicio.registrar(usuario, valida()));

        InOrder o = inOrder(helper, cheques, chequeService, bancos, transacciones);
        o.verify(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, "btnNuevoNRCH");
        o.verify(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, "btnDetalleCH");
        o.verify(cheques).obtener(COD);
        o.verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        o.verify(chequeService).exigirSucursalEscritura(usuario, SUCURSAL);
        o.verify(bancos).obtener(2);
        ArgumentCaptor<ChTransaccionBancaria> c = ArgumentCaptor.forClass(ChTransaccionBancaria.class);
        o.verify(transacciones).registrar(c.capture());
        ChTransaccionBancaria g = c.getValue();
        assertEquals(Integer.valueOf(COD), g.getCodCheque());
        assertEquals("TT262318S5JD", g.getNroTransaccion());
        assertEquals(Integer.valueOf(2), g.getCodBanco());
        assertEquals(d(2026, 8, 19), g.getFechaTransaccion());
        assertEquals(Integer.valueOf(7), g.getAudUsuario(), "el usuario de auditoria sale del token (codUsuario 7)");
    }

    @Test
    @DisplayName("registrar sin btnNuevoNRCH (el mismo boton que notas y postergaciones): 403 que dice cual falta y no se toca nada")
    void registrarSinBoton() {
        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, valida()));

        assertEquals(MensajesCheque.sinPermiso("btnNuevoNRCH"), e.getMessage());
        verify(cheques, never()).obtener(anyInt());
        verify(transacciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: sin cuerpo, sin cheque, cheque inexistente o de una sucursal que no se ve")
    void registrarCheque() {
        conBotones("btnNuevoNRCH");
        assertEquals(MensajesCheque.SIN_DATOS, mensaje(() -> servicio.registrar(usuario, null)));
        TransaccionBancariaRequest sinCheque = valida();
        sinCheque.setCodCheque(0);
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.registrar(usuario, sinCheque)));
        TransaccionBancariaRequest otro = valida();
        otro.setCodCheque(88);
        assertEquals(MensajesCheque.chequeNoEncontrado(88), mensaje(() -> servicio.registrar(usuario, otro)));
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, valida()));
        verify(transacciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: lo obligatorio (numero vacio, fecha vacia) se informa junto, ANTES del permiso de sucursal, y no consulta el banco")
    void registrarObligatorios() {
        conBotones("btnNuevoNRCH");
        doThrow(new SpBusinessException(MensajesCheque.SUCURSAL_AJENA)).when(chequeService).exigirSucursalEscritura(any(), any());

        String m = mensaje(() -> servicio.registrar(usuario, tr("", 2, null)));

        assertEquals(MensajesCheque.FALTA_NRO_TRANSACCION + "\n" + MensajesCheque.FALTA_FECHA_TRANSACCION, m);
        assertEquals(MensajesCheque.FALTA_NRO_TRANSACCION, mensaje(() -> servicio.registrar(usuario, tr(null, 2, d(2026, 1, 1)))));
        assertEquals(MensajesCheque.FALTA_FECHA_TRANSACCION, mensaje(() -> servicio.registrar(usuario, tr("ABCDE", 2, null))));
        verify(chequeService, never()).exigirSucursalEscritura(any(), any());
        verify(bancos, never()).obtener(anyInt());
        verify(transacciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: el permiso de sucursal va ANTES del largo del numero (saveNroTransaccion): sin permiso, un numero corto da el error de la sucursal")
    void registrarPermisoAntesQueElLargo() {
        conBotones("btnNuevoNRCH");
        doThrow(new SpBusinessException(MensajesCheque.SUCURSAL_AJENA)).when(chequeService).exigirSucursalEscritura(any(), any());

        assertEquals(MensajesCheque.SUCURSAL_AJENA, mensaje(() -> servicio.registrar(usuario, tr("123", 2, d(2026, 1, 1)))));
        verify(transacciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: mas de 4 caracteres (4 no, 5 si) y hasta 30 (31 se rechaza en vez de truncar); tal cual llega, sin recortar")
    void registrarLargoDelNumero() {
        conBotones("btnNuevoNRCH");

        assertEquals(MensajesCheque.nroTransaccionCorto("1234", 4), mensaje(() -> servicio.registrar(usuario, tr("1234", 2, d(2026, 1, 1)))));
        for (String ok : new String[] {"12345", "TT26216QW3N3", "123456789012345678901234567890", "     ", "a-b/c.d_e"}) {
            assertEquals(1, servicio.registrar(usuario, tr(ok, 2, d(2026, 1, 1))), "«" + ok + "»");
        }
        String treintaYUno = "1234567890123456789012345678901";
        assertEquals(MensajesCheque.nroTransaccionLargo(treintaYUno, 31, 30),
                mensaje(() -> servicio.registrar(usuario, tr(treintaYUno, 2, d(2026, 1, 1)))));
        verify(transacciones, times(5)).registrar(any());
    }

    @Test
    @DisplayName("registrar: el banco es obligatorio y tiene que existir (el JSF lo garantizaba con el desplegable); los errores se juntan con el del largo")
    void registrarBanco() {
        conBotones("btnNuevoNRCH");

        assertEquals(MensajesCheque.FALTA_BANCO_DE_TRANSACCION, mensaje(() -> servicio.registrar(usuario, tr("ABCDE", null, d(2026, 1, 1)))));
        assertEquals(MensajesCheque.FALTA_BANCO_DE_TRANSACCION, mensaje(() -> servicio.registrar(usuario, tr("ABCDE", 0, d(2026, 1, 1)))));
        assertEquals("No se encontró el banco 99. Es posible que ya lo hayan eliminado: actualiza la pantalla.",
                mensaje(() -> servicio.registrar(usuario, tr("ABCDE", 99, d(2026, 1, 1)))));
        assertEquals(MensajesCheque.nroTransaccionCorto("12", 2) + "\n" + MensajesCheque.bancoNoEncontrado(99),
                mensaje(() -> servicio.registrar(usuario, tr("12", 99, d(2026, 1, 1)))));
        verify(transacciones, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: no valida duplicados (el legacy tampoco): el mismo numero dos veces se inserta dos veces")
    void registrarDuplicadosPermitidos() {
        conBotones("btnNuevoNRCH");
        servicio.registrar(usuario, valida());
        servicio.registrar(usuario, valida());
        verify(transacciones, times(2)).registrar(any());
        verify(transacciones, never()).listarPorCheque(anyInt());
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    @DisplayName("eliminar NO tiene boton propio (como hoy): con solo btnDetalleCH (el del panel) elimina; pide cheque existente y sucursal visible")
    void eliminarSinBoton() {
        when(transacciones.listarPorCheque(COD)).thenReturn(Arrays.asList(fila(1, "TT262318S5JD"), fila(2, "14910211612")));

        int filas = servicio.eliminar(usuario, tr("TT262318S5JD", null, null));

        assertEquals(1, filas);
        InOrder o = inOrder(cheques, chequeService, transacciones);
        o.verify(cheques).obtener(COD);
        o.verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        o.verify(transacciones).listarPorCheque(COD);
        o.verify(transacciones).eliminar(COD, "TT262318S5JD", 7);
        verificarSoloSePidioElDetalle(usuario);
        verify(chequeService, never()).exigirSucursalEscritura(any(), any());
    }

    @Test
    @DisplayName("eliminar: la transaccion tiene que ser de ESE cheque (la clave es cheque + numero); si no, error con el dato y no se borra")
    void eliminarDeOtroCheque() {
        when(transacciones.listarPorCheque(COD)).thenReturn(Collections.singletonList(fila(1, "14910211612")));

        String m = mensaje(() -> servicio.eliminar(usuario, tr("TT262318S5JD", null, null)));

        assertEquals("El cheque 18129 no tiene la transacción bancaria «TT262318S5JD». Es posible que ya la hayan eliminado: actualiza la pantalla.", m);
        verify(transacciones, never()).eliminar(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("eliminar una transaccion REPETIDA borra todas las del par (como el legacy); el mensaje lo dice")
    void eliminarRepetidas() {
        when(transacciones.listarPorCheque(COD)).thenReturn(Arrays.asList(fila(1, "TT262318S5JD"), fila(2, "tt262318s5jd")));

        assertEquals(2, servicio.eliminar(usuario, tr("TT262318S5JD", null, null)));

        verify(transacciones, times(1)).eliminar(COD, "TT262318S5JD", 7);
        assertEquals("Transacción bancaria eliminada.", MensajesCheque.transaccionEliminada(1, "X"));
        assertEquals("Se eliminaron las 2 transacciones bancarias «TT262318S5JD» que estaban repetidas en el cheque: "
                + "el sistema borra todas las que tienen el mismo número.", MensajesCheque.transaccionEliminada(2, "TT262318S5JD"));
    }

    @Test
    @DisplayName("eliminar: sin cuerpo, sin numero, sin cheque o con un cheque que no se ve")
    void eliminarParametros() {
        assertEquals(MensajesCheque.SIN_DATOS, mensaje(() -> servicio.eliminar(usuario, null)));
        assertEquals(MensajesCheque.FALTA_TRANSACCION_A_ELIMINAR, mensaje(() -> servicio.eliminar(usuario, tr(null, null, null))));
        assertEquals(MensajesCheque.FALTA_TRANSACCION_A_ELIMINAR, mensaje(() -> servicio.eliminar(usuario, tr("", null, null))));
        TransaccionBancariaRequest sinCheque = tr("TT262318S5JD", null, null);
        sinCheque.setCodCheque(null);
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.eliminar(usuario, sinCheque)));
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        assertThrows(SinPermisoException.class, () -> servicio.eliminar(usuario, tr("TT262318S5JD", null, null)));
        verify(transacciones, never()).eliminar(anyInt(), anyString(), anyInt());
    }
}
