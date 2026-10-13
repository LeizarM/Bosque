package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.dao.ICoche;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Coche;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los endpoints viejos {@code /tareas-rutinarias/*-coche} exigen la misma vista que la pantalla
 * de gestion ({@code tacCoche/Coche}). Antes los tres dejaban pasar a cualquier ROLE_LIM.
 *
 * <p>El controlador tiene decenas de dependencias, asi que no se construye: se toma el real con sus
 * metodos y se le inyectan solo las tres que estos endpoints tocan.
 */
class TareasRutinariasCocheCerradoTest {

    private ICoche cocheDao;
    private IVistaDao vistaDao;
    private AccesoModuloHelper acceso;
    private TareasRutinariasController controlador;

    @BeforeEach
    void setUp() {
        cocheDao = mock(ICoche.class);
        vistaDao = mock(IVistaDao.class);
        acceso = mock(AccesoModuloHelper.class);

        controlador = mock(TareasRutinariasController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controlador, "cocheDao", cocheDao);
        ReflectionTestUtils.setField(controlador, "vistaDao", vistaDao);
        ReflectionTestUtils.setField(controlador, "accesoModuloHelper", acceso);
    }

    private static Authentication token(int codUsuario) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(codUsuario, 75, 1, "ROLE_LIM"));
        return t;
    }

    private static Vista ruta(String direccion) {
        Vista v = new Vista();
        v.setDireccion(direccion);
        return v;
    }

    private static Coche coche(long id) {
        Coche c = new Coche();
        c.setIdCoche(id);
        c.setMarca("Toyota");
        c.setPlaca("917-SKP");
        return c;
    }

    @Test
    void sinLaVistaLosTresEndpointsDanTresCientosTresYNoTocanElDao() {
        Authentication auth = token(28);
        // Tiene la revision de autos del dia, que es otra pantalla: no habilita el catalogo.
        when(vistaDao.obtainRoutes(28)).thenReturn(Arrays.asList(ruta("tacTareas/Coches"), ruta("modLogistica")));

        assertThrows(AccessDeniedException.class, () -> controlador.registrarCoche(coche(0), auth));
        assertThrows(AccessDeniedException.class, () -> controlador.eliminarCoche(coche(3), auth));
        assertThrows(AccessDeniedException.class, () -> controlador.obtenerCoche(new Coche(), auth));

        verify(cocheDao, never()).registrar(any(), anyString());
        verify(cocheDao, never()).listar(any());
    }

    @Test
    void conLaVistaLosTresEndpointsLlegaAlDao() {
        Authentication auth = token(28);
        when(vistaDao.obtainRoutes(28)).thenReturn(Collections.singletonList(ruta(" tacCoche/Coche ")));
        when(cocheDao.registrar(any(), anyString())).thenReturn(new RespuestaSp(0, "ok", 5L));

        controlador.registrarCoche(coche(0), auth);
        controlador.eliminarCoche(coche(3), auth);
        controlador.obtenerCoche(null, auth);

        verify(cocheDao).registrar(any(Coche.class), org.mockito.ArgumentMatchers.eq("I"));
        verify(cocheDao).registrar(any(Coche.class), org.mockito.ArgumentMatchers.eq("D"));
        verify(cocheDao).listar(null);
    }

    @Test
    void elAdministradorPasaSinConsultarElMenu() {
        Authentication auth = token(1);
        when(acceso.esAdmin(auth)).thenReturn(true);
        when(cocheDao.registrar(any(), anyString())).thenReturn(new RespuestaSp(0, "ok", 5L));

        controlador.registrarCoche(coche(0), auth);

        verify(vistaDao, never()).obtainRoutes(org.mockito.ArgumentMatchers.anyInt());
        verify(cocheDao).registrar(any(Coche.class), org.mockito.ArgumentMatchers.eq("I"));
    }

    @Test
    void laDireccionExigidaEsLaMismaDeLaPantallaNueva() {
        // Si alguien cambia el literal en un lado y no en el otro, el permiso se parte en dos.
        org.junit.jupiter.api.Assertions.assertEquals("tacCoche/Coche", CocheController.PANTALLA);
    }
}
