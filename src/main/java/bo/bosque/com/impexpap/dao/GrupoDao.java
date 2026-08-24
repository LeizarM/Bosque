package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Grupo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Los listados usan el overload de Map y nunca el de modelo.
 * <p>
 * ejecutarListado(model, ...) arma la sentencia con TODOS los campos no nulos
 * del objeto, y los int/long primitivos valen 0, que no es null y por lo tanto
 * se envia. El SP solo declara tres parametros, asi que mandarle esParaVenta o
 * activo termina en "no es un parametro del procedimiento" y un HTTP 500.
 */
@Repository
public class GrupoDao implements IGrupo {

    private static final String SP_ABM  = "p_abm_tcom_Grupo";
    private static final String SP_LIST = "p_list_tcom_Grupo";

    private final SpHelper spHelper;

    public GrupoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarGrupo(Grupo mb, String acc) {
        // ejecutarAbm si acepta el modelo: SimpleJdbcCall lee los metadatos del
        // SP y descarta los campos que no correspondan a un parametro.
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    @Override
    public List<Grupo> obtenerGrupos(long idGrupo) {
        return spHelper.ejecutarListado(SP_LIST, filtro(idGrupo, 0L), "L", Grupo.class);
    }

    @Override
    public List<Grupo> obtenerGruposAsignables(long idVendedor) {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, idVendedor), "A", Grupo.class);
    }

    @Override
    public List<Grupo> obtenerGruposTodos() {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, 0L), "E", Grupo.class);
    }

    /** Exactamente los parametros que declara p_list_tcom_Grupo. */
    private Map<String, Object> filtro(long idGrupo, long idVendedor) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idGrupo", idGrupo);
        p.put("idVendedor", idVendedor);
        return p;
    }
}
