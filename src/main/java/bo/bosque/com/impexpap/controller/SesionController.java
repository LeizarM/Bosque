package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.SesionService;
import bo.bosque.com.impexpap.dto.CerrarSesionDto;
import bo.bosque.com.impexpap.dto.HistorialSesionesDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
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

/** Pantalla Sesiones (trec_sesion): quién está conectado, el historial y el cierre de una sesión. Solo administradores. */
@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@RequestMapping("/sesiones")
@PreAuthorize("hasRole('ROLE_ADM')")
public class SesionController {

    /** {@code tb_vista.direccion} de la pantalla; el literal está también en el frontend (rutaSesiones). */
    public static final String PANTALLA = "trecSesion/Sesiones";

    static final String MSG_CERRADA = "Sesión cerrada. Esa persona vuelve al inicio de sesión en su próxima acción o en 2 minutos como máximo.";

    private final SesionService servicio;

    public SesionController(SesionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/abiertas")
    public ResponseEntity<ApiResponse<?>> abiertas(Authentication auth) {
        return ResponseEntity.ok(new ApiResponse<>("Sesiones abiertas.", servicio.abiertas(DatosToken.de(auth)),
                HttpStatus.OK.value()));
    }

    @PostMapping("/historial")
    public ResponseEntity<ApiResponse<?>> historial(@RequestBody HistorialSesionesDto dto) {
        return ResponseEntity.ok(new ApiResponse<>("Historial de sesiones.",
                servicio.historial(dto.getDesde(), dto.getHasta(), dto.getCodUsuario()), HttpStatus.OK.value()));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<ApiResponse<?>> cerrar(@RequestBody CerrarSesionDto dto, Authentication auth) {
        if (dto.getIdSesion() == null || dto.getIdSesion() <= 0) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Falta la sesión.", null, HttpStatus.BAD_REQUEST.value()));
        }
        servicio.cerrarPorAdministrador(dto.getIdSesion(), DatosToken.de(auth));
        return ResponseEntity.ok(new ApiResponse<>(MSG_CERRADA, null, HttpStatus.OK.value()));
    }
}
