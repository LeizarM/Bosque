package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRsptaSemana;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * {@code tcpe_rsptaSemana} (v12/v13): el CRUD de {@link ITcpeCrud}, salvo el alta genérica, más responder desde el
 * inicio con {@code p_abm_tcpe_RsptaSemana 'I'}.
 */
public interface ITcpeRsptaSemana extends ITcpeCrud<TcpeRsptaSemana> {

    /**
     * {@code I}: {@code codUsuario} (el del token, también como {@code audUsuario}) responde con un texto o, si es de
     * escala, con {@code valor}. El SP valida todo y devuelve el agradecimiento; {@code idGenerado} = codRsptaSemana.
     */
    RespuestaSp responder(Long codPreguntaSemana, String respuesta, Integer valor, long codUsuario);
}
