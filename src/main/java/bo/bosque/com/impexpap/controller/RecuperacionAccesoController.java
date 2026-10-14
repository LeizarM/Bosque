package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.RecuperacionAccesoService;
import bo.bosque.com.impexpap.commons.RecuperacionAccesoService.CorreoNoEnviadoException;
import bo.bosque.com.impexpap.dto.RecuperacionEnviadaDto;
import bo.bosque.com.impexpap.dto.RestablecerContrasenaDto;
import bo.bosque.com.impexpap.dto.SolicitudRecuperacionDto;
import bo.bosque.com.impexpap.security.ClienteIp;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Recuperación de acceso con un código enviado al correo corporativo. Público por estar bajo /auth/**,
 * y con el mismo límite por IP que el login (RateLimitFilter).
 */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@RequestMapping("/auth/recuperacion")
public class RecuperacionAccesoController {

    static final String MSG_RESTABLECIDA = "Listo: cambiamos tu contraseña. Ya puedes iniciar sesión.";

    private final RecuperacionAccesoService servicio;
    private final ClienteIp clienteIp;

    public RecuperacionAccesoController(RecuperacionAccesoService servicio, ClienteIp clienteIp) {
        this.servicio = servicio;
        this.clienteIp = clienteIp;
    }

    /** Envía el código. En {@code data} va el correo enmascarado y los minutos de vigencia. */
    @PostMapping("/solicitar")
    public ResponseEntity<ApiResponse<?>> solicitar(@RequestBody SolicitudRecuperacionDto dto, HttpServletRequest request) {
        if (vacio(dto.getLogin())) return incompleto("Escribe tu usuario.");
        RecuperacionEnviadaDto enviado = servicio.solicitar(dto.getLogin(), clienteIp.de(request));
        return ResponseEntity.ok(new ApiResponse<>(
                "Te enviamos un código a " + enviado.getCorreo() + ".", enviado, HttpStatus.OK.value()));
    }

    /** Valida el código y cambia la contraseña; si el usuario estaba bloqueado por intentos, lo desbloquea. */
    @PostMapping("/restablecer")
    public ResponseEntity<ApiResponse<?>> restablecer(@RequestBody RestablecerContrasenaDto dto, HttpServletRequest request) {
        if (vacio(dto.getLogin()) || vacio(dto.getCodigo()) || vacio(dto.getNuevaContrasena())) {
            return incompleto("Completa el usuario, el código y la nueva contraseña.");
        }
        servicio.restablecer(dto.getLogin(), dto.getCodigo(), dto.getNuevaContrasena(), clienteIp.de(request));
        return ResponseEntity.ok(new ApiResponse<>(MSG_RESTABLECIDA, null, HttpStatus.OK.value()));
    }

    @ExceptionHandler(CorreoNoEnviadoException.class)
    public ResponseEntity<ApiResponse<?>> correoNoEnviado(CorreoNoEnviadoException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(e.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE.value()));
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private static ResponseEntity<ApiResponse<?>> incompleto(String mensaje) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(mensaje, null, HttpStatus.BAD_REQUEST.value()));
    }
}
