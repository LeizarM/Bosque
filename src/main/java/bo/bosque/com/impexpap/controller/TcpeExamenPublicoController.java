package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dto.TcpeLatidoDto;
import bo.bosque.com.impexpap.dto.TcpeRegistroPostulanteDto;
import bo.bosque.com.impexpap.dto.TcpeRendirAccionDto;
import bo.bosque.com.impexpap.security.jwt.DatosPostulante;
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
 * Lo que hace el postulante externo con su token ({@code ROLE_POSTULANTE}): completar su registro y rendir. El
 * {@code codUsuario} es siempre el del token ({@link DatosPostulante}), nunca el del cuerpo. {@code MainSecurity}
 * ya exige el rol para {@code /examen-publico/**}; el {@code @PreAuthorize} es la segunda reja.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examen-publico")
@PreAuthorize("hasRole('POSTULANTE')")
public class TcpeExamenPublicoController {

    /** El postulante no existe en {@code tb_usuario}: sus escrituras quedan con {@code audUsuario} NULL. */
    private static final Long SIN_AUDITOR = null;

    private final ITcpeAcceso acceso;
    private final TcpeRendicion rendicion;

    public TcpeExamenPublicoController(ITcpeAcceso acceso, TcpeRendicion rendicion) {
        this.acceso = acceso;
        this.rendicion = rendicion;
    }

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<?>> registro(@Valid @RequestBody TcpeRegistroPostulanteDto dto,
                                                   Authentication auth) {
        return TcpeRespuestas.escritura(acceso.registrar(DatosPostulante.codUsuarioDe(auth), dto));
    }

    @PostMapping("/examenes/listar")
    public ResponseEntity<ApiResponse<?>> listar(Authentication auth) {
        return rendicion.listar(DatosPostulante.codUsuarioDe(auth));
    }

    @PostMapping("/examenes/estado")
    public ResponseEntity<ApiResponse<?>> estado(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.estado(DatosPostulante.codUsuarioDe(auth), dto);
    }

    @PostMapping("/examenes/preguntas")
    public ResponseEntity<ApiResponse<?>> preguntas(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.preguntas(DatosPostulante.codUsuarioDe(auth), dto);
    }

    @PostMapping("/examenes/opciones")
    public ResponseEntity<ApiResponse<?>> opciones(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.opciones(DatosPostulante.codUsuarioDe(auth), dto);
    }

    @PostMapping("/examenes/revision")
    public ResponseEntity<ApiResponse<?>> revision(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.revision(DatosPostulante.codUsuarioDe(auth), dto);
    }

    @PostMapping("/examenes/iniciar")
    public ResponseEntity<ApiResponse<?>> iniciar(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.iniciar(DatosPostulante.codUsuarioDe(auth), SIN_AUDITOR, dto);
    }

    @PostMapping("/examenes/responder")
    public ResponseEntity<ApiResponse<?>> responder(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.responder(DatosPostulante.codUsuarioDe(auth), SIN_AUDITOR, dto);
    }

    @PostMapping("/examenes/latido")
    public ResponseEntity<ApiResponse<?>> latido(@Valid @RequestBody TcpeLatidoDto dto, Authentication auth) {
        return rendicion.latido(DatosPostulante.codUsuarioDe(auth), dto);
    }

    @PostMapping("/examenes/finalizar")
    public ResponseEntity<ApiResponse<?>> finalizar(@Valid @RequestBody TcpeRendirAccionDto dto, Authentication auth) {
        return rendicion.finalizar(DatosPostulante.codUsuarioDe(auth), SIN_AUDITOR, dto);
    }
}
