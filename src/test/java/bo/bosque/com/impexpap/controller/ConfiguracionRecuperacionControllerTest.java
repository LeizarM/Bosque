package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.ConfiguracionRecuperacionService;
import bo.bosque.com.impexpap.commons.CorreoRecuperacion;
import bo.bosque.com.impexpap.dto.GuardarConfiguracionDto;
import bo.bosque.com.impexpap.dto.ProbarCorreoDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Pantalla de configuración de la recuperación: solo administradores, auditoría del token y correo de prueba. */
class ConfiguracionRecuperacionControllerTest {

    private ConfiguracionRecuperacionService servicio;
    private CorreoRecuperacion correo;
    private ConfiguracionRecuperacionController controlador;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        servicio = mock(ConfiguracionRecuperacionService.class);
        correo = mock(CorreoRecuperacion.class);
        controlador = new ConfiguracionRecuperacionController(servicio, correo);
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "admin", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADM")));
        t.setDetails(new DatosToken(28, 75, 1, "ROLE_ADM"));
        auth = t;
    }

    private static ProbarCorreoDto prueba(String correo) {
        ProbarCorreoDto d = new ProbarCorreoDto();
        d.setCorreo(correo);
        return d;
    }

    @Test
    void soloAdministradoresDelSistema() {
        PreAuthorize regla = ConfiguracionRecuperacionController.class.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('ROLE_ADM')", regla.value());
    }

    @Test
    void laPantallaEsLaDireccionDeLaVista() {
        assertEquals("trecConfiguracion/Recuperacion", ConfiguracionRecuperacionController.PANTALLA,
                "tiene que ser el literal de tb_vista.direccion (sql/05) y el de rutaConfiguracionRecuperacion en Flutter");
    }

    @Test
    void guardarAuditaConElUsuarioDelToken() {
        GuardarConfiguracionDto d = new GuardarConfiguracionDto();
        d.setClave("smtpPuerto");
        d.setValor("465");

        ResponseEntity<ApiResponse<?>> r = controlador.guardar(d, auth);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(servicio).guardar("smtpPuerto", "465", 28);
    }

    @Test
    void pruebaConCorreoInvalidoNiIntentaEnviar() {
        assertEquals(HttpStatus.BAD_REQUEST, controlador.probar(prueba("bosque.esppapel.com")).getStatusCode());
        verifyNoInteractions(correo);
    }

    @Test
    void pruebaQueFallaDiceElMotivoAlAdministrador() {
        doThrow(new MailAuthenticationException("535 Authentication failed")).when(correo).enviar(any(), anyString(), anyString());

        ResponseEntity<ApiResponse<?>> r = controlador.probar(prueba("mjaimes@esppapel.com"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        assertTrue(r.getBody().getMessage().contains("535 Authentication failed"));
    }

    @Test
    void pruebaQueSale() {
        ResponseEntity<ApiResponse<?>> r = controlador.probar(prueba(" mjaimes@esppapel.com "));

        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(correo).enviar(any(), anyString(), anyString());
    }
}
