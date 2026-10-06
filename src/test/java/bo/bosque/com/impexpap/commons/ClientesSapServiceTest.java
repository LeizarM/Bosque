package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ISocioNegocioSap;
import bo.bosque.com.impexpap.security.jwt.DatosToken;

/**
 * "ACTUALIZAR DATOS SAP": el servicio con el DAO simulado. Lo que se prueba es la regla (permiso del legacy, usuario del
 * token, una sola a la vez) y que la falla diga <i>que paso y que hacer</i> sin filtrar el detalle de la base.
 */
class ClientesSapServiceTest {

    private ISocioNegocioSap dao;
    private AccesoModuloHelper acceso;
    private ClientesSapService servicio;
    private Authentication usuario;

    @BeforeEach
    void preparar() {
        dao = mock(ISocioNegocioSap.class);
        acceso = mock(AccesoModuloHelper.class);
        servicio = new ClientesSapService(dao, acceso);
        usuario = token();
    }

    private static Authentication token() {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(7, 3, 1, "lim"));
        return t;
    }

    private void conPermiso() {
        doNothing().when(acceso).exigirBoton(usuario, 42, "btnNuevoCH");
    }

    private void sinPermiso() {
        doThrow(new AccessDeniedException("No tiene habilitado 'btnNuevoCH' en esta pantalla."))
                .when(acceso).exigirBoton(usuario, 42, "btnNuevoCH");
    }

    // ------------------------------------------------------------------ permiso

    @Test
    @DisplayName("exige el boton btnNuevoCH de la vista 42, el mismo del legacy (wInfoCenter.esAutorizado('btnNuevoCH'))")
    void exigeElBotonDelLegacy() {
        conPermiso();

        servicio.actualizarDesdeSap(usuario);

        verify(acceso).exigirBoton(usuario, 42, "btnNuevoCH");
    }

    @Test
    @DisplayName("sin el boton: 403 que dice que permiso falta y que hacer, y no se toca la base")
    void sinElBoton() {
        sinPermiso();

        SinPermisoException e = assertThrows(SinPermisoException.class, () -> servicio.actualizarDesdeSap(usuario));

        assertTrue(e.getMessage().contains("traer los clientes nuevos de SAP"), e.getMessage());
        assertTrue(e.getMessage().contains("btnNuevoCH"), e.getMessage());
        assertTrue(e.getMessage().contains("administrador"), "dice a quien pedirlo: " + e.getMessage());
        verify(dao, never()).actualizarDesdeSap(anyInt());
    }

    // ------------------------------------------------------------------ camino feliz

    @Test
    @DisplayName("llama al procedimiento una vez, con el usuario DEL TOKEN")
    void usuarioDelToken() {
        conPermiso();

        servicio.actualizarDesdeSap(usuario);

        verify(dao, times(1)).actualizarDesdeSap(7);
    }

    // ------------------------------------------------------------------ fallas

    @Test
    @DisplayName("un error del propio procedimiento pasa tal cual (ya trae su mensaje)")
    void errorDelProcedimiento() {
        conPermiso();
        doThrow(new SpBusinessException("Mensaje del procedimiento.")).when(dao).actualizarDesdeSap(7);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.actualizarDesdeSap(usuario));

        assertEquals("Mensaje del procedimiento.", e.getMessage());
    }

    @Test
    @DisplayName("SAP sin responder (falla de base de datos): mensaje explicito, sin el detalle interno ni promesas que el procedimiento no cumple")
    void sapSinResponder() {
        conPermiso();
        SQLException sap = new SQLException("OLE DB provider \"SQLNCLI\" for linked server (null) returned message "
                + "\"Login timeout expired\" 192.168.3.114", "42000", 7399);
        doThrow(new RuntimeException("Error de conexión o sintaxis en la base de datos.",
                new DataAccessResourceFailureException("x", sap))).when(dao).actualizarDesdeSap(7);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.actualizarDesdeSap(usuario));

        assertEquals(MensajesClientesSap.NO_SE_PUDO, e.getMessage());
        assertTrue(e.getMessage().contains("SAP") && e.getMessage().contains("Intenta de nuevo") && e.getMessage().contains("avisa a Sistemas"),
                "dice que fallo, por que y que hacer: " + e.getMessage());
        assertFalse(e.getMessage().contains("192.168") || e.getMessage().contains("7399") || e.getMessage().contains("SQLNCLI"),
                "no filtra datos internos: " + e.getMessage());
        // El procedimiento no es atomico: no se puede prometer que no se toco nada.
        assertFalse(e.getMessage().contains("no se modificó"), e.getMessage());
    }

    @Test
    @DisplayName("cualquier otra falla de base de datos tambien se traduce al mensaje explicito (no hay logica especial por numero de error)")
    void otraFalla() {
        conPermiso();
        doThrow(new RuntimeException("x", new SQLException("Procedure has too many arguments specified.", "S0001", 8144)))
                .when(dao).actualizarDesdeSap(7);

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.actualizarDesdeSap(usuario));

        assertEquals(MensajesClientesSap.NO_SE_PUDO, e.getMessage());
    }

    @Test
    @DisplayName("tras una falla se puede volver a intentar: el seguro de 'una a la vez' se libera siempre")
    void sePuedeReintentar() {
        conPermiso();
        doThrow(new RuntimeException("Error de conexión", new SQLException("caido", "08S01", 0)))
                .doNothing()
                .when(dao).actualizarDesdeSap(7);

        assertThrows(SpBusinessException.class, () -> servicio.actualizarDesdeSap(usuario));

        servicio.actualizarDesdeSap(usuario);
        verify(dao, times(2)).actualizarDesdeSap(7);
    }

    // ------------------------------------------------------------------ una a la vez

    @Test
    @DisplayName("una segunda actualizacion mientras otra corre recibe un mensaje claro (no espera ni falla por clave duplicada); el que no tiene permiso sigue viendo el 403")
    void unaALaVez() throws Exception {
        conPermiso();
        CountDownLatch enSap = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        doAnswer(inv -> {
            enSap.countDown();
            assertTrue(soltar.await(10, TimeUnit.SECONDS));
            return null;
        }).doNothing().when(dao).actualizarDesdeSap(7);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> primera = pool.submit(() -> servicio.actualizarDesdeSap(usuario));
            assertTrue(enSap.await(10, TimeUnit.SECONDS), "la primera llego al procedimiento");

            SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.actualizarDesdeSap(usuario));
            assertEquals(MensajesClientesSap.YA_EN_CURSO, e.getMessage());

            // El permiso se comprueba ANTES que el seguro: quien no puede no se entera de que hay una en curso.
            Authentication otro = token();
            doThrow(new AccessDeniedException("x")).when(acceso).exigirBoton(otro, 42, "btnNuevoCH");
            assertThrows(SinPermisoException.class, () -> servicio.actualizarDesdeSap(otro));

            soltar.countDown();
            primera.get(10, TimeUnit.SECONDS);

            // Terminada la primera, otra puede correr.
            servicio.actualizarDesdeSap(usuario);
            verify(dao, times(2)).actualizarDesdeSap(7);
        } finally {
            soltar.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("un token sin los datos del usuario (token viejo) sigue dando el 403 de siempre, no un error de base de datos")
    void tokenViejo() {
        Authentication viejo = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        doNothing().when(acceso).exigirBoton(viejo, 42, "btnNuevoCH");

        assertThrows(AccessDeniedException.class, () -> servicio.actualizarDesdeSap(viejo));
        verify(dao, never()).actualizarDesdeSap(anyInt());
    }

    @Test
    @DisplayName("los textos nombran el boton real, tutean, y el resultado no da ningun numero de clientes")
    void textos() {
        assertTrue(MensajesClientesSap.sinPermiso("btnNuevoCH").contains("btnNuevoCH"));
        assertEquals("Clientes actualizados desde SAP. Los cheques de los clientes que antes no estaban cargados ya aparecen en la lista.",
                MensajesClientesSap.ACTUALIZADO);
        assertFalse(MensajesClientesSap.ACTUALIZADO.matches(".*\\d.*"), "el procedimiento no dice cuantos trajo");
        assertFalse(MensajesClientesSap.NO_SE_PUDO.contains("tenés") || MensajesClientesSap.ACTUALIZADO.contains("tenés"),
                "tuteo neutro, sin voseo");
    }
}
