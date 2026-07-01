package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.ChipTigo;
import bo.bosque.com.impexpap.model.SolicitudPermiso;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SolicitudPermisoDao implements ISolicitudPermiso {
    private final SpHelper spHelper;
    public SolicitudPermisoDao(SpHelper spHelper){this.spHelper = spHelper;}

    @Override
    public RespuestaSp registrarSolicitud(SolicitudPermiso s,String acc) {
        return this.spHelper.ejecutarAbm("p_abm_SolicitudVacacion", s, acc);
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
        // Llama a p_list_Solicitudes con la acción 'EMP' (Mis Solicitudes)
        return this.spHelper.ejecutarListado("p_list_SolicitudVacacion", filtro, "A", SolicitudPermiso.class);
    }
    /**
     * Obtendra una lista de tipo de permiso
     * @return
     */
    public List<Tipos> listTipoPermiso() {
        return new Tipos().listTipoPermiso();
    }
    @Override
    public List<SolicitudPermiso> previsualizarSaldo(SolicitudPermiso filtro) {
        return this.spHelper.ejecutarListado("p_list_SolicitudVacacion", filtro, "C", SolicitudPermiso.class);
    }
}