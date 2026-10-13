package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La regla generica de pantalla: la vista tiene que estar en el menu de quien llama. Sin base. */
class AccesoPantallaTest {

    private static final String PANTALLA = "tacCoche/Coche";

    private IVistaDao vistaDao;
    private AccesoModuloHelper acceso;
    private AccesoPantalla pantallas;

    @BeforeEach
    void setUp() {
        vistaDao = mock(IVistaDao.class);
        acceso = mock(AccesoModuloHelper.class);
        pantallas = new AccesoPantalla(vistaDao, acceso);
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

    @Test
    void conLaPantallaEnElMenuPasaSinImportarMayusculasNiEspacios() {
        Authentication auth = token(28);
        when(vistaDao.obtainRoutes(28)).thenReturn(Arrays.asList(
                ruta("tprPrecios/Precios"), ruta("  TACCOCHE/coche ")));

        assertDoesNotThrow(() -> pantallas.exigir(auth, PANTALLA),
                "la direccion viene de un char y puede traer espacios");
    }

    @Test
    void sinLaPantallaSeNiegaConTresCientosTres() {
        Authentication auth = token(28);
        // Tiene el modulo padre y una hermana, pero no la hija: el permiso no se hereda.
        when(vistaDao.obtainRoutes(28)).thenReturn(Arrays.asList(
                ruta("modLogistica"), ruta("tmme_RegistroResma/resma")));

        assertThrows(AccessDeniedException.class, () -> pantallas.exigir(auth, PANTALLA));
        assertFalse(pantallas.tiene(auth, PANTALLA));
    }

    @Test
    void sinRespuestaDelMenuSeNiega() {
        Authentication auth = token(28);
        when(vistaDao.obtainRoutes(28)).thenReturn(null);

        assertFalse(pantallas.tiene(auth, PANTALLA), "falla cerrado");
    }

    @Test
    void direccionNulaSeNiega() {
        assertFalse(pantallas.tiene(token(28), null));
    }

    @Test
    void elAdministradorNoConsultaElMenu() {
        Authentication auth = token(1);
        when(acceso.esAdmin(auth)).thenReturn(true);

        assertTrue(pantallas.tiene(auth, PANTALLA));
        verify(vistaDao, never()).obtainRoutes(anyInt());
    }

    @Test
    void unTokenSinIdentidadCortaConTresCientosTres() {
        // Sin DatosToken en los details (token viejo): DatosToken.de ya lanza 403.
        Authentication sinDetalles = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));

        assertThrows(AccessDeniedException.class, () -> pantallas.exigir(sinDetalles, PANTALLA));
        verify(vistaDao, never()).obtainRoutes(anyInt());
    }
}
