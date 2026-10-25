package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkEvento;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkEventoDao implements ITtkEvento {

    private static final String SP_LIST = "p_list_ttk_Evento";
    private static final String SP_ABM = "p_abm_ttk_Evento";

    private final SpHelper spHelper;

    public TtkEventoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkEvento> listar(TtkEvento f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codEvento", f.getCodEvento(), "estado", f.getEstado(), "anio", f.getAnio()), "L", TtkEvento.class);
    }

    @Override
    public RespuestaSp registrar(TtkEvento mb, String accion) {
        Map<String, Object> p = TtkParametros.esAltaOEdicion(accion)
                ? TtkParametros.de(
                        "codEvento", "U".equals(accion) ? mb.getCodEvento() : null,
                        "nombre", mb.getNombre(), "descripcion", mb.getDescripcion(), "fecha", mb.getFecha(),
                        "lugar", mb.getLugar(), "usaTickets", mb.getUsaTickets(), "usaBingo", mb.getUsaBingo(),
                        "mesesReciente", mb.getMesesReciente(), "audUsuario", mb.getAudUsuario())
                : TtkParametros.de("codEvento", mb.getCodEvento(), "audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
