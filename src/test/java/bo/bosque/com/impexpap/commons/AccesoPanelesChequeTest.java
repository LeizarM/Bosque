package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dao.IProgramador;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.model.UsuarioBtn;

/**
 * Los botones de los paneles contra el {@link AccesoModuloHelper} REAL (solo los DAO del ACL estan simulados): el
 * administrador pasa siempre, quien no tiene el boton recibe un 403 que dice cual falta, y el boton tiene que ser de la vista 42.
 */
class AccesoPanelesChequeTest {

    private IProgramador programadorDao;
    private IUsuarioBtn usuarioBtnDao;
    private IChCheque cheques;
    private ChequeService chequeService;
    private AccesoPanelesCheque acceso;

    @BeforeEach
    void preparar() {
        programadorDao = mock(IProgramador.class);
        usuarioBtnDao = mock(IUsuarioBtn.class);
        cheques = mock(IChCheque.class);
        chequeService = mock(ChequeService.class);
        acceso = new AccesoPanelesCheque(cheques, chequeService, new AccesoModuloHelper(programadorDao, usuarioBtnDao));
    }

    private static ChequeFilaDto cheque(int cod, long sucursal) {
        ChequeFilaDto c = new ChequeFilaDto();
        c.setCodCheque(cod);
        c.setCodSucursal(sucursal);
        return c;
    }

    private void dado(UsuarioBtn... botones) {
        MiEquipoDto yo = new MiEquipoDto();
        yo.setCodUsuario(77L);
        when(programadorDao.resolverPermiso(anyString())).thenReturn(yo);
        List<UsuarioBtn> lst = new ArrayList<>(Arrays.asList(botones));
        when(usuarioBtnDao.botonesXUsuario(77)).thenReturn(lst);
    }

    private static UsuarioBtn boton(String nombre, int permiso, int codVista) {
        UsuarioBtn b = new UsuarioBtn();
        b.setBoton(nombre);
        b.setPermiso(permiso);
        b.setPertenVist(codVista);
        return b;
    }

    @Test
    @DisplayName("el boton de 'Nuevo' es btnNuevoNRCH y el de eliminar una nota es btnEliminarNRCH (tb_vistaBtn de la vista 42)")
    void nombresDeLosBotones() {
        assertEquals("btnNuevoNRCH", AccesoPanelesCheque.BTN_NUEVO);
        assertEquals("btnEliminarNRCH", AccesoPanelesCheque.BTN_ELIMINAR_NOTA);
        assertEquals(42, ChequeService.VISTA_CHEQUES);
    }

    @Test
    @DisplayName("el administrador pasa los dos botones sin consultar el ACL")
    void adminPasa() {
        assertDoesNotThrow(() -> acceso.exigirBoton(PanelesChequeBase.token("ROLE_ADM"), AccesoPanelesCheque.BTN_NUEVO));
        assertDoesNotThrow(() -> acceso.exigirBoton(PanelesChequeBase.token("ROLE_ADM"), AccesoPanelesCheque.BTN_ELIMINAR_NOTA));
        verify(programadorDao, never()).resolverPermiso(anyString());
        verify(usuarioBtnDao, never()).botonesXUsuario(anyInt());
    }

    @Test
    @DisplayName("ROLE_LIM con btnNuevoNRCH puede registrar, pero no eliminar notas: son dos botones distintos")
    void limConUnoSolo() {
        dado(boton("btnNuevoNRCH", 1, 42));

        assertDoesNotThrow(() -> acceso.exigirBoton(PanelesChequeBase.token("ROLE_LIM"), AccesoPanelesCheque.BTN_NUEVO));
        SinPermisoException e = assertThrows(SinPermisoException.class,
                () -> acceso.exigirBoton(PanelesChequeBase.token("ROLE_LIM"), AccesoPanelesCheque.BTN_ELIMINAR_NOTA));
        assertEquals(MensajesCheque.sinPermiso("btnEliminarNRCH"), e.getMessage());
        assertEquals("No tienes permiso para eliminar notas de remisión de un cheque. Tu usuario no tiene asignado el botón "
                + "btnEliminarNRCH; pídele al administrador que te lo asigne.", e.getMessage());
    }

    @Test
    @DisplayName("sin el boton: 403 con el texto que dice que permite y cual falta (btnNuevoNRCH)")
    void sinElBoton() {
        dado();

        SinPermisoException e = assertThrows(SinPermisoException.class,
                () -> acceso.exigirBoton(PanelesChequeBase.token("ROLE_LIM"), AccesoPanelesCheque.BTN_NUEVO));

        assertEquals("No tienes permiso para registrar notas de remisión, transacciones bancarias y postergaciones de un cheque. "
                + "Tu usuario no tiene asignado el botón btnNuevoNRCH; pídele al administrador que te lo asigne.", e.getMessage());
    }

    @Test
    @DisplayName("los paneles se alcanzan desde «Completar»: el boton que exigen todos es btnDetalleCH (cheque.xhtml:198; diferencia #4)")
    void elBotonDelPanelEsBtnDetalleCH() {
        assertEquals("btnDetalleCH", AccesoPanelesCheque.BTN_DETALLE);
    }

    @Test
    @DisplayName("chequeVisible: sin btnDetalleCH, 403 que lo nombra y NO toca el cheque ni la sucursal, aunque tenga los botones de panel")
    void chequeVisibleSinDetalle() {
        dado(boton("btnNuevoNRCH", 1, 42), boton("btnEliminarNRCH", 1, 42), boton("btnChqSucrs", 1, 42));

        SinPermisoException e = assertThrows(SinPermisoException.class,
                () -> acceso.chequeVisible(PanelesChequeBase.token("ROLE_LIM"), 18129));

        assertEquals(MensajesCheque.sinPermiso("btnDetalleCH"), e.getMessage());
        assertEquals("No tienes permiso para ver el detalle de un cheque y hacer sus acciones. Tu usuario no tiene asignado el botón "
                + "btnDetalleCH; pídele al administrador que te lo asigne.", e.getMessage());
        verify(cheques, never()).obtener(anyInt());
        verify(chequeService, never()).exigirSucursalLectura(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("chequeVisible: con btnDetalleCH (de la vista 42) sigue con el cheque y la sucursal; el administrador pasa sin consultar el ACL")
    void chequeVisibleConDetalle() {
        ChequeFilaDto c = cheque(18129, 3L);
        when(cheques.obtener(18129)).thenReturn(c);
        dado(boton("btnDetalleCH", 1, 42));
        Authentication lim = PanelesChequeBase.token("ROLE_LIM");

        assertSame(c, acceso.chequeVisible(lim, 18129));
        verify(chequeService).exigirSucursalLectura(lim, 3L);

        Authentication adm = PanelesChequeBase.token("ROLE_ADM");
        assertSame(c, acceso.chequeVisible(adm, 18129));
        verify(programadorDao, org.mockito.Mockito.times(1)).resolverPermiso(anyString());   // solo el de ROLE_LIM
    }

    @Test
    @DisplayName("chequeVisible: btnDetalleCH con permiso 0, o de otra vista, no vale")
    void chequeVisibleDetalleQueNoVale() {
        dado(boton("btnDetalleCH", 0, 42));
        assertThrows(SinPermisoException.class, () -> acceso.chequeVisible(PanelesChequeBase.token("ROLE_LIM"), 18129));

        dado(boton("btnDetalleCH", 1, 43));
        assertThrows(SinPermisoException.class, () -> acceso.chequeVisible(PanelesChequeBase.token("ROLE_LIM"), 18129));
        verify(cheques, never()).obtener(anyInt());
    }

    @Test
    @DisplayName("un boton con permiso 0, o con ese nombre pero de otra vista, no vale")
    void permisoCeroOOtraVista() {
        dado(boton("btnNuevoNRCH", 0, 42), boton("btnEliminarNRCH", 1, 43));

        assertThrows(SinPermisoException.class,
                () -> acceso.exigirBoton(PanelesChequeBase.token("ROLE_LIM"), AccesoPanelesCheque.BTN_NUEVO));
        assertThrows(SinPermisoException.class,
                () -> acceso.exigirBoton(PanelesChequeBase.token("ROLE_LIM"), AccesoPanelesCheque.BTN_ELIMINAR_NOTA));
    }
}
