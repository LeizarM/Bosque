package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.dao.ITcpePreguntaSemana;
import bo.bosque.com.impexpap.dao.ITcpeRsptaSemana;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaFiltroDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
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
 * Preguntas de la semana (contratos 16 y 17). {@code vigentes} y {@code responder} son del inicio: cualquier empleado,
 * sin pantalla, con el usuario del token. La gestión pide {@code tcpeExamenes/PreguntaSemana} (el admin pasa).
 * Los roles de clase son los de todo empleado (adm o lim); MainSecurity ya deja fuera al postulante.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes/pregunta-semana")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpePreguntaSemanaController {

    private final ITcpePreguntaSemana preguntas;
    private final ITcpeRsptaSemana respuestas;
    private final TcpePermisos permisos;

    public TcpePreguntaSemanaController(ITcpePreguntaSemana preguntas, ITcpeRsptaSemana respuestas,
                                        TcpePermisos permisos) {
        this.preguntas = preguntas;
        this.respuestas = respuestas;
        this.permisos = permisos;
    }

    /** Las vigentes de hoy para quien mira, con lo que ya respondió; 204 si no hay ninguna. */
    @PostMapping("/vigentes")
    public ResponseEntity<ApiResponse<?>> vigentes(Authentication auth) {
        return TcpeRespuestas.listado(preguntas.vigentes(DatosToken.codUsuarioDe(auth)));
    }

    /** {@code message} = el agradecimiento que escribe el SP; {@code data} = codRsptaSemana. */
    @PostMapping("/responder")
    public ResponseEntity<ApiResponse<?>> responder(@Valid @RequestBody TcpePreguntaSemanaFiltroDto dto,
                                                    Authentication auth) {
        return TcpeRespuestas.escritura(respuestas.responder(dto.getCodPreguntaSemana(), dto.getRespuesta(),
                dto.getValor(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@Valid @RequestBody(required = false) TcpePreguntaSemanaFiltroDto dto,
                                                 Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.PREGUNTA_SEMANA);
        return TcpeRespuestas.listado(preguntas.gestion(dto == null ? null : dto.getActivo()));
    }

    /** Código nulo o 0 = alta; si no, cambio completo. {@code idGenerado} = codPreguntaSemana. */
    @PostMapping("/guardar")
    public ResponseEntity<ApiResponse<?>> guardar(@Valid @RequestBody TcpePreguntaSemana modelo, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.PREGUNTA_SEMANA);
        return TcpeRespuestas.escritura(preguntas.guardar(modelo, DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/activar")
    public ResponseEntity<ApiResponse<?>> activar(@Valid @RequestBody TcpePreguntaSemanaFiltroDto dto,
                                                  Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.PREGUNTA_SEMANA);
        return TcpeRespuestas.escritura(preguntas.activar(dto.getCodPreguntaSemana(), dto.getActivo(),
                DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminar(@Valid @RequestBody TcpePreguntaSemanaFiltroDto dto,
                                                   Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.PREGUNTA_SEMANA);
        return TcpeRespuestas.escritura(preguntas.eliminar(dto.getCodPreguntaSemana(), DatosToken.codUsuarioDe(auth)));
    }

    @PostMapping("/respuestas")
    public ResponseEntity<ApiResponse<?>> respuestas(@Valid @RequestBody TcpePreguntaSemanaFiltroDto dto,
                                                     Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.PREGUNTA_SEMANA);
        return TcpeRespuestas.listado(preguntas.respuestas(dto.getCodPreguntaSemana()));
    }

    /** De escala: cuántos eligieron cada valor, con el promedio; una abierta responde 204. */
    @PostMapping("/distribucion")
    public ResponseEntity<ApiResponse<?>> distribucion(@Valid @RequestBody TcpePreguntaSemanaFiltroDto dto,
                                                       Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.PREGUNTA_SEMANA);
        return TcpeRespuestas.listado(preguntas.distribucion(dto.getCodPreguntaSemana()));
    }

    /** Quita una respuesta (por ejemplo, un texto fuera de lugar): la persona puede volver a responder. */
    @PostMapping("/quitar-respuesta")
    public ResponseEntity<ApiResponse<?>> quitarRespuesta(@Valid @RequestBody TcpePreguntaSemanaFiltroDto dto,
                                                          Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.PREGUNTA_SEMANA);
        return TcpeRespuestas.escritura(respuestas.eliminar(dto.getCodRsptaSemana(), DatosToken.codUsuarioDe(auth)));
    }
}
