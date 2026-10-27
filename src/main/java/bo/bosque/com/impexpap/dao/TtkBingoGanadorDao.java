package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoGanador;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkBingoGanadorDao implements ITtkBingoGanador {

    private static final String SP_LIST = "p_list_ttk_BingoGanador";
    private static final String SP_ABM = "p_abm_ttk_BingoGanador";

    private final SpHelper spHelper;

    public TtkBingoGanadorDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkBingoGanador> listar(TtkBingoGanador f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codGanador", f.getCodGanador(), "codJuego", f.getCodJuego(), "codBingo", f.getCodBingo()), "L",
                TtkBingoGanador.class);
    }

    @Override
    public List<TtkBingoGanador> publicos(long codBingo, int nroJuego) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo, "nroJuego", nroJuego), "P",
                TtkBingoGanador.class);
    }

    @Override
    public RespuestaSp registrar(TtkBingoGanador mb, String accion) {
        Map<String, Object> p = "I".equals(accion)
                ? TtkParametros.de("codBingo", mb.getCodBingo(), "codCarton", mb.getCodCarton(),
                        "nroCarton", mb.getNroCarton(), "audUsuario", mb.getAudUsuario())
                : TtkParametros.de("codGanador", mb.getCodGanador(), "audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
