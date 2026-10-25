package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TtkBingoParticipante;
import bo.bosque.com.impexpap.dto.TtkBingoPublico;
import bo.bosque.com.impexpap.model.TtkBingo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkBingoDao implements ITtkBingo {

    private static final String SP_LIST = "p_list_ttk_Bingo";
    private static final String SP_ABM = "p_abm_ttk_Bingo";

    private final SpHelper spHelper;

    public TtkBingoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkBingo> listar(TtkBingo f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codBingo", f.getCodBingo(), "codEvento", f.getCodEvento()), "L", TtkBingo.class);
    }

    @Override
    public List<TtkBingoParticipante> misBingos(long audUsuario) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de("audUsuario", audUsuario), "A",
                TtkBingoParticipante.class);
    }

    @Override
    public TtkBingoPublico publico(long codBingo) {
        List<TtkBingoPublico> filas = spHelper.ejecutarListado(SP_LIST, TtkParametros.de("codBingo", codBingo), "P",
                TtkBingoPublico.class);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp registrar(TtkBingo mb, String accion) {
        Map<String, Object> p = TtkParametros.esAltaOEdicion(accion)
                ? TtkParametros.de(
                        "codBingo", "U".equals(accion) ? mb.getCodBingo() : null,
                        "codEvento", "I".equals(accion) ? mb.getCodEvento() : null,
                        "nroBolillos", mb.getNroBolillos(), "filas", mb.getFilas(), "columnas", mb.getColumnas(),
                        "letras", mb.getLetras(), "conComodin", mb.getConComodin(), "nroJuegos", mb.getNroJuegos(),
                        "nroCartones", mb.getNroCartones(), "maxCartonesEmpleado", mb.getMaxCartonesEmpleado(),
                        "tamRanking", mb.getTamRanking(), "codTipoTicket", mb.getCodTipoTicket(),
                        "modalidad", mb.getModalidad(), "conAviso", mb.getConAviso(),
                        "audUsuario", mb.getAudUsuario())
                : TtkParametros.de("codBingo", mb.getCodBingo(), "audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
