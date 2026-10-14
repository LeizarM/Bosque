package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.ConfiguracionRecuperacionService;
import bo.bosque.com.impexpap.commons.CorreoRecuperacion;
import bo.bosque.com.impexpap.dto.GuardarConfiguracionDto;
import bo.bosque.com.impexpap.dto.ProbarCorreoDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * Parámetros de la recuperación de acceso (trec_configuracion): SMTP, remitente, dominios y límites.
 * Solo administradores del sistema. Un cambio rige en el siguiente pedido de código, sin reiniciar.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@RequestMapping("/recuperacion-configuracion")
@PreAuthorize("hasRole('ROLE_ADM')")
public class ConfiguracionRecuperacionController {

    /** {@code tb_vista.direccion} de la pantalla; el literal está también en el frontend (rutaConfiguracionRecuperacion). */
    public static final String PANTALLA = "trecConfiguracion/Recuperacion";

    static final String MSG_GUARDADO = "Guardado. Rige desde el próximo pedido de código.";
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final ConfiguracionRecuperacionService servicio;
    private final CorreoRecuperacion correo;

    public ConfiguracionRecuperacionController(ConfiguracionRecuperacionService servicio, CorreoRecuperacion correo) {
        this.servicio = servicio;
        this.correo = correo;
    }

    /** Los parámetros en el orden de la pantalla; de la contraseña solo se dice si está cargada. */
    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar() {
        return ResponseEntity.ok(new ApiResponse<>("Parámetros de la recuperación de acceso.",
                servicio.paraPantalla(), HttpStatus.OK.value()));
    }

    @PostMapping("/guardar")
    public ResponseEntity<ApiResponse<?>> guardar(@RequestBody GuardarConfiguracionDto dto, Authentication auth) {
        if (dto.getClave() == null || dto.getClave().trim().isEmpty()) {
            return respuesta(HttpStatus.BAD_REQUEST, "Falta el parámetro.");
        }
        servicio.guardar(dto.getClave().trim(), dto.getValor(), DatosToken.codUsuarioDe(auth));
        return respuesta(HttpStatus.OK, MSG_GUARDADO);
    }

    /** Manda un correo con la configuración vigente, para comprobarla sin esperar a que alguien pida un código. */
    @PostMapping("/probar")
    public ResponseEntity<ApiResponse<?>> probar(@RequestBody ProbarCorreoDto dto) {
        String para = dto.getCorreo() == null ? "" : dto.getCorreo().trim();
        if (!CORREO.matcher(para).matches()) return respuesta(HttpStatus.BAD_REQUEST, "Escribe un correo válido.");
        try {
            correo.enviar(new String[]{para}, "Prueba de correo de Bosque",
                    "Si lees esto, la configuración de correo de la recuperación de acceso funciona.");
        } catch (RuntimeException e) {
            log.warn("Falló el correo de prueba a {}", para, e);
            // Solo lo ve un administrador: el motivo técnico es lo que le sirve para corregir la configuración.
            return respuesta(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo enviar: " + NestedExceptionUtils.getMostSpecificCause(e).getMessage());
        }
        return respuesta(HttpStatus.OK, "Enviamos un correo de prueba a " + para + ". Revisa esa casilla.");
    }

    private static ResponseEntity<ApiResponse<?>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ApiResponse<>(mensaje, null, estado.value()));
    }
}
