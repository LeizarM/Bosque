package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoSorteo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TtkBingoSorteoDao implements ITtkBingoSorteo {

    private static final String SP_LIST = "p_list_ttk_BingoSorteo";
    private static final String SP_ABM = "p_abm_ttk_BingoSorteo";

    private final SpHelper spHelper;

    public TtkBingoSorteoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkBingoSorteo> tablero(long codBingo) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo), "T", TtkBingoSorteo.class);
    }

    @Override
    public RespuestaSp registrar(TtkBingoSorteo mb, String accion) {
        return spHelper.ejecutarAbmMap(SP_ABM, TtkParametros.de(
                "codSorteo", "D".equals(accion) ? mb.getCodSorteo() : null,
                "codBingo", mb.getCodBingo(), "nroBolillo", mb.getNroBolillo(),
                "audUsuario", mb.getAudUsuario()), accion);
    }
}
