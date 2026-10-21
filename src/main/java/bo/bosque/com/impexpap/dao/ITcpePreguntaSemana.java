package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaVigenteDto;
import bo.bosque.com.impexpap.dto.TcpeRsptaSemanaDetalleDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * {@code tcpe_preguntaSemana} (v12, contrato 15.3): el CRUD de {@link ITcpeCrud} más las acciones del inicio
 * ({@code V}) y de la gestión ({@code L} con estado, {@code R} y {@code A}).
 */
public interface ITcpePreguntaSemana extends ITcpeCrud<TcpePreguntaSemana> {

    /** {@code L} para la gestión: estado de hoy y cuántos respondieron. {@code activo} nulo = todas. */
    List<TcpePreguntaSemanaDto> gestion(Boolean activo);

    /** {@code V}: las vigentes de hoy para {@code codUsuario} (el del token), con lo que ya respondió. */
    List<TcpePreguntaSemanaVigenteDto> vigentes(long codUsuario);

    /** {@code R}: quiénes respondieron y qué escribieron. */
    List<TcpeRsptaSemanaDetalleDto> respuestas(Long codPreguntaSemana);

    /** {@code A}: activa o desactiva; el SP pide {@code activo}. */
    RespuestaSp activar(Long codPreguntaSemana, Boolean activo, long audUsuario);
}
