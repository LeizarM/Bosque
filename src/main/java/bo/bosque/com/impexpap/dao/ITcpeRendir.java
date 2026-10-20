package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeRendirExamenDto;
import bo.bosque.com.impexpap.dto.TcpeRendirOpcionDto;
import bo.bosque.com.impexpap.dto.TcpeRendirPreguntaDto;
import bo.bosque.com.impexpap.dto.TcpeRevisionItemDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Rendición ({@code p_list/p_abm_tcpe_Rendir}, contrato 3.4) del postulante y del empleado. {@code codUsuario} es el
 * dueño del examen y sale siempre del token; {@code audUsuario} es el {@code tb_usuario} que escribe (null para el
 * postulante, que no existe en {@code tb_usuario}).
 */
public interface ITcpeRendir {

    /** {@code M}: los exámenes del usuario. */
    List<TcpeRendirExamenDto> misExamenes(long codUsuario);

    /** {@code E}: un examen del usuario, o null si no es suyo o no existe. */
    TcpeRendirExamenDto estado(long codExamen, long codUsuario);

    /** {@code P}: preguntas (solo con el examen en curso). */
    List<TcpeRendirPreguntaDto> preguntas(long codExamen, long codUsuario);

    /** {@code O}: opciones con lo marcado, sin la correcta (solo con el examen en curso). */
    List<TcpeRendirOpcionDto> opciones(long codExamen, long codUsuario);

    /** {@code F}: devolución del examen entregado; vacía si no es suyo o no se entregó. */
    List<TcpeRevisionItemDto> revision(long codExamen, long codUsuario);

    /**
     * {@code I}: pone {@code fechaInicio} si no la tenía (y crea la foto de un pendiente del legacy).
     * {@code idGenerado} = segundos restantes. Si ya venció, el SP lo entrega (con COMMIT) y devuelve error: no
     * llamar dentro de una transacción del backend. Lo mismo vale para {@link #responder}.
     */
    RespuestaSp iniciar(long codExamen, long codUsuario, Long audUsuario);

    /**
     * {@code R}: guarda una respuesta.
     * @param opciones   cerrada: los {@code codRsptaOpH} marcados (vacía = sin responder); null si es abierta
     * @param respuestaU abierta: el texto; null si es cerrada
     */
    RespuestaSp responder(long codExamen, long codUsuario, Long codPreguntaH, List<Long> opciones, String respuestaU,
                          Long audUsuario);

    /**
     * {@code A} (latido, contrato 8.3): pregunta que está viendo, actividad y salida de la pestaña. Sin transacción;
     * {@code idGenerado} = segundos restantes. Solo en curso y en el examen propio (lo valida el SP).
     */
    RespuestaSp latido(long codExamen, long codUsuario, Long codPreguntaH, Boolean salioPestana);

    /** {@code F}: entrega y califica las cerradas. Idempotente. {@code idGenerado} = codExamen. */
    RespuestaSp finalizar(long codExamen, long codUsuario, Long audUsuario);

    /** {@code V}: entrega los vencidos y cierra los ausentes del lote (o de todos si es null). Devuelve cuántos. */
    long entregarVencidos(Long codRolExamen, Long audUsuario);

    /** {@code V} con {@code @codUsuario} (detalles v4): solo los exámenes de ese usuario, para su login. */
    long entregarVencidosDe(long codUsuario);
}
