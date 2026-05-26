package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Anticipo;
import bo.bosque.com.impexpap.model.Multas;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MultasDao implements IMultas{
    private final SpHelper spHelper;

    public MultasDao(SpHelper spHelper){this.spHelper=spHelper;}

    @Override
    public RespuestaSp generarMultas(Multas m, String acc) {
        if ("U2".equals(acc)) {
            // Para U2 usamos Map explícito para evitar conflicto XML type con SimpleJdbcCall
            Map<String, Object> params = new java.util.LinkedHashMap<>();
            params.put("xmlMultas", m.getXmlMultas());
            params.put("audUsuarioI", m.getAudUsuarioI());
            return this.spHelper.ejecutarAbmMap("p_abm_Multa", params, acc);
        }
        return this.spHelper.ejecutarAbm("p_abm_Multa", m, acc);
    }
    @Override
    public List<Multas> listarMultas(Multas m){
        return this.spHelper.ejecutarListado("p_list_Multa",m,"E",Multas.class);
    }
}
