package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.SolicitudPermiso;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SolicitudPermisoDao implements ISolicitudPermiso {
    private final SpHelper spHelper;
    public SolicitudPermisoDao(SpHelper spHelper){this.spHelper = spHelper;}

    /**
     * Formatea un Timestamp a String ISO-8601 compatible con CONVERT(DATETIME, @param, 126) en SQL Server.
     * Evita que el driver JDBC de Linux intente convertir el Timestamp a DATETIME implícitamente.
     */
    private String formatFecha(java.sql.Timestamp ts) {
        if (ts == null) return null;
        // Formato: yyyy-MM-dd'T'HH:mm:ss.SSS  → compatible con CONVERT(DATETIME, @p, 126)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return sdf.format(ts);
    }

    @Override
    public RespuestaSp registrarSolicitud(SolicitudPermiso s, String acc) {
        // Usamos ejecutarAbmMap para enviar las fechas como String (VARCHAR),
        // evitando que SimpleJdbcCall o el driver JDBC intenten convertirlas a Timestamp.
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codSolicitud",   s.getCodSolicitud());
        params.put("codEmpleado",    s.getCodEmpleado());
        params.put("codRelEmplEmpr", s.getCodRelEmplEmpr());
        params.put("tipoPermiso",    s.getTipoPermiso());
        params.put("desde",          formatFecha(s.getDesde()));  // ← String, no Timestamp
        params.put("hasta",          formatFecha(s.getHasta()));  // ← String, no Timestamp
        params.put("motivo",         s.getMotivo());
        params.put("cantidadDias",   s.getCantidadDias());
        params.put("estado",         s.getEstado());
        params.put("audUsuarioI",    s.getAudUsuarioI());
        return this.spHelper.ejecutarAbmMap("p_abm_SolicitudVacacion", params, acc);
    }

    @Override
    public RespuestaSp aprobarSolicitud(SolicitudPermiso s,String acc) {
        return this.spHelper.ejecutarAbm("p_abm_SolicitudVacacion", s, acc);
    }

    @Override
    public RespuestaSp rechazarSolicitud(SolicitudPermiso s,String acc) {
        return this.spHelper.ejecutarAbm("p_abm_SolicitudVacacion", s, acc);
    }
    @Override
    public List<SolicitudPermiso> listarPendientes(SolicitudPermiso filtro) {
        // Usamos un objeto vacío solo para disparar la acción 'A'
        return spHelper.ejecutarListado(
                "p_list_SolicitudVacacion",
                filtro,
                "P",
                SolicitudPermiso.class
        );
    }
    @Override
    public List<SolicitudPermiso> listarMisSolicitudes(SolicitudPermiso filtro) {
        return this.spHelper.ejecutarListado("p_list_SolicitudVacacion", filtro, "A", SolicitudPermiso.class);
    }

    public List<Tipos> listTipoPermiso() {
        return new Tipos().listTipoPermiso();
    }
    @Override
    public List<SolicitudPermiso> previsualizarSaldo(SolicitudPermiso filtro) {
        // La previsualización también envía fechas → usamos Map explícito con String
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codEmpleado",    filtro.getCodEmpleado());
        params.put("codRelEmplEmpr", filtro.getCodRelEmplEmpr());
        params.put("tipoPermiso",    filtro.getTipoPermiso());
        params.put("desde",          formatFecha(filtro.getDesde()));  // ← String
        params.put("hasta",          formatFecha(filtro.getHasta()));  // ← String
        return this.spHelper.ejecutarListado("p_list_SolicitudVacacion", params, "C", SolicitudPermiso.class);
    }
}