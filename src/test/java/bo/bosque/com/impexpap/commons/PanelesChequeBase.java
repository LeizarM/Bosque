package bo.bosque.com.impexpap.commons;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import bo.bosque.com.impexpap.dao.IChCheque;
import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * Lo comun de las pruebas de los tres paneles del detalle del cheque: un cheque 18129 de la sucursal 3, el usuario 7
 * (ROLE_LIM, con {@code btnDetalleCH} y ningun otro boton salvo que el caso lo diga) y un administrador. Los permisos de sucursal estan simulados en
 * {@link ChequeService} (ya probado aparte); aqui se prueba que cada servicio los pide, y en que orden.
 */
abstract class PanelesChequeBase {

    static final int COD = 18129;
    static final long SUCURSAL = 3L;

    IChCheque cheques;
    ChequeService chequeService;
    AccesoModuloHelper helper;
    AccesoPanelesCheque acceso;

    Authentication usuario;
    Authentication admin;

    @BeforeEach
    void prepararBase() {
        cheques = mock(IChCheque.class);
        chequeService = mock(ChequeService.class);
        helper = mock(AccesoModuloHelper.class);
        acceso = new AccesoPanelesCheque(cheques, chequeService, helper);

        usuario = token("ROLE_LIM");
        admin = token("ROLE_ADM");
        when(helper.esAdmin(admin)).thenReturn(true);

        cheque(chequeDe(COD, SUCURSAL, "PEN"));
        soloDetalle();
    }

    static Authentication token(String rol) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority(rol)));
        t.setDetails(new DatosToken(7, 3, 1, rol.equals("ROLE_ADM") ? "adm" : "lim"));
        return t;
    }

    static Date d(int anio, int mes, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(anio, mes, dia));
    }

    static ChequeFilaDto chequeDe(int cod, long sucursal, String estado) {
        ChequeFilaDto c = new ChequeFilaDto();
        c.setCodCheque(cod);
        c.setNrocheque("5551");
        c.setEstado(estado);
        c.setCodSucursal(sucursal);
        return c;
    }

    void cheque(ChequeFilaDto c) {
        when(cheques.obtener(c.getCodCheque())).thenReturn(c);
    }

    /**
     * El usuario no tiene NINGUN boton de la vista 42, ni siquiera {@code btnDetalleCH} («Completar»): exigirBoton corta con 403
     * (como el real) y no llega a ningun panel. El administrador pasa en el helper real.
     */
    void sinBotones() {
        doThrow(new AccessDeniedException("sin boton")).when(helper).exigirBoton(any(), anyInt(), anyString());
    }

    /**
     * El usuario entro al detalle ({@code btnDetalleCH}) y no tiene ningun otro boton: es el estado de partida de cada prueba
     * (sin {@code btnNuevoNRCH} ni {@code btnEliminarNRCH}). Desde la diferencia #4 de la auditoria de permisos los paneles
     * se alcanzan solo con ese boton.
     */
    void soloDetalle() {
        conBotones();
    }

    /** El usuario tiene estos botones de la vista 42 Y {@code btnDetalleCH} (sin el no se llega a los paneles). */
    void conBotones(String... botones) {
        conBotonesSinDetalle(botones);
        doNothing().when(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, AccesoPanelesCheque.BTN_DETALLE);
    }

    /** El usuario tiene estos botones de la vista 42 pero NO {@code btnDetalleCH}: no llega a los paneles. */
    void conBotonesSinDetalle(String... botones) {
        sinBotones();
        for (String b : botones) {
            doNothing().when(helper).exigirBoton(usuario, ChequeService.VISTA_CHEQUES, b);
        }
    }

    /** La unica pregunta de boton que se hizo fue {@code btnDetalleCH} (las operaciones sin boton propio). */
    void verificarSoloSePidioElDetalle(Authentication quien) {
        org.mockito.Mockito.verify(helper).exigirBoton(quien, ChequeService.VISTA_CHEQUES, AccesoPanelesCheque.BTN_DETALLE);
        org.mockito.Mockito.verify(helper, org.mockito.Mockito.never()).exigirBoton(
                any(), anyInt(), org.mockito.ArgumentMatchers.argThat(b -> !AccesoPanelesCheque.BTN_DETALLE.equals(b)));
    }
}
