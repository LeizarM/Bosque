package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NotaPendiente;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NotaPendienteDao implements INotaPendiente {

    private static final String SP_LIST = "p_list_noPagado";

    private final SpHelper spHelper;

    public NotaPendienteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Se usa la rama B y no la A: la A devuelve las columnas de los CASE sin
     * alias, asi que no hay nombre por el cual mapearlas.
     */
    @Override
    public List<NotaPendiente> obtenerPendientes() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<String, Object>(),
                "B", NotaPendiente.class);
    }
}
