package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La app anterior al SQL 96 pide /abrir-flujo antes de entrar a Caja Fuerte, Revisar Autos o
 * Caja Chica, y no sigue sin un número mayor que 0. Se le devuelve el codEmpleado para que
 * siga funcionando sin obligar a actualizar (2026-10-13), con el mismo permiso de antes.
 */
class TareasRutinariasAbrirFlujoAppAnteriorTest {

    private IVistaDao vistaDao;
    private AccesoModuloHelper acceso;
    private TareasRutinariasController controlador;

    @BeforeEach
    void setUp() {
        vistaDao = mock(IVistaDao.class);
        acceso = mock(AccesoModuloHelper.class);
        controlador = mock(TareasRutinariasController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controlador, "vistaDao", vistaDao);
        ReflectionTestUtils.setField(controlador, "accesoModuloHelper", acceso);
    }

    private static Authentication token(int codUsuario) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "usuario", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(codUsuario, 75, 1, "ROLE_LIM"));
        return t;
    }

    private void conPantalla(int codUsuario, String direccion) {
        Vista v = new Vista();
        v.setDireccion(direccion);
        when(vistaDao.obtainRoutes(codUsuario)).thenReturn(Collections.singletonList(v));
    }

    private static Map<String, Object> flujo(long idTarRuti) {
        return Collections.singletonMap("idTarRuti", idTarRuti);
    }

    @Test
    void losTresFlujosDevuelvenUnNumeroMayorQueCero() {
        Authentication auth = token(28);
        when(acceso.codEmpleadoDelToken(auth)).thenReturn(75L);
        String[][] casos = {{"40", "tacTareas/CajaFuerte"}, {"41", "tacTareas/Coches"}, {"42", "tacTareas/CajaChica"}};
        for (String[] caso : casos) {
            conPantalla(28, caso[1]);
            ResponseEntity<ApiResponse<?>> r = controlador.abrirFlujoAppAnterior(flujo(Long.parseLong(caso[0])), auth);
            assertEquals(200, r.getStatusCodeValue(), caso[1]);
            assertEquals(75L, r.getBody().getData(), caso[1]);
        }
    }

    @Test
    void sinLaVistaDelSubmoduloNoEntra() {
        Authentication auth = token(28);
        conPantalla(28, "tacTareas/Coches");
        assertThrows(AccessDeniedException.class, () -> controlador.abrirFlujoAppAnterior(flujo(40), auth));
    }

    @Test
    void otroFlujoOSinFlujoSeRechaza() {
        Authentication auth = token(28);
        // Cierre de Operaciones (2) se abre con la ocurrencia de Verificar Cierre, no por aquí.
        assertThrows(SpBusinessException.class, () -> controlador.abrirFlujoAppAnterior(flujo(2), auth));
        assertThrows(SpBusinessException.class, () -> controlador.abrirFlujoAppAnterior(null, auth));
    }
}
