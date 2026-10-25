package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkParticipante;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkParticipanteDao implements ITtkParticipante {

    private static final String SP_LIST = "p_list_ttk_Participante";
    private static final String SP_ABM = "p_abm_ttk_Participante";

    private final SpHelper spHelper;

    public TtkParticipanteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkParticipante> listar(TtkParticipante f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codParticipante", f.getCodParticipante(), "codEvento", f.getCodEvento(),
                "categoria", f.getCategoria(), "buscar", f.getBuscar()), "L", TtkParticipante.class);
    }

    @Override
    public List<TtkParticipante> empleados(TtkParticipante f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codEvento", f.getCodEvento(), "buscar", f.getBuscar()), "E", TtkParticipante.class);
    }

    @Override
    public RespuestaSp registrar(TtkParticipante mb, String accion) {
        Map<String, Object> p;
        if ("G".equals(accion)) {
            p = TtkParametros.de("codEvento", mb.getCodEvento(), "audUsuario", mb.getAudUsuario());
        } else if ("I".equals(accion)) {
            p = TtkParametros.de("codEvento", mb.getCodEvento(), "codEmpleado", mb.getCodEmpleado(),
                    "nombreInvitado", mb.getNombreInvitado(), "documento", mb.getDocumento(),
                    "codEmpleadoAnfitrion", mb.getCodEmpleadoAnfitrion(), "categoria", mb.getCategoria(),
                    "observacion", mb.getObservacion(), "audUsuario", mb.getAudUsuario());
        } else if ("U".equals(accion)) {
            p = TtkParametros.de("codParticipante", mb.getCodParticipante(), "nombreInvitado", mb.getNombreInvitado(),
                    "documento", mb.getDocumento(), "codEmpleadoAnfitrion", mb.getCodEmpleadoAnfitrion(),
                    "categoria", mb.getCategoria(), "observacion", mb.getObservacion(), "audUsuario", mb.getAudUsuario());
        } else {
            p = TtkParametros.de("codParticipante", mb.getCodParticipante(), "audUsuario", mb.getAudUsuario());
        }
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
