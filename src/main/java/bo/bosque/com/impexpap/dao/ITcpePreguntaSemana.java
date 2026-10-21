package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaEscalaDto;
import bo.bosque.com.impexpap.dto.TcpePreguntaSemanaVigenteDto;
import bo.bosque.com.impexpap.dto.TcpeRsptaSemanaDetalleDto;
import bo.bosque.com.impexpap.model.TcpePreguntaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * {@code tcpe_preguntaSemana} (contratos 16 y 17): el CRUD de {@link ITcpeCrud} más las acciones del inicio
 * ({@code V}) y de la gestión ({@code L} con estado, {@code R}, {@code E} y {@code A}).
 */
public interface ITcpePreguntaSemana extends ITcpeCrud<TcpePreguntaSemana> {

    /** {@code L} para la gestión: estado de hoy y cuántos respondieron. {@code activo} nulo = todas. */
    List<TcpePreguntaSemanaDto> gestion(Boolean activo);

    /** {@code V}: las vigentes de hoy para {@code codUsuario} (el del token), con lo que ya respondió. */
    List<TcpePreguntaSemanaVigenteDto> vigentes(long codUsuario);

    /** {@code R}: quiénes respondieron y qué (el texto o el valor). */
    List<TcpeRsptaSemanaDetalleDto> respuestas(Long codPreguntaSemana);

    /** {@code E}: la escala valor por valor, con cuántos lo eligieron; una abierta no trae filas. */
    List<TcpePreguntaSemanaEscalaDto> distribucion(Long codPreguntaSemana);

    /** {@code A}: activa o desactiva; el SP pide {@code activo}. */
    RespuestaSp activar(Long codPreguntaSemana, Boolean activo, long audUsuario);
}
