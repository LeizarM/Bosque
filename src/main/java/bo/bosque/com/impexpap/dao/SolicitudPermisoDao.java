package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SolicitudPermiso;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.utils.Tipos;

import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SolicitudPermisoDao implements ISolicitudPermiso {
    private final SpHelper spHelper;

    public SolicitudPermisoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * REGISTRAR O ACTUALIZAR SOLICITUD (Acción 'I' o 'U')
     * Se usa ejecutarAbmMap para que las fechas (java.sql.Timestamp) viajen como
     * objetos nativos al PreparedStatement, evitando que Jackson las serialice
     * a String ISO 8601 que el driver JTDS en Linux no puede convertir.
     */
    @Override
    public RespuestaSp registrarSolicitud(SolicitudPermiso s, String acc) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (s.getCodSolicitud() != 0) params.put("codSolicitud", s.getCodSolicitud());
        params.put("codEmpleado", s.getCodEmpleado());
        params.put("codRelEmplEmpr", s.getCodRelEmplEmpr());
        params.put("tipoPermiso", s.getTipoPermiso());
        params.put("desde", s.getDesde());          // java.sql.Timestamp nativo
        params.put("hasta", s.getHasta());          // java.sql.Timestamp nativo
        params.put("motivo", s.getMotivo());
        params.put("cantidadDias", s.getCantidadDias());
        params.put("estado", s.getEstado());
        params.put("audUsuarioI", s.getAudUsuarioI());
        
        System.out.println("[SolicitudPermisoDao] Ejecutando ABM '" + acc + "' | Fechas nativas enviadas -> Desde: " + s.getDesde() + ", Hasta: " + s.getHasta());
        
        try {
            return this.spHelper.ejecutarAbmMap("p_abm_SolicitudVacacion", params, acc);
        } catch (Exception e) {
            System.err.println("[SolicitudPermisoDao] Error al ejecutar ABM '" + acc + "': " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * APROBAR SOLICITUD (Acción 'A')
     */
    @Override
    public RespuestaSp aprobarSolicitud(SolicitudPermiso s, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_SolicitudVacacion", s, acc);
    }

    /**
     * RECHAZAR SOLICITUD (Acción 'R')
     */
    @Override
    public RespuestaSp rechazarSolicitud(SolicitudPermiso s, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_SolicitudVacacion", s, acc);
    }

    /**
     * ANULAR SOLICITUD APROBADA (Acción 'AN')
     */
    @Override
    public RespuestaSp anularSolicitud(SolicitudPermiso s, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_SolicitudVacacion", s, acc);
    }

    @Override
    public List<SolicitudPermiso> listarPendientes(SolicitudPermiso filtro) {
        // Usamos un objeto vacío solo para disparar la acción 'P'
        return spHelper.ejecutarListado(
                "p_list_SolicitudVacacion",
                filtro,
                "P",
                SolicitudPermiso.class);
    }

    @Override
    public List<SolicitudPermiso> listarMisSolicitudes(SolicitudPermiso filtro) {
        // Llama a p_list_Solicitudes con la acción 'EMP' (Mis Solicitudes)
        return this.spHelper.ejecutarListado("p_list_SolicitudVacacion", filtro, "A", SolicitudPermiso.class);
    }

    /**
     * Obtendra una lista de tipo de permiso
     *
     * @return
     */
    public List<Tipos> listTipoPermiso(SolicitudPermiso s) {
        return this.spHelper.ejecutarListado("p_list_SolicitudVacacion", s, "TP", Tipos.class);
    }

    @Override
    public List<SolicitudPermiso> previsualizarSaldo(SolicitudPermiso filtro) {
        // AQUÍ TAMBIÉN PASABA EL BUG: al enviar fechas (desde/hasta) para la previsualización,
        // Jackson los convertía a String y Linux fallaba. Lo forzamos por Map explícito.
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codEmpleado", filtro.getCodEmpleado());
        params.put("tipoPermiso", filtro.getTipoPermiso());
        params.put("desde", filtro.getDesde()); // Timestamp nativo
        params.put("hasta", filtro.getHasta()); // Timestamp nativo

        System.out.println("[SolicitudPermisoDao] Previsualizando Saldo 'C' | Fechas nativas enviadas -> Desde: " + filtro.getDesde() + ", Hasta: " + filtro.getHasta());
        
        try {
            return this.spHelper.ejecutarListado("p_list_SolicitudVacacion", params, "C", SolicitudPermiso.class);
        } catch (Exception e) {
            System.err.println("[SolicitudPermisoDao] Error al previsualizar saldo: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}