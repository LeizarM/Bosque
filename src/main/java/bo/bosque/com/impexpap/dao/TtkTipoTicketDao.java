package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkTipoTicket;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TtkTipoTicketDao implements ITtkTipoTicket {

    private static final String SP_LIST = "p_list_ttk_TipoTicket";
    private static final String SP_ABM = "p_abm_ttk_TipoTicket";

    private final SpHelper spHelper;

    public TtkTipoTicketDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkTipoTicket> listar(TtkTipoTicket f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codTipoTicket", f.getCodTipoTicket(), "codEvento", f.getCodEvento()), "L", TtkTipoTicket.class);
    }

    @Override
    public RespuestaSp registrar(TtkTipoTicket mb, String accion) {
        Map<String, Object> p = TtkParametros.esAltaOEdicion(accion)
                ? TtkParametros.de(
                        "codTipoTicket", "U".equals(accion) ? mb.getCodTipoTicket() : null,
                        "codEvento", "I".equals(accion) ? mb.getCodEvento() : null,
                        "nombre", mb.getNombre(), "leyenda", mb.getLeyenda(), "cantEmpleado", mb.getCantEmpleado(),
                        "cantReciente", mb.getCantReciente(), "cantInvitado", mb.getCantInvitado(),
                        "colorHex", mb.getColorHex(), "orden", mb.getOrden(), "audUsuario", mb.getAudUsuario())
                : TtkParametros.de("codTipoTicket", mb.getCodTipoTicket(), "audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }
}
