package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dto.TcpeLatidoDto;
import bo.bosque.com.impexpap.dto.TcpeRendirAccionDto;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Exámenes del <b>empleado</b> con su sesión del sistema. El dueño es el {@code tcpe_usuario} del {@code codEmpleado}
 * del token ({@code p_list_tcpe_Acceso 'E'}), nunca del cuerpo; sin uno, va 0 y el SP no encuentra nada suyo. Los
 * pasos son los del postulante ({@link TcpeRendicion}).
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes/mis-examenes")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpeMisExamenesController {

    /** Ningún {@code tcpe_usuario} tiene código 0: con él, el SP responde que el examen no es suyo. */
    private static final long SIN_USUARIO = 0L;

    private final ITcpeAcceso acceso;
    private final TcpeRendicion rendicion;

    public TcpeMisExamenesController(ITcpeAcceso acceso, TcpeRendicion rendicion) {
        this.acceso = acceso;
        this.rendicion = rendicion;
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(Authentication auth) {
        Long codUsuario = acceso.codUsuarioDeEmpleado(codEmpleado(auth));
        return codUsuario == null ? TcpeRespuestas.sinDatos() : rendicion.listar(codUsuario);
    }

    @PostMapping("/estado")
    public ResponseEntity<ApiResponse<?>> estado(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.estado(codUsuario(auth), dto);
    }

    @PostMapping("/preguntas")
    public ResponseEntity<ApiResponse<?>> preguntas(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.preguntas(codUsuario(auth), dto);
    }

    @PostMapping("/opciones")
    public ResponseEntity<ApiResponse<?>> opciones(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.opciones(codUsuario(auth), dto);
    }

    @PostMapping("/revision")
    public ResponseEntity<ApiResponse<?>> revision(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.revision(codUsuario(auth), dto);
    }

    @PostMapping("/iniciar")
    public ResponseEntity<ApiResponse<?>> iniciar(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.iniciar(codUsuario(auth), (long) DatosToken.codUsuarioDe(auth), dto);
    }

    @PostMapping("/responder")
    public ResponseEntity<ApiResponse<?>> responder(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.responder(codUsuario(auth), (long) DatosToken.codUsuarioDe(auth), dto);
    }

    @PostMapping("/latido")
    public ResponseEntity<ApiResponse<?>> latido(@Valid @RequestBody TcpeLatidoDto dto, Authentication auth) {
        return rendicion.latido(codUsuario(auth), dto);
    }

    @PostMapping("/finalizar")
    public ResponseEntity<ApiResponse<?>> finalizar(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.finalizar(codUsuario(auth), (long) DatosToken.codUsuarioDe(auth), dto);
    }

    private long codUsuario(Authentication auth) {
        Long cod = acceso.codUsuarioDeEmpleado(codEmpleado(auth));
        return cod == null ? SIN_USUARIO : cod;
    }

    private static long codEmpleado(Authentication auth) {
        int codEmpleado = DatosToken.de(auth).getCodEmpleado();
        if (codEmpleado <= 0) {
            // Con su mensaje (403): un AccessDeniedException a secas llega a la UI con un texto fijo.
            throw new SinPermisoException("Su usuario no está vinculado a un empleado. Cierre sesión y vuelva a entrar.");
        }
        return codEmpleado;
    }
}
