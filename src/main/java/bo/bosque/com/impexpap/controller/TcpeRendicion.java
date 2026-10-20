package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.dto.TcpeLatidoDto;
import bo.bosque.com.impexpap.dto.TcpeRendirAccionDto;
import bo.bosque.com.impexpap.dto.TcpeRendirExamenDto;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Pasos de la rendición, latido y revisión final, comunes al postulante y al empleado; el dueño sale del token y las
 * reglas y mensajes, del SP. {@code data}: segundos restantes en iniciar/responder/latido y {@code codExamen} en
 * finalizar. Sin {@code @Transactional}: al vencer, el SP entrega y hace COMMIT antes del error.
 */
@Component
public class TcpeRendicion {

    private final ITcpeRendir dao;

    public TcpeRendicion(ITcpeRendir dao) {
        this.dao = dao;
    }

    ResponseEntity<ApiResponse<?>> listar(long codUsuario) {
        return TcpeRespuestas.listado(dao.misExamenes(codUsuario));
    }

    /** Sin fila (no existe o no es suyo), 204. */
    ResponseEntity<ApiResponse<?>> estado(long codUsuario, TcpeRendirAccionDto dto) {
        TcpeRendirExamenDto e = dao.estado(dto.getCodExamen(), codUsuario);
        return e == null ? TcpeRespuestas.sinDatos() : TcpeRespuestas.ok(e);
    }

    ResponseEntity<ApiResponse<?>> preguntas(long codUsuario, TcpeRendirAccionDto dto) {
        return TcpeRespuestas.listado(dao.preguntas(dto.getCodExamen(), codUsuario));
    }

    ResponseEntity<ApiResponse<?>> opciones(long codUsuario, TcpeRendirAccionDto dto) {
        return TcpeRespuestas.listado(dao.opciones(dto.getCodExamen(), codUsuario));
    }

    /** Devolución del examen ya entregado ({@code p_list_tcpe_Rendir 'F'}); 204 si no hay nada que mostrar. */
    ResponseEntity<ApiResponse<?>> revision(long codUsuario, TcpeRendirAccionDto dto) {
        return TcpeRespuestas.listado(dao.revision(dto.getCodExamen(), codUsuario));
    }

    /** {@code data} = segundos restantes. */
    ResponseEntity<ApiResponse<?>> iniciar(long codUsuario, Long audUsuario, TcpeRendirAccionDto dto) {
        return TcpeRespuestas.escritura(dao.iniciar(dto.getCodExamen(), codUsuario, audUsuario));
    }

    /** {@code data} = segundos restantes. */
    ResponseEntity<ApiResponse<?>> responder(long codUsuario, Long audUsuario, TcpeRendirAccionDto dto) {
        return TcpeRespuestas.escritura(dao.responder(dto.getCodExamen(), codUsuario, dto.getCodPreguntaH(),
                dto.getOpciones(), dto.getRespuestaU(), audUsuario));
    }

    /** {@code data} = segundos restantes (sirve también para sincronizar el cronómetro). */
    ResponseEntity<ApiResponse<?>> latido(long codUsuario, TcpeLatidoDto dto) {
        return TcpeRespuestas.escritura(
                dao.latido(dto.getCodExamen(), codUsuario, dto.getCodPreguntaH(), dto.getSalioPestana()));
    }

    /** {@code data} = codExamen. */
    ResponseEntity<ApiResponse<?>> finalizar(long codUsuario, Long audUsuario, TcpeRendirAccionDto dto) {
        return TcpeRespuestas.escritura(dao.finalizar(dto.getCodExamen(), codUsuario, audUsuario));
    }
}
