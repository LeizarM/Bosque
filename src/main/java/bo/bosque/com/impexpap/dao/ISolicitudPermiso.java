package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ChipTigo;
import bo.bosque.com.impexpap.model.SolicitudPermiso;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;

import java.util.List;
import java.util.Map;

public interface ISolicitudPermiso {
    /** Registra una nueva solicitud por parte del empleado (ACCION 'I') */
    //Map<String, Object> registrarSolicitud(SolicitudPermiso solicitud);
    RespuestaSp registrarSolicitud(SolicitudPermiso s, String acc);

    /** Aprueba una solicitud existente (Jefe o RRHH) (ACCION 'A') */
    RespuestaSp aprobarSolicitud(SolicitudPermiso s, String acc);

    /** Rechaza una solicitud existente (ACCION 'R') */
    RespuestaSp rechazarSolicitud(SolicitudPermiso s, String acc);
    /** Listar solicitudes de vacion pendientes*/
    List<SolicitudPermiso> listarPendientes(SolicitudPermiso filtro);
    /** Listar solicitudes de vacion pendientes individuales*/
    List<SolicitudPermiso> listarMisSolicitudes(SolicitudPermiso filtro);
    /** Previsualizar saldo de vacaciones (ACCION 'C') */
    List<SolicitudPermiso> previsualizarSaldo(SolicitudPermiso filtro);
    /**
     * lista tipo permiso
     * @return
     */
    List<Tipos>listTipoPermiso();
}
