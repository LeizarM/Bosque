package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Permiso;
import bo.bosque.com.impexpap.model.TigoEjecutado;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PermisoDao implements IPermiso{

    @Autowired
    JdbcTemplate jdbcTemplate;
    private final SpHelper spHelper;

    public PermisoDao (SpHelper spHelper){this.spHelper = spHelper;}

    /**
     * DIAS TOTALES DISPONIBLES DE VACACION EMPLEADO
     */
    @Override
    public List<Permiso> diasDisponibles(Permiso filtro) {
        return spHelper.ejecutarListado(
                "p_list_Permiso",
                filtro,
                "H1",
                Permiso.class
        );
    }
}
