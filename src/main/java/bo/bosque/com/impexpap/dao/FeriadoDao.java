package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DiaNoLaborable;
import bo.bosque.com.impexpap.model.SolicitudPermiso;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class FeriadoDao implements IDiaNoLaborable{

    private final SpHelper spHelper;
    public FeriadoDao(SpHelper spHelper){this.spHelper = spHelper;}

    public List<DiaNoLaborable> obtenerFeriadosPorEmpleado(DiaNoLaborable filtro) {
        // Usa la accion 'F' para obtener globales + específicos de sucursal

        return this.spHelper.ejecutarListado("p_list_DiaNoLaborable", filtro, "F", DiaNoLaborable.class);
    }
}
