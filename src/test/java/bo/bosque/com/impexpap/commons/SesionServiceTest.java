package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ISesion;
import bo.bosque.com.impexpap.model.Sesion;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Sesiones: nada de este registro puede impedir entrar ni trabajar, y cada cierre da su propio motivo. */
class SesionServiceTest {

    private ISesion dao;
    private SesionService servicio;
    private final DatosToken conSesion = new DatosToken(5, 7, 1, "adm", "Juan Pérez", 33);
    private final DatosToken sinSesion = new DatosToken(5, 7, 1, "adm", "Juan Pérez", 0);

    @BeforeEach
    void setUp() {
        dao = mock(ISesion.class);
        JwtProvider jwt = mock(JwtProvider.class);
        when(jwt.getExpiracionSegundos()).thenReturn(36000);
        servicio = new SesionService(dao, jwt);
    }

    private static Sesion cerrada(String motivo) {
        Sesion s = new Sesion();
        s.setIdSesion(33);
        s.setCodUsuario(5);
        s.setFechaCierre(motivo == null ? null : new Date());
        s.setMotivoCierre(motivo);
        return s;
    }

    /** Como los deserializa JacksonConfig: 00:00 en La Paz (sin horario de verano, un día son siempre 24 h). */
    private static Date dia(String yyyyMMdd) throws Exception {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        f.setTimeZone(TimeZone.getTimeZone("America/La_Paz"));
        return f.parse(yyyyMMdd);
    }

    @Test
    void alEntrarLaSesionDuraLoMismoQueElToken() {
        when(dao.iniciar(5, "web", "1.0.2", "10.0.0.1", 600)).thenReturn(33L);

        assertEquals(33L, servicio.iniciar(5, "web", "1.0.2", "10.0.0.1"));
    }

    @Test
    void sinLaTablaDeSesionesSeEntraIgualConUnTokenSinSesion() {
        when(dao.iniciar(anyLong(), any(), any(), any(), anyInt())).thenThrow(new RuntimeException("Could not find stored procedure"));

        assertEquals(0L, servicio.iniciar(5, "web", "1.0.2", "10.0.0.1"));
    }

    @Test
    void cadaCierreDaSuMotivo() {
        assertEquals(SesionService.MSG_CERRADA_ADMIN, SesionService.mensajeDeCierre("A"));
        assertEquals(SesionService.MSG_CLAVE_CAMBIADA, SesionService.mensajeDeCierre("R"));
        assertEquals(SesionService.MSG_CERRADA, SesionService.mensajeDeCierre("B"));
        assertEquals(SesionService.MSG_CERRADA, SesionService.mensajeDeCierre("S"));
    }

    @Test
    void unaSesionQueNoExisteOEsDeOtroUsuarioCuentaComoCerrada() {
        // p_list_Usuario 'B' la da por cerrada sin motivo.
        assertEquals(SesionService.MSG_CERRADA, SesionService.mensajeDeCierre(null));
    }

    @Test
    void unAdministradorNoCierraSuPropiaSesionDesdeLaLista() {
        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.cerrarPorAdministrador(33, conSesion));

        assertEquals(SesionService.MSG_PROPIA, e.getMessage());
        verifyNoInteractions(dao);
    }

    @Test
    void unAdministradorCierraLaDeOtroConSuUsuarioComoResponsable() {
        servicio.cerrarPorAdministrador(40, conSesion);

        verify(dao).cerrar(40, 0, SesionService.MOTIVO_ADMIN, 5L);
    }

    @Test
    void laListaMarcaLaSesionDeQuienMira() {
        Sesion propia = cerrada(null);
        Sesion otra = cerrada(null);
        otra.setIdSesion(40);
        when(dao.abiertas()).thenReturn(Arrays.asList(propia, otra));

        List<Sesion> filas = servicio.abiertas(conSesion);

        assertTrue(filas.get(0).isEsActual());
        assertFalse(filas.get(1).isEsActual());
    }

    @Test
    void elHistorialIncluyeElUltimoDiaYLimitaElRango() throws Exception {
        servicio.historial(dia("2026-10-01"), dia("2026-10-31"), 0L);
        verify(dao).historial(dia("2026-10-01"), dia("2026-11-01"), null);

        assertEquals(SesionService.MSG_RANGO, assertThrows(SpBusinessException.class,
                () -> servicio.historial(dia("2026-10-01"), dia("2026-11-01"), null)).getMessage());
        assertEquals(SesionService.MSG_ORDEN, assertThrows(SpBusinessException.class,
                () -> servicio.historial(dia("2026-10-02"), dia("2026-10-01"), null)).getMessage());
        assertEquals(SesionService.MSG_FECHAS, assertThrows(SpBusinessException.class,
                () -> servicio.historial(null, dia("2026-10-01"), null)).getMessage());
    }

    @Test
    void laSenalYLosCierresEnBloqueNoCortanNadaSiFallan() {
        doThrow(new RuntimeException("sin conexión")).when(dao).senal(anyLong(), anyLong(), any(), any(), any());
        when(dao.cerrarTodas(anyLong(), anyString(), any(), anyLong())).thenThrow(new RuntimeException("sin conexión"));

        servicio.senal(conSesion, "web", "1.0.2", "10.0.0.1");
        servicio.cerrarTodas(5, SesionService.MOTIVO_CLAVE, null);
        servicio.senal(sinSesion, "web", "1.0.2", "10.0.0.1");

        verify(dao).senal(33, 5, "web", "1.0.2", "10.0.0.1");
        verify(dao).cerrarTodas(5L, SesionService.MOTIVO_CLAVE, null, 0L);
    }

    @Test
    void salirCierraSoloLaSesionDelToken() {
        servicio.salir(conSesion);
        servicio.salir(null);
        servicio.salir(sinSesion);

        verify(dao, times(1)).cerrar(anyLong(), anyLong(), anyString(), any());
        verify(dao).cerrar(33, 5, SesionService.MOTIVO_SALIO, null);
    }
}
