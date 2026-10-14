package bo.bosque.com.impexpap.security.jwt;

import bo.bosque.com.impexpap.commons.SesionService;
import bo.bosque.com.impexpap.controller.LoginController;
import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.security.ClienteIp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import io.jsonwebtoken.Claims;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.FilterChain;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Un token vigente de un ex empleado, de alguien bloqueado por un administrador o de una sesión cerrada ya no pasa:
 * 401 con el motivo, sin tocar el contador de la IP.
 */
class JwtTokenFilterTest {

    private JwtTokenFilter filtro;
    private JwtProvider jwt;
    private ILoginDao ldao;
    private JwtEntryPoint entryPoint;
    private SesionService sesiones;
    private FilterChain cadena;
    private MockHttpServletRequest pedido;
    private MockHttpServletResponse respuesta;
    private final DatosToken datos = new DatosToken(5, 7, 1, "adm", "Juan Pérez", 33);

    @BeforeEach
    void setUp() {
        jwt = mock(JwtProvider.class);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("jperez");
        when(jwt.leer("tok")).thenReturn(claims);
        when(jwt.datosDe(claims)).thenReturn(datos);
        ClienteIp ip = mock(ClienteIp.class);
        when(ip.de(any())).thenReturn("10.0.0.1");
        ldao = mock(ILoginDao.class);
        entryPoint = mock(JwtEntryPoint.class);
        sesiones = mock(SesionService.class);
        cadena = mock(FilterChain.class);

        filtro = new JwtTokenFilter();
        ReflectionTestUtils.setField(filtro, "jwtProvider", jwt);
        ReflectionTestUtils.setField(filtro, "ldao", ldao);
        ReflectionTestUtils.setField(filtro, "jwtEntryPoint", entryPoint);
        ReflectionTestUtils.setField(filtro, "clienteIp", ip);
        ReflectionTestUtils.setField(filtro, "sesiones", sesiones);

        pedido = new MockHttpServletRequest("POST", "/ventas/listar");
        pedido.addHeader("Authorization", "Bearer tok");
        respuesta = new MockHttpServletResponse();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static UsuarioToken usuario(boolean relacionActiva, boolean noBloqueado) {
        return usuario(relacionActiva, noBloqueado, false, null);
    }

    private static UsuarioToken usuario(boolean relacionActiva, boolean noBloqueado, boolean sesionCerrada, String motivo) {
        return new UsuarioToken("jperez", "hash", relacionActiva, noBloqueado,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADM")), sesionCerrada, motivo);
    }

    @Test
    void unUsuarioVigenteSigueYQuedaAutenticado() throws Exception {
        when(ldao.cargarParaToken("jperez", 33)).thenReturn(usuario(true, true));

        filtro.doFilter(pedido, respuesta, cadena);

        verify(cadena).doFilter(pedido, respuesta);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(entryPoint).resetFailureCount("10.0.0.1");
    }

    @Test
    void elTokenSeVerificaUnaVezYElUsuarioConSuSesionSaleDeUnaSolaConsulta() throws Exception {
        when(ldao.cargarParaToken("jperez", 33)).thenReturn(usuario(true, true));

        filtro.doFilter(pedido, respuesta, cadena);

        verify(jwt, times(1)).leer("tok");
        verify(ldao, times(1)).cargarParaToken("jperez", 33);
        verify(ldao, never()).loadUserByUsername(anyString());
        verifyNoInteractions(sesiones);
    }

    @Test
    void unTokenInvalidoNoConsultaLaBase() throws Exception {
        pedido = new MockHttpServletRequest("POST", "/ventas/listar");
        pedido.addHeader("Authorization", "Bearer otro");

        filtro.doFilter(pedido, respuesta, cadena);

        verify(cadena).doFilter(pedido, respuesta);
        verify(ldao, never()).cargarParaToken(anyString(), anyLong());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void unExEmpleadoRecibe401ConElMotivoYSeCierranSusSesiones() throws Exception {
        when(ldao.cargarParaToken("jperez", 33)).thenReturn(usuario(false, true));

        filtro.doFilter(pedido, respuesta, cadena);

        assertEquals(401, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains(JwtTokenFilter.SESION_CERRADA));
        assertTrue(respuesta.getContentAsString().contains(LoginController.MSG_INACTIVO));
        verify(cadena, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(sesiones).cerrarTodas(5, SesionService.MOTIVO_BAJA, null);
    }

    @Test
    void unBloqueadoPorUnAdministradorRecibeSuMensajeYNoCuentaComoFalloDeLaIp() throws Exception {
        when(ldao.cargarParaToken("jperez", 33)).thenReturn(usuario(true, false));

        filtro.doFilter(pedido, respuesta, cadena);

        assertEquals(401, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains(JwtTokenFilter.MSG_BLOQUEADO_ADMIN));
        verify(cadena, never()).doFilter(any(), any());
        verify(entryPoint, never()).commence(any(), any(), any());
        verify(entryPoint, never()).resetFailureCount(anyString());
        verify(sesiones).cerrarTodas(5, SesionService.MOTIVO_BLOQUEO, null);
    }

    @Test
    void unaSesionCerradaPorUnAdministradorYaNoPasaAunqueElUsuarioEsteActivo() throws Exception {
        when(ldao.cargarParaToken("jperez", 33)).thenReturn(usuario(true, true, true, "A"));

        filtro.doFilter(pedido, respuesta, cadena);

        assertEquals(401, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains(SesionService.MSG_CERRADA_ADMIN));
        verify(cadena, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void elUsuarioQuedaAutenticadoConLaSesionDelToken() throws Exception {
        when(ldao.cargarParaToken("jperez", 33)).thenReturn(usuario(true, true));

        filtro.doFilter(pedido, respuesta, cadena);

        assertEquals(33, ((DatosToken) SecurityContextHolder.getContext().getAuthentication().getDetails()).getIdSesion());
    }

    @Test
    void elLoginYLaRecuperacionNoPasanPorElFiltroAunqueTraiganToken() throws Exception {
        for (String ruta : new String[]{"/auth/login", "/auth/recuperacion/solicitar"}) {
            MockHttpServletRequest publico = new MockHttpServletRequest("POST", ruta);
            publico.addHeader("Authorization", "Bearer tok");

            filtro.doFilter(publico, new MockHttpServletResponse(), cadena);

            verify(cadena).doFilter(eq(publico), any());
        }
        verify(ldao, never()).cargarParaToken(anyString(), anyLong());
    }

    @Test
    void siEsExEmpleadoYBloqueadoGanaInactivoComoEnElLogin() {
        assertEquals(LoginController.MSG_INACTIVO, JwtTokenFilter.motivoDeCierre(usuario(false, false)));
    }

    @Test
    void sinTokenSigueComoAntes() throws Exception {
        MockHttpServletRequest sinToken = new MockHttpServletRequest("POST", "/ventas/listar");

        filtro.doFilter(sinToken, respuesta, cadena);

        verify(cadena).doFilter(sinToken, respuesta);
        verify(ldao, never()).cargarParaToken(anyString(), anyLong());
    }
}
