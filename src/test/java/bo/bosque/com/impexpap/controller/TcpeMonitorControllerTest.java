package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.ITcpeMonitor;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.dto.TcpeMonitorExamenDto;
import bo.bosque.com.impexpap.dto.TcpeMonitorFiltroDto;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Permiso del panel "En vivo" (contrato 8.5), con la regla real de {@link TcpePermisos}: pasa con la pantalla
 * {@code tcpeExamenes/EnVivo} o {@code tcpeExamenes/Resultados}; con otra, 403; el administrador, siempre.
 */
class TcpeMonitorControllerTest {

    private IVistaDao vistas;
    private ITcpeMonitor monitor;
    private TcpeMonitorController controlador;

    @BeforeEach
    void preparar() {
        vistas = mock(IVistaDao.class);
        monitor = mock(ITcpeMonitor.class);
        AccesoModuloHelper acceso = new AccesoModuloHelper(null, null);
        controlador = new TcpeMonitorController(monitor, new TcpePermisos(vistas, acceso));
        when(monitor.listar(30L)).thenReturn(Collections.singletonList(new TcpeMonitorExamenDto()));
    }

    private static Authentication usuario(String rol) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "evaluador", null, Collections.singletonList(new SimpleGrantedAuthority(rol)));
        t.setDetails(new DatosToken(34, 1, 1, rol));
        return t;
    }

    private void conPantalla(String direccion) {
        when(vistas.obtainRoutes(34)).thenReturn(Collections.singletonList(new Vista().setDireccion(direccion)));
    }

    private static TcpeMonitorFiltroDto lote(long codRolExamen) {
        TcpeMonitorFiltroDto f = new TcpeMonitorFiltroDto();
        f.setCodRolExamen(codRolExamen);
        return f;
    }

    @Test
    @DisplayName("con la pantalla EnVivo: lista el lote")
    void conEnVivo() {
        conPantalla(TcpePermisos.EN_VIVO);
        assertEquals(200, controlador.listar(lote(30L), usuario("ROLE_LIM")).getStatusCodeValue());
        verify(monitor).listar(30L);
    }

    @Test
    @DisplayName("con la pantalla Resultados: también pasa")
    void conResultados() {
        conPantalla(TcpePermisos.RESULTADOS);
        assertEquals(200, controlador.listar(lote(30L), usuario("ROLE_LIM")).getStatusCodeValue());
    }

    @Test
    @DisplayName("con otra pantalla del módulo (Lotes): 403 y no se consulta el monitor")
    void sinPantalla() {
        conPantalla(TcpePermisos.LOTES);
        assertThrows(SinPermisoException.class, () -> controlador.listar(lote(30L), usuario("ROLE_LIM")));
        assertThrows(SinPermisoException.class, () -> controlador.lotes(usuario("ROLE_LIM")));
        assertThrows(SinPermisoException.class, () -> controlador.espejo(new TcpeMonitorFiltroDto(), usuario("ROLE_LIM")));
        verify(monitor, never()).listar(any());
        verify(monitor, never()).lotes();
        verify(monitor, never()).espejo(any());
    }

    @Test
    @DisplayName("el administrador pasa siempre, sin consultar sus pantallas")
    void administrador() {
        assertEquals(200, controlador.listar(lote(30L), usuario("ROLE_ADM")).getStatusCodeValue());
        verify(vistas, never()).obtainRoutes(anyInt());
    }
}
