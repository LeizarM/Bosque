package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoCelda;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TtkBingoCeldaDao implements ITtkBingoCelda {

    private static final String SP_LIST = "p_list_ttk_BingoCelda";

    private final SpHelper spHelper;

    public TtkBingoCeldaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkBingoCelda> delCarton(long codCarton) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codCarton", codCarton), "E", TtkBingoCelda.class);
    }

    @Override
    public List<TtkBingoCelda> verCarton(long codCarton, long audUsuario) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codCarton", codCarton, "audUsuario", audUsuario),
                "C", TtkBingoCelda.class);
    }

    @Override
    public List<TtkBingoCelda> misCeldas(Long codBingo, long audUsuario) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo, "audUsuario", audUsuario),
                "M", TtkBingoCelda.class);
    }

    @Override
    public List<TtkBingoCelda> impresion(TtkBingoCelda f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codBingo", f.getCodBingo(), "codEmpleado", f.getCodEmpleado(), "nroDesde", f.getNroDesde(),
                "nroHasta", f.getNroHasta(), "soloAsignados", f.getSoloAsignados(), "soloImpresos", f.getSoloImpresos()),
                "P", TtkBingoCelda.class);
    }
}
