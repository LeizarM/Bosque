package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.LogEstados;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ILogEstados {

    /**
     * Registrar un registro de Log de Estados.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I')
     */
    RespuestaSp registrarLogEstados(LogEstados mb, String acc);

    /**
     * [L] Historial completo de logs sin filtro (todos los registros).
     * El SP no filtra por @idLog; este método devuelve todos.
     */
    List<LogEstados> obtenerLogEstados(long idLog);

    /**
     * [L] Logs de estados de una solicitud específica.
     * @param idSolicitud ID de la solicitud
     */
    List<LogEstados> obtenerLogPorSolicitud(long idSolicitud);

    /**
     * [L] Logs de estados de una transacción específica.
     * @param idTransaccion ID de la transacción
     */
    List<LogEstados> obtenerLogPorTransaccion(long idTransaccion);

    /**
     * [F] Timeline completo de una solicitud: incluye cambios de la solicitud,
     * sus cotizaciones y sus transacciones, en orden cronológico.
     * @param idSolicitud ID de la solicitud raíz
     */
    List<LogEstados> obtenerTimelineCompleto(long idSolicitud);
}

