package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.SesionService;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.dao.IRelEmpEmpr;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dao.IVistaUsuario;
import bo.bosque.com.impexpap.model.Login;
import bo.bosque.com.impexpap.security.ClienteIp;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.security.model.Jwt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Login: qué responde según el estado del usuario y su última relación laboral. */
class LoginControllerTest {

    private ILoginDao dao;
    private AuthenticationManager autenticador;
    private JwtProvider jwt;
    private SesionService sesiones;
    private IRelEmpEmpr relaciones;
    private MockHttpServletRequest pedido;
    private LoginController controlador;

    @BeforeEach
    void setUp() {
        dao = mock(ILoginDao.class);
        autenticador = mock(AuthenticationManager.class);
        jwt = mock(JwtProvider.class);
        sesiones = mock(SesionService.class);
        relaciones = mock(IRelEmpEmpr.class);
        ClienteIp ip = mock(ClienteIp.class);
        when(ip.de(any())).thenReturn("10.0.0.1");
        controlador = new LoginController(mock(PasswordEncoder.class), autenticador, jwt, dao,
                mock(IVistaUsuario.class), mock(IEmpleado.class), mock(IUsuarioBtn.class), ip, sesiones, relaciones);
        pedido = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(pedido));
    }

    /** El Authentication que deja JwtTokenFilter: la identidad del token en los details. */
    private static Authentication conToken(int codUsuario, long idSesion) {
        return conToken(codUsuario, idSesion, "lim");
    }

    private static Authentication conToken(int codUsuario, long idSesion, String tipoUsuario) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("jperez", null,
                Collections.<GrantedAuthority>singletonList(
                        new SimpleGrantedAuthority("ROLE_" + tipoUsuario.toUpperCase())));
        auth.setDetails(new DatosToken(codUsuario, 7, 1, tipoUsuario, "Juan Pérez", idSesion));
        return auth;
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    /** Lo que devuelve p_list_Usuario 'C': codUsuario > 0 activo, < 0 bloqueado, 0 no existe. */
    private void cuenta(int codUsuario, boolean relacionActiva) {
        Login l = new Login();
        l.setCodUsuario(codUsuario);
        l.setRelacionActiva(relacionActiva);
        when(dao.verifyUser(anyString(), anyString())).thenReturn(l);
    }

    private void claveErrada(int codUsuarioTrasElIntento) {
        when(autenticador.authenticate(any())).thenThrow(new BadCredentialsException("clave errada"));
        Login tras = new Login();
        tras.setCodUsuario(codUsuarioTrasElIntento);
        when(dao.registerFailedAttempt(anyString(), anyString())).thenReturn(tras);
    }

    private ResponseEntity<?> entrar() {
        Login credenciales = new Login();
        credenciales.setLogin("jperez");
        credenciales.setPassword2("clave");
        return controlador.login(credenciales, mock(BindingResult.class));
    }

    private static Object mensaje(ResponseEntity<?> r) {
        return ((Map<?, ?>) r.getBody()).get("mensaje");
    }

    @Test
    void siLaBaseNoRespondeElLoginLoDiceYNoCulpaALaContrasena() {
        when(dao.verifyUser(anyString(), anyString())).thenThrow(new CannotGetJdbcConnectionException("sin conexión"));

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        assertEquals(LoginController.MSG_SIN_BASE, mensaje(r));
    }

    @Test
    void usuarioInexistenteRecibeElMensajeGenerico() {
        cuenta(0, true);

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals(LoginController.MSG_CREDENCIALES, mensaje(r));
        verify(autenticador, never()).authenticate(any());
    }

    @Test
    void exEmpleadoConUsuarioActivoNoEntraYNiSeLePideLaClave() {
        cuenta(5, false);

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(LoginController.MSG_INACTIVO, mensaje(r));
        verify(autenticador, never()).authenticate(any());
        verify(dao, never()).registerFailedAttempt(anyString(), anyString());
    }

    @Test
    void exEmpleadoBloqueadoVeInactivoAntesQueBloqueado() {
        cuenta(-5, false);

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(LoginController.MSG_INACTIVO, mensaje(r));
    }

    @Test
    void bloqueadoConRelacionActivaVeBloqueado() {
        cuenta(-5, true);

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(LoginController.MSG_BLOQUEADO, mensaje(r));
        verify(autenticador, never()).authenticate(any());
    }

    @Test
    void claveErradaRegistraElIntentoYDevuelveElGenerico() {
        cuenta(5, true);
        claveErrada(5);

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals(LoginController.MSG_CREDENCIALES, mensaje(r));
        verify(dao).registerFailedAttempt("jperez", "10.0.0.1");
    }

    @Test
    void elIntentoQueBloqueaLaCuentaYaLoAvisa() {
        cuenta(5, true);
        claveErrada(-5);

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(LoginController.MSG_BLOQUEADO, mensaje(r));
    }

    @Test
    void usuarioActivoConClaveCorrectaEntraConSuSesionEnElToken() {
        cuenta(5, true);
        List<GrantedAuthority> roles = Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM"));
        when(autenticador.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(new User("jperez", "hash", roles), null, roles));
        pedido.addHeader("X-Plataforma", "android");
        pedido.addHeader("X-Version-App", "1.0.2");
        when(sesiones.iniciar(5, "android", "1.0.2", "10.0.0.1")).thenReturn(33L);
        when(jwt.generateToken(any(), any(), eq(33L))).thenReturn("token");

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(dao).registerSuccessfulLogin("jperez", "10.0.0.1");
        verify(jwt).generateToken(any(), any(), eq(33L));
    }

    @Test
    void elLoginTraeLasInicialesDelAvatarYSiFallanEntraIgual() {
        Login l = new Login();
        l.setCodUsuario(5);
        l.setCodEmpleado(172);
        when(dao.verifyUser(anyString(), anyString())).thenReturn(l);
        List<GrantedAuthority> roles = Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM"));
        when(autenticador.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(new User("jperez", "hash", roles), null, roles));
        when(relaciones.inicialesDe(172)).thenReturn("MJ");

        assertEquals("MJ", ((Jwt) ((Map<?, ?>) entrar().getBody()).get("data")).getIniciales());

        when(relaciones.inicialesDe(172)).thenThrow(new RuntimeException("SP sin la columna"));
        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("", ((Jwt) ((Map<?, ?>) r.getBody()).get("data")).getIniciales());
    }

    @Test
    void siElEstadoCambiaEnMedioDelLoginSeAvisaYNoSaleUnError500() {
        cuenta(5, true);
        when(autenticador.authenticate(any())).thenThrow(new LockedException("bloqueado"));
        ResponseEntity<?> bloqueado = entrar();

        reset(autenticador);
        when(autenticador.authenticate(any())).thenThrow(new DisabledException("baja"));
        ResponseEntity<?> inactivo = entrar();

        assertEquals(HttpStatus.FORBIDDEN, bloqueado.getStatusCode());
        assertEquals(LoginController.MSG_BLOQUEADO, mensaje(bloqueado));
        assertEquals(HttpStatus.FORBIDDEN, inactivo.getStatusCode());
        assertEquals(LoginController.MSG_INACTIVO, mensaje(inactivo));
    }

    @Test
    void elChequeoDeSesionAnotaLaSenalConPlataformaVersionEIp() {
        Authentication auth = conToken(5, 33);
        pedido.addHeader("X-Plataforma", "web");
        pedido.addHeader("X-Version-App", "1.0.2");

        ResponseEntity<Void> r = controlador.verificarSesion(auth, pedido);

        assertEquals(HttpStatus.NO_CONTENT, r.getStatusCode());
        verify(sesiones).senal((DatosToken) auth.getDetails(), "web", "1.0.2", "10.0.0.1");
    }

    @Test
    void sinTokenElChequeoYLaSalidaRespondenSinTocarNada() {
        assertEquals(HttpStatus.NO_CONTENT, controlador.verificarSesion(null, pedido).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controlador.salir(null).getStatusCode());

        verify(sesiones).senal(null, null, null, "10.0.0.1");
        verify(sesiones).salir(null);
    }

    @Test
    void unExEmpleadoQueCambiaSuClaveVeElMotivoYNoUnError500() {
        cuenta(5, false);
        Login datos = new Login();
        datos.setLogin("jperez");
        datos.setPassword2("clave");

        ResponseEntity<?> r = controlador.login(datos, (Authentication) null);

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(LoginController.MSG_INACTIVO, ((Map<?, ?>) r.getBody()).get("error"));
        verify(autenticador, never()).authenticate(any());
    }

    private ResponseEntity<?> cambiarClave() {
        Login datos = new Login();
        datos.setLogin("jperez");
        datos.setPassword2("clave");
        datos.setNpassword("nueva-clave");
        return controlador.login(datos, (Authentication) null);
    }

    @Test
    void cambiarLaClaveConUnaErradaCuentaComoIntentoFallido() {
        cuenta(5, true);
        claveErrada(5);

        ResponseEntity<?> r = cambiarClave();

        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals(LoginController.MSG_CREDENCIALES, ((Map<?, ?>) r.getBody()).get("error"));
        verify(dao).registerFailedAttempt("jperez", "10.0.0.1");
        verify(dao, never()).abmLogin(any(), anyString());
    }

    @Test
    void elIntentoQueBloqueaAlCambiarLaClaveLoAvisa() {
        cuenta(5, true);
        claveErrada(-5);

        ResponseEntity<?> r = cambiarClave();

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(LoginController.MSG_BLOQUEADO, ((Map<?, ?>) r.getBody()).get("error"));
    }

    @Test
    void unBloqueadoNoPuedeProbarClavesEnElCambioDeClave() {
        cuenta(-5, true);

        ResponseEntity<?> r = cambiarClave();

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        verify(autenticador, never()).authenticate(any());
    }

    @Test
    void cambiarLaClaveConLaActualCierraLasSesionesAbiertas() {
        cuenta(5, true);
        when(autenticador.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken("jperez", null,
                Collections.<GrantedAuthority>emptyList()));
        when(dao.abmLogin(any(), eq("Q"))).thenReturn(true);

        ResponseEntity<?> r = cambiarClave();

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        verify(sesiones).cerrarTodas(5, SesionService.MOTIVO_CLAVE, null);
    }

    @Test
    void siLaBaseFallaDentroDeLaAutenticacionTambienEs503() {
        cuenta(5, true);
        when(autenticador.authenticate(any())).thenThrow(
                new InternalAuthenticationServiceException("envuelta", new CannotGetJdbcConnectionException("sin conexión")));

        ResponseEntity<?> r = entrar();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        assertEquals(LoginController.MSG_SIN_BASE, mensaje(r));
    }

    @Test
    void siUnAdministradorCambiaLaClaveDeOtroSeCierranTodasSusSesiones() {
        when(dao.abmLogin(any(), eq("Q"))).thenReturn(true);
        Map<String, Object> datos = new java.util.HashMap<>();
        datos.put("codUsuario", 9);
        datos.put("npassword", "nueva-clave");

        controlador.changePasswordDefault(datos, conToken(5, 33, "adm"));

        verify(sesiones).cerrarTodas(9, SesionService.MOTIVO_CLAVE, 5L, 0L);
    }

    @Test
    void quienNoEsAdministradorNoPuedeCambiarLaClaveDeOtro() {
        Map<String, Object> datos = new java.util.HashMap<>();
        datos.put("codUsuario", 9);
        datos.put("npassword", "nueva-clave");

        ResponseEntity<?> r = controlador.changePasswordDefault(datos, conToken(5, 33));

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(LoginController.MSG_CLAVE_AJENA, ((Map<?, ?>) r.getBody()).get("error"));
        verify(dao, never()).abmLogin(any(), anyString());
        verifyNoInteractions(sesiones);
    }

    @Test
    void quienCambiaSuPropiaClaveSigueEnLaSesionDesdeLaQueLaCambio() {
        when(dao.abmLogin(any(), eq("Q"))).thenReturn(true);
        Map<String, Object> datos = new java.util.HashMap<>();
        datos.put("codUsuario", 5);
        datos.put("npassword", "nueva-clave");

        controlador.changePasswordDefault(datos, conToken(5, 33));

        verify(sesiones).cerrarTodas(5, SesionService.MOTIVO_CLAVE, 5L, 33L);
    }
}
