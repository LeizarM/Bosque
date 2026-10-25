package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TtkEmpleadoBingo;
import bo.bosque.com.impexpap.dto.TtkRankingCarton;
import bo.bosque.com.impexpap.model.TtkBingoCarton;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkBingoCartonDao implements ITtkBingoCarton {

    private static final String SP_LIST = "p_list_ttk_BingoCarton";
    private static final String SP_ABM = "p_abm_ttk_BingoCarton";

    private final SpHelper spHelper;

    public TtkBingoCartonDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkBingoCarton> listar(TtkBingoCarton f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codCarton", f.getCodCarton(), "codBingo", f.getCodBingo(), "codEmpleado", f.getCodEmpleado(),
                "codParticipante", f.getCodParticipante(), "soloDisponibles", f.getSoloDisponibles(),
                "buscar", f.getBuscar()), "L", TtkBingoCarton.class);
    }

    @Override
    public List<TtkBingoCarton> disponibles(long codBingo) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo), "D", TtkBingoCarton.class);
    }

    @Override
    public List<TtkBingoCarton> mios(Long codBingo, long audUsuario) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo, "audUsuario", audUsuario), "M",
                TtkBingoCarton.class);
    }

    @Override
    public List<TtkEmpleadoBingo> empleados(TtkBingoCarton f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codBingo", f.getCodBingo(), "buscar", f.getBuscar()), "E", TtkEmpleadoBingo.class);
    }

    @Override
    public List<TtkRankingCarton> ranking(long codBingo) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo), "R", TtkRankingCarton.class);
    }

    @Override
    public TtkRankingCarton validar(TtkBingoCarton f) {
        List<TtkRankingCarton> filas = spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codCarton", f.getCodCarton(), "codBingo", f.getCodBingo(), "nroCarton", f.getNroCarton()), "V",
                TtkRankingCarton.class);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp registrar(TtkBingoCarton mb, String accion) {
        Map<String, Object> p = "G".equals(accion)
                ? TtkParametros.de("codBingo", mb.getCodBingo(), "audUsuario", mb.getAudUsuario())
                : TtkParametros.de("codCarton", mb.getCodCarton(),
                        "codEmpleado", "A".equals(accion) ? mb.getCodEmpleado() : null,
                        "codParticipante", "A".equals(accion) ? mb.getCodParticipante() : null,
                        "impreso", "A".equals(accion) || "F".equals(accion) ? mb.getImpreso() : null,
                        "audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
