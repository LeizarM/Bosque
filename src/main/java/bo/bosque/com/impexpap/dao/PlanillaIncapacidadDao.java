package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PlanillaIncapacidad;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PlanillaIncapacidadDao implements IPlanillaIncapacidad {

    private final SpHelper spHelper;

    public PlanillaIncapacidadDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /** ACCION A: carga las bajas nuevas y lista las que empiezan en el rango. Map: estos SP filtran por NULL, no por 0. */
    @Override
    public List<PlanillaIncapacidad> listarRevision(Date desde, Date hasta, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("desde", desde);
        params.put("hasta", hasta);
        params.put("audUsuario", audUsuario);
        return this.spHelper.ejecutarListado("p_list_planillaIncapacidad", params, "A", PlanillaIncapacidad.class);
    }

    /** ACCION B: solo fueRevisado; el resto de la fila no se toca. */
    @Override
    public RespuestaSp marcarRevisado(long idPIT, int fueRevisado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idPIT", idPIT);
        params.put("fueRevisado", fueRevisado);
        params.put("audUsuario", audUsuario);
        return this.spHelper.ejecutarAbmMap("p_abm_planillaIncapacidad", params, "B");
    }
}
