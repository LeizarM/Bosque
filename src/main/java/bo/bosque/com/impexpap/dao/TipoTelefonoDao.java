package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.model.TipoTelefono;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/** Acceso a {@code trh_tipoTelefono}. Todo pasa por procedimiento almacenado vía {@link SpHelper}. */
@Repository
public class TipoTelefonoDao implements ITipoTelefono {

    private static final String SP_ABM = "p_abm_TipoTelefono";
    private static final String SP_LIST = "p_list_TipoTelefono";

    private final SpHelper spHelper;

    public TipoTelefonoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TipoTelefono> listar() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<String, Object>(), "P", TipoTelefono.class);
    }

    @Override
    public RespuestaSp registrar(TipoTelefono tipo) {
        boolean alta = tipo.getCodTipoTel() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codTipoTel", tipo.getCodTipoTel());
        p.put("tipo", tipo.getTipo());
        p.put("audUsuarioI", tipo.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp eliminar(long codTipoTel, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codTipoTel", codTipoTel);
        p.put("audUsuarioI", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }
}
