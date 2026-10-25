package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TtkValeImpresion;
import bo.bosque.com.impexpap.dto.TtkValeLote;
import bo.bosque.com.impexpap.model.TtkParticipanteTicket;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TtkParticipanteTicketDao implements ITtkParticipanteTicket {

    private static final String SP_LIST = "p_list_ttk_ParticipanteTicket";
    private static final String SP_ABM = "p_abm_ttk_ParticipanteTicket";

    private final SpHelper spHelper;

    public TtkParticipanteTicketDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TtkParticipanteTicket> listar(TtkParticipanteTicket f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codParticipanteTicket", f.getCodParticipanteTicket(), "codParticipante", f.getCodParticipante(),
                "codEvento", f.getCodEvento()), "L", TtkParticipanteTicket.class);
    }

    @Override
    public List<TtkValeImpresion> impresion(TtkParticipanteTicket f) {
        return spHelper.ejecutarListado(SP_LIST, TtkParametros.de(
                "codEvento", f.getCodEvento(), "codParticipante", f.getCodParticipante(),
                "codTipoTicket", f.getCodTipoTicket()), "P", TtkValeImpresion.class);
    }

    @Override
    public RespuestaSp registrar(TtkParticipanteTicket mb, String accion) {
        return spHelper.ejecutarAbmMap(SP_ABM, TtkParametros.de(
                "codParticipante", mb.getCodParticipante(), "codTipoTicket", mb.getCodTipoTicket(),
                "cantidad", "U".equals(accion) ? mb.getCantidad() : null,
                "audUsuario", mb.getAudUsuario()), accion);
    }

    /** La lista viaja como texto "1,2,3" (solo formato): el SP la parte y decide a quién aplica. */
    @Override
    public RespuestaSp cambiarLote(TtkValeLote lote, long audUsuario) {
        String lista = lote.getParticipantes() == null ? "" : SpHelper.listaDeIds(lote.getParticipantes());
        return spHelper.ejecutarAbmMap(SP_ABM, TtkParametros.de(
                "codEvento", lote.getCodEvento(), "codTipoTicket", lote.getCodTipoTicket(),
                "cantidad", lote.getCantidad(), "participantes", lista.isEmpty() ? null : lista,
                "categoria", lote.getCategoria(), "audUsuario", audUsuario), "M");
    }
}
