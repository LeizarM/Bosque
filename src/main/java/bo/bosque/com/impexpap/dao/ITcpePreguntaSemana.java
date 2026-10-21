package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaBancoDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaEstadisticaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaVigenteDto;
import bo.bosque.com.impexpap.dto.TcpeRsptaSemanaDetalleDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * {@code tcpe_preguntaSemana} (v11, contrato 15.3): el CRUD de {@link ITcpeCrud} más las acciones del inicio
 * ({@code V} + {@code O}) y de la gestión ({@code L} con estado, {@code E}, {@code R}, {@code B} y {@code A}).
 */
public interface ITcpePreguntaSemana extends ITcpeCrud<TcpePreguntaSemana> {

    /** {@code L} para la gestión: estado de hoy, pregunta y aciertos. {@code activo} nulo = todas. */
    List<TcpePreguntaSemanaDto> gestion(Boolean activo);

    /** {@code V} y {@code O}: las vigentes de hoy para {@code codUsuario} (el del token), cada una con sus opciones. */
    List<TcpePreguntaSemanaVigenteDto> vigentes(long codUsuario);

    /** {@code E}: cuántos eligieron cada opción. */
    List<TcpePreguntaSemanaEstadisticaDto> estadistica(Long codPreguntaSemana);

    /** {@code R}: quiénes respondieron. */
    List<TcpeRsptaSemanaDetalleDto> respuestas(Long codPreguntaSemana);

    /** {@code B}: preguntas del banco para elegir, por categoría o texto (sin ninguno, vacía). */
    List<TcpePreguntaSemanaBancoDto> banco(Long codClasificacion, String texto);

    /** {@code A}: activa o desactiva; el SP pide {@code activo}. */
    RespuestaSp activar(Long codPreguntaSemana, Boolean activo, long audUsuario);
}
