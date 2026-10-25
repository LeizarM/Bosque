package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoJuego;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkBingoJuegoDao implements ITtkBingoJuego {

    private static final String SP_LIST = "p_list_ttk_BingoJuego";
    private static final String SP_ABM = "p_abm_ttk_BingoJuego";

    private final SpHelper spHelper;

    public TtkBingoJuegoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkBingoJuego> listar(TtkBingoJuego f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codJuego", f.getCodJuego(), "codBingo", f.getCodBingo()), "L", TtkBingoJuego.class);
    }

    @Override
    public RespuestaSp registrar(TtkBingoJuego mb, String accion) {
        Map<String, Object> p = "U".equals(accion)
                ? TtkParametros.de("codJuego", mb.getCodJuego(), "nombre", mb.getNombre(), "figura", mb.getFigura(),
                        "audUsuario", mb.getAudUsuario())
                : TtkParametros.de("codJuego", mb.getCodJuego(), "audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
