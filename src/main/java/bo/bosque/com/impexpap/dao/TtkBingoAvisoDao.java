package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoAviso;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkBingoAvisoDao implements ITtkBingoAviso {

    private static final String SP_LIST = "p_list_ttk_BingoAviso";
    private static final String SP_ABM = "p_abm_ttk_BingoAviso";

    private final SpHelper spHelper;

    public TtkBingoAvisoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkBingoAviso> pendientes(long codBingo) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo), "L", TtkBingoAviso.class);
    }

    @Override
    public TtkBingoAviso buscar(long codAviso) {
        List<TtkBingoAviso> filas = spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codAviso", codAviso), "L",
                TtkBingoAviso.class);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp registrar(TtkBingoAviso mb, String accion) {
        Map<String, Object> p = "I".equals(accion)
                ? TtkParametros.de("codCarton", mb.getCodCarton(), "audUsuario", mb.getAudUsuario())
                : TtkParametros.de("codAviso", mb.getCodAviso(), "audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
