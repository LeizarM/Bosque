package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.LogEstados;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class LogEstadosDao implements ILogEstados {

    private static final String SP_LIST = "p_list_tpex_LogEstados";
    private static final String SP_ABM  = "p_abm_tpex_LogEstados";

    private final SpHelper spHelper;

    public LogEstadosDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarLogEstados(LogEstados mb, String acc) {
        log.info("Registrando LogEstados: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    // ── L: Historial sin filtro (todos los logs) ───────────────────────────
    // NOTA: el SP no filtra por @idLog en ningún WHERE; este método
    // devuelve todos los registros independientemente del valor de idLog.
    // Se usa Map vacío para que todos los filtros queden en DEFAULT NULL.
    @Override
    public List<LogEstados> obtenerLogEstados(long idLog) {
        Map<String, Object> filtro = new HashMap<>();
        // idLog no es usado como filtro por el SP — se devuelven todos los logs
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", LogEstados.class);
    }

    // ── L: Logs de una solicitud específica ────────────────────────────────
    // FIX: Acción corregida de "S" a "L" (el SP solo implementa L, B, F).
    // FIX: Usa Map para enviar SOLO @idSolicitud; evita que idCotizacion=0
    //      e idTransaccion=0 (default de long) funcionen como filtros reales.
    @Override
    public List<LogEstados> obtenerLogPorSolicitud(long idSolicitud) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSolicitud", idSolicitud);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", LogEstados.class);
    }

    // ── L: Logs de una transacción específica ──────────────────────────────
    // FIX: Acción corregida de "T" a "L" (el SP solo implementa L, B, F).
    // FIX: Usa Map para enviar SOLO @idTransaccion.
    @Override
    public List<LogEstados> obtenerLogPorTransaccion(long idTransaccion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTransaccion", idTransaccion);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", LogEstados.class);
    }

    // ── F: Timeline completo de la solicitud (solicitud + cotiz + trx) ─────
    // NUEVO: usa la acción F del SP que une por subqueries todos los logs
    // relacionados con la solicitud raíz, en orden cronológico ASC.
    @Override
    public List<LogEstados> obtenerTimelineCompleto(long idSolicitud) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSolicitud", idSolicitud);
        return spHelper.ejecutarListado(SP_LIST, filtro, "F", LogEstados.class);
    }
}
