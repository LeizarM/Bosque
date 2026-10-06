package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IChNotaRemision;
import bo.bosque.com.impexpap.dto.NotaRemisionDto;
import bo.bosque.com.impexpap.dto.NotaRemisionRequest;
import bo.bosque.com.impexpap.model.ChNotaRemision;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * El panel de notas de remision con el DAO simulado: lo que se prueba son las REGLAS (permisos, orden, campos, paridad con el
 * legacy) y los mensajes, no el SQL.
 */
class NotaRemisionChequeServiceTest extends PanelesChequeBase {

    private IChNotaRemision notas;
    private NotaRemisionChequeService servicio;

    @BeforeEach
    void preparar() {
        notas = mock(IChNotaRemision.class);
        servicio = new NotaRemisionChequeService(notas, acceso);
        when(notas.registrar(any())).thenReturn(new RespuestaSp(0, "", 1L));
        when(notas.eliminar(anyInt(), anyString(), anyInt())).thenReturn(new RespuestaSp(0, "", 1L));
    }

    private static NotaRemisionRequest nota(String nota, Integer factura, Date fecha) {
        NotaRemisionRequest r = new NotaRemisionRequest();
        r.setCodCheque(COD);
        r.setNotaRemision(nota);
        r.setNroFactura(factura);
        r.setFechaFactura(fecha);
        return r;
    }

    private static NotaRemisionRequest valida() {
        return nota("262211881", 1856, d(2026, 8, 31));
    }

    private static NotaRemisionDto fila(int n, String nota) {
        NotaRemisionDto f = new NotaRemisionDto();
        f.setFila(n);
        f.setCodCheque(COD);
        f.setNotaRemision(nota);
        f.setNroFactura(1856);
        f.setFechaFactura(d(2026, 8, 31));
        return f;
    }

    private static String mensaje(Runnable accion) {
        return assertThrows(SpBusinessException.class, accion::run).getMessage();
    }

    // ------------------------------------------------------------------ listar

    @Test
    @DisplayName("listar: devuelve las notas del cheque; sin boton propio: pide solo btnDetalleCH, que el cheque exista y su sucursal sea visible")
    void listar() {
        List<NotaRemisionDto> filas = Arrays.asList(fila(1, "262211881"), fila(2, "262211882"));
        when(notas.listarPorCheque(COD)).thenReturn(filas);

        assertSame(filas, servicio.listar(usuario, COD));

        verify(cheques).obtener(COD);
        verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        verificarSoloSePidioElDetalle(usuario);
        verify(chequeService, never()).exigirSucursalEscritura(any(), any());
    }

    @Test
    @DisplayName("listar: cheque inexistente, o sin codigo, se explica con el dato concreto y no consulta la tabla")
    void listarChequeInexistente() {
        assertEquals(MensajesCheque.chequeNoEncontrado(999), mensaje(() -> servicio.listar(usuario, 999)));
        assertEquals("No se encontró el cheque 999. Es posible que ya no exista: actualiza la pantalla.",
                MensajesCheque.chequeNoEncontrado(999));
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.listar(usuario, null)));
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.listar(usuario, 0)));
        verify(notas, never()).listarPorCheque(anyInt());
    }

    @Test
    @DisplayName("listar: si la sucursal del cheque no es visible para el usuario, 403 y no se consulta la tabla")
    void listarSucursalNoVisible() {
        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());

        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.listar(usuario, COD));

        assertEquals(MensajesCheque.SOLO_SU_SUCURSAL, e.getMessage());
        verify(notas, never()).listarPorCheque(anyInt());
    }

    // ------------------------------------------------------------------ registrar

    @Test
    @DisplayName("registrar: guarda la nota con el cheque y el usuario del TOKEN, devuelve 1 y pide en orden btnNuevoNRCH, btnDetalleCH, cheque, sucursal visible, sucursal de escritura")
    void registrar() {
        conBotones("btnNuevoNRCH");

        int filas = servicio.registrar(usuario, valida());

        assertEquals(1, filas);
        InOrder o = inOrder(helper, cheques, chequeService, notas);
        o.verify(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, "btnNuevoNRCH");
        o.verify(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, "btnDetalleCH");
        o.verify(cheques).obtener(COD);
        o.verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        o.verify(chequeService).exigirSucursalEscritura(usuario, SUCURSAL);
        ArgumentCaptor<ChNotaRemision> c = ArgumentCaptor.forClass(ChNotaRemision.class);
        o.verify(notas).registrar(c.capture());
        ChNotaRemision g = c.getValue();
        assertEquals(Integer.valueOf(COD), g.getCodCheque());
        assertEquals("262211881", g.getNotaRemision());
        assertEquals(Integer.valueOf(1856), g.getNroFactura());
        assertEquals(d(2026, 8, 31), g.getFechaFactura());
        assertEquals(Integer.valueOf(7), g.getAudUsuario(), "el usuario de auditoria sale del token (codUsuario 7)");
    }

    @Test
    @DisplayName("registrar sin btnNuevoNRCH: 403 que dice cual boton falta, y no se toca el cheque ni la tabla")
    void registrarSinBoton() {
        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, valida()));

        assertEquals(MensajesCheque.sinPermiso("btnNuevoNRCH"), e.getMessage());
        verify(cheques, never()).obtener(anyInt());
        verify(notas, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: el administrador pasa el boton (btnNuevoNRCH) en el helper real; el servicio solo lo pregunta")
    void registrarAdmin() {
        doThrow(new org.springframework.security.access.AccessDeniedException("x")).when(helper).exigirBoton(eq(usuario), anyInt(), anyString());
        org.mockito.Mockito.doNothing().when(helper).exigirBoton(eq(admin), anyInt(), anyString());

        assertEquals(1, servicio.registrar(admin, valida()));
        verify(notas).registrar(any());
    }

    @Test
    @DisplayName("registrar: sin cuerpo, sin cheque, con un cheque que no existe o de una sucursal que no puede ver")
    void registrarCheque() {
        conBotones("btnNuevoNRCH");
        assertEquals(MensajesCheque.SIN_DATOS, mensaje(() -> servicio.registrar(usuario, null)));

        NotaRemisionRequest sinCheque = valida();
        sinCheque.setCodCheque(null);
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.registrar(usuario, sinCheque)));

        NotaRemisionRequest otro = valida();
        otro.setCodCheque(77);
        assertEquals(MensajesCheque.chequeNoEncontrado(77), mensaje(() -> servicio.registrar(usuario, otro)));

        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        assertThrows(SinPermisoException.class, () -> servicio.registrar(usuario, valida()));
        verify(notas, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: los errores de campo se acumulan, uno por linea, en el orden del formulario (nota, factura, fecha) y no se escribe nada")
    void registrarErroresAcumulados() {
        conBotones("btnNuevoNRCH");

        String m = mensaje(() -> servicio.registrar(usuario, nota("12a", 0, null)));

        assertEquals(String.join("\n",
                "El número de la nota de remisión «12a» no es válido: tiene caracteres que no se aceptan («a»). "
                        + "Solo acepta números y espacios, de 5 a 10 caracteres.",
                "El número de factura debe ser mayor a cero y escribiste 0. Escribe el número real de la factura de SAP.",
                MensajesCheque.FALTA_FECHA_FACTURA), m);
        verify(notas, never()).registrar(any());
        verify(chequeService, never()).exigirSucursalEscritura(any(), any());
    }

    @Test
    @DisplayName("registrar: cada campo, solo: nota vacia, factura vacia, fecha vacia")
    void registrarCamposFaltantes() {
        conBotones("btnNuevoNRCH");
        assertEquals(MensajesCheque.FALTA_NOTA_REMISION, mensaje(() -> servicio.registrar(usuario, nota("", 1, d(2026, 1, 1)))));
        assertEquals(MensajesCheque.FALTA_NRO_FACTURA, mensaje(() -> servicio.registrar(usuario, nota("12345", null, d(2026, 1, 1)))));
        assertEquals(MensajesCheque.FALTA_FECHA_FACTURA, mensaje(() -> servicio.registrar(usuario, nota("12345", 1, null))));
    }

    @Test
    @DisplayName("registrar: nota de 5 a 10 digitos o espacios (' 17330357', '4386 4420'); la factura de 1 a 999999")
    void registrarLimites() {
        conBotones("btnNuevoNRCH");
        for (String n : new String[] {"12345", "1234567890", " 17330357", "4386 4420", "     "}) {
            assertEquals(1, servicio.registrar(usuario, nota(n, 1, d(2026, 1, 1))), "«" + n + "»");
        }
        assertEquals(1, servicio.registrar(usuario, nota("12345", 999_999, d(2026, 1, 1))));
        assertEquals(MensajesCheque.nroFacturaMuyLargo(1_000_000, 999_999),
                mensaje(() -> servicio.registrar(usuario, nota("12345", 1_000_000, d(2026, 1, 1)))));
        mensaje(() -> servicio.registrar(usuario, nota("1234", 1, d(2026, 1, 1))));
        mensaje(() -> servicio.registrar(usuario, nota("12345678901", 1, d(2026, 1, 1))));
        verify(notas, times(6)).registrar(any());
    }

    @Test
    @DisplayName("registrar: primero los campos y despues el permiso de escritura en la sucursal (orden de saveNotaRemision); sin permiso no escribe")
    void registrarSucursalSinEscritura() {
        conBotones("btnNuevoNRCH");
        doThrow(new SpBusinessException(MensajesCheque.SUCURSAL_AJENA)).when(chequeService).exigirSucursalEscritura(any(), any());

        // con un campo mal, el mensaje es el del campo, no el de la sucursal
        assertEquals(MensajesCheque.FALTA_FECHA_FACTURA, mensaje(() -> servicio.registrar(usuario, nota("12345", 1, null))));
        // con todo bien, el de la sucursal
        assertEquals(MensajesCheque.SUCURSAL_AJENA, mensaje(() -> servicio.registrar(usuario, valida())));
        verify(notas, never()).registrar(any());
    }

    @Test
    @DisplayName("registrar: NO valida duplicados (ni Java ni el SP en el legacy): la misma nota dos veces se inserta dos veces y no se consulta la lista")
    void registrarDuplicadosPermitidos() {
        conBotones("btnNuevoNRCH");

        servicio.registrar(usuario, valida());
        servicio.registrar(usuario, valida());

        verify(notas, times(2)).registrar(any());
        verify(notas, never()).listarPorCheque(anyInt());
    }

    @Test
    @DisplayName("registrar no mira si el cheque esta cerrado: se puede agregar una nota a un cheque CER (el boton 'Nuevo' no dependia del estado)")
    void registrarEnChequeCerrado() {
        cheque(chequeDe(COD, SUCURSAL, "CER"));
        conBotones("btnNuevoNRCH");

        assertEquals(1, servicio.registrar(usuario, valida()));
    }

    // ------------------------------------------------------------------ eliminar

    @Test
    @DisplayName("eliminar sin btnEliminarNRCH: 403 que dice cual boton falta; btnNuevoNRCH no alcanza")
    void eliminarSinBoton() {
        conBotones("btnNuevoNRCH");

        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.eliminar(usuario, nota("262211881", null, null)));

        assertEquals(MensajesCheque.sinPermiso("btnEliminarNRCH"), e.getMessage());
        verify(notas, never()).eliminar(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("eliminar: borra la nota del cheque con el usuario del token y devuelve las filas eliminadas (1)")
    void eliminar() {
        conBotones("btnEliminarNRCH");
        when(notas.listarPorCheque(COD)).thenReturn(Arrays.asList(fila(1, "262211881"), fila(2, "262211882")));

        int filas = servicio.eliminar(usuario, nota("262211881", null, null));

        assertEquals(1, filas);
        InOrder o = inOrder(helper, cheques, chequeService, notas);
        o.verify(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, "btnEliminarNRCH");
        o.verify(cheques).obtener(COD);
        o.verify(chequeService).exigirSucursalLectura(usuario, SUCURSAL);
        o.verify(notas).listarPorCheque(COD);
        o.verify(notas).eliminar(COD, "262211881", 7);
        verify(chequeService, never()).exigirSucursalEscritura(any(), any());   // el legacy no miraba la sucursal de escritura al borrar
    }

    @Test
    @DisplayName("eliminar IGNORA que el cheque este cerrado (WizardCheque.esAutorizadoB tiene ese parametro comentado): se conserva")
    void eliminarEnChequeCerrado() {
        cheque(chequeDe(COD, SUCURSAL, "CER"));
        conBotones("btnEliminarNRCH");
        when(notas.listarPorCheque(COD)).thenReturn(Collections.singletonList(fila(1, "262211881")));

        assertEquals(1, servicio.eliminar(usuario, nota("262211881", null, null)));
        verify(notas).eliminar(COD, "262211881", 7);
    }

    @Test
    @DisplayName("eliminar una nota REPETIDA borra todas las del par (como el legacy): devuelve cuantas y el mensaje lo dice")
    void eliminarRepetidas() {
        conBotones("btnEliminarNRCH");
        when(notas.listarPorCheque(COD)).thenReturn(Arrays.asList(fila(1, "262211881"), fila(2, "262211882"), fila(3, "262211881")));

        int filas = servicio.eliminar(usuario, nota("262211881", null, null));

        assertEquals(2, filas);
        verify(notas, times(1)).eliminar(COD, "262211881", 7);
        assertEquals("Se eliminaron las 2 notas de remisión «262211881» que estaban repetidas en el cheque: "
                + "el sistema borra todas las que tienen el mismo número.", MensajesCheque.notaEliminada(2, "262211881"));
        assertEquals("Nota de remisión eliminada.", MensajesCheque.notaEliminada(1, "262211881"));
    }

    @Test
    @DisplayName("eliminar: la nota tiene que ser de ESE cheque; si no, error con el dato concreto y no se borra nada")
    void eliminarDeOtroCheque() {
        conBotones("btnEliminarNRCH");
        when(notas.listarPorCheque(COD)).thenReturn(Collections.singletonList(fila(1, "262211882")));

        String m = mensaje(() -> servicio.eliminar(usuario, nota("262211881", null, null)));

        assertEquals("El cheque 18129 no tiene la nota de remisión «262211881». Es posible que ya la hayan eliminado: actualiza la pantalla.", m);
        verify(notas, never()).eliminar(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("eliminar compara como SQL Server (sin los espacios del final), pero el espacio inicial si cuenta; el SP recibe el texto tal cual")
    void eliminarComparaComoSql() {
        conBotones("btnEliminarNRCH");
        when(notas.listarPorCheque(COD)).thenReturn(Collections.singletonList(fila(1, " 17330357")));

        assertEquals(1, servicio.eliminar(usuario, nota(" 17330357  ", null, null)));
        verify(notas).eliminar(COD, " 17330357  ", 7);

        mensaje(() -> servicio.eliminar(usuario, nota("17330357", null, null)));
    }

    @Test
    @DisplayName("eliminar: sin cuerpo, sin nota, sin cheque o con un cheque que no se ve")
    void eliminarParametros() {
        conBotones("btnEliminarNRCH");
        assertEquals(MensajesCheque.SIN_DATOS, mensaje(() -> servicio.eliminar(usuario, null)));
        assertEquals(MensajesCheque.FALTA_NOTA_A_ELIMINAR, mensaje(() -> servicio.eliminar(usuario, nota(null, null, null))));
        assertEquals(MensajesCheque.FALTA_NOTA_A_ELIMINAR, mensaje(() -> servicio.eliminar(usuario, nota("", null, null))));
        NotaRemisionRequest sinCheque = nota("262211881", null, null);
        sinCheque.setCodCheque(0);
        assertEquals(MensajesCheque.FALTA_CHEQUE_DEL_PANEL, mensaje(() -> servicio.eliminar(usuario, sinCheque)));

        doThrow(new SinPermisoException(MensajesCheque.SOLO_SU_SUCURSAL)).when(chequeService).exigirSucursalLectura(any(), any());
        assertThrows(SinPermisoException.class, () -> servicio.eliminar(usuario, nota("262211881", null, null)));
        verify(notas, never()).eliminar(anyInt(), anyString(), anyInt());
        verifyNoMoreInteractions(notas);
    }
}
