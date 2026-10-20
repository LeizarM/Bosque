package bo.bosque.com.impexpap.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El chequeo periódico de sesión de la app no gasta el cupo de 5 por minuto del login. */
class RateLimitFilterTest {

    private RateLimitFilter filtro;
    private FilterChain cadena;

    @BeforeEach
    void setUp() {
        ClienteIp ip = mock(ClienteIp.class);
        when(ip.de(any())).thenReturn("10.0.0.1");
        filtro = new RateLimitFilter(ip);
        cadena = mock(FilterChain.class);
    }

    private void pedir(String metodo, String ruta, int veces) throws Exception {
        for (int i = 0; i < veces; i++) {
            filtro.doFilter(new MockHttpServletRequest(metodo, ruta), new MockHttpServletResponse(), cadena);
        }
    }

    @Test
    void elChequeoDeSesionNoTieneCupo() throws Exception {
        pedir("GET", "/auth/sesion", 10);

        verify(cadena, times(10)).doFilter(any(), any());
    }

    @Test
    void elLoginSigueLimitadoACincoPorMinuto() throws Exception {
        pedir("POST", "/auth/login", 6);

        verify(cadena, times(5)).doFilter(any(), any());
    }

    @Test
    void elCambioDeClavePublicoTieneSuCupoSinGastarElDelLogin() throws Exception {
        pedir("POST", "/auth/changePassword", 6);
        pedir("POST", "/auth/login", 1);

        verify(cadena, times(6)).doFilter(any(), any());
    }

    @Test
    void elLoginDePostulantesTieneCupoPropioDeSesentaPorMinuto() throws Exception {
        pedir("POST", "/examen-publico/auth/login", 61);

        verify(cadena, times(60)).doFilter(any(), any());
    }

    @Test
    void elLoginDePostulantesNoGastaElCupoDeLosEmpleados() throws Exception {
        pedir("POST", "/examen-publico/auth/login", 30);
        pedir("POST", "/auth/login", 5);

        verify(cadena, times(35)).doFilter(any(), any());
    }

    @Test
    void losLatidosYLaConfiguracionDelExamenNoTienenCupo() throws Exception {
        // Una sala entera late cada 15 s desde la misma IP: el latido no puede caer en el cupo del login.
        pedir("POST", "/examen-publico/examenes/latido", 100);
        pedir("POST", "/examenes/mis-examenes/latido", 100);
        pedir("POST", "/examen-publico/config", 100);

        verify(cadena, times(300)).doFilter(any(), any());
    }

    @Test
    void elCambioDeClaveConSesionNoTieneCupo() throws Exception {
        pedir("POST", "/auth/changePasswordDefault", 10);

        verify(cadena, times(10)).doFilter(any(), any());
    }
}
