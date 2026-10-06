// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\DependientesJefeDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DependienteCargo;
import bo.bosque.com.impexpap.utils.ListadoConEstado;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
public class DependientesJefeDao implements IDependientesJefe {

    private final SpHelper spHelper;

    public DependientesJefeDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public ListadoConEstado<DependienteCargo> listarDependientes(
            long codUsuario, String profundidad, String alcanceSucursal) {

        Map<String, Object> params = new HashMap<>();
        params.put("codUsuario", codUsuario);
        params.put("profundidad", profundidad);
        params.put("alcanceSucursal", alcanceSucursal);
        // nivelMaximoJefe ya NO se envía — el SP lo resuelve de dbo.tac_configuracion.

        return spHelper.ejecutarListadoConEstado(
                "p_list_tac_dependientesJefe", params, DependienteCargo.class);
    }
}
