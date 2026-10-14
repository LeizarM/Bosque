package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.RecuperacionAccesoService;
import bo.bosque.com.impexpap.commons.RecuperacionAccesoService.CorreoNoEnviadoException;
import bo.bosque.com.impexpap.dto.RecuperacionEnviadaDto;
import bo.bosque.com.impexpap.dto.RestablecerContrasenaDto;
import bo.bosque.com.impexpap.dto.SolicitudRecuperacionDto;
import bo.bosque.com.impexpap.security.ClienteIp;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Endpoints públicos de recuperación: validación de entrada, respuesta y 503 si el correo no sale. */
class RecuperacionAccesoControllerTest {

    private RecuperacionAccesoService servicio;
    private RecuperacionAccesoController controlador;
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        servicio = mock(RecuperacionAccesoService.class);
        ClienteIp ip = mock(ClienteIp.class);
        when(ip.de(any())).thenReturn("10.0.0.1");
        controlador = new RecuperacionAccesoController(servicio, ip);
    }

    private static SolicitudRecuperacionDto solicitud(String login) {
        SolicitudRecuperacionDto d = new SolicitudRecuperacionDto();
        d.setLogin(login);
        return d;
    }

    private static RestablecerContrasenaDto restablecimiento(String login, String codigo, String clave) {
        RestablecerContrasenaDto d = new RestablecerContrasenaDto();
        d.setLogin(login);
        d.setCodigo(codigo);
        d.setNuevaContrasena(clave);
        return d;
    }

    @Test
    void sinDatosNiSeLlamaAlServicio() {
        assertEquals(HttpStatus.BAD_REQUEST, controlador.solicitar(solicitud("  "), request).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                controlador.restablecer(restablecimiento("jperez", "", "nueva-clave"), request).getStatusCode());
        verifyNoInteractions(servicio);
    }

    @Test
    void solicitarDevuelveElCorreoEnmascarado() {
        RecuperacionEnviadaDto enviado = new RecuperacionEnviadaDto("j****z@esppapel.com", 15, 60);
        when(servicio.solicitar("jperez", "10.0.0.1")).thenReturn(enviado);

        ResponseEntity<ApiResponse<?>> r = controlador.solicitar(solicitud("jperez"), request);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("Te enviamos un código a j****z@esppapel.com.", r.getBody().getMessage());
        assertSame(enviado, r.getBody().getData());
    }

    @Test
    void restablecerAvisaQueYaPuedeEntrar() {
        ResponseEntity<ApiResponse<?>> r = controlador.restablecer(restablecimiento("jperez", "123456", "nueva-clave"), request);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(RecuperacionAccesoController.MSG_RESTABLECIDA, r.getBody().getMessage());
        verify(servicio).restablecer("jperez", "123456", "nueva-clave", "10.0.0.1");
    }

    @Test
    void correoQueNoSaleResponde503ConElAviso() {
        ResponseEntity<ApiResponse<?>> r = controlador.correoNoEnviado(new CorreoNoEnviadoException("No pudimos enviar el correo."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        assertEquals("No pudimos enviar el correo.", r.getBody().getMessage());
    }
}
